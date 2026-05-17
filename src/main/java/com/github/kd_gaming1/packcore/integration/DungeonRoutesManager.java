package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

/**
 * Configures which dungeon secret-routing system is active.
 *
 * <p>The two options are mutually exclusive:
 * <ul>
 *   <li><b>Skyblocker Waypoints</b> — enables Skyblocker's built-in
 *       {@code SecretWaypoints} and disables Secret Routes Mod.</li>
 *   <li><b>Secret Routes Mod</b> — enables the Secret Routes Mod and
 *       disables Skyblocker's waypoints.</li>
 * </ul>
 */
public class DungeonRoutesManager {

    private static final String SKYBLOCKER_CONFIG_MANAGER = "de.hysky.skyblocker.config.SkyblockerConfigManager";
    private static final String SRM_CONFIG_CLASS = "xyz.yourboykyle.secretroutes.config.SRMConfig";

    public enum DungeonRoutesMode {
        SKYBLOCKER_WAYPOINTS,
        SECRET_ROUTES_MOD
    }

    /**
     * Applies the chosen dungeon-routes mode.
     *
     * @param mode the mode to activate
     * @return true if the primary target mode was successfully enabled
     */
    public static boolean apply(DungeonRoutesMode mode) {
        boolean skyblockerLoaded = FabricLoader.getInstance().isModLoaded("skyblocker");
        boolean srmLoaded = FabricLoader.getInstance().isModLoaded("secretroutesmod");

        return switch (mode) {
            case SKYBLOCKER_WAYPOINTS -> {
                if (!skyblockerLoaded) {
                    PackCore.LOGGER.warn("DungeonRoutes SKYBLOCKER_WAYPOINTS requires Skyblocker, but it is not loaded");
                    yield false;
                }
                boolean ok = setSkyblockerSecretWaypoints(true);
                if (srmLoaded) {
                    if (!setSrmEnabled(false)) {
                        PackCore.LOGGER.warn("Could not disable Secret Routes Mod automatically — you may need to disable it manually to avoid conflicts");
                    }
                }
                yield ok;
            }
            case SECRET_ROUTES_MOD -> {
                if (!srmLoaded) {
                    PackCore.LOGGER.warn("DungeonRoutes SECRET_ROUTES_MOD requires Secret Routes Mod, but it is not loaded");
                    yield false;
                }
                boolean ok = setSrmEnabled(true);
                if (skyblockerLoaded) {
                    if (!setSkyblockerSecretWaypoints(false)) {
                        PackCore.LOGGER.warn("Could not disable Skyblocker secret waypoints automatically — you may need to disable them manually to avoid conflicts");
                    }
                }
                yield ok;
            }
        };
    }

    // ── Skyblocker SecretWaypoints ────────────────────────────────────────────

    private static boolean setSkyblockerSecretWaypoints(boolean enabled) {
        try {
            Class<?> configManager = Class.forName(SKYBLOCKER_CONFIG_MANAGER);
            Method update = configManager.getDeclaredMethod("update", Consumer.class);
            update.setAccessible(true);
            update.invoke(null, (Consumer<Object>) config -> {
                try {
                    Field dungeonsField = config.getClass().getDeclaredField("dungeons");
                    dungeonsField.setAccessible(true);
                    Object dungeons = dungeonsField.get(config);

                    Field secretWaypointsField = dungeons.getClass().getDeclaredField("secretWaypoints");
                    secretWaypointsField.setAccessible(true);
                    Object secretWaypoints = secretWaypointsField.get(dungeons);

                    Field enableField = secretWaypoints.getClass().getDeclaredField("enableSecretWaypoints");
                    enableField.setAccessible(true);
                    enableField.setBoolean(secretWaypoints, enabled);
                } catch (Exception e) {
                    PackCore.LOGGER.warn("Skyblocker: failed to update SecretWaypoints config", e);
                }
            });
            PackCore.LOGGER.info("Skyblocker: secretWaypoints.enableSecretWaypoints → {}", enabled);
            return true;
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Skyblocker not present");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("Skyblocker: failed to update SecretWaypoints via reflection", e);
            return false;
        }
    }

    // ── Secret Routes Mod ─────────────────────────────────────────────────────

    private static boolean setSrmEnabled(boolean enabled) {
        try {
            Class<?> srmConfigClass = Class.forName(SRM_CONFIG_CLASS);

            // ── Try modern YACL path first ──
            // YACL: ConfigClassHandler HANDLER; instance field modEnabled; save via HANDLER.save()
            try {
                Field handlerField = srmConfigClass.getDeclaredField("HANDLER");
                handlerField.setAccessible(true);
                Object handler = handlerField.get(null); // HANDLER is static

                // Get config instance: HANDLER.instance()
                Method instanceMethod = findInHierarchy(handler.getClass(), "instance");
                if (instanceMethod == null) {
                    throw new NoSuchMethodException("HANDLER.instance()");
                }
                instanceMethod.setAccessible(true);
                Object configInstance = instanceMethod.invoke(handler);

                // Set instance field modEnabled
                Field modEnabledField = srmConfigClass.getDeclaredField("modEnabled");
                modEnabledField.setAccessible(true);
                modEnabledField.setBoolean(configInstance, enabled);

                // Save: HANDLER.save()
                Method saveMethod = findInHierarchy(handler.getClass(), "save");
                if (saveMethod != null) {
                    saveMethod.setAccessible(true);
                    saveMethod.invoke(handler);
                }

                PackCore.LOGGER.info("SecretRoutes (YACL): modEnabled → {}", enabled);
                return true;
            } catch (NoSuchFieldException yaclMissing) {
                // YACL fields not found — fall through to old OneConfig path
                PackCore.LOGGER.debug("SecretRoutes: YACL structure not detected, trying OneConfig fallback");
            }

            // ── Fallback: old OneConfig path ──
            // OneConfig: static final SRMConfig INSTANCE; static boolean modEnabled
            Field instanceField = srmConfigClass.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            Object instance = instanceField.get(null);

            Field modEnabledField = srmConfigClass.getDeclaredField("modEnabled");
            modEnabledField.setAccessible(true);

            boolean isStatic = Modifier.isStatic(modEnabledField.getModifiers());
            if (isStatic) {
                modEnabledField.setBoolean(null, enabled);
            } else {
                modEnabledField.setBoolean(instance, enabled);
            }

            trySaveSrmConfig(instance);

            PackCore.LOGGER.info("SecretRoutes (OneConfig): modEnabled → {}", enabled);
            return true;

        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Secret Routes Mod not present");
            return false;
        } catch (NoSuchFieldException e) {
            PackCore.LOGGER.warn("SecretRoutes: expected field not found, mod structure may have changed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("SecretRoutes: failed to set modEnabled via reflection", e);
            return false;
        }
    }

    private static void trySaveSrmConfig(Object configInstance) {
        if (configInstance == null) return;
        try {
            Method saveMethod = findInHierarchy(configInstance.getClass(), "save");
            if (saveMethod != null) {
                saveMethod.setAccessible(true);
                saveMethod.invoke(configInstance);
            }
        } catch (Exception e) {
            PackCore.LOGGER.debug("SecretRoutes: could not auto-save config — change will apply in-memory only: {}", e.getMessage());
        }
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private static Method findInHierarchy(Class<?> clazz, String name, Class<?>... params) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
            for (Class<?> iface : c.getInterfaces()) {
                try {
                    Method m = iface.getDeclaredMethod(name, params);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored) {}
            }
        }
        return null;
    }
}