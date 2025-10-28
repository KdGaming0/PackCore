package com.github.kd_gaming1.packcore.integration.itembackground;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.util.wizard.WizardDataStore;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Manager for applying item background styles by modifying Skyblocker's configuration at runtime.
 * Uses reflection to interact with Skyblocker's config system.
 */
public class ItemBackgroundManager {

    private static final String SKYBLOCKER_MOD_ID = "skyblocker";
    private static final String CONFIG_MANAGER_CLASS = "de.hysky.skyblocker.config.SkyblockerConfigManager";
    private static final String ITEM_BACKGROUND_ENUM_CLASS = "de.hysky.skyblocker.config.configs.GeneralConfig$ItemBackgroundStyle";
    private static final float DEFAULT_OPACITY = 0.5f;

    /**
     * Applies the item background style selected in the wizard.
     *
     * @return true if successfully applied or no action needed, false on failure
     */
    public static boolean applyItemBackgroundFromWizard() {
        String itemBackground = WizardDataStore.getInstance().getItemBackground();

        if (shouldSkipApplication(itemBackground)) {
            return true;
        }

        if (!isSkyblockerLoaded()) {
            PackCore.LOGGER.warn("Skyblocker not loaded; cannot apply item background");
            return false;
        }

        String style = mapToSkyblockerStyle(itemBackground);
        return applyItemBackgroundStyle(style);
    }

    /**
     * Applies a specific item background style.
     *
     * @param style the style enum name (CIRCULAR, SQUARE, or NONE)
     * @return true if successfully applied, false otherwise
     */
    private static boolean applyItemBackgroundStyle(String style) {
        try {
            Class<?> configManager = Class.forName(CONFIG_MANAGER_CLASS);
            Method updateMethod = configManager.getDeclaredMethod("update", Consumer.class);

            Consumer<Object> configUpdater = config -> updateItemBackgroundConfig(config, style);
            updateMethod.invoke(null, configUpdater);

            PackCore.LOGGER.info("Successfully applied Skyblocker item background: {}", style);
            return true;

        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.warn("Skyblocker config manager not found - mod may not be loaded correctly");
            return false;
        } catch (NoSuchMethodException e) {
            PackCore.LOGGER.warn("Skyblocker config update method not found - API may have changed");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.error("Unexpected error while applying item background config", e);
            return false;
        }
    }

    /**
     * Updates the Skyblocker config object with the new item background settings.
     *
     * @param config the Skyblocker config instance
     * @param style the background style to apply
     */
    private static void updateItemBackgroundConfig(Object config, String style) {
        try {
            // Navigate to itemInfoDisplay config
            Field generalField = config.getClass().getField("general");
            Object general = generalField.get(config);

            Field itemInfoDisplayField = general.getClass().getField("itemInfoDisplay");
            Object itemInfoDisplay = itemInfoDisplayField.get(general);

            // Set the background style enum
            Class<?> styleEnum = Class.forName(ITEM_BACKGROUND_ENUM_CLASS);
            Object enumValue = Enum.valueOf((Class<Enum>) styleEnum, style);

            Field styleField = itemInfoDisplay.getClass().getField("itemBackgroundStyle");
            styleField.set(itemInfoDisplay, enumValue);

            // Enable item rarity backgrounds (required for backgrounds to show)
            Field rarityBackgroundsField = itemInfoDisplay.getClass().getField("itemRarityBackgrounds");
            rarityBackgroundsField.setBoolean(itemInfoDisplay, true);

            // Set default opacity
            Field opacityField = itemInfoDisplay.getClass().getField("itemBackgroundOpacity");
            opacityField.setFloat(itemInfoDisplay, DEFAULT_OPACITY);

            PackCore.LOGGER.debug("Item background config updated: style={}, opacity={}", style, DEFAULT_OPACITY);

        } catch (NoSuchFieldException e) {
            PackCore.LOGGER.error("Required config field not found - Skyblocker structure may have changed: {}", e.getMessage());
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.error("Item background style enum not found - Skyblocker may have changed");
        } catch (IllegalAccessException e) {
            PackCore.LOGGER.error("Cannot access config fields - security restrictions may apply");
        } catch (Exception e) {
            PackCore.LOGGER.error("Unexpected error updating item background config", e);
        }
    }

    /**
     * Maps wizard style names to Skyblocker enum values.
     *
     * @param wizardStyle the style name from the wizard
     * @return the corresponding Skyblocker enum name
     */
    private static String mapToSkyblockerStyle(String wizardStyle) {
        return switch (wizardStyle.toLowerCase()) {
            case "circular" -> "CIRCULAR";
            case "square" -> "SQUARE";
            default -> {
                PackCore.LOGGER.warn("Unknown item background style '{}', defaulting to SQUARE", wizardStyle);
                yield "SQUARE";
            }
        };
    }

    /**
     * Checks if the application should be skipped.
     */
    private static boolean shouldSkipApplication(String itemBackground) {
        return itemBackground == null || itemBackground.isEmpty() || "None".equalsIgnoreCase(itemBackground);
    }

    /**
     * Checks if Skyblocker is loaded.
     */
    private static boolean isSkyblockerLoaded() {
        return FabricLoader.getInstance().isModLoaded(SKYBLOCKER_MOD_ID);
    }
}