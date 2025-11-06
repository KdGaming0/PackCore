package com.github.kd_gaming1.packcore.command.scamshield;

import com.github.kd_gaming1.packcore.scamshield.storage.DetectionStats;
import com.github.kd_gaming1.packcore.scamshield.storage.ScamShieldDataManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

/**
 * Commands for viewing and managing ScamShield statistics.
 */
public class ScamShieldStatsCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> registerStats() {
        return ClientCommandManager.literal("stats").executes(ScamShieldStatsCommands::showStats);
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> registerClear() {
        return ClientCommandManager.literal("clear").executes(ScamShieldStatsCommands::clearHistory);
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
}