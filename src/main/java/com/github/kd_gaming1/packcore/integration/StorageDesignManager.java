package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;

public class StorageDesignManager {

    private static final String MOD_ID = "enhanced_storage";
    private static final String CONFIG_CLASS = "com.github.kdgaming0.enhancedstorage.config.EnhancedStorageConfig";
    private static final String FIELD_NAME = "enableStorageOverlay";

    public enum StorageDesign {
        OVERLAY, VANILLA
    }

    public static boolean apply(StorageDesign design) {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            PackCore.LOGGER.warn("StorageDesign: Enhanced Storage not loaded, cannot apply");
            return false;
        }

        try {
            Class<?> configClass = Class.forName(CONFIG_CLASS);
            Field field = configClass.getDeclaredField(FIELD_NAME);
            field.setAccessible(true);
            field.setBoolean(null, design == StorageDesign.OVERLAY);

            MidnightConfig.write(MOD_ID);
            PackCore.LOGGER.info("StorageDesign: applied {} immediately", design);
            return true;
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.warn("StorageDesign: Enhanced Storage config class not found");
            return false;
        } catch (NoSuchFieldException e) {
            PackCore.LOGGER.warn("StorageDesign: Enhanced Storage field '{}' not found", FIELD_NAME);
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.error("StorageDesign: failed to apply via reflection", e);
            return false;
        }
    }
}
