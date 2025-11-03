package com.github.kd_gaming1.packcore.command.packcore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PackCoreCommand {

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("packcore")
                .executes(PackCoreCommand::showQuickHelp)
                .then(HelpCommand.register())
                .then(WizardCommand.register())
                .then(MenuCommand.register())
                .then(GuideCommand.register())
                .then(ConfigManagerCommand.register())
                .then(StatusCommand.register())
                .then(PerformanceCommand.register())
                .then(TabDesignCommand.register())
        );
    }

    private static int showQuickHelp(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();

        source.sendFeedback(Text.literal("PackCore Commands").formatted(Formatting.GOLD, Formatting.BOLD));
        source.sendFeedback(Text.literal("Type ").formatted(Formatting.GRAY)
                .append(Text.literal("/packcore help").formatted(Formatting.GREEN))
                .append(Text.literal(" for full command list").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("Quick Commands:").formatted(Formatting.YELLOW));
        source.sendFeedback(Text.literal("  • ").formatted(Formatting.GRAY)
                .append(Text.literal("/packcore wizard").formatted(Formatting.GREEN))
                .append(Text.literal(" - Open setup wizard").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("  • ").formatted(Formatting.GRAY)
                .append(Text.literal("/packcore menu toggle").formatted(Formatting.GREEN))
                .append(Text.literal(" - Toggle custom menu").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("  • ").formatted(Formatting.GRAY)
                .append(Text.literal("/packcore configmanager").formatted(Formatting.GREEN))
                .append(Text.literal(" - Open config manager").formatted(Formatting.GRAY)));

        return 1;
    }
}