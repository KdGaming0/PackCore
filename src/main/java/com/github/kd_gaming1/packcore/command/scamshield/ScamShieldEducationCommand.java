package com.github.kd_gaming1.packcore.command.scamshield;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.ui.screen.scamshield.ScamEducationScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Command for opening the ScamShield education screen.
 */
public class ScamShieldEducationCommand {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("education").executes(ScamShieldEducationCommand::execute);
    }

    private static int execute(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            source.sendError(Text.literal("§c[ScamShield] Must be in-game to open education screen"));
            return 0;
        }

        source.sendFeedback(Text.literal("§e[ScamShield] §7Opening education screen..."));

        try {
            // Open the education screen
            client.send(() -> {
                client.setScreen(new ScamEducationScreen(null));
            });
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("§c[ScamShield] Failed to open education screen: " + e.getMessage()));
            PackCore.LOGGER.error("[ScamShield] Education screen error", e);
            return 0;
        }
    }
}