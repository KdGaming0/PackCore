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

    // Commands mentioned without the slash (more suspicious - trying to hide)
    private static final Pattern COMMAND_WITHOUT_SLASH = Pattern.compile(
            "\\b(visit|party|coopadd|coop add)\\s+(me|\\w+)\\b",
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

    @Override
    public void analyze(String message, String rawMessage, String sender,
                        ConversationContext context, DetectionResult.Builder result) {
        if (!enabled) {
            return;
        }

        int score = 0;

        // Pattern 1: Direct command instruction (very suspicious)
        Matcher commandMatcher = COMMAND_INSTRUCTION.matcher(message);
        if (commandMatcher.find()) {
            String command = commandMatcher.group(2).toLowerCase();

            // /coopadd is EXTREMELY suspicious (island theft)
            if (command.contains("coop")) {
                score += 100;
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.debug("[ScamShield]   Co-op command instruction: +50 points");
                }
            }
            // /visit is very suspicious (item theft, scam islands)
            else if (command.contains("visit")) {
                score += 40;
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.debug("[ScamShield]   Visit command instruction: +40 points");
                }
            }
            // /party or /trade are moderately suspicious
            else if (command.contains("party") || command.contains("trade")) {
                score += 25;
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.debug("[ScamShield]   Party/Trade command instruction: +25 points");
                }
            }
        }

        // Pattern 2: Command linked to reward (PDF: "Do /p scammer to receive 5 million free coins!")
        Matcher rewardMatcher = COMMAND_WITH_REWARD.matcher(message);
        if (rewardMatcher.find()) {
            score += 45;
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.debug("[ScamShield]   Command with reward promise: +45 points");
            }
        }

        // Pattern 3: Multiple commands in one message (PDF: "/coopadd Scammer DenyCoop")
        // This is VERY suspicious - trying to trick with complex commands
        String lower = message.toLowerCase();
        int commandCount = 0;
        if (lower.contains("/visit") || lower.contains("visit me")) commandCount++;
        if (lower.contains("/party") || lower.contains("/p ")) commandCount++;
        if (lower.contains("/coopadd") || lower.contains("coopadd")) commandCount++;
        if (lower.contains("/trade")) commandCount++;

        if (commandCount >= 2) {
            score += 35;
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.debug("[ScamShield]   Multiple commands in message: +35 points");
            }
        }

        // Pattern 4: Fake command variants (PDF: "/coopadd Scammer DenyCoop for me to clear it out")
        // Scammers invent fake command parameters to confuse victims
        if ((lower.contains("denycoop") || lower.contains("deny coop") ||
                lower.contains("clear it out") || lower.contains("to clear")) &&
                (lower.contains("coopadd") || lower.contains("coop"))) {
            score += 60; // VERY high - this is a known trick
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.debug("[ScamShield]   Fake co-op command variant: +60 points");
            }
        }

        // Pattern 5: Imperative language with commands (DO this, MUST do, TYPE this)
        if ((lower.contains("do /") || lower.contains("type /") ||
                lower.contains("run /") || lower.contains("use /") ||
                lower.contains("enter /"))) {
            score += 20;
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.debug("[ScamShield]   Imperative command language: +20 points");
            }
        }

        // Pattern 6: Command in ALL CAPS (trying to grab attention)
        if (rawMessage != null && rawMessage.toUpperCase().equals(rawMessage)) {
            if (rawMessage.contains("/VISIT") || rawMessage.contains("/PARTY") ||
                    rawMessage.contains("/COOPADD") || rawMessage.contains("VISIT ME")) {
                score += 15;
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.debug("[ScamShield]   ALL CAPS command: +15 points");
                }
            }
        }

        if (score > 0) {
            result.addScamTypeContribution(getId(), score);

            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.debug("[ScamShield] {} detected: +{} points",
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