package com.github.kd_gaming1.packcore.command.scamshield;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

/**
 * Displays help information for ScamShield commands.
 */
public class ScamShieldHelpCommand {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("help").executes(ScamShieldHelpCommand::execute);
    }

    private static int execute(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();

        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        source.sendFeedback(Text.literal("§e§lScamShield Commands"));
        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        source.sendFeedback(Text.literal(""));

        source.sendFeedback(Text.literal("§6System Control:"));
        source.sendFeedback(Text.literal("  §e/scamshield toggle §7- Enable/disable ScamShield"));
        source.sendFeedback(Text.literal("  §e/scamshield reload §7- Reload pattern files"));
        source.sendFeedback(Text.literal(""));

        source.sendFeedback(Text.literal("§6Statistics:"));
        source.sendFeedback(Text.literal("  §e/scamshield stats §7- View detection statistics"));
        source.sendFeedback(Text.literal("  §e/scamshield clear §7- Clear detection history"));
        source.sendFeedback(Text.literal(""));

        source.sendFeedback(Text.literal("§6Testing:"));
        source.sendFeedback(Text.literal("  §e/scamshield test <message> §7- Test a message"));
        source.sendFeedback(Text.literal("  §e/scamshield debug §7- Run full debug test suite"));
        source.sendFeedback(Text.literal(""));

        source.sendFeedback(Text.literal("§6Whitelist:"));
        source.sendFeedback(Text.literal("  §e/scamshield whitelist add <player> §7- Add player"));
        source.sendFeedback(Text.literal("  §e/scamshield whitelist remove <player> §7- Remove player"));
        source.sendFeedback(Text.literal("  §e/scamshield whitelist list §7- List all whitelisted"));
        source.sendFeedback(Text.literal("  §e/scamshield whitelist clear §7- Clear whitelist"));
        source.sendFeedback(Text.literal(""));

        source.sendFeedback(Text.literal("§6Preview & Education:"));
        source.sendFeedback(Text.literal("  §e/scamshield preview <level> <type> §7- Preview warnings"));
        source.sendFeedback(Text.literal("  §e/scamshield previewscreen §7- Preview warning screen"));
        source.sendFeedback(Text.literal("  §e/scamshield education §7- Open education screen"));
        source.sendFeedback(Text.literal(""));

        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        source.sendFeedback(Text.literal("§7For more info: §fhttps://github.com/kd-gaming1/PackCore"));

        return 1;
    }
}