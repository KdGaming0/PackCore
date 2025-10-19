package com.github.kd_gaming1.packcore.command;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamDetector;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamPattern;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

/**
 * In-game testing commands for ScamShield pattern detection.
 * Allows developers to test patterns directly in Minecraft without external frameworks.
 *
 * Usage:
 * /scamshield test analyze "message here"
 * /scamshield test pattern "pattern_id" "message here"
 * /scamshield test batch
 */
public class ScamShieldTestCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("scamshield")
                .then(ClientCommandManager.literal("test")
                        .then(ClientCommandManager.literal("analyze")
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes(ScamShieldTestCommand::analyzeMessage)))

                        .then(ClientCommandManager.literal("pattern")
                                .then(argument("patternId", StringArgumentType.word())
                                        .then(argument("message", StringArgumentType.greedyString())
                                                .executes(ScamShieldTestCommand::testSinglePattern))))

                        .then(ClientCommandManager.literal("batch")
                                .executes(ScamShieldTestCommand::runBatchTests))

                        .then(ClientCommandManager.literal("list")
                                .executes(ScamShieldTestCommand::listPatterns))
                )
        );
    }

    // ==================== COMMAND HANDLERS ====================

    /**
     * /scamshield test analyze "message"
     * Analyzes a message with all loaded patterns and shows results
     */
    private static int analyzeMessage(CommandContext<FabricClientCommandSource> ctx) {
        String message = StringArgumentType.getString(ctx, "message");
        FabricClientCommandSource source = ctx.getSource();

        ScamDetector detector = PackCore.getScamDetector();
        DetectionResult result = detector.analyze(message, "TestUser");

        source.sendFeedback(Text.literal("§e§l[ScamShield Test - Message Analysis]"));
        source.sendFeedback(Text.literal("§7Message: §f" + message));
        source.sendFeedback(Text.literal("§7Triggered: §f" + (result.isTriggered() ? "§cYES" : "§aNo")));
        source.sendFeedback(Text.literal("§7Total Score: §f" + result.getTotalScore()));
        source.sendFeedback(Text.literal("§7Matched Patterns: §f" + result.getMatchedPatterns().size()));

        if (!result.getMatchedPatterns().isEmpty()) {
            source.sendFeedback(Text.literal("\n§e§lMatched Patterns:"));
            result.getMatchedPatterns().forEach(match -> {
                ScamPattern pattern = match.getPattern();
                source.sendFeedback(Text.literal(String.format(
                        "§7  • %s §8(weight: %d, category: %s)",
                        pattern.getId(),
                        pattern.getWeight(),
                        pattern.getCategory().getDisplayName()
                )));
            });
        }

        source.sendFeedback(Text.literal("§7Primary Category: §e" + result.getPrimaryCategory().getDisplayName()));
        return 1;
    }

    /**
     * /scamshield test pattern "pattern_id" "message"
     * Tests a specific pattern against a message
     */
    private static int testSinglePattern(CommandContext<FabricClientCommandSource> ctx) {
        String patternId = StringArgumentType.getString(ctx, "patternId");
        String message = StringArgumentType.getString(ctx, "message");
        FabricClientCommandSource source = ctx.getSource();

        ScamDetector detector = PackCore.getScamDetector();
        ScamPattern pattern = detector.getPatterns().stream()
                .filter(p -> p.getId().equals(patternId))
                .findFirst()
                .orElse(null);

        if (pattern == null) {
            source.sendFeedback(Text.literal("§c[ScamShield Test] Pattern not found: §f" + patternId));
            return 0;
        }

        boolean matches = pattern.matches(message);

        source.sendFeedback(Text.literal("§e§l[ScamShield Test - Single Pattern]"));
        source.sendFeedback(Text.literal("§7Pattern ID: §f" + pattern.getId()));
        source.sendFeedback(Text.literal("§7Category: §f" + pattern.getCategory().getDisplayName()));
        source.sendFeedback(Text.literal("§7Weight: §f" + pattern.getWeight()));
        source.sendFeedback(Text.literal("§7Regex: §f" + pattern.getRegex().pattern()));
        source.sendFeedback(Text.literal("§7Enabled: §f" + (pattern.isEnabled() ? "§aYes" : "§cNo")));
        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("§7Message: §f" + message));
        source.sendFeedback(Text.literal("§7Match Result: " + (matches ? "§a✓ MATCH" : "§c✗ NO MATCH")));

        return 1;
    }

    /**
     * /scamshield test batch
     * Runs a suite of predefined test cases
     */
    private static int runBatchTests(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();

        source.sendFeedback(Text.literal("§e§l[ScamShield Batch Test Suite]"));
        source.sendFeedback(Text.literal("§7Running predefined test cases...\n"));

        TestCase[] testCases = getTestCases();
        int passed = 0;
        int failed = 0;

        ScamDetector detector = PackCore.getScamDetector();

        for (TestCase testCase : testCases) {
            DetectionResult result = detector.analyze(testCase.message, "TestUser");
            boolean success = testCase.expectedTriggered == result.isTriggered();

            if (success) {
                passed++;
                source.sendFeedback(Text.literal("§a✓ " + testCase.name));
            } else {
                failed++;
                source.sendFeedback(Text.literal("§c✗ " + testCase.name));
                source.sendFeedback(Text.literal("§7   Expected: " + (testCase.expectedTriggered ? "TRIGGER" : "SAFE") +
                        ", Got: " + (result.isTriggered() ? "TRIGGER" : "SAFE") +
                        ", Score: " + result.getTotalScore()));
            }
        }

        source.sendFeedback(Text.literal("\n§e§l[Test Results]"));
        source.sendFeedback(Text.literal("§aPassed: §f" + passed));
        source.sendFeedback(Text.literal("§cFailed: §f" + failed));
        source.sendFeedback(Text.literal("§7Total: §f" + testCases.length));

        double passRate = (double) passed / testCases.length * 100;
        Text resultText = passRate == 100 ?
                Text.literal("§a§lALL TESTS PASSED!") :
                Text.literal("§c" + String.format("%.1f%% pass rate", passRate));

        source.sendFeedback(resultText);

        return 1;
    }

    /**
     * /scamshield test list
     * Lists all loaded patterns
     */
    private static int listPatterns(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        ScamDetector detector = PackCore.getScamDetector();
        List<ScamPattern> patterns = detector.getPatterns();

        source.sendFeedback(Text.literal("§e§l[Loaded Patterns]"));
        source.sendFeedback(Text.literal("§7Total: §f" + patterns.size() + "\n"));

        for (ScamPattern pattern : patterns) {
            String status = pattern.isEnabled() ? "§a✓" : "§c✗";
            source.sendFeedback(Text.literal(String.format(
                    "%s §f%s §7| Weight: %d | Category: %s",
                    status,
                    pattern.getId(),
                    pattern.getWeight(),
                    pattern.getCategory().getDisplayName()
            )));
        }

        return 1;
    }

    // ==================== TEST CASE DEFINITIONS ====================

    /**
     * Defines a test case with a message and expected result
     */
    private static class TestCase {
        String name;
        String message;
        boolean expectedTriggered;

        TestCase(String name, String message, boolean expectedTriggered) {
            this.name = name;
            this.message = message;
            this.expectedTriggered = expectedTriggered;
        }
    }

    /**
     * Predefined test cases for batch testing
     * Adjust thresholds and patterns based on your config
     */
    private static TestCase[] getTestCases() {
        return new TestCase[]{
                // Discord verification scams - SHOULD TRIGGER
                new TestCase(
                        "Discord verify urgent message",
                        "verify your discord account urgent now",
                        true
                ),
                new TestCase(
                        "Discord link request",
                        "link your discord to hypixel",
                        true
                ),

                // Urgency tactics - SHOULD TRIGGER (with other patterns)
                new TestCase(
                        "Limited time offer",
                        "limited time free vip expires soon hurry",
                        true
                ),
                new TestCase(
                        "Time pressure combined",
                        "verify discord account asap or banned",
                        true
                ),

                // Phishing attempts - SHOULD TRIGGER
                new TestCase(
                        "Password request",
                        "send me your password code for verification",
                        true
                ),
                new TestCase(
                        "Email request",
                        "provide your email and password immediately",
                        true
                ),

                // Fake threats - SHOULD TRIGGER
                new TestCase(
                        "Account banned threat",
                        "your account has been banned suspended",
                        true
                ),
                new TestCase(
                        "Action required threat",
                        "action required your account locked",
                        true
                ),

                // Too good to be true - MIGHT TRIGGER (depends on threshold)
                new TestCase(
                        "Free items offer",
                        "free vip and coins giveaway",
                        false  // Usually lower score, adjust if needed
                ),

                // Legitimate messages - SHOULD NOT TRIGGER
                new TestCase(
                        "Normal greeting",
                        "hey what's your minecraft username",
                        false
                ),
                new TestCase(
                        "Guild recruitment",
                        "join our skyblock guild for fun",
                        false
                ),
                new TestCase(
                        "Game discussion",
                        "does anyone want to play bedwars",
                        false
                ),
                new TestCase(
                        "Build showcase",
                        "check out this cool castle i built",
                        false
                ),
                new TestCase(
                        "Help request",
                        "can someone help me with the crystal hollows",
                        false
                ),

                // Edge cases
                new TestCase(
                        "Empty-like message",
                        "hi there friend",
                        false
                ),
                new TestCase(
                        "Misspelled scam attempt",
                        "veryfy your accout",
                        false  // Should NOT match due to typos
                ),
        };
    }

    // ==================== HELPER METHOD ====================

    /**
     * Quick method to check if a pattern would trigger
     * Useful for debugging specific patterns
     */
    public static boolean quickTest(String message, int threshold) {
        ScamDetector detector = PackCore.getScamDetector();
        DetectionResult result = detector.analyze(message, "QuickTest");
        return result.getTotalScore() >= threshold;
    }
}