package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import eu.midnightdust.lib.config.MidnightConfig;

import java.lang.reflect.Field;

public final class SkyblockEnhancementsConfigManager {

    private static final String CONFIG_CLASS = "com.github.kd_gaming1.skyblockenhancements.config.SkyblockEnhancementsConfig";
    private static final String ENABLE_FIELD = "enablePriceTooltips";
    private static final String CONFIG_ID = "skyblock_enhancements";

    private SkyblockEnhancementsConfigManager() {
    }

    public static boolean enablePriceTooltips() {
        try {
            Class<?> configClass = Class.forName(CONFIG_CLASS);
            Class<? extends MidnightConfig> midnightClass = configClass.asSubclass(MidnightConfig.class);
            MidnightConfig.init(CONFIG_ID, midnightClass);

            Field field = configClass.getDeclaredField(ENABLE_FIELD);
            field.setAccessible(true);
            Object raw = field.get(null);
            boolean current = raw instanceof Boolean bool ? bool : field.getBoolean(null);
            if (current) {
                PackCore.LOGGER.info("SkyblockEnhancements: {} already enabled", ENABLE_FIELD);
                return true;
            }

            field.setBoolean(null, true);
            MidnightConfig.write(CONFIG_ID);
            PackCore.LOGGER.info("SkyblockEnhancements: enabled {}", ENABLE_FIELD);
            return true;
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("SkyblockEnhancements: config class not found, skipping");
            return false;
        } catch (NoSuchFieldException e) {
            PackCore.LOGGER.warn("SkyblockEnhancements: field '{}' not found", ENABLE_FIELD);
            return false;
        } catch (ClassCastException e) {
            PackCore.LOGGER.warn("SkyblockEnhancements: config class is not a MidnightConfig", e);
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("SkyblockEnhancements: failed to update config", e);
            return false;
        }
    }
}
