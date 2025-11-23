package com.github.kd_gaming1.packcore.integration.tabdesign;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.util.wizard.WizardDataStore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility for applying the selected Tab Design (SkyHanni or Skyblocker)
 * by changing the config of the respective mod at runtime.
 */
public class TabDesignManager {

    // Track whether we've already applied SkyHanni config to prevent re-application
    private static final AtomicBoolean skyhanniConfigApplied = new AtomicBoolean(false);

    // Store the pending enable state
    private static volatile Boolean pendingSkyHanniState = null;

    // Register the join listener once on class initialization
    static {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, clientPlayNetworkHandler) -> {
            // Only execute if we have a pending state and haven't applied yet
            if (pendingSkyHanniState != null && skyhanniConfigApplied.compareAndSet(false, true)) {
                boolean enable = pendingSkyHanniState;
                scheduleDelayedCommand(enable);
            }
        });
    }

    public static boolean applyTabDesignFromWizard() {
        String tabDesign = WizardDataStore.getInstance().getTabDesign();

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

    public static void resetSkyHanniState() {
        skyhanniConfigApplied.set(false);
        pendingSkyHanniState = null;
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

    // ===== SkyHanni (using command) =====

    private static boolean enableSkyHanniTabList(boolean enable) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();

            // If player is already in a world, run with delay
            if (client.player != null) {
                scheduleDelayedCommand(enable);
                skyhanniConfigApplied.set(true);
                return true;
            }

            // Otherwise, set pending state and wait for join event
            pendingSkyHanniState = enable;
            PackCore.LOGGER.info("Queued SkyHanni command to run on world join");
            return true;

        } catch (Exception e) {
            PackCore.LOGGER.warn("Could not queue SkyHanni config command", e);
            return false;
        }
    }

    private static void scheduleDelayedCommand(boolean enable) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Schedule command execution after a delay
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait 2 seconds for full initialization
                client.execute(() -> {
                    try {
                        executeSkyHanniCommand(enable);
                        PackCore.LOGGER.info("Executed SkyHanni command after delay");
                    } catch (Exception e) {
                        PackCore.LOGGER.warn("Failed to execute SkyHanni command", e);
                        skyhanniConfigApplied.set(false);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                skyhanniConfigApplied.set(false);
            }
        }, "SkyHanni-Config-Delay").start();
    }

    private static void executeSkyHanniCommand(boolean enable) {
        String command = "shconfig set config.gui.compactTabList.enabled " + enable;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        //? if >=1.21.8 {
        
        /*player.networkHandler.sendChatCommand(command);
        
        *///?} else {
        player.networkHandler.sendCommand(command);
        //?}
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
            boolean changed = enableSkyblockerTabList(true);
            if (skyhanniPresent) {
                enableSkyHanniTabList(false);
            }
            return changed;
        } else if ("skyhanni".equalsIgnoreCase(design) && skyhanniPresent) {
            boolean changed = enableSkyHanniTabList(true);
            if (skyblockerPresent) {
                enableSkyblockerTabList(false);
            }
            return changed;
        }

        return false;
    }
}