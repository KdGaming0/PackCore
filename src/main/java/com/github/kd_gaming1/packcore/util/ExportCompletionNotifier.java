package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.gui.titlescreen.toast.ExportCompletionToast;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class ExportCompletionNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore-Export");

    public static void notifyExportComplete(String configName, Path exportPath) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        // If player is in-game, send chat message
        if (player != null && client.world != null) {
            sendChatNotification(player, configName, exportPath);
        } else {
            // If not in-game, show toast
            showToastNotification(configName, exportPath);
        }
    }

    private static void sendChatNotification(ClientPlayerEntity player, String configName, Path exportPath) {
        try {
            String folderPath = exportPath.getParent().toUri().toString();
            ClickEvent clickEvent = new ClickEvent.OpenFile(exportPath.getParent());

            Text message = Text.literal("[PackCore] ").formatted(Formatting.GOLD, Formatting.BOLD)
                    .append(Text.literal("Export completed! ").formatted(Formatting.GREEN))
                    .append(Text.literal("\"" + configName + "\"").formatted(Formatting.YELLOW))
                    .append(Text.literal(" [Click to open folder]").formatted(Formatting.AQUA, Formatting.UNDERLINE)
                            .styled(style -> style.withClickEvent(clickEvent)));

            player.sendMessage(message, false);
        } catch (Exception e) {
            LOGGER.warn("Failed to send chat notification", e);
            // Fallback to toast
            showToastNotification(configName, exportPath);
        }
    }

    private static void showToastNotification(String configName, Path exportPath) {
        MinecraftClient.getInstance().getToastManager().add(
                new ExportCompletionToast(configName, exportPath)
        );
    }
}