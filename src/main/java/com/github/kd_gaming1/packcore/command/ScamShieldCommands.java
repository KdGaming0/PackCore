package com.github.kd_gaming1.packcore.command;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.ScamShieldWhitelist;
import com.github.kd_gaming1.packcore.scamshield.debug.ScamShieldDebugger;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.storage.DetectionStats;
import com.github.kd_gaming1.packcore.scamshield.storage.ScamShieldDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Commands for ScamShield system.
 *
 * Available commands:
 * /scamshield toggle - Enable/disable ScamShield
 * /scamshield reload - Reload pattern files
 * /scamshield stats - View detection statistics
 * /scamshield clear - Clear detection history
 * /scamshield test <message> - Test a single message
 * /scamshield debug - Run full debug test suite
 * /scamshield whitelist add <player> - Add player to whitelist
 * /scamshield whitelist remove <player> - Remove player from whitelist
 * /scamshield whitelist list - List whitelisted players
 */
public class ScamShieldCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("scamshield")
                        .then(literal("toggle")
                                .executes(ScamShieldCommands::toggleScamShield)
                        )
                        .then(literal("reload")
                                .executes(ScamShieldCommands::reloadPatterns)
                        )
                        .then(literal("stats")
                                .executes(ScamShieldCommands::showStats)
                        )
                        .then(literal("clear")
                                .executes(ScamShieldCommands::clearHistory)
                        )
                        .then(literal("test")
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes(ScamShieldCommands::testMessage)
                                )
                        )
                        .then(literal("debug")
                                .executes(ScamShieldCommands::runDebugTests)
                        )
                        .then(literal("whitelist")
                                .then(literal("add")
                                        .then(argument("player", StringArgumentType.word())
                                                .executes(ScamShieldCommands::whitelistAdd)
                                        )
                                )
                                .then(literal("remove")
                                        .then(argument("player", StringArgumentType.word())
                                                .executes(ScamShieldCommands::whitelistRemove)
                                        )
                                )
                                .then(literal("list")
                                        .executes(ScamShieldCommands::whitelistList)
                                )
                                .then(literal("clear")
                                        .executes(ScamShieldCommands::whitelistClear)
                                )
                        )
        );
    }

    private static int toggleScamShield(CommandContext<FabricClientCommandSource> context) {
        PackCoreConfig.enableScamShield = !PackCoreConfig.enableScamShield;
        PackCoreConfig.write(PackCore.MOD_ID);

        String status = PackCoreConfig.enableScamShield ? "§aenabled" : "§cdisabled";
        context.getSource().sendFeedback(
                Text.literal("§e[ScamShield] §7System " + status)
        );
        return 1;
    }

    private static int reloadPatterns(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(
                Text.literal("§e[ScamShield] §7Reloading pattern files...")
        );

        try {
            PackCore.getScamDetector().reloadScamTypes();
            context.getSource().sendFeedback(
                    Text.literal("§a[ScamShield] ✓ Pattern files reloaded successfully!")
            );
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(
                    Text.literal("§c[ScamShield] Failed to reload patterns: " + e.getMessage())
            );
            return 0;
        }
    }

    private static int showStats(CommandContext<FabricClientCommandSource> context) {
        DetectionStats stats = ScamShieldDataManager.getInstance().getStats();

        context.getSource().sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        context.getSource().sendFeedback(Text.literal("§e[ScamShield Statistics]"));
        context.getSource().sendFeedback(Text.literal(""));
        context.getSource().sendFeedback(
                Text.literal("§7Total Detections: §f" + stats.getTotalDetections())
        );
        context.getSource().sendFeedback(
                Text.literal("§7Unique Scammers: §f" + stats.getUniqueSenders())
        );

        if (!stats.getCategoryCounts().isEmpty()) {
            context.getSource().sendFeedback(Text.literal(""));
            context.getSource().sendFeedback(Text.literal("§7Detections by Category:"));
            stats.getCategoryCounts().forEach((category, count) -> {
                context.getSource().sendFeedback(
                        Text.literal("§7  • §e" + category + "§7: §f" + count)
                );
            });
        }

        context.getSource().sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return 1;
    }

    private static int clearHistory(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(
                Text.literal("§e[ScamShield] §7Clearing detection history...")
        );

        ScamShieldDataManager.getInstance().clearHistoryAsync().thenRun(() -> {
            context.getSource().sendFeedback(
                    Text.literal("§a[ScamShield] ✓ History cleared!")
            );
        });

        return 1;
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

    private static int whitelistAdd(CommandContext<FabricClientCommandSource> context) {
        String player = StringArgumentType.getString(context, "player");
        FabricClientCommandSource source = context.getSource();

        boolean added = ScamShieldWhitelist.getInstance().add(player);

        if (added) {
            source.sendFeedback(
                    Text.literal("§a[ScamShield] ✓ Added §f" + player + "§a to whitelist")
            );
            return 1;
        } else {
            source.sendError(
                    Text.literal("§c[ScamShield] Player already whitelisted")
            );
            return 0;
        }
    }

    private static int whitelistRemove(CommandContext<FabricClientCommandSource> context) {
        String player = StringArgumentType.getString(context, "player");
        FabricClientCommandSource source = context.getSource();

        boolean removed = ScamShieldWhitelist.getInstance().remove(player);

        if (removed) {
            source.sendFeedback(
                    Text.literal("§a[ScamShield] ✓ Removed §f" + player + "§a from whitelist")
            );
            return 1;
        } else {
            source.sendError(
                    Text.literal("§c[ScamShield] Player not in whitelist")
            );
            return 0;
        }
    }

    private static int whitelistList(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        var whitelist = ScamShieldWhitelist.getInstance().getWhitelistedPlayers();

        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        source.sendFeedback(Text.literal("§e[ScamShield Whitelist]"));
        source.sendFeedback(Text.literal(""));

        if (whitelist.isEmpty()) {
            source.sendFeedback(Text.literal("§7No whitelisted players"));
        } else {
            source.sendFeedback(
                    Text.literal("§7Whitelisted Players: §f" + whitelist.size())
            );
            source.sendFeedback(Text.literal(""));
            whitelist.forEach(player -> {
                source.sendFeedback(Text.literal("§7  • §f" + player));
            });
        }

        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return 1;
    }

    private static int whitelistClear(CommandContext<FabricClientCommandSource> context) {
        ScamShieldWhitelist.getInstance().clear();
        context.getSource().sendFeedback(
                Text.literal("§a[ScamShield] ✓ Whitelist cleared")
        );
        return 1;
    }
}