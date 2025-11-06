package com.github.kd_gaming1.packcore.command.scamshield;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/**
 * Main entry point for all ScamShield commands.
 * Delegates to specialized command classes for organization.
 */
public class ScamShieldCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("scamshield")
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