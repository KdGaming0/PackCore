package com.github.kd_gaming1.packcore.scamshield;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.util.HypixelEventUtil;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts and parses Hypixel chat messages for scam detection.
 */
public class ChatMessageInterceptor {
    private static final ChatMessageInterceptor INSTANCE = new ChatMessageInterceptor();

    // Regex pattern for extracting sender and message from Hypixel chat
    private static final Pattern HYPIXEL_CHAT_PATTERN = Pattern.compile(
            "(?:\\[\\d+\\]\\s*)?(?:\\[[^\\]]+\\]\\s*)*([a-zA-Z0-9_]+):\\s+(.+)$"
    );

    private ChatMessageInterceptor() {}

    public static ChatMessageInterceptor getInstance() {
        return INSTANCE;
    }

    /**
     * Register the chat message event listener.
     * Should be called during mod initialization.
     */
    public void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            // Skip overlay messages (action bar, boss bar, etc.)
            if (overlay) {
                return;
            }

            // Only process if connected to Hypixel
            if (!HypixelEventUtil.isHelloPacketReceived()) {
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield] Skipping - not connected to Hypixel");
                }
                return;
            }

            String rawMessage = message.getString();
            if (rawMessage == null || rawMessage.isEmpty()) {
                return;
            }

            // Remove Minecraft color codes (§ followed by any character)
            String cleanMessage = rawMessage.replaceAll("§.", "");

            // Extract sender and message content
            ChatMessageData chatData = extractChatData(cleanMessage);

            // Skip if not a player chat message
            if (chatData == null) {
                // System/server message - log as such
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield] [SERVER] Received message: '{}'", cleanMessage);
                }
                return;
            }

            // Skip if sender is NPC
            if (cleanMessage.startsWith("[NPC]")) {
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield] [NPC] Skipping CHAT message from NPC: '{}'", cleanMessage);
                }
                return;
            }
            // Player message - log with sender
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield] [PLAYER: {}] Message: '{}'",
                        chatData.sender, chatData.message);
            }

            ScamShieldChatHandler.getInstance().processChatMessage(chatData.message, chatData.sender);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> {
            if (!HypixelEventUtil.isHelloPacketReceived()) {
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield] Skipping CHAT - not connected to Hypixel");
                }
                return;
            }

            String rawMessage = message.getString();
            if (rawMessage == null || rawMessage.isEmpty()) {
                return;
            }

            // Remove color codes
            String cleanMessage = rawMessage.replaceAll("§.", "");

            // Skip if sender is NPC
            if (cleanMessage.startsWith("[NPC]")) {
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield] [NPC] Skipping CHAT message from NPC: '{}'", cleanMessage);
                }
                return;
            }

            // Use the provided sender directly
            if (sender != null && !cleanMessage.isEmpty()) {
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield] [CHAT PLAYER: {}] Message: '{}'", sender, cleanMessage);
                }
                ScamShieldChatHandler.getInstance().processChatMessage(cleanMessage, String.valueOf(sender));
            }
        });
    }

    /**
     * Extract sender and message content from Hypixel chat format.
     * Handles formats like:
     * - [80] [VIP+] gamer90_34: message
     * - [230] gamer2_902: message
     * - [MVP++] PlayerName: message
     *
     * @param cleanMessage Message with color codes already removed
     * @return ChatMessageData with sender and message, or null if not a chat message
     */
    private ChatMessageData extractChatData(String cleanMessage) {
        // Filter out common system messages first
        if (isSystemMessage(cleanMessage)) {
            return null;
        }

        Matcher matcher = HYPIXEL_CHAT_PATTERN.matcher(cleanMessage);

        if (matcher.find()) {
            String sender = matcher.group(1);
            String messageContent = matcher.group(2);

            if (sender != null && messageContent != null && !messageContent.isEmpty()) {
                return new ChatMessageData(sender, messageContent);
            }
        }

        return null; // Not a player chat message
    }

    /**
     * Check if a message is a system message that should not be processed.
     */
    private boolean isSystemMessage(String cleanMessage) {
        return cleanMessage.startsWith("You are ") ||
                cleanMessage.startsWith("Profile ID:") ||
                cleanMessage.contains(" is holding [") ||
                cleanMessage.contains(" joined the lobby!") ||
                cleanMessage.contains(" has quit!") ||
                cleanMessage.contains(" ➤ ");  // Party messages
    }

    /**
     * Simple data class to hold extracted chat information.
     */
    public static class ChatMessageData {
        public final String sender;
        public final String message;

        public ChatMessageData(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }
    }
}