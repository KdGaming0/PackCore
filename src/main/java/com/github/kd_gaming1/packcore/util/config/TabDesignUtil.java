package com.github.kd_gaming1.packcore.util.config;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.help.WizardDataManager;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Utility for applying the selected Tab Design (SkyHanni or Skyblocker)
 * by changing the config of the respective mod at runtime.
 */
public class TabDesignUtil {

    /**
     * Applies the user's selected tab design by toggling Skyblocker/SkyHanni configs accordingly.
     * Call this after the wizard is finished and before you reload resources.
     *
     * @return true if the config was updated successfully, false otherwise.
     */
    public static boolean applyTabDesignFromWizard() {
        String tabDesign = WizardDataManager.getInstance().getTabDesign();

        // If nothing selected, do nothing
        if (tabDesign == null || tabDesign.isEmpty() || "None".equals(tabDesign)) return false;

        boolean skyblockerPresent = isModLoaded("skyblocker");
        boolean skyhanniPresent = isModLoaded("skyhanni");

        boolean changed = false;

        if ("Skyblocker".equalsIgnoreCase(tabDesign) && skyblockerPresent) {
            changed = enableSkyblockerTabList(true);
            if (skyhanniPresent) disableSkyHanniTabList();
        } else if ("SkyHanni".equalsIgnoreCase(tabDesign) && skyhanniPresent) {
            changed = enableSkyHanniTabList(true);
            if (skyblockerPresent) disableSkyblockerTabList();
        }

        return changed;
    }

    private static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    // ===== Skyblocker =====

    /**
     * Enables/disables the Skyblocker TabList feature by updating its config at runtime.
     */
    private static boolean enableSkyblockerTabList(boolean enable) {
        try {
            // SkyblockerConfigManager.update(config -> config.uiAndVisuals.tabList.enabled = enable)
            Class<?> configManager = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
            Method updateMethod = configManager.getDeclaredMethod("update", java.util.function.Consumer.class);

            // Lambda: config -> config.uiAndVisuals.tabList.enabled = enable
            java.util.function.Consumer<Object> consumer = config -> {
                try {
                    Object uiAndVisuals = config.getClass().getField("uiAndVisuals").get(config);
                    Object tabList = uiAndVisuals.getClass().getField("tabList").get(uiAndVisuals);
                    tabList.getClass().getField("enabled").setBoolean(tabList, enable);
                } catch (Exception e) {
                    PackCore.LOGGER.warn("Failed to toggle Skyblocker TabList via reflection", e);
                }
            };

            updateMethod.invoke(null, consumer);
            PackCore.LOGGER.info("Set Skyblocker TabList.enabled = " + enable);
            return true;
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Skyblocker not present");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("Could not update Skyblocker TabList config", e);
            return false;
        }
    }

    private static void disableSkyblockerTabList() {
        enableSkyblockerTabList(false);
    }

    // ===== SkyHanni =====

    /**
     * Enables/disables the SkyHanni compact tab by updating its config at runtime.
     * (Assumes config is at SkyHanniMod.feature.inventory.tabListConfig.enabled)
     */
    private static boolean enableSkyHanniTabList(boolean enable) {
        try {
            // SkyHanniMod.feature.inventory.tabListConfig.enabled = enable
            Class<?> skyHanniMod = Class.forName("at.hannibal2.skyhanni.SkyHanniMod");
            Object feature = skyHanniMod.getField("feature").get(null);
            Object inventory = feature.getClass().getField("inventory").get(feature);

            // Try tabListConfig, fallback to tabList if present
            Optional<Object> tabListOpt = getFieldIfPresent(inventory, "tabListConfig")
                    .or(() -> getFieldIfPresent(inventory, "tabList"));

            if (tabListOpt.isPresent()) {
                Object tabListConfig = tabListOpt.get();
                tabListConfig.getClass().getField("enabled").setBoolean(tabListConfig, enable);

                // Save config
                Class<?> configManager = Class.forName("at.hannibal2.skyhanni.config.ConfigManager");
                Method saveMethod = configManager.getDeclaredMethod("save");
                saveMethod.invoke(null);

                PackCore.LOGGER.info("Set SkyHanni tab list enabled = " + enable);
                return true;
            } else {
                PackCore.LOGGER.warn("SkyHanni tab list config field not found via reflection");
                return false;
            }
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("SkyHanni not present");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("Could not update SkyHanni tab list config", e);
            return false;
        }
    }

    private static void disableSkyHanniTabList() {
        enableSkyHanniTabList(false);
    }

    /**
     * Utility: Try to get a field by name, returning Optional.empty() if not present.
     */
    private static Optional<Object> getFieldIfPresent(Object obj, String field) {
        try {
            return Optional.of(obj.getClass().getField(field).get(obj));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
