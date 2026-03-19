package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public class TabDesignManager {

    public enum TabDesign {
        COMPACT, FANCY
    }

    public static boolean apply(TabDesign design) {
        boolean skyblockerLoaded = FabricLoader.getInstance().isModLoaded("skyblocker");
        boolean skyhanniLoaded   = FabricLoader.getInstance().isModLoaded("skyhanni");

        return switch (design) {
            case COMPACT -> {
                if (!skyhanniLoaded) {
                    PackCore.LOGGER.warn("TabDesign COMPACT requires SkyHanni, but it is not loaded");
                    yield false;
                }
                boolean ok = setSkyHanniEnabled(true);
                if (skyblockerLoaded) ok &= setSkyblockerEnabled(false);
                yield ok;
            }
            case FANCY -> {
                if (!skyblockerLoaded) {
                    PackCore.LOGGER.warn("TabDesign FANCY requires Skyblocker, but it is not loaded");
                    yield false;
                }
                boolean ok = setSkyblockerEnabled(true);
                if (skyhanniLoaded) ok &= setSkyHanniEnabled(false);
                yield ok;
            }
        };
    }

    // ── Skyblocker (reflection) ───────────────────────────────────────────────

    private static boolean setSkyblockerEnabled(boolean enabled) {
        try {
            Class<?> configManager = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
            java.lang.reflect.Method update = configManager.getDeclaredMethod("update", java.util.function.Consumer.class);
            update.setAccessible(true);
            update.invoke(null, (java.util.function.Consumer<Object>) config -> {
                try {
                    java.lang.reflect.Field uiField = config.getClass().getDeclaredField("uiAndVisuals");
                    uiField.setAccessible(true);
                    Object uiAndVisuals = uiField.get(config);

                    java.lang.reflect.Field tabField = uiAndVisuals.getClass().getDeclaredField("tabHud");
                    tabField.setAccessible(true);
                    Object tabHud = tabField.get(uiAndVisuals);

                    java.lang.reflect.Field enabledField = tabHud.getClass().getDeclaredField("tabHudEnabled");
                    enabledField.setAccessible(true);
                    enabledField.setBoolean(tabHud, true);

                    java.lang.reflect.Field vanillaField = tabHud.getClass().getDeclaredField("showVanillaTabByDefault");
                    vanillaField.setAccessible(true);
                    vanillaField.setBoolean(tabHud, !enabled);
                } catch (Exception e) {
                    PackCore.LOGGER.warn("Skyblocker: failed to update TabHud config", e);
                }
            });
            PackCore.LOGGER.info("Skyblocker: tabHudEnabled=true showVanillaTabByDefault={}", !enabled);
            return true;
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Skyblocker not present");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("Skyblocker: config update failed", e);
            return false;
        }
    }

    // ── SkyHanni (reflection) ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static boolean setSkyHanniEnabled(boolean enabled) {
        try {
            Class<?> skyHanniModClass = Class.forName("at.hannibal2.skyhanni.SkyHanniMod");
            Object   instance         = getField(skyHanniModClass, null, "INSTANCE");

            Object feature        = getField(skyHanniModClass,          instance,      "feature");
            Object gui            = getField(feature.getClass(),         feature,       "gui");
            Object compactTabList = getField(gui.getClass(),             gui,           "compactTabList");
            Object enabledProp    = getField(compactTabList.getClass(),  compactTabList, "enabled");

            // Walk superclasses AND interfaces — set() is on the Property interface,
            // not on the shaded PropertyImpl concrete class.
            findMethod(enabledProp.getClass(), "set", Object.class).invoke(enabledProp, enabled);

            Object   configManager = getField(skyHanniModClass, instance, "configManager");
            Class<?> fileTypeEnum  = Class.forName("at.hannibal2.skyhanni.config.ConfigFileType");
            Object   features      = Enum.valueOf((Class<Enum>) fileTypeEnum, "FEATURES");
            findMethod(configManager.getClass(), "saveConfig", fileTypeEnum, String.class)
                    .invoke(configManager, features, "packcore");

            PackCore.LOGGER.info("SkyHanni: compactTabList.enabled → {}", enabled);
            return true;
        } catch (Exception e) {
            PackCore.LOGGER.warn("SkyHanni: failed to set compactTabList via reflection", e);
            return false;
        }
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private static Object getField(Class<?> clazz, Object instance, String name) throws Exception {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(instance);
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException("Cannot find field '" + name + "' on " + clazz.getName());
    }

    private static void setBoolean(Class<?> clazz, Object instance, String name, boolean value) throws Exception {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.setBoolean(instance, value);
                return;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException("Cannot find boolean field '" + name + "' on " + clazz.getName());
    }

    /** Walks superclasses then interfaces, calls setAccessible to cross module boundaries. */
    private static Method findMethod(Class<?> clazz, String name, Class<?>... params)
            throws NoSuchMethodException {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            try {
                Method m = iface.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException("Cannot find method '" + name + "' on " + clazz.getName());
    }
}