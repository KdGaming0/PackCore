package com.github.kd_gaming1.packcore.command.packcore;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.github.kd_gaming1.packcore.command.CommandHelper.sendCopyCommand;

public class HelpCommand {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("help").executes(HelpCommand::execute);
    }

    private static int execute(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();

        source.sendFeedback(Text.literal("═══════════════════════════════════").formatted(Formatting.GOLD));
        source.sendFeedback(Text.literal("    PackCore Commands Help").formatted(Formatting.GOLD, Formatting.BOLD));
        source.sendFeedback(Text.literal("═══════════════════════════════════").formatted(Formatting.GOLD));
        source.sendFeedback(Text.literal(""));

        // Setup & Configuration
        source.sendFeedback(Text.literal("⚙ Setup & Configuration:").formatted(Formatting.YELLOW, Formatting.BOLD));
        sendCopyCommand(source, "§a/packcore wizard §7- Open the setup wizard", "/packcore wizard");
        sendCopyCommand(source, "§a/packcore configmanager §7- Open config manager GUI", "/packcore configmanager");
        sendCopyCommand(source, "§a/packcore menu toggle §7- Enable/disable custom menu", "/packcore menu toggle");
        sendCopyCommand(source, "§a/packcore menu enable §7- Enable custom menu", "/packcore menu enable");
        sendCopyCommand(source, "§a/packcore menu disable §7- Disable custom menu", "/packcore menu disable");
        source.sendFeedback(Text.literal(""));

        // Performance & Design
        source.sendFeedback(Text.literal("🚀 Performance & Design:").formatted(Formatting.YELLOW, Formatting.BOLD));
        sendCopyCommand(source, "§a/packcore performance list §7- List performance profiles", "/packcore performance list");
        sendCopyCommand(source, "§a/packcore performance apply <profile> §7- Apply performance profile", "/packcore performance apply");
        sendCopyCommand(source, "§a/packcore tabdesign list §7- List available tab designs", "/packcore tabdesign list");
        sendCopyCommand(source, "§a/packcore tabdesign apply <design> §7- Apply tab design", "/packcore tabdesign apply <design>");
        source.sendFeedback(Text.literal(""));

        // Information
        source.sendFeedback(Text.literal("ℹ Information:").formatted(Formatting.YELLOW, Formatting.BOLD));
        sendCopyCommand(source, "§a/packcore status §7- Show current status", "/packcore status");
        sendCopyCommand(source, "§a/packcore guide §7- Open guide system", "/packcore guide");
        sendCopyCommand(source, "§a/packcore help §7- Show this help message", "/packcore help");

        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("═══════════════════════════════════").formatted(Formatting.GOLD));
        source.sendFeedback(Text.literal("💡 Tip: Click any command to copy it!").formatted(Formatting.GRAY, Formatting.ITALIC));

        return 1;
    }

}