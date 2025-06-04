package com.kd_gaming1.commands;

import com.kd_gaming1.config.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Command to configure the dialog window settings.
 * Usage: /packcore dialog <true|false>
 */
public class DialogConfigCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("packcore")
                .then(CommandManager.literal("dialog")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(DialogConfigCommand::executeDialogConfig))
                        .executes(DialogConfigCommand::showDialogStatus)));
    }

    private static int executeDialogConfig(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ServerCommandSource source = context.getSource();

        // Update the config
        ModConfig.setPromptSetDefaultConfig(enabled);

        // Send feedback
        String status = enabled ? "§aenabled" : "§cdisabled";
        source.sendFeedback(() -> Text.literal("§6Dialog window has been " + status + "§6."), false);

        if (enabled) {
            source.sendFeedback(() -> Text.literal("§7The selection dialog will now appear on next startup if multiple config files are found."), false);
        } else {
            source.sendFeedback(() -> Text.literal("§7The selection dialog will be skipped and auto-extract single configs."), false);
        }

        return 1;
    }

    private static int showDialogStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        boolean currentStatus = ModConfig.getPromptSetDefaultConfig();

        String status = currentStatus ? "§aenabled" : "§cdisabled";
        source.sendFeedback(() -> Text.literal("§6Dialog window is currently " + status + "§6."), false);
        source.sendFeedback(() -> Text.literal("§7Use §f/packcore dialog <true|false> §7to change this setting."), false);

        return 1;
    }
}