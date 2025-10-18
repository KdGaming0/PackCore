package com.github.kd_gaming1.packcore.notification;

import com.github.kd_gaming1.packcore.ui.toast.BackupCompletionToast;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Notifies users when backup operations complete
 */
public class BackupNotifications {
    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore-Backup");

    public static void notifyBackupComplete(String backupName, Path backupPath, boolean isRestore) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        // If player is in-game, send chat message
        if (player != null && client.world != null) {
            sendChatNotification(player, backupName, backupPath, isRestore);
        } else {
            // If not in-game, show toast
            showToastNotification(backupName, backupPath, isRestore);
        }
    }

    private static void sendChatNotification(ClientPlayerEntity player, String backupName,
                                             Path backupPath, boolean isRestore) {
        try {
            ClickEvent clickEvent = new ClickEvent.OpenFile(backupPath.getParent());

            Text message = Text.literal("[PackCore] ").formatted(Formatting.GOLD, Formatting.BOLD)
                    .append(Text.literal(isRestore ? "Restore completed! " : "Backup created! ")
                            .formatted(Formatting.GREEN))
                    .append(Text.literal("\"" + backupName + "\"").formatted(Formatting.YELLOW))
                    .append(Text.literal(" [Click to open folder]")
                            .formatted(Formatting.AQUA, Formatting.UNDERLINE)
                            .styled(style -> style.withClickEvent(clickEvent)));

            player.sendMessage(message, false);
        } catch (Exception e) {
            LOGGER.warn("Failed to send chat notification", e);
            // Fallback to toast
            showToastNotification(backupName, backupPath, isRestore);
        }
    }

    private static void showToastNotification(String backupName, Path backupPath, boolean isRestore) {
        MinecraftClient.getInstance().getToastManager().add(
                new BackupCompletionToast(backupName, backupPath, isRestore)
        );
    }
}