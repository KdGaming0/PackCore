package com.github.kd_gaming1.packcore.command.scamshield;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.github.kd_gaming1.packcore.command.CommandHelper.sendCopyCommand;

/**
 * Displays help information for ScamShield commands.
 */
public class ScamShieldHelpCommand {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("help").executes(ScamShieldHelpCommand::execute);
    }

    public static int execute(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();

        source.sendFeedback(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.GRAY));
        source.sendFeedback(Text.literal("ScamShield Commands").formatted(Formatting.YELLOW, Formatting.BOLD));
        source.sendFeedback(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.GRAY));
        source.sendFeedback(Text.literal(""));

        // System Control
        source.sendFeedback(Text.literal("System Control:").formatted(Formatting.GOLD, Formatting.BOLD));
        sendCopyCommand(source, "§e/scamshield toggle §7- Enable/disable ScamShield", "/scamshield toggle");
        sendCopyCommand(source, "§e/scamshield reload §7- Reload pattern files", "/scamshield reload");
        source.sendFeedback(Text.literal(""));

        // Statistics
        source.sendFeedback(Text.literal("Statistics:").formatted(Formatting.GOLD, Formatting.BOLD));
        sendCopyCommand(source, "§e/scamshield stats §7- View detection statistics", "/scamshield stats");
        sendCopyCommand(source, "§e/scamshield clear §7- Clear detection history", "/scamshield clear");
        source.sendFeedback(Text.literal(""));

        // Testing
        source.sendFeedback(Text.literal("Testing:").formatted(Formatting.GOLD, Formatting.BOLD));
        sendCopyCommand(source, "§e/scamshield test <message> §7- Test a message", "/scamshield test <message>");
        sendCopyCommand(source, "§e/scamshield debug §7- Run full debug test suite", "/scamshield debug");
        source.sendFeedback(Text.literal(""));

        // Whitelist
        source.sendFeedback(Text.literal("Whitelist:").formatted(Formatting.GOLD, Formatting.BOLD));
        sendCopyCommand(source, "§e/scamshield whitelist add <player> §7- Add player", "/scamshield whitelist add <player>");
        sendCopyCommand(source, "§e/scamshield whitelist remove <player> §7- Remove player", "/scamshield whitelist remove <player>");
        sendCopyCommand(source, "§e/scamshield whitelist list §7- List all whitelisted", "/scamshield whitelist list");
        sendCopyCommand(source, "§e/scamshield whitelist clear §7- Clear whitelist", "/scamshield whitelist clear");
        source.sendFeedback(Text.literal(""));

        // Preview & Education
        source.sendFeedback(Text.literal("Preview & Education:").formatted(Formatting.GOLD, Formatting.BOLD));
        sendCopyCommand(source, "§e/scamshield preview <level> <type> §7- Preview warnings", "/scamshield preview <level> <type>");
        sendCopyCommand(source, "§e/scamshield preview screen §7- Preview warning screen", "/scamshield previewscreen");
        sendCopyCommand(source, "§e/scamshield education §7- Open education screen", "/scamshield education");
        source.sendFeedback(Text.literal(""));

        source.sendFeedback(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.GRAY));
        source.sendFeedback(Text.literal("Tip: Click any command to copy it!")
                .formatted(Formatting.GRAY, Formatting.ITALIC));

        return 1;
    }

}