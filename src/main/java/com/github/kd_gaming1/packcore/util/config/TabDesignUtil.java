package com.github.kd_gaming1.packcore.util.config;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.help.WizardDataManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

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

        if ("skyblocker".equalsIgnoreCase(tabDesign) && skyblockerPresent) {
            boolean changed = enableSkyblockerTabList(true);
            if (skyhanniPresent) enableSkyHanniTabList(false);
            return changed;
        } else if ("skyhanni".equalsIgnoreCase(tabDesign) && skyhanniPresent) {
            boolean changed = enableSkyHanniTabList(true);
            if (skyblockerPresent) enableSkyblockerTabList(false);
            return changed;
        }

        return false;
    }

    private static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    // ===== Skyblocker (using reflection) =====

    private static boolean enableSkyblockerTabList(boolean enable) {
        try {
            Class<?> configManager = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
            Method updateMethod = configManager.getDeclaredMethod("update", java.util.function.Consumer.class);

            java.util.function.Consumer<Object> consumer = config -> updateSkyblockerConfig(config, enable);

            updateMethod.invoke(null, consumer);
            PackCore.LOGGER.info("Set Skyblocker TabHud enabled = {}", enable);
            return true;

        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Skyblocker not present");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("Could not update Skyblocker config", e);
            return false;
        }
    }

    private static void updateSkyblockerConfig(Object config, boolean enable) {
        try {
            Object uiAndVisuals = config.getClass().getField("uiAndVisuals").get(config);
            Object tabHud = uiAndVisuals.getClass().getField("tabHud").get(uiAndVisuals);

            tabHud.getClass().getField("tabHudEnabled").setBoolean(tabHud, enable);
            tabHud.getClass().getField("showVanillaTabByDefault").setBoolean(tabHud, !enable);
        } catch (Exception e) {
            PackCore.LOGGER.warn("Failed to update Skyblocker TabHud config", e);
        }
    }

    private static boolean enableSkyHanniTabList(boolean enable) {
        //TODO find out how to do reflection so there is no delay in applying the config
        try {
            MinecraftClient client = MinecraftClient.getInstance();

            // If player is already in a world, run immediately
            if (client.player != null) {
                executeSkyHanniCommand(enable);
                return true;
            }

            // Otherwise, queue command for when world is joined
            ClientPlayConnectionEvents.JOIN.register((handler, sender, clientPlayNetworkHandler) -> {
                try {
                    executeSkyHanniCommand(enable);
                    PackCore.LOGGER.info("Executed SkyHanni command after join");
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

    private static void executeSkyHanniCommand(boolean enable) {
        String command = "shconfig set config.gui.compactTabList.enabled " + enable;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        player.networkHandler.sendCommand(command);
        PackCore.LOGGER.info("Executed SkyHanni command: /{}", command);
    }
}