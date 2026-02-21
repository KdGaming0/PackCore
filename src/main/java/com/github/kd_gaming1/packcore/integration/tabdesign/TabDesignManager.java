package com.github.kd_gaming1.packcore.integration.tabdesign;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.util.wizard.WizardDataStore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility for applying the selected Tab Design (SkyHanni or Skyblocker)
 * by changing the config of the respective mod at runtime.
 */
public class TabDesignManager {

    // Store the pending enable state atomically to avoid race conditions
    private static final AtomicReference<Boolean> pendingSkyHanniState = new AtomicReference<>(null);

    // Register the join listener once on class initialization
    static {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, clientPlayNetworkHandler) -> {
            Boolean state = pendingSkyHanniState.getAndSet(null);
            if (state != null) {
                scheduleDelayedCommand(state);
            }
        });
    }

    public static boolean applyTabDesignFromWizard() {
        String tabDesign = WizardDataStore.getInstance().getTabDesign();

        if (tabDesign == null || tabDesign.isEmpty() || "None".equals(tabDesign)) {
            return false;
        }

        return applyTabDesign(tabDesign);
    }

    private static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    // ===== Skyblocker (using reflection) =====

    private static boolean enableSkyblockerTabList(boolean skyblockerSelected) {
        try {
            Class<?> configManager = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
            java.lang.reflect.Method updateMethod = configManager.getDeclaredMethod("update", java.util.function.Consumer.class);

            java.util.function.Consumer<Object> consumer = config -> updateSkyblockerConfig(config, skyblockerSelected);

            updateMethod.invoke(null, consumer);
            PackCore.LOGGER.info("Set Skyblocker TabHud: tabHudEnabled=true, showVanillaTabByDefault={}", !skyblockerSelected);
            return true;

        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Skyblocker not present");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("Could not update Skyblocker config", e);
            return false;
        }
    }

    private static void updateSkyblockerConfig(Object config, boolean skyblockerSelected) {
        try {
            Object uiAndVisuals = config.getClass().getField("uiAndVisuals").get(config);
            Object tabHud = uiAndVisuals.getClass().getField("tabHud").get(uiAndVisuals);

            // tabHudEnabled is ALWAYS true — Skyblocker's tab must stay enabled regardless.
            // When SkyHanni is selected, showVanillaTabByDefault=true lets vanilla (SkyHanni) show through.
            // When Skyblocker is selected, showVanillaTabByDefault=false so Skyblocker renders its own tab.
            tabHud.getClass().getField("tabHudEnabled").setBoolean(tabHud, true);
            tabHud.getClass().getField("showVanillaTabByDefault").setBoolean(tabHud, !skyblockerSelected);
        } catch (Exception e) {
            PackCore.LOGGER.warn("Failed to update Skyblocker TabHud config", e);
        }
    }

    // ===== SkyHanni (using command) =====

    private static boolean enableSkyHanniTabList(boolean enable) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player != null) {
                scheduleDelayedCommand(enable);
                return true;
            }

            // Reset so the JOIN listener will fire again
            pendingSkyHanniState.set(enable);
            PackCore.LOGGER.info("Queued SkyHanni command to run on world join");
            return true;

        } catch (Exception e) {
            PackCore.LOGGER.warn("Could not queue SkyHanni config command", e);
            return false;
        }
    }

    private static void scheduleDelayedCommand(boolean enable) {
        MinecraftClient client = MinecraftClient.getInstance();

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                client.execute(() -> {
                    try {
                        executeSkyHanniCommand(enable);
                        PackCore.LOGGER.info("Executed SkyHanni command after delay");
                    } catch (Exception e) {
                        PackCore.LOGGER.warn("Failed to execute SkyHanni command", e);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "SkyHanni-Config-Delay").start();
    }

    private static void executeSkyHanniCommand(boolean enable) {
        String command = "shconfig set config.gui.compactTabList.enabled " + enable;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        player.networkHandler.sendChatCommand(command);
        PackCore.LOGGER.info("Executed SkyHanni command: /{}", command);
    }

    /**
     * Availability information for tab design mods
     */
    public static class TabDesignAvailability {
        private final boolean skyhanniAvailable;
        private final boolean skyblockerAvailable;

        public TabDesignAvailability(boolean skyhanniAvailable, boolean skyblockerAvailable) {
            this.skyhanniAvailable = skyhanniAvailable;
            this.skyblockerAvailable = skyblockerAvailable;
        }

        public boolean isSkyHanniAvailable() {
            return skyhanniAvailable;
        }

        public boolean isSkyblockerAvailable() {
            return skyblockerAvailable;
        }
    }

    /**
     * Get the availability of tab design mods
     */
    public static TabDesignAvailability getAvailability() {
        return new TabDesignAvailability(
                isModLoaded("skyhanni"),
                isModLoaded("skyblocker")
        );
    }

    /**
     * Apply a specific tab design by name
     *
     * @param design "skyhanni" or "skyblocker"
     * @return true if successfully applied
     */
    public static boolean applyTabDesign(String design) {
        if (design == null || design.isEmpty()) {
            return false;
        }

        boolean skyblockerPresent = isModLoaded("skyblocker");
        boolean skyhanniPresent = isModLoaded("skyhanni");

        if ("skyblocker".equalsIgnoreCase(design) && skyblockerPresent) {
            // skyblockerSelected=true → tabHudEnabled=true, showVanillaTabByDefault=false
            boolean changed = enableSkyblockerTabList(true);
            if (skyhanniPresent) enableSkyHanniTabList(false);
            return changed;
        } else if ("skyhanni".equalsIgnoreCase(design) && skyhanniPresent) {
            // skyblockerSelected=false → tabHudEnabled=true, showVanillaTabByDefault=true
            boolean changed = enableSkyHanniTabList(true);
            if (skyblockerPresent) enableSkyblockerTabList(false);
            return changed;
        }

        return false;
    }
}