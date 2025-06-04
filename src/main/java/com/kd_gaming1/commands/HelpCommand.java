package com.kd_gaming1.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Help command for PackCore.
 * Usage: /packcore help
 */
public class HelpCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("packcore")
                .executes(HelpCommand::executeHelp)
                .then(CommandManager.literal("help")
                        .executes(HelpCommand::executeHelp)));
    }

    private static final List<Text> HELP_TEXTS = List.of(
        Text.literal("§6=== PackCore Commands ==="),
        Text.literal("§e/packcore §7- Show this help"),
        Text.literal("§e/packcore archive <target> [filename]"),
        Text.literal("  §7Archive presets:"),
        Text.literal("  §f- vanilla-configs §7(options.txt, servers.dat)"),
        Text.literal("  §f- mod-configs §7(config folder only)"),
        Text.literal("  §f- all-configs §7(vanilla + mod configs)"),
        Text.literal("  §7Or specify any folder name"),
        Text.literal(""),
        Text.literal("§e/packcore dialog [true|false]"),
        Text.literal("  §7Enable/disable the config selection dialog"),
        Text.literal(""),
        Text.literal("§7Archives are saved to: §fSkyblock Enhanced/CustomConfigs/")
    );

    private static int executeHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        HELP_TEXTS.forEach(text -> source.sendFeedback(() -> text, false));
        return 1;
    }
}
