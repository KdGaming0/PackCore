package com.github.kd_gaming1.packcore.command.packcore;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        sendCommand(source, "/packcore wizard", "Open the setup wizard");
        sendCommand(source, "/packcore configmanager", "Open config manager GUI");
        sendCommand(source, "/packcore menu toggle", "Enable/disable custom menu");
        sendCommand(source, "/packcore menu enable", "Enable custom menu");
        sendCommand(source, "/packcore menu disable", "Disable custom menu");
        source.sendFeedback(Text.literal(""));

        // Performance & Design
        source.sendFeedback(Text.literal("🚀 Performance & Design:").formatted(Formatting.YELLOW, Formatting.BOLD));
        sendCommand(source, "/packcore performance list", "List performance profiles");
        sendCommand(source, "/packcore performance apply <profile>", "Apply performance profile");
        sendCommand(source, "/packcore tabdesign list", "List available tab designs");
        sendCommand(source, "/packcore tabdesign apply <design>", "Apply tab design");
        source.sendFeedback(Text.literal(""));

        // Information
        source.sendFeedback(Text.literal("ℹ Information:").formatted(Formatting.YELLOW, Formatting.BOLD));
        sendCommand(source, "/packcore status", "Show current status");
        sendCommand(source, "/packcore guide", "Open guide system");
        sendCommand(source, "/packcore help", "Show this help message");

        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("═══════════════════════════════════").formatted(Formatting.GOLD));
        source.sendFeedback(Text.literal("💡 Tip: Click any command to copy it!").formatted(Formatting.GRAY, Formatting.ITALIC));

        return 1;
    }

    private static void sendCommand(FabricClientCommandSource source, String command, String description) {
        MutableText commandText = Text.literal("  " + command)
                .formatted(Formatting.GREEN)
                .styled(style -> style
                        .withClickEvent(new ClickEvent.SuggestCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal("Click to copy command").formatted(Formatting.YELLOW))));

        MutableText descText = Text.literal(" - " + description).formatted(Formatting.GRAY);

        source.sendFeedback(commandText.append(descText));
    }

}