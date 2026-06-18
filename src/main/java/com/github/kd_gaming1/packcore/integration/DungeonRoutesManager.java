package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Configures which dungeon secret-routing system is active.
 *
 * <p>The two options are mutually exclusive:
 * <ul>
 *   <li><b>Skyblocker Waypoints</b> (recommended) — enables Skyblocker's built-in
 *       {@code SecretWaypoints} and disables Stella dungeon features.</li>
 *   <li><b>Stella</b> — enables Stella's dungeon secret routes/waypoints and
 *       disables Skyblocker's waypoints.</li>
 * </ul>
 */
public class DungeonRoutesManager {

    private static final String SKYBLOCKER_CONFIG_MANAGER = "de.hysky.skyblocker.config.SkyblockerConfigManager";

    public enum DungeonRoutesMode {
        SKYBLOCKER_WAYPOINTS,
        STELLA
    }

    /**
     * Applies the chosen dungeon-routes mode.
     *
     * @param mode the mode to activate
     * @return true if the primary target mode was successfully enabled
     */
    public static boolean apply(DungeonRoutesMode mode) {
        boolean skyblockerLoaded = FabricLoader.getInstance().isModLoaded("skyblocker");
        boolean stellaLoaded = StellaConfigurator.isLoaded();

        return switch (mode) {
            case SKYBLOCKER_WAYPOINTS -> {
                if (!skyblockerLoaded) {
                    PackCore.LOGGER.warn("DungeonRoutes SKYBLOCKER_WAYPOINTS requires Skyblocker, but it is not loaded");
                    yield false;
                }
                boolean ok = setSkyblockerSecretWaypoints(true);
                if (stellaLoaded) {
                    if (!setStellaDungeonFeatures(false)) {
                        PackCore.LOGGER.warn("Could not disable Stella dungeon features automatically — you may need to disable them manually to avoid conflicts");
                    }
                }
                yield ok;
            }
            case STELLA -> {
                if (!stellaLoaded) {
                    PackCore.LOGGER.warn("DungeonRoutes STELLA requires Stella, but it is not loaded");
                    yield false;
                }
                boolean ok = setStellaDungeonFeatures(true);
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
            PackCore.LOGGER.info("Skyblocker: secretWaypoints.enableSecretWaypoints -> {}", enabled);
            return true;
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Skyblocker not present");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("Skyblocker: failed to update SecretWaypoints via reflection", e);
            return false;
        }
    }

    // ── Stella dungeon features ───────────────────────────────────────────────

    private static boolean setStellaDungeonFeatures(boolean enabled) {
        boolean ok = true;
        if (!StellaConfigurator.set("secretRoutes", enabled)) ok = false;
        if (!StellaConfigurator.set("secretWaypoints", enabled)) ok = false;
        return ok;
    }
}
