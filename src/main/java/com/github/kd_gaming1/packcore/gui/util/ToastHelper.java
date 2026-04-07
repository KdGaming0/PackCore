package com.github.kd_gaming1.packcore.gui.util;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Shows PackCore toast notifications. */
public final class ToastHelper {

    private ToastHelper() {}

    public static void showUpdateAvailable(String latestVersion, boolean isBeta) {
        if (!PackCoreConfig.showUpdateToast) return;
        if (isBeta && !PackCoreConfig.showBetaUpdateNotifications) return;
        show(
                Component.translatable(isBeta
                        ? "gui.packcore.toast.update.beta.title"
                        : "gui.packcore.toast.update.title"),
                Component.translatable(isBeta
                        ? "gui.packcore.toast.update.beta.message"
                        : "gui.packcore.toast.update.message", latestVersion)
        );
    }

    public static void showBackupCreated(String backupName) {
        if (!PackCoreConfig.showBackupToast) return;
        show(
                Component.translatable("gui.packcore.toast.backup.title"),
                Component.translatable("gui.packcore.toast.backup.message", backupName)
        );
    }

    public static void showLowRam() {
        if (!PackCoreConfig.showRamWarningToast) return;
        show(
                Component.translatable("gui.packcore.toast.ram.title"),
                Component.translatable("gui.packcore.toast.ram.message")
        );
    }

    public static void show(Component title, Component message) {
        Minecraft.getInstance().getToastManager().addToast(new PackCoreToast(title, message));
    }
}