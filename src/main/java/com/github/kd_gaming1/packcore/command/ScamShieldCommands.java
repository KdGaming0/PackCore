package com.github.kd_gaming1.packcore.command;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.scamshield.ScamShieldWhitelist;
import com.github.kd_gaming1.packcore.scamshield.detector.PatternStats;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamDetector;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamPattern;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

/**
 * Commands for managing ScamShield.
 */
public class ScamShieldCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("scamshield")
                .then(ClientCommandManager.literal("reload")
                        .executes(ScamShieldCommands::reloadPatterns))

                .then(ClientCommandManager.literal("stats")
                        .executes(ScamShieldCommands::showStats))
                        .then(ClientCommandManager.literal("reset")
                                .executes(ScamShieldCommands::resetStats))

                .then(ClientCommandManager.literal("whitelist")
                        .then(ClientCommandManager.literal("add")
                                .then(argument("player", StringArgumentType.word())
                                        .executes(ScamShieldCommands::addWhitelist)))
                        .then(ClientCommandManager.literal("remove")
                                .then(argument("player", StringArgumentType.word())
                                        .executes(ScamShieldCommands::removeWhitelist)))
                        .then(ClientCommandManager.literal("list")
                                .executes(ScamShieldCommands::listWhitelist))
                        .then(ClientCommandManager.literal("clear")
                                .executes(ScamShieldCommands::clearWhitelist)))
        );
    }

    private static int reloadPatterns(CommandContext<FabricClientCommandSource> ctx) {
        PackCore.getScamDetector().reloadPatterns();
        ctx.getSource().sendFeedback(Text.literal("§a[ScamShield] Patterns reloaded successfully!"));
        return 1;
    }

    private static int showStats(CommandContext<FabricClientCommandSource> ctx) {
        Map<String, PatternStats> stats = PackCore.getScamDetector().getPatternStats();

        ctx.getSource().sendFeedback(Text.literal("§e§l[ScamShield Statistics]"));
        ctx.getSource().sendFeedback(Text.literal("§7Total patterns: §f" + stats.size()));

        long totalMatches = stats.values().stream().mapToLong(PatternStats::getMatchCount).sum();
        long totalTimeouts = stats.values().stream().mapToLong(PatternStats::getTimeoutCount).sum();

        ctx.getSource().sendFeedback(Text.literal("§7Total matches: §f" + totalMatches));
        ctx.getSource().sendFeedback(Text.literal("§7Total timeouts: §c" + totalTimeouts));

        // Show top 5 most-matched patterns with more detail
        ctx.getSource().sendFeedback(Text.literal("\n§e§lTop 5 Most Matched Patterns:"));
        stats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().getMatchCount(), a.getValue().getMatchCount()))
                .limit(5)
                .forEach(entry -> {
                    PatternStats stat = entry.getValue();
                    String timeSinceFirst = stat.getFirstMatchTimestamp() > 0
                            ? formatDuration(System.currentTimeMillis() - stat.getFirstMatchTimestamp())
                            : "never";

                    ctx.getSource().sendFeedback(Text.literal(String.format(
                            "§7  %s: §f%d matches §7(first: %s ago)",
                            entry.getKey(), stat.getMatchCount(), timeSinceFirst
                    )));
                });

        // Show patterns with timeouts (potential problems)
        long patternsWithTimeouts = stats.values().stream()
                .filter(s -> s.getTimeoutCount() > 0)
                .count();

        if (patternsWithTimeouts > 0) {
            ctx.getSource().sendFeedback(Text.literal("\n§c§lWarning: " + patternsWithTimeouts +
                    " pattern(s) have timeouts!"));
        }

        return 1;
    }

    private static int resetStats(CommandContext<FabricClientCommandSource> ctx) {
        ScamDetector detector = PackCore.getScamDetector();

        // Reset all pattern statistics
        for (ScamPattern pattern : detector.getPatterns()) {
            pattern.getStats().reset();
        }

        // Save the reset stats
        detector.saveStats();

        ctx.getSource().sendFeedback(Text.literal("§a[ScamShield] All statistics have been reset"));
        return 1;
    }

    private static int addWhitelist(CommandContext<FabricClientCommandSource> ctx) {
        String player = StringArgumentType.getString(ctx, "player");
        boolean added = ScamShieldWhitelist.getInstance().add(player);

        if (added) {
            ctx.getSource().sendFeedback(Text.literal("§a[ScamShield] Added §f" + player + "§a to whitelist"));
        } else {
            ctx.getSource().sendFeedback(Text.literal("§c[ScamShield] Player already whitelisted"));
        }

        return 1;
    }

    private static int removeWhitelist(CommandContext<FabricClientCommandSource> ctx) {
        String player = StringArgumentType.getString(ctx, "player");
        boolean removed = ScamShieldWhitelist.getInstance().remove(player);

        if (removed) {
            ctx.getSource().sendFeedback(Text.literal("§a[ScamShield] Removed §f" + player + "§a from whitelist"));
        } else {
            ctx.getSource().sendFeedback(Text.literal("§c[ScamShield] Player not found in whitelist"));
        }

        return 1;
    }

    private static int listWhitelist(CommandContext<FabricClientCommandSource> ctx) {
        var players = ScamShieldWhitelist.getInstance().getWhitelistedPlayers();

        if (players.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("§e[ScamShield] Whitelist is empty"));
        } else {
            ctx.getSource().sendFeedback(Text.literal("§e§l[ScamShield Whitelist] §7(" + players.size() + " players)"));
            players.forEach(player ->
                    ctx.getSource().sendFeedback(Text.literal("§7  - §f" + player)));
        }

        return 1;
    }

    private static int clearWhitelist(CommandContext<FabricClientCommandSource> ctx) {
        ScamShieldWhitelist.getInstance().clear();
        ctx.getSource().sendFeedback(Text.literal("§a[ScamShield] Whitelist cleared"));
        return 1;
    }

    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d";
        if (hours > 0) return hours + "h";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }
}