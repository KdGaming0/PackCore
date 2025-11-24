package com.github.kd_gaming1.packcore.command.scamshield;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Main entry point for all ScamShield commands.
 * Delegates to specialized command classes for organization.
 */
public class ScamShieldCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("scamshield")
                    .executes(ScamShieldHelpCommand::execute)

                .then(ScamShieldHelpCommand.register())
                .then(ScamShieldControlCommands.registerToggle())
                .then(ScamShieldControlCommands.registerReload())
                .then(ScamShieldStatsCommands.registerStats())
                .then(ScamShieldStatsCommands.registerClear())
                .then(ScamShieldTestCommands.registerTest())
                .then(ScamShieldTestCommands.registerDebug())
                .then(ScamShieldWhitelistCommands.register())
                .then(ScamShieldPreviewCommands.register())
                .then(ScamShieldEducationCommand.register())
        );
    }

}