package com.github.kd_gaming1.packcore.command.packcore;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class StatusCommand {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("status").executes(StatusCommand::execute);
    }

    private static int execute(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();
        var modpackInfo = PackCore.getModpackInfo();
        var currentConfig = ConfigFileRepository.getCurrentConfig();

        source.sendFeedback(Text.literal("=== PackCore Status ===").formatted(Formatting.GOLD));
        source.sendFeedback(Text.literal("Modpack: " + modpackInfo.getName() + " v" + modpackInfo.getVersion()));
        source.sendFeedback(Text.literal("Active Config: " + currentConfig.getName() + " v" + currentConfig.getVersion()));
        source.sendFeedback(Text.literal("Custom Menu: " + (PackCoreConfig.enableCustomMenu ? "Enabled" : "Disabled")));
        source.sendFeedback(Text.literal("Config Applied: " + (PackCoreConfig.defaultConfigSuccessfullyApplied ? "Yes" : "No")));

        return 1;
    }
}