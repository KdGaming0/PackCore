package com.github.kd_gaming1.packcore.command.packcore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.github.kd_gaming1.packcore.command.CommandHelper.sendCopyCommand;

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

        sendCopyCommand(source, "§7Type §a/packcore help §7for full command list", "/packcore help");

        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("Quick Commands:").formatted(Formatting.YELLOW));

        sendCopyCommand(source, "§7 • §a/packcore wizard §7- Open setup wizard", "/packcore wizard");
        sendCopyCommand(source, "§7 • §a/packcore menu toggle §7- Toggle custom menu", "/packcore menu toggle");
        sendCopyCommand(source, "§7 • §a/packcore configmanager §7- Open config manager", "/packcore configmanager");


        return 1;
    }
}