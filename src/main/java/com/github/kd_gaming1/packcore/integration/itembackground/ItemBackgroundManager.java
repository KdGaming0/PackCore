package com.github.kd_gaming1.packcore.integration.itembackground;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.util.wizard.WizardDataStore;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility for applying the selected Item Background style
 * by changing Skyblocker config at runtime via reflection.
 */
public class ItemBackgroundManager {

    public static boolean applyItemBackgroundFromWizard() {
        String itemBackground = WizardDataStore.getInstance().getItemBackground();

        if (itemBackground == null || itemBackground.isEmpty() || "None".equals(itemBackground)) {
            return false;
        }

        boolean skyblockerPresent = isModLoaded("skyblocker");

        if (!skyblockerPresent) {
            PackCore.LOGGER.warn("Skyblocker not present, cannot apply item background");
            return false;
        }

        return switch (itemBackground) {
            case "No Background" -> setSkyblockerItemBackground(false, false);
            case "Circular" -> setSkyblockerItemBackground(true, true);
            case "Square" -> setSkyblockerItemBackground(true, false);
            default -> {
                PackCore.LOGGER.warn("Unknown item background style: {}", itemBackground);
                yield false;
            }
        };
    }

    private static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    /**
     * Set Skyblocker item background configuration
     *
     * @param enabled Whether item backgrounds are enabled at all
     * @param circular Whether to use circular backgrounds (false = square)
     * @return true if successful
     */
    private static boolean setSkyblockerItemBackground(boolean enabled, boolean circular) {
        try {
            Class<?> configManager = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
            Method updateMethod = configManager.getDeclaredMethod("update", java.util.function.Consumer.class);

            java.util.function.Consumer<Object> consumer = config -> updateItemBackgroundConfig(config, enabled, circular);

            updateMethod.invoke(null, consumer);
            PackCore.LOGGER.info("Set Skyblocker item background: enabled={}, circular={}", enabled, circular);
            return true;

        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Skyblocker not present");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("Could not update Skyblocker item background config", e);
            return false;
        }
    }

    private static void updateItemBackgroundConfig(Object config, boolean enabled, boolean circular) {
        try {
            // Navigate to uiAndVisuals.itemInfoDisplay
            Object uiAndVisuals = config.getClass().getField("uiAndVisuals").get(config);
            Object itemInfoDisplay = uiAndVisuals.getClass().getField("itemInfoDisplay").get(uiAndVisuals);

            // Set the item background enabled state
            Field itemBackgroundField = itemInfoDisplay.getClass().getField("itemBackground");
            itemBackgroundField.setBoolean(itemInfoDisplay, enabled);

            // If enabled, set whether it's circular or not
            if (enabled) {
                try {
                    // Try to set circular background option if it exists
                    Field circularBackgroundField = itemInfoDisplay.getClass().getField("circularItemBackgrounds");
                    circularBackgroundField.setBoolean(itemInfoDisplay, circular);
                    PackCore.LOGGER.debug("Set circular backgrounds: {}", circular);
                } catch (NoSuchFieldException e) {
                    // Field might not exist in this version of Skyblocker
                    PackCore.LOGGER.debug("Circular background field not found, using default style");
                }
            }

            PackCore.LOGGER.debug("Updated Skyblocker item background config: enabled={}, circular={}", enabled, circular);

        } catch (NoSuchFieldException e) {
            PackCore.LOGGER.warn("Failed to find item background config fields in Skyblocker. Config structure may have changed.", e);
        } catch (Exception e) {
            PackCore.LOGGER.warn("Failed to update Skyblocker item background config", e);
        }
    }

    /**
     * Check if Skyblocker is available
     */
    public static boolean isSkyblockerAvailable() {
        return isModLoaded("skyblocker");
    }

    /**
     * Apply a specific item background style by name
     *
     * @param style "No Background", "Circular", or "Square"
     * @return true if successfully applied
     */
    public static boolean applyItemBackground(String style) {
        if (style == null || style.isEmpty()) {
            return false;
        }

        if (!isModLoaded("skyblocker")) {
            PackCore.LOGGER.warn("Skyblocker not present");
            return false;
        }

        return switch (style) {
            case "No Background" -> setSkyblockerItemBackground(false, false);
            case "Circular" -> setSkyblockerItemBackground(true, true);
            case "Square" -> setSkyblockerItemBackground(true, false);
            default -> {
                PackCore.LOGGER.warn("Unknown item background style: {}", style);
                yield false;
            }
        };
    }
}