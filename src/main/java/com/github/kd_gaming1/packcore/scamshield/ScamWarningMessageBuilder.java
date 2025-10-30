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

        // Header with spacing
        message.append(Text.literal("\n⚠ LOW CONFIDENCE ⚠\n")
                .styled(style -> style.withColor(0xFFAA00).withBold(true)));
        message.append(Text.literal("ScamShield Notice\n\n")
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

        // Action buttons with spacing
        message.append(Text.literal("\n"));
        appendLowActions(message, result);
        message.append(Text.literal("\n"));

        return message;
    }

    private static void appendLowBase(MutableText message) {
        message.append(Text.literal("This user sent unusual messages.\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        message.append(Text.literal("\nIt might be harmless, but stay cautious:\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("• Don't click suspicious links\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• Don't enter login info\n")
                .styled(style -> style.withColor(0xFFFF55)));
    }

    private static void appendLowPhishing(MutableText message) {
        message.append(Text.literal("Possible suspicious link or verification request detected.\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        message.append(Text.literal("\n⚠ Stay Safe:\n")
                .styled(style -> style.withColor(0xFFAA00).withBold(true)));

        message.append(Text.literal("• Scams often ask you to verify through fake sites\n")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("• Don't click links from strangers\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• Never enter your email or password\n")
                .styled(style -> style.withColor(0xFFFF55)));
    }

    private static void appendLowGiveaway(MutableText message) {
        message.append(Text.literal("\"Too good to be true\" offer detected.\n")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("(Free ranks, giveaways, high-profit trades)\n\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("Could be real, but scammers often:\n")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("• Promise big rewards to make you act fast\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• Ask you to trade or visit their island first\n")
                .styled(style -> style.withColor(0xFFFF55)));
    }

    private static void appendLowCoopIsland(MutableText message) {
        message.append(Text.literal("Co-op or island access mentioned.\n\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        message.append(Text.literal("⚠ Warning:\n")
                .styled(style -> style.withColor(0xFFAA00).withBold(true)));
        message.append(Text.literal("Some scammers use co-op to steal items or delete islands.\n\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("Double-check before using ")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("/coopadd")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
    }

    private static void appendLowActions(MutableText message, DetectionResult result) {
        message.append(Text.literal("📚 Learn more: ")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(buildEducationButton());
        message.append(Text.literal("\n"));

        message.append(Text.literal("✓ Trust this player? ")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(buildWhitelistButton(result.getSender()));
    }

    // MEDIUM CONFIDENCE
    private static Text buildMediumConfidenceMessage(DetectionResult result, ScamCategory category) {
        MutableText message = Text.literal("");

        // Header with spacing
        message.append(Text.literal("\n🟠 MEDIUM CONFIDENCE 🟠\n")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("ScamShield Warning\n\n")
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

        // Action buttons with spacing
        message.append(Text.literal("\n"));
        appendMediumActions(message, result);
        message.append(Text.literal("\n"));

        return message;
    }

    private static void appendMediumBase(MutableText message) {
        message.append(Text.literal("Multiple messages match known scam patterns.\n\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        message.append(Text.literal("🛑 DO NOT:\n")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("• Click suspicious links\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• Log in on unfamiliar websites\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• Enter email, password, or codes\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• \"Verify\" through Discord\n")
                .styled(style -> style.withColor(0xFFFF55)));
    }

    private static void appendMediumPhishing(MutableText message) {
        message.append(Text.literal("Phishing/Verification scam detected.\n\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        message.append(Text.literal("⚠ This scammer likely wants to:\n")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("• Make you \"verify\" through fake sites\n")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("• Steal your account login details\n\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("✓ Only log in on official Microsoft/Minecraft pages\n")
                .styled(style -> style.withColor(0x55FF55)));
        message.append(Text.literal("✗ Never enter credentials elsewhere\n")
                .styled(style -> style.withColor(0xFF5555)));
    }

    private static void appendMediumGiveaway(MutableText message) {
        message.append(Text.literal("\"Free rank\" or giveaway scam detected.\n\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        message.append(Text.literal("Common tactics:\n")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("• Ask you to join Discord first\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• Request coins before \"rewarding\" you\n\n")
                .styled(style -> style.withColor(0xFFFF55)));

        message.append(Text.literal("⚠ If it sounds too good to be true, it usually is.\n")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
    }

    private static void appendMediumTrade(MutableText message) {
        message.append(Text.literal("Trade or \"item flip\" scam detected.\n\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        message.append(Text.literal("Red flags:\n")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("• Deals far better than market value\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• Pressure to trade quickly\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• Asking for payment before delivery\n\n")
                .styled(style -> style.withColor(0xFFFF55)));

        message.append(Text.literal("⚠ Be cautious when paying before receiving items.\n")
                .styled(style -> style.withColor(0xFF5555)));
    }

    private static void appendMediumCoopIsland(MutableText message) {
        message.append(Text.literal("Co-op access scam detected.\n\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        message.append(Text.literal("⚠ Danger:\n")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("Scammers may ask you to run ")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(Text.literal("/coopadd")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("\nfor \"gifts\" or \"help.\"\n\n")
                .styled(style -> style.withColor(0xAAAAAA)));

        message.append(Text.literal("Once added, they can:\n")
                .styled(style -> style.withColor(0xFF5555)));
        message.append(Text.literal("• Steal your items\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• Delete your island\n\n")
                .styled(style -> style.withColor(0xFFFF55)));

        message.append(Text.literal("⛔ Don't add players you don't personally know.\n")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
    }

    private static void appendMediumActions(MutableText message, DetectionResult result) {
        message.append(Text.literal("📚 Learn more: ")
                .styled(style -> style.withColor(0xAAAAAA)));
        message.append(buildEducationButton());
        message.append(Text.literal("\n\n"));

        message.append(Text.literal("⚠ Whitelist only if you fully trust this player:\n")
                .styled(style -> style.withColor(0xFF5555)));
        message.append(Text.literal("/scamshield whitelist add " + result.getSender())
                .styled(style -> style.withColor(0xAAAAAA).withItalic(true)));
    }

    // HIGH CONFIDENCE
    private static Text buildHighConfidenceMessage(DetectionResult result, ScamCategory category) {
        MutableText message = Text.literal("");

        // Critical header
        message.append(Text.literal("\n⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal("   CRITICAL SCAM ALERT   \n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal("⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔\n\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        message.append(Text.literal("🚨 NEARLY CERTAIN SCAM ATTEMPT 🚨\n\n")
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
        message.append(Text.literal("\n"));
        appendHighActions(message, result);
        message.append(Text.literal("\n"));

        return message;
    }

    private static void appendHighBase(MutableText message) {
        message.append(Text.literal("🚨 Scam Type: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("Account Theft\n\n")
                .styled(style -> style.withColor(0xFFFFFF).withBold(true)));

        message.append(Text.literal("⛔ STOP ALL COMMUNICATION NOW!\n\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        message.append(Text.literal("What they want:\n")
                .styled(style -> style.withColor(0xFFFF55).withBold(true)));
        message.append(Text.literal("• Your login credentials\n")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("• Your 2FA codes\n")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("• Access to steal everything\n\n")
                .styled(style -> style.withColor(0xFFFFFF)));

        appendRecommendedActions(message);
    }

    private static void appendHighPhishing(MutableText message) {
        message.append(Text.literal("🚨 Scam Type: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("Discord/Verification Phishing\n\n")
                .styled(style -> style.withColor(0xFFFFFF).withBold(true)));

        message.append(Text.literal("⛔ STOP! DO NOT:\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal("• Click any links\n")
                .styled(style -> style.withColor(0xFF5555)));
        message.append(Text.literal("• \"Verify\" your account\n")
                .styled(style -> style.withColor(0xFF5555)));
        message.append(Text.literal("• Enter Microsoft login details\n\n")
                .styled(style -> style.withColor(0xFF5555)));

        appendRecommendedActions(message);
    }

    private static void appendHighGiveaway(MutableText message) {
        message.append(Text.literal("🚨 Scam Type: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("Fake Giveaway/Free Rank\n\n")
                .styled(style -> style.withColor(0xFFFFFF).withBold(true)));

        message.append(Text.literal("⛔ STOP! DO NOT:\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal("• Send any items or coins\n")
                .styled(style -> style.withColor(0xFF5555)));
        message.append(Text.literal("• Log in to any links\n")
                .styled(style -> style.withColor(0xFF5555)));
        message.append(Text.literal("• Join their Discord\n\n")
                .styled(style -> style.withColor(0xFF5555)));

        appendRecommendedActions(message);
    }

    private static void appendHighTrade(MutableText message) {
        message.append(Text.literal("🚨 Scam Type: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("Trade/Item Flip Scam\n\n")
                .styled(style -> style.withColor(0xFFFFFF).withBold(true)));

        message.append(Text.literal("⛔ STOP THE TRADE!\n\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        message.append(Text.literal("✅ Keep your items and coins safe\n")
                .styled(style -> style.withColor(0x55FF55)));
        message.append(Text.literal("✅ Report and block immediately\n\n")
                .styled(style -> style.withColor(0x55FF55)));

        appendRecommendedActions(message);
    }

    private static void appendHighCoopIsland(MutableText message) {
        message.append(Text.literal("🚨 Scam Type: ")
                .styled(style -> style.withColor(0xFF5555).withBold(true)));
        message.append(Text.literal("Co-op Access Theft\n\n")
                .styled(style -> style.withColor(0xFFFFFF).withBold(true)));

        message.append(Text.literal("⛔ DO NOT RUN ")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal("/coopadd")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));
        message.append(Text.literal("!\n\n")
                .styled(style -> style.withColor(0xFF0000).withBold(true)));

        message.append(Text.literal("They can:\n")
                .styled(style -> style.withColor(0xFF5555)));
        message.append(Text.literal("• Steal ALL your items\n")
                .styled(style -> style.withColor(0xFFFF55)));
        message.append(Text.literal("• Delete your island entirely\n\n")
                .styled(style -> style.withColor(0xFFFF55)));

        appendRecommendedActions(message);
    }

    private static void appendRecommendedActions(MutableText message) {
        message.append(Text.literal("✅ TAKE ACTION NOW:\n")
                .styled(style -> style.withColor(0x55FF55).withBold(true)));
        message.append(Text.literal("1. Stop replying immediately\n")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("2. Use ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("/report")
                .styled(style -> style.withColor(0x55FF55).withBold(true)));
        message.append(Text.literal(" to notify staff\n")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("3. Block with ")
                .styled(style -> style.withColor(0xFFFFFF)));
        message.append(Text.literal("/ignore add <PlayerName>\n")
                .styled(style -> style.withColor(0x55FF55).withBold(true)));
    }

    private static void appendHighActions(MutableText message, DetectionResult result) {
        message.append(Text.literal("📚 Learn how to protect your account:\n")
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
                                Text.literal("§aClick to learn about common scams\n§7Run: /scamshield education")
                        )));
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
                                        + "\n§7Suggest: " + command)
                        )));
    }
}