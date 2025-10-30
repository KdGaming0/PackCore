package com.github.kd_gaming1.packcore.command.packcore;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class PackCoreCommand {

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("packcore")
            .then(GuideCommand.register())
            .then(ConfigManagerCommand.register())
            .then(StatusCommand.register())
            .then(PerformanceCommand.register())
            .then(TabDesignCommand.register())
        );
    }
}