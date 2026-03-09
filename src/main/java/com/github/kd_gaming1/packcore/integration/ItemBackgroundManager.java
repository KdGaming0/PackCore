package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class ItemBackgroundManager {

    private static final String CONFIG_MANAGER_CLASS = "de.hysky.skyblocker.config.SkyblockerConfigManager";
    private static final String ITEM_BACKGROUND_ENUM_CLASS = "de.hysky.skyblocker.config.configs.GeneralConfig$ItemBackgroundStyle";
    private static final float DEFAULT_OPACITY = 0.5f;

    public enum ItemBackground {
        NONE, CIRCLE, SQUARE
    }

    public static boolean apply(ItemBackground background) {
        if (!FabricLoader.getInstance().isModLoaded("skyblocker")) {
            PackCore.LOGGER.warn("ItemBackground: Skyblocker not loaded, cannot apply");
            return false;
        }

        // NONE means disable rarity backgrounds entirely, others map to Skyblocker enum values
        String skyblockerStyle = switch (background) {
            case NONE -> null;
            case CIRCLE -> "CIRCULAR";
            case SQUARE -> "SQUARE";
        };

        try {
            Class<?> configManager = Class.forName(CONFIG_MANAGER_CLASS);
            Method update = configManager.getDeclaredMethod("update", Consumer.class);
            update.invoke(null, (Consumer<Object>) config -> updateConfig(config, skyblockerStyle));
            PackCore.LOGGER.info("ItemBackground: applied {}", background);
            return true;
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.warn("ItemBackground: Skyblocker config manager not found");
            return false;
        } catch (NoSuchMethodException e) {
            PackCore.LOGGER.warn("ItemBackground: update method not found, Skyblocker API may have changed");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.error("ItemBackground: failed to apply", e);
            return false;
        }
    }

    private static void updateConfig(Object config, String skyblockerStyle) {
        try {
            Object general = config.getClass().getField("general").get(config);
            Object itemInfoDisplay = general.getClass().getField("itemInfoDisplay").get(general);

            itemInfoDisplay.getClass().getField("itemRarityBackgrounds").setBoolean(itemInfoDisplay, skyblockerStyle != null);

            if (skyblockerStyle != null) {
                Class<?> styleEnum = Class.forName(ITEM_BACKGROUND_ENUM_CLASS);
                Object enumValue = Enum.valueOf((Class<Enum>) styleEnum, skyblockerStyle);
                itemInfoDisplay.getClass().getField("itemBackgroundStyle").set(itemInfoDisplay, enumValue);
                itemInfoDisplay.getClass().getField("itemBackgroundOpacity").setFloat(itemInfoDisplay, DEFAULT_OPACITY);
            }
        } catch (NoSuchFieldException e) {
            PackCore.LOGGER.error("ItemBackground: config field not found, Skyblocker structure may have changed: {}", e.getMessage());
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.error("ItemBackground: style enum not found");
        } catch (IllegalAccessException e) {
            PackCore.LOGGER.error("ItemBackground: cannot access config fields");
        } catch (Exception e) {
            PackCore.LOGGER.error("ItemBackground: unexpected error updating config", e);
        }
    }
}