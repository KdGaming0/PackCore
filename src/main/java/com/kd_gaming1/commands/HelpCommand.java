package com.kd_gaming1.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

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

    private static int executeHelp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("§6=== PackCore Commands ==="), false);
        source.sendFeedback(() -> Text.literal("§e/packcore §7- Show this help"), false);
        source.sendFeedback(() -> Text.literal("§e/packcore archive <target> [filename]"), false);
        source.sendFeedback(() -> Text.literal("  §7Archive presets:"), false);
        source.sendFeedback(() -> Text.literal("  §f- options §7(options.txt, servers.dat)"), false);
        source.sendFeedback(() -> Text.literal("  §f- vanilla-configs §7(all vanilla MC configs)"), false);
        source.sendFeedback(() -> Text.literal("  §f- mod-configs §7(config folder only)"), false);
        source.sendFeedback(() -> Text.literal("  §f- all-configs §7(vanilla + mod configs)"), false);
        source.sendFeedback(() -> Text.literal("  §7Or specify any folder name"), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("§e/packcore dialog [true|false]"), false);
        source.sendFeedback(() -> Text.literal("  §7Enable/disable the config selection dialog"), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("§7Archives are saved to: §fSkyblock Enhanced/CustomConfigs/"), false);

        return 1;
    }
}