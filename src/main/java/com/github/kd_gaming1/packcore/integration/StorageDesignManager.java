package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.concurrent.atomic.AtomicReference;

public class StorageDesignManager {

    private static final AtomicReference<Boolean> pendingOverlayState = new AtomicReference<>(null);

    static {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Boolean state = pendingOverlayState.getAndSet(null);
            if (state != null) scheduleCommand(state);
        });
    }

    public enum StorageDesign {
        OVERLAY, VANILLA
    }

    public static boolean apply(StorageDesign design) {
        if (!FabricLoader.getInstance().isModLoaded("firmament")) {
            PackCore.LOGGER.warn("StorageDesign: Firmament not loaded, cannot apply");
            return false;
        }

        boolean enableOverlay = design == StorageDesign.OVERLAY;

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            scheduleCommand(enableOverlay);
        } else {
            pendingOverlayState.set(enableOverlay);
            PackCore.LOGGER.info("StorageDesign: queued command for next world join");
        }

        return true;
    }

    private static void scheduleCommand(boolean enable) {
        Minecraft client = Minecraft.getInstance();
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                client.execute(() -> {
                    LocalPlayer player = client.player;
                    if (player == null) return;
                    // /firm config toggle storage-overlay always-replace
                    // The toggle command flips the value, so we need to check current state.
                    String command = "firm config set storage-overlay always-replace " + enable;
                    player.connection.sendCommand(command);
                    PackCore.LOGGER.info("StorageDesign: executed /{}", command);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "packcore-firmament-config").start();
    }
}