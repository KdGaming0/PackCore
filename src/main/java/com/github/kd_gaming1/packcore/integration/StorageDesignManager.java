package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StorageDesignManager {

    private static final String CONFIG_NAME = "storage-overlay";
    private static final String OPTION_NAME = "always-replace";

    public enum StorageDesign {
        OVERLAY, VANILLA
    }

    public static boolean apply(StorageDesign design) {
        if (!FabricLoader.getInstance().isModLoaded("firmament")) {
            PackCore.LOGGER.warn("StorageDesign: Firmament not loaded, cannot apply");
            return false;
        }
        try {
            applyViaReflection(design == StorageDesign.OVERLAY);
            PackCore.LOGGER.info("StorageDesign: applied {} immediately", design);
            return true;
        } catch (Exception e) {
            PackCore.LOGGER.error("StorageDesign: failed to apply via reflection", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyViaReflection(boolean enable) throws Exception {
        Class<?> managedConfigClass = Class.forName("moe.nea.firmament.util.data.ManagedConfig");
        Field companionField = managedConfigClass.getDeclaredField("Companion");
        companionField.setAccessible(true);
        Object companion = companionField.get(null);

        Method getAllConfigs = companion.getClass().getDeclaredMethod("getAllManagedConfigs");
        getAllConfigs.setAccessible(true);
        Object instanceList = getAllConfigs.invoke(companion);
        List<Object> configs =
                (List<Object>) instanceList.getClass().getMethod("getAll").invoke(instanceList);

        for (Object config : configs) {
            String name = (String) config.getClass().getMethod("getName").invoke(config);
            if (!CONFIG_NAME.equals(name)) continue;

            Map<String, Object> options =
                    (Map<String, Object>) config.getClass().getMethod("getAllOptions").invoke(config);

            if (!options.containsKey(OPTION_NAME)) {
                PackCore.LOGGER.error(
                        "StorageDesign: option '{}' not found. Available: {}", OPTION_NAME, options.keySet());
                throw new IllegalStateException(
                        "Option '" + OPTION_NAME + "' not found in config '" + CONFIG_NAME + "'");
            }
            Object option = options.get(OPTION_NAME);

            Method setter = findMethod(option.getClass(), "setValue", Object.class);
            setter.invoke(option, enable);

            Method save = findMethod(config.getClass(), "markDirty", java.util.concurrent.CompletableFuture.class);
            save.invoke(config, (Object) null);
            return;
        }

        List<String> allNames = configs.stream()
                .map(c -> {
                    try { return (String) c.getClass().getMethod("getName").invoke(c); }
                    catch (Exception ex) { return "<error>"; }
                })
                .collect(Collectors.toList());
        PackCore.LOGGER.error(
                "StorageDesign: config '{}' not found. Known: {}", CONFIG_NAME, allNames);
        throw new IllegalStateException("Firmament config '" + CONFIG_NAME + "' not found");
    }

    private static Method findMethod(Class<?> start, String name, Class<?>... params)
            throws NoSuchMethodException {
        for (Class<?> c = start; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // keep walking
            }
        }
        try {
            assert start != null;
            Method m = start.getMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException ignored) {
            // fall through
        }
        throw new NoSuchMethodException("Cannot find " + name + " on " + start.getName());
    }
}