package com.github.kd_gaming1.packcore.util.config;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.help.WizardDataManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;

/**
 * Utility for applying the selected Tab Design (SkyHanni or Skyblocker)
 * by changing the config of the respective mod at runtime.
 */
public class TabDesignUtil {

    public static boolean applyTabDesignFromWizard() {
        String tabDesign = WizardDataManager.getInstance().getTabDesign();

        if (tabDesign == null || tabDesign.isEmpty() || "None".equals(tabDesign)) {
            return false;
        }

        boolean skyblockerPresent = isModLoaded("skyblocker");
        boolean skyhanniPresent = isModLoaded("skyhanni");
        boolean changed = false;

        if ("Skyblocker".equalsIgnoreCase(tabDesign) && skyblockerPresent) {
            changed = enableSkyblockerTabList(true);
            if (skyhanniPresent) enableSkyHanniTabList(false);
        } else if ("SkyHanni".equalsIgnoreCase(tabDesign) && skyhanniPresent) {
            changed = enableSkyHanniTabList(true);
            if (skyblockerPresent) enableSkyblockerTabList(false);
        }

        return changed;
    }

    private static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    // ===== Skyblocker (using reflection) =====

    private static boolean enableSkyblockerTabList(boolean enable) {
        try {
            Class<?> configManager = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
            Method updateMethod = configManager.getDeclaredMethod("update", java.util.function.Consumer.class);

            java.util.function.Consumer<Object> consumer = config -> {
                try {
                    Object uiAndVisuals = config.getClass().getField("uiAndVisuals").get(config);
                    Object tabHud = uiAndVisuals.getClass().getField("tabHud").get(uiAndVisuals);

                    tabHud.getClass().getField("tabHudEnabled").setBoolean(tabHud, enable);
                    tabHud.getClass().getField("showVanillaTabByDefault").setBoolean(tabHud, !enable);
                } catch (Exception e) {
                    PackCore.LOGGER.warn("Failed to update Skyblocker TabHud config", e);
                }
            };

            updateMethod.invoke(null, consumer);
            PackCore.LOGGER.info("Set Skyblocker TabHud enabled = " + enable);
            return true;

        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Skyblocker not present");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("Could not update Skyblocker config", e);
            return false;
        }
    }

    // ===== SkyHanni (using command) =====

    private static boolean enableSkyHanniTabList(boolean enable) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();

            // If player is already in a world, run immediately
            if (client.player != null) {
                String command = "shconfig set config.gui.compactTabList.enabled " + enable;
                client.player.networkHandler.sendCommand(command);
                PackCore.LOGGER.info("Executed SkyHanni command: /" + command);
                return true;
            }

            // Otherwise, queue command for when world is joined
            ClientPlayConnectionEvents.JOIN.register((handler, sender, clientPlayNetworkHandler) -> {
                try {
                    String command = "shconfig set config.gui.compactTabList.enabled " + enable;
                    MinecraftClient.getInstance().player.networkHandler.sendCommand(command);
                    PackCore.LOGGER.info("Executed SkyHanni command after join: /" + command);
                } catch (Exception e) {
                    PackCore.LOGGER.warn("Failed to execute SkyHanni command after join", e);
                }
            });

            PackCore.LOGGER.info("Queued SkyHanni command to run on world join");
            return true;

        } catch (Exception e) {
            PackCore.LOGGER.warn("Could not queue SkyHanni config command", e);
            return false;
        }
    }
}