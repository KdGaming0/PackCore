package com.github.kd_gaming1.packcore.command.scamshield;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.scamshield.debug.ScamShieldDebugger;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

/**
 * Commands for testing ScamShield detection capabilities.
 */
public class ScamShieldTestCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> registerTest() {
        return ClientCommandManager.literal("test")
                .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                        .executes(ScamShieldTestCommands::testMessage)
                );
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> registerDebug() {
        return ClientCommandManager.literal("debug").executes(ScamShieldTestCommands::runDebugTests);
    }

    private static int testMessage(CommandContext<FabricClientCommandSource> context) {
        String message = StringArgumentType.getString(context, "message");
        FabricClientCommandSource source = context.getSource();

        source.sendFeedback(Text.literal("§e[ScamShield] §7Testing message..."));
        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        try {
            DetectionResult result = PackCore.getScamDetector().analyze(message, "TestUser");

            source.sendFeedback(Text.literal("§7Message: §f" + message));
            source.sendFeedback(Text.literal(""));

            if (result.isTriggered()) {
                source.sendFeedback(Text.literal("§c§l⚠ SCAM DETECTED"));
                source.sendFeedback(
                        Text.literal("§7Category: §e" + result.getPrimaryCategory().getDisplayName())
                );
            } else {
                source.sendFeedback(Text.literal("§a✓ No scam detected"));
            }

            source.sendFeedback(Text.literal(""));
            source.sendFeedback(Text.literal("§7Score Breakdown:"));
            source.sendFeedback(Text.literal("§7  Total: §f" + result.getTotalScore()));
            source.sendFeedback(Text.literal("§7  ScamType: §f" + result.getScamTypeScore()));
            source.sendFeedback(Text.literal("§7  Progression: §f" + result.getProgressionScore()));

            if (!result.getScamTypeContributions().isEmpty()) {
                source.sendFeedback(Text.literal(""));
                source.sendFeedback(Text.literal("§7Detected Patterns:"));
                result.getScamTypeContributions().forEach((type, score) -> {
                    source.sendFeedback(Text.literal("§7  • §e" + type + "§7: §f" + score + " points"));
                });
            }

            source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("§c[ScamShield] Error: " + e.getMessage()));
            PackCore.LOGGER.error("[ScamShield] Test command error", e);
            return 0;
        }
    }

    private static int runDebugTests(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();

        source.sendFeedback(Text.literal("§e[ScamShield] §7Running debug test suite..."));
        source.sendFeedback(Text.literal("§7This will take about 30 seconds..."));
        source.sendFeedback(Text.literal("§7Check console/logs for detailed output!"));
        source.sendFeedback(Text.literal(""));

        // Run tests asynchronously to avoid blocking
        new Thread(() -> {
            try {
                ScamShieldDebugger debugger = new ScamShieldDebugger();
                ScamShieldDebugger.DebugReport report = debugger.runTests();

                // Send summary to chat
                source.sendFeedback(Text.literal(""));
                source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                source.sendFeedback(Text.literal("§e[Debug Test Summary]"));
                source.sendFeedback(Text.literal(""));
                source.sendFeedback(
                        Text.literal("§7Total Tests: §f" + report.getTotalTests())
                );
                source.sendFeedback(
                        Text.literal("§a✓ Passed: §f" + report.getPassedTests())
                );
                source.sendFeedback(
                        Text.literal("§c✗ Failed: §f" + report.getFailedTests())
                );
                source.sendFeedback(
                        Text.literal("§7Pass Rate: §f" + report.getPassRate() + "%")
                );

                if (report.getPassRate() >= 90) {
                    source.sendFeedback(Text.literal(""));
                    source.sendFeedback(Text.literal("§a§l✓ EXCELLENT PERFORMANCE!"));
                } else if (report.getPassRate() >= 75) {
                    source.sendFeedback(Text.literal(""));
                    source.sendFeedback(Text.literal("§e⚠ GOOD - Some improvements needed"));
                } else {
                    source.sendFeedback(Text.literal(""));
                    source.sendFeedback(Text.literal("§c✗ NEEDS IMPROVEMENT"));
                    source.sendFeedback(Text.literal("§7Check console for failed tests"));
                }

                source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

            } catch (Exception e) {
                source.sendError(Text.literal("§c[ScamShield] Debug test failed: " + e.getMessage()));
                PackCore.LOGGER.error("[ScamShield] Debug test error", e);
            }
        }, "ScamShield-Debug").start();

        return 1;
    }
}