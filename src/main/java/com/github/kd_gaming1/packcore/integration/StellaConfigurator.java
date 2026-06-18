package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Reflection-based bridge to Stella's custom Kotlin DSL config system.
 *
 * <p>Stella is an optional runtime dependency; this class degrades gracefully when it is absent.
 */
public final class StellaConfigurator {

    private static final String MOD_ID = "stella";
    private static final String CONFIG_KT = "co.stellarskys.stella.utils.ConfigKt";
    private static final String CONFIG_CLASS = "co.stellarskys.stella.api.config.core.Config";

    private StellaConfigurator() {}

    /** Whether Stella is loaded. */
    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /**
     * Reads a value from Stella's config cache.
     *
     * @param key the config key
     * @return the current value, or {@code null} if Stella is absent or the key is missing
     */
    public static Object get(String key) {
        if (!isLoaded()) return null;
        try {
            Object config = getConfigInstance();
            Method getMethod = config.getClass().getMethod("get", String.class);
            return getMethod.invoke(config, key);
        } catch (Exception e) {
            PackCore.LOGGER.warn("Stella: failed to read config key '{}'", key, e);
            return null;
        }
    }

    /**
     * Writes a value to Stella's config and persists it.
     *
     * @param key   the config key
     * @param value the new value
     * @return true if the write succeeded
     */
    public static boolean set(String key, Object value) {
        if (!isLoaded()) {
            PackCore.LOGGER.warn("Stella: not loaded, cannot set '{}'", key);
            return false;
        }

        try {
            Object config = getConfigInstance();
            Class<?> configClass = Class.forName(CONFIG_CLASS);

            Field elementMapField = configClass.getDeclaredField("elementMap");
            elementMapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> elementMap = (Map<String, Object>) elementMapField.get(config);
            Object element = elementMap.get(key);

            // Stella persists from the element's backing value (toJson reads getValue()), so a
            // valueCache-only write would be silently discarded by save(). Bail rather than
            // report a success that never lands on disk.
            if (element == null) {
                PackCore.LOGGER.warn("Stella: no config element for '{}', skipping", key);
                return false;
            }

            // Mirror Stella's own load path: setValue() updates the element, then valueCache is
            // synced so live get() reads agree this session (Stella assumes the two stay equal).
            element.getClass().getMethod("setValue", Object.class).invoke(element, value);
            updateValueCache(config, configClass, key, value);

            configClass.getMethod("save").invoke(config);
            PackCore.LOGGER.info("Stella: set {} = {}", key, value);
            return true;
        } catch (Exception e) {
            PackCore.LOGGER.error("Stella: failed to set '{}'", key, e);
            return false;
        }
    }

    private static Object getConfigInstance() throws Exception {
        Class<?> configKt = Class.forName(CONFIG_KT);
        Field configField = configKt.getDeclaredField("config");
        configField.setAccessible(true);
        return configField.get(null);
    }

    private static void updateValueCache(Object config, Class<?> configClass, String key, Object value)
            throws Exception {
        Field valueCacheField = configClass.getDeclaredField("valueCache");
        valueCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> valueCache = (Map<String, Object>) valueCacheField.get(config);
        valueCache.put(key, value);
    }
}
