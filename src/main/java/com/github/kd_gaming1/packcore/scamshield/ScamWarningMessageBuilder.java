package com.github.kd_gaming1.packcore.scamshield;

import com.github.kd_gaming1.packcore.scamshield.detector.ConfidenceLevel;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamCategory;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Builds tiered warning messages for scam detections.
 * Uses category-specific variants with proper Minecraft formatting.
 */
public class ScamWarningMessageBuilder {
    /**
     * Build warning message based on detection confidence and category.
     */
    public static Text buildWarningMessage(DetectionResult result) {
        ConfidenceLevel level = result.getConfidenceLevel();
        ScamCategory category = result.getPrimaryCategory();

        return switch (level) {
            case LOW -> buildLowConfidenceMessage(result, category);
            case MEDIUM -> buildMediumConfidenceMessage(result, category);
            case HIGH -> buildHighConfidenceMessage(result, category);
        };
    }

    // LOW CONFIDENCE
    private static Text buildLowConfidenceMessage(DetectionResult result, ScamCategory category) {
        MutableText message = Text.literal("");

        // Header
        message.append(Text.literal("⚠ LOW CONFIDENCE ⚠\n")
                .styled(style -> style.withColor(0xFFAA00).withBold(true)));

        // Category-specific message
        switch (category) {
            case DISCORD_VERIFY:
            case PHISHING:
                appendLowPhishing(message);
                break;
            case FAKE_REWARD:
                appendLowGiveaway(message);
                break;
            case ACCOUNT_THEFT:
                appendLowCoopIsland(message);
                break;
            default:
                appendLowBase(message);
                break;
        }

        // Action buttons
        appendLowActions(message, result);

        return message;
    }

    private static void appendLowBase(MutableText message) {
        message.append(Text.literal("⚠ ScamShield Notice: ")
                .styled(style -> style.withColor(0xFFAA00).withBold(true)));
        message.append(Text.literal("This user has sent one or more messages that look a bit unusual. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("It might be harmless, but stay cautious before clicking links or entering any login info.\n")
                .styled(style -> style.withColor(0xAAAAAA)));
    }

    private static void appendLowPhishing(MutableText message) {
        message.append(Text.literal("⚠ ScamShield Notice: ")
                .styled(style -> style.withColor(0xFFAA00).withBold(true)));
        message.append(Text.literal("This user sent a message that looks like it might contain a suspicious link or verification request. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("It might be harmless, but scams often ask you to log in or verify through fake websites or Discord bots.\n")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("Stay cautious before clicking any link or entering your email or password.\n")
                .styled(style -> style.withColor(0xFFFF55)));
    }

    private static void appendLowGiveaway(MutableText message) {
        message.append(Text.literal("⚠ ScamShield Notice: ")
                .styled(style -> style.withColor(0xFFAA00).withBold(true)));
        message.append(Text.literal("This message might be related to \"too good to be true\" offers, such as free ranks, giveaways, or high-profit trades. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("It could be real, but scams often promise rewards to make you act fast.\n")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("Think before trading or visiting other islands.\n")
                .styled(style -> style.withColor(0xFFFF55)));
    }

    private static void appendLowCoopIsland(MutableText message) {
        message.append(Text.literal("⚠ ScamShield Notice: ")
                .styled(style -> style.withColor(0xFFAA00).withBold(true)));
        message.append(Text.literal("This player mentioned co-op or island access. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("Some scammers use co-op invites to steal your items or delete your island.\n")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("Double-check who you're adding or inviting before using ")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("/coopadd")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal(".\n")
                .styled(style -> style.withColor(0xFFFF55)));
    }

    private static void appendLowActions(MutableText message, DetectionResult result) {
        message.append(Text.literal("Learn how to spot common scams and stay protected: ")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(buildEducationButton());
        message.append(Text.literal("\n"));

        message.append(Text.literal("Trust this player 100%? ")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(buildWhitelistButton(result.getSender()));
    }

    // MEDIUM CONFIDENCE
    private static Text buildMediumConfidenceMessage(DetectionResult result, ScamCategory category) {
        MutableText message = Text.literal("");

        // Header
        message.append(Text.literal("🟠 MEDIUM CONFIDENCE 🟠\n")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));

        // Category-specific message
        switch (category) {
            case DISCORD_VERIFY:
            case PHISHING:
                appendMediumPhishing(message);
                break;
            case FAKE_REWARD:
                appendMediumGiveaway(message);
                break;
            case ACCOUNT_THEFT:
                appendMediumCoopIsland(message);
                break;
            case CUSTOM:
                if (result.getTriggeredScamTypes().stream()
                        .anyMatch(type -> type.contains("trade") || type.contains("manipulation"))) {
                    appendMediumTrade(message);
                } else {
                    appendMediumBase(message);
                }
                break;
            default:
                appendMediumBase(message);
                break;
        }

        // Action buttons
        appendMediumActions(message, result);

        return message;
    }

    private static void appendMediumBase(MutableText message) {
        message.append(Text.literal("🟠 ScamShield Warning: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("This user has sent multiple messages matching known scam patterns. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("Scammers often use fake verification steps or Discord links to steal Minecraft accounts.\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("Avoid clicking links or logging in on unfamiliar websites. ")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("Never enter your email, password, or any code you receive by email outside the official Microsoft or Minecraft login pages — and never \"verify\" through Discord.\n")
                .styled(style -> style.withColor(0xFF5555)));
    }

    private static void appendMediumPhishing(MutableText message) {
        message.append(Text.literal("🟠 ScamShield Warning: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("This user's messages match known phishing and verification scams. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("They might be pretending to \"verify\" your account or asking you to link it through a Discord or website.\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("⚠ Never enter your Microsoft or Minecraft login details anywhere except the official login page. ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("Scammers use fake verification steps to steal accounts.\n")
                .styled(style -> style.withColor(0xAAAAAA)));
    }

    private static void appendMediumGiveaway(MutableText message) {
        message.append(Text.literal("🟠 ScamShield Warning: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("Messages from this user match common \"free rank\" or \"giveaway\" scams. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("Scammers often ask you to join a Discord or send coins before they \"reward\" you.\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("If something sounds too good to be true, it usually is.\n")
                .styled(style -> style.withColor(0xFF5555)));
    }

    private static void appendMediumTrade(MutableText message) {
        message.append(Text.literal("🟠 ScamShield Warning: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("This user's messages match known trade or \"item flip\" scams. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("Scammers often offer deals far better than market value, then disappear once they receive your item or coins.\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("Be cautious when trading or paying before you receive anything.\n")
                .styled(style -> style.withColor(0xFFFF55)));
    }

    private static void appendMediumCoopIsland(MutableText message) {
        message.append(Text.literal("🟠 ScamShield Warning: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("This message pattern matches known co-op scams. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("Scammers might tell you to run ")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("/coopadd")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal(" or invite them to your island \"for gifts\" or \"help.\"\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("Once added, they can take your items or even delete your island. ")
                .styled(style -> style.withColor(0xFF5555)));
        message.append(Text.literal("Don't add players you don't personally know.\n")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
    }

    private static void appendMediumActions(MutableText message, DetectionResult result) {
        message.append(Text.literal("Learn how to stay safe: ")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(buildEducationButton());
        message.append(Text.literal("\n"));

        message.append(Text.literal("Whitelist only if you fully trust this player: ")
                .styled(style -> style.withColor(0xFF5555)));
        message.append(Text.literal("/scamshield whitelist add " + result.getSender())
                .styled(style -> style.withColor(0xAAAAAA).withItalic(true)));
    }

    // HIGH CONFIDENCE
    private static Text buildHighConfidenceMessage(DetectionResult result, ScamCategory category) {
        MutableText message = Text.literal("");

        // Critical header
        message.append(Text.literal("⛔⛔⛔ ")
                .styled(style -> style.withColor(0xFF0000)));
        message.append(Text.literal("[SCAM SHIELD] CRITICAL SCAM ALERT")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal(" ⛔⛔⛔\n")
                .styled(style -> style.withColor(0xFF0000)));

        message.append(Text.literal("NEARLY CERTAIN SCAM ATTEMPT IN PROGRESS!\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        // Category-specific critical message
        switch (category) {
            case DISCORD_VERIFY:
            case PHISHING:
                appendHighPhishing(message);
                break;
            case FAKE_REWARD:
                appendHighGiveaway(message);
                break;
            case ACCOUNT_THEFT:
                appendHighCoopIsland(message);
                break;
            case CUSTOM:
                if (result.getTriggeredScamTypes().stream()
                        .anyMatch(type -> type.contains("trade") || type.contains("manipulation"))) {
                    appendHighTrade(message);
                } else {
                    appendHighBase(message);
                }
                break;
            default:
                appendHighBase(message);
                break;
        }

        // Critical actions
        appendHighActions(message, result);

        return message;
    }

    private static void appendHighBase(MutableText message) {
        message.append(Text.literal("\n🚨 Scam Type: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("Phishing\n")
                .styled(style -> style.withColor(0xFFFFFF).withBold(true)));

        message.append(Text.literal("This scammer likely wants your login credentials or personal information.\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("\n⛔ STOP ALL COMMUNICATION NOW!\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        message.append(Text.literal("\n📋 What This Scammer Wants:\n")
                .styled(style -> style.withColor(0xFFFF55).withBold(true)));
        message.append(Text.literal("• Your account credentials (username/password)\n")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("• Your 2FA or authentication codes\n")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("• Access to steal your items or account\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        appendRecommendedActions(message);
    }

    private static void appendHighPhishing(MutableText message) {
        message.append(Text.literal("\n⛔ ")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal("[SCAM SHIELD] CRITICAL SCAM ALERT\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        message.append(Text.literal("ScamShield has detected strong signs of a Discord or verification scam.\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        message.append(Text.literal("\nThe user is likely trying to get you to log in or \"verify\" your account on a fake site or bot.\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("\n⚠ Do NOT click any link or enter your Microsoft details anywhere but the official login page.\n")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));

        message.append(Text.literal("\nReport the player with ")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("/report")
                .styled(style -> style.withColor(0x55FF55).withBold(true)));
        message.append(Text.literal(" and block them with ")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("/ignore add <PlayerName>")
                .styled(style -> style.withColor(0x55FF55).withBold(true)));
        message.append(Text.literal(".\n")
                .styled(style -> style.withColor(0xFFFF55)));
    }

    private static void appendHighGiveaway(MutableText message) {
        message.append(Text.literal("\n⛔ ")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal("[SCAM SHIELD] CRITICAL SCAM ALERT\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        message.append(Text.literal("This is almost certainly a giveaway scam. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("The scammer is trying to get you to send coins or log in to a fake site in exchange for a \"free rank\" or \"reward.\"\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("\nStop all communication. Do NOT send any items or coins.\n")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("Report and block this player immediately.\n")
                .styled(style -> style.withColor(0xFF5555)));
    }

    private static void appendHighTrade(MutableText message) {
        message.append(Text.literal("\n⛔ ")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal("[SCAM SHIELD] CRITICAL SCAM ALERT\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        message.append(Text.literal("ScamShield detected a strong match with known trade or flip scams. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("These scams trick players into trading valuable items for worthless ones or paying before receiving anything.\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("\n✅ Don't continue the trade.\n")
                .styled(style -> style.withColor(0x55FF55)));
        message.append(Text.literal("✅ Report and block the player.\n")
                .styled(style -> style.withColor(0x55FF55)));
        message.append(Text.literal("✅ Keep your items and coins secure.\n")
                .styled(style -> style.withColor(0x55FF55)));
    }

    private static void appendHighCoopIsland(MutableText message) {
        message.append(Text.literal("\n⛔ ")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal("[SCAM SHIELD] CRITICAL SCAM ALERT\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        message.append(Text.literal("Detected a known co-op access scam. ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("The scammer may ask you to type ")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("/coopadd")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal(" or invite them to your island.\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("\n⚠ Once added, they can steal your items or delete your island entirely.\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        message.append(Text.literal("\nStop all communication now. Report and block the player.\n")
                .styled(style -> style.withColor(0xFF5555)));
    }

    private static void appendRecommendedActions(MutableText message) {
        message.append(Text.literal("\n✅ RECOMMENDED ACTIONS:\n")
                .styled(style -> style.withColor(0x55FF55).withBold(true)));
        message.append(Text.literal("• Stop replying to this user immediately\n")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("• Use ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("/report")
                .styled(style -> style.withColor(0x55FF55).withBold(true)));
        message.append(Text.literal(" to notify Hypixel staff\n")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("• Block them with ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("/ignore add <PlayerName>\n")
                .styled(style -> style.withColor(0x55FF55).withBold(true)));
        message.append(Text.literal("• Never share the information they requested\n")
                .styled(style -> style.withColor(0xFFFFFF)));
    }

    private static void appendHighActions(MutableText message, DetectionResult result) {
        message.append(Text.literal("\nLearn how to protect your account: ")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(buildEducationButton());
    }

    // HELPER METHODS - Clickable Buttons

    /**
     * Builds a clickable ScamShield Education menu button.
     */
    private static MutableText buildEducationButton() {
        return Text.literal("[Open ScamShield Education Menu]")
                .styled(style -> style
                        .withColor(Formatting.BLUE)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.RunCommand("/scamshield education"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal("§aClick to learn about common scams\n§7Run: /scamshield education"))));
    }

    /**
     * Builds a clickable whitelist button for the specified player.
     *
     * @param playerName The name of the player to whitelist.
     */
    private static MutableText buildWhitelistButton(String playerName) {
        String command = "/scamshield whitelist add " + playerName;

        return Text.literal("[Whitelist User]")
                .styled(style -> style
                        .withColor(Formatting.GREEN)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.SuggestCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal("§aClick to whitelist §e" + playerName
                                        + "\n§7This will skip future scam checks for this player."
                                        + "\n§7Suggest: " + command))));
    }
}
