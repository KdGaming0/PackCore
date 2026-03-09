package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.concurrent.atomic.AtomicReference;

public class TabDesignManager {

    private static final AtomicReference<Boolean> pendingSkyHanniEnable = new AtomicReference<>(null);

    static {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> {
                    Boolean state = pendingSkyHanniEnable.getAndSet(null);
                    if (state != null) scheduleSkyHanniCommand(state);
                }
        );
    }

    public enum TabDesign {
        COMPACT, FANCY
    }

    public static boolean apply(TabDesign design) {
        boolean skyblockerLoaded = FabricLoader.getInstance().isModLoaded("skyblocker");
        boolean skyhanniLoaded = FabricLoader.getInstance().isModLoaded("skyhanni");

        return switch (design) {
            case COMPACT -> {
                // SkyHanni tab on, Skyblocker defers to vanilla
                boolean ok = true;
                if (skyhanniLoaded) ok = setSkyHanniEnabled(true);
                if (skyblockerLoaded) setSkyblockerEnabled(false);
                yield ok;
            }
            case FANCY -> {
                // Skyblocker tab on, SkyHanni off
                boolean ok = true;
                if (skyblockerLoaded) ok = setSkyblockerEnabled(true);
                if (skyhanniLoaded) setSkyHanniEnabled(false);
                yield ok;
            }
        };
    }

    // --- Skyblocker (reflection) ---

    private static boolean setSkyblockerEnabled(boolean enabled) {
        try {
            Class<?> configManager = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
            java.lang.reflect.Method update = configManager.getDeclaredMethod("update", java.util.function.Consumer.class);
            update.invoke(null, (java.util.function.Consumer<Object>) config -> {
                try {
                    Object uiAndVisuals = config.getClass().getField("uiAndVisuals").get(config);
                    Object tabHud = uiAndVisuals.getClass().getField("tabHud").get(uiAndVisuals);
                    tabHud.getClass().getField("tabHudEnabled").setBoolean(tabHud, true);
                    // showVanillaTabByDefault=true lets vanilla/SkyHanni show; false means Skyblocker renders
                    tabHud.getClass().getField("showVanillaTabByDefault").setBoolean(tabHud, !enabled);
                } catch (Exception e) {
                    PackCore.LOGGER.warn("Skyblocker: failed to update TabHud config", e);
                }
            });
            PackCore.LOGGER.info("Skyblocker: tabHudEnabled=true showVanillaTabByDefault={}", !enabled);
            return true;
        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Skyblocker not present");
            return false;
        } catch (Exception e) {
            PackCore.LOGGER.warn("Skyblocker: config update failed", e);
            return false;
        }
    }

    // --- SkyHanni (chat command) ---

    private static boolean setSkyHanniEnabled(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            scheduleSkyHanniCommand(enabled);
        } else {
            pendingSkyHanniEnable.set(enabled);
            PackCore.LOGGER.info("SkyHanni: queued command for next world join");
        }
        return true;
    }

    private static void scheduleSkyHanniCommand(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                client.execute(() -> {
                    LocalPlayer player = client.player;
                    if (player == null) return;
                    String command = "shconfig set config.gui.compactTabList.enabled " + enabled;
                    player.connection.sendCommand(command);
                    PackCore.LOGGER.info("SkyHanni: executed /{}", command);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "packcore-skyhanni-config").start();
    }
}