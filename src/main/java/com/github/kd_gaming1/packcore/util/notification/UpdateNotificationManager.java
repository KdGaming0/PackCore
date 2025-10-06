package com.github.kd_gaming1.packcore.util.notification;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.titlescreen.toast.UpdateNotificationToast;
import com.github.kd_gaming1.packcore.util.api.UpdateCheckResult;
import com.github.kd_gaming1.packcore.util.modpack.ModpackInfo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class UpdateNotificationManager {
    private static final long MAIN_MENU_TOAST_COOLDOWN_MS = 300_000; // 5 minutes for main menu
    private static final Set<String> shownVersionsThisSession = new HashSet<>();
    private static long lastMainMenuToastTime = 0;
    private static boolean hasShownInGameNotificationThisSession = false;

    static {
        // Register event for in-game notifications
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!hasShownInGameNotificationThisSession) {
                checkAndShowInGameNotification();
            }
        });
    }

    public static boolean shouldShowMainMenuToast(String newVersion) {
        long now = System.currentTimeMillis();
        return !shownVersionsThisSession.contains(newVersion) &&
                (now - lastMainMenuToastTime > MAIN_MENU_TOAST_COOLDOWN_MS);
    }

    public static void showMainMenuToast(String currentVersion, String newVersion, String modpackName) {
        MinecraftClient.getInstance().getToastManager().add(
                new UpdateNotificationToast(currentVersion, newVersion, modpackName)
        );
        shownVersionsThisSession.add(newVersion);
        lastMainMenuToastTime = System.currentTimeMillis();
    }

    private static void checkAndShowInGameNotification() {
        ModpackInfo info = PackCore.getModpackInfo();
        if (info == null || !PackCoreConfig.enableUpdateNotifications) return;

        UpdateCheckResult result = PackCore.getUpdateManager().checkForUpdates(info);
        if (result.isSuccess() && result.isUpdateAvailable()) {
            showInGameChatMessage(info.getVersion(), result.getVersionNumber(),
                    result.getModrinthUrl(), info.getName());
            hasShownInGameNotificationThisSession = true;
        }
    }

    private static void showInGameChatMessage(String currentVersion, String newVersion,
                                              String modrinthUrl, String modpackName) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        try {
            URI uri = URI.create(modrinthUrl);
            ClickEvent clickEvent = new ClickEvent.OpenUrl(uri);

            Text updateMessage = Text.literal("[" + modpackName + "] ")
                    .formatted(Formatting.GOLD, Formatting.BOLD)
                    .append(Text.literal("Update available! ").formatted(Formatting.YELLOW))
                    .append(Text.literal(currentVersion + " → " + newVersion).formatted(Formatting.WHITE))
                    .append(Text.literal(" [Click to view]").formatted(Formatting.AQUA, Formatting.UNDERLINE)
                            .styled(style -> style.withClickEvent(clickEvent)));

            player.sendMessage(updateMessage, false);
        } catch (IllegalArgumentException e) {
            // Handle invalid URL gracefully
            PackCore.LOGGER.error("Invalid Modrinth URL: " + modrinthUrl, e);
        }
    }

    // Call this when a new session starts or when versions change
    public static void resetSessionData() {
        shownVersionsThisSession.clear();
        hasShownInGameNotificationThisSession = false;
        lastMainMenuToastTime = 0;
    }
}