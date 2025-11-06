package com.github.kd_gaming1.packcore.scamshield.detector.types;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.context.ConversationContext;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects when scammers instruct victims to run specific commands.
 *
 * Scammers CONSTANTLY tell victims to run commands:
 * - "/visit me" (to steal items)
 * - "/party scammer" or "/p scammer" (to move to private chat)
 * - "/coopadd Scammer" (to steal island)
 * - "/trade me" (for trade window scams)
 *
 * Key insight: LEGITIMATE players rarely tell you to run commands in public chat.
 * They might say "wanna party?" but not "DO /party [username]!"
 *
 * This is one of the STRONGEST scam indicators.
 */
public class CommandInstructionScam implements ScamType {
    private boolean enabled = true;

    // Match commands like "/visit me", "do /party", "/coopadd username"
    private static final Pattern COMMAND_INSTRUCTION = Pattern.compile(
            "\\b(do|type|run|use|enter)?\\s*(/visit|/party|/p|/coopadd|/coop|/trade|visit|party|coopadd)\\s+(me|\\w+)",
            Pattern.CASE_INSENSITIVE
    );

    // "to receive" + command = very suspicious
    private static final Pattern COMMAND_WITH_REWARD = Pattern.compile(
            "\\b(to (receive|get|claim|win)).*?(/visit|/party|/p|/coopadd|visit|party)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getId() {
        return "command_instruction_scam";
    }

    @Override
    public String getDisplayName() {
        return "Command Instruction";
    }

    // Replace the existing analyze method in CommandInstructionScam with this
    @Override
    public void analyze(String message, String rawMessage, String sender,
                        ConversationContext context, DetectionResult.Builder result) {
        if (!enabled) {
            return;
        }

        int score = 0;
        String lower = message.toLowerCase();

        boolean tradeOrPromoContext = lower.matches(".*\\b(wts|wtb|wtt|selling|buying|trade|trading|visit my island|visit me for|lowball|lowballing|lb|lbing)\\b.*")
                || lower.contains("youtube")
                || lower.contains("subscriber")
                || lower.contains("series")
                || lower.contains("hype")
                || lower.matches(".*\\d+[mkb].*");

        // Pattern 1: Direct command instruction (very suspicious normally)
        Matcher commandMatcher = COMMAND_INSTRUCTION.matcher(message);
        if (commandMatcher.find()) {
            String command = commandMatcher.group(2).toLowerCase();

            if (command.contains("coop")) {
                // Keep co-op add extremely suspicious
                score += 100;
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield]   Co-op command instruction: +100 points");
                }
            }
            else if (command.contains("visit")) {
                // VISIT is commonly used legitimately to show an item. Reduce base penalty.
                // If in trade/promo context, make it much less severe.
                int visitPenalty = tradeOrPromoContext ? 5 : 15;
                score += visitPenalty;
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield]   Visit command instruction: +{} points (promoContext={})",
                            visitPenalty, tradeOrPromoContext);
                }
            }
            else if (command.contains("party") || command.contains("trade")) {
                int penalty = tradeOrPromoContext ? 5 : 10;
                score += penalty;
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield]   Party/Trade command instruction: +{} points (promoContext={})",
                            penalty, tradeOrPromoContext);
                }
            }
        }

        // Pattern 2: Command linked to reward (still highly suspicious)
        Matcher rewardMatcher = COMMAND_WITH_REWARD.matcher(message);
        if (rewardMatcher.find()) {
            score += 45;
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield]   Command with reward promise: +45 points");
            }
        }

        // Pattern 3: Multiple commands in one message (very suspicious)
        int commandCount = 0;
        if (lower.contains("/visit") || lower.contains("visit me")) commandCount++;
        if (lower.contains("/party") || lower.contains("/p ")) commandCount++;
        if (lower.contains("/coopadd") || lower.contains("coopadd")) commandCount++;
        if (lower.contains("/trade")) commandCount++;

        if (commandCount >= 2) {
            score += 35;
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield]   Multiple commands in message: +35 points");
            }
        }

        // Pattern 4: Fake co-op variants remain very suspicious
        if ((lower.contains("denycoop") || lower.contains("deny coop") ||
                lower.contains("clear it out") || lower.contains("to clear")) &&
                (lower.contains("coopadd") || lower.contains("coop"))) {
            score += 60;
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield]   Fake co-op command variant: +60 points");
            }
        }

        // Pattern 5: Imperative language with commands (reduce for promo/trade context)
        if ((lower.contains("do /") || lower.contains("type /") ||
                lower.contains("run /") || lower.contains("use /") ||
                lower.contains("enter /"))) {
            int impPenalty = tradeOrPromoContext ? 5 : 20;
            score += impPenalty;
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield]   Imperative command language: +{} points (promoContext={})",
                        impPenalty, tradeOrPromoContext);
            }
        }

        // Pattern 6: ALL CAPS commands
        if (rawMessage != null && rawMessage.toUpperCase().equals(rawMessage)) {
            if (rawMessage.contains("/VISIT") || rawMessage.contains("/PARTY") ||
                    rawMessage.contains("/COOPADD") || rawMessage.contains("VISIT ME")) {
                score += 15;
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield]   ALL CAPS command: +15 points");
                }
            }
        }

        if (score > 0) {
            result.addScamTypeContribution(getId(), score);

            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield] {} detected: +{} points",
                        getDisplayName(), score);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}