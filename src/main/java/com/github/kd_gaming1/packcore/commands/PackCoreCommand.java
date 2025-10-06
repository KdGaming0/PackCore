package com.github.kd_gaming1.packcore.commands;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.help.guide.BaseGuidePage;
import com.github.kd_gaming1.packcore.gui.configscreen.ModpackConfigMenuScreen;
import com.github.kd_gaming1.packcore.util.ConfigFileUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PackCoreCommand {

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("packcore")
                .then(ClientCommandManager.literal("guide")
                        .executes(PackCoreCommand::openGuide)));

        dispatcher.register(ClientCommandManager.literal("packcore")
                .then(ClientCommandManager.literal("configmanager")
                        .executes(PackCoreCommand::openConfig)));

        dispatcher.register(ClientCommandManager.literal("packcore")
                .then(ClientCommandManager.literal("status")
                    .executes(context -> {
                        var source = context.getSource();
                        var modpackInfo = PackCore.getModpackInfo();
                        var currentConfig = ConfigFileUtils.getCurrentConfig();

                        source.sendFeedback(Text.literal("=== PackCore Status ===").formatted(Formatting.GOLD));
                        source.sendFeedback(Text.literal("Modpack: " + modpackInfo.getName() + " v" + modpackInfo.getVersion()));
                        source.sendFeedback(Text.literal("Active Config: " + currentConfig.getName() + " v" + currentConfig.getVersion()));
                        source.sendFeedback(Text.literal("Custom Menu: " + (PackCoreConfig.enableCustomMenu ? "Enabled" : "Disabled")));
                        source.sendFeedback(Text.literal("Config Applied: " + (PackCoreConfig.defaultConfigSuccessfullyApplied ? "Yes" : "No")));

                        return 1;
                    })));
    }

    private static int openGuide(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            client.setScreen(new BaseGuidePage());
        });

        context.getSource().sendFeedback(Text.literal("Opening PackCore Guide..."));
        return 1;
    }

    private static int openConfig(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            client.setScreen(new ModpackConfigMenuScreen());
        });

        context.getSource().sendFeedback(Text.literal("Opening Modpack Configuration..."));
        return 1;
    }
}