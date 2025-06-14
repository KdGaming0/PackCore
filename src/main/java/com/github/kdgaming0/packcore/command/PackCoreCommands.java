package com.github.kdgaming0.packcore.command;

import net.minecraftforge.client.ClientCommandHandler;

/**
 * Registers all PackCore commands with the Forge command system.
 */
public class PackCoreCommands {

    public static void registerCommands() {
        ClientCommandHandler.instance.registerCommand(new HelpCommand());
        ClientCommandHandler.instance.registerCommand(new ArchiveCommand());
        ClientCommandHandler.instance.registerCommand(new DialogConfigCommand());
    }
}