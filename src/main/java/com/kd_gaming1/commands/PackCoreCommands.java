package com.kd_gaming1.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Registers all PackCore commands with the Fabric command system.
 */
public class PackCoreCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            HelpCommand.register(dispatcher);
            ArchiveCommand.register(dispatcher);
            DialogConfigCommand.register(dispatcher);
        });
    }
}