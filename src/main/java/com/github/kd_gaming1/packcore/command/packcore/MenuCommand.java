package com.github.kd_gaming1.packcore.command.packcore;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MenuCommand {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("menu")
                .then(ClientCommandManager.literal("enable")
                        .executes(ctx -> setMenuEnabled(ctx, true)))
                .then(ClientCommandManager.literal("disable")
                        .executes(ctx -> setMenuEnabled(ctx, false)))
                .then(ClientCommandManager.literal("toggle")
                        .executes(MenuCommand::toggleMenu))
                .executes(MenuCommand::showMenuStatus);
    }

    private static int setMenuEnabled(CommandContext<FabricClientCommandSource> context, boolean enabled) {
        PackCoreConfig.enableCustomMenu = enabled;
        PackCoreConfig.write(PackCore.MOD_ID);

        String status = enabled ? "enabled" : "disabled";
        Formatting color = enabled ? Formatting.GREEN : Formatting.RED;

        context.getSource().sendFeedback(
                Text.literal("✓ Custom menu " + status + "!")
                        .formatted(color)
        );

        context.getSource().sendFeedback(
                Text.literal("ℹ Restart the game for changes to take effect.")
                        .formatted(Formatting.YELLOW)
        );

        return 1;
    }

    private static int toggleMenu(CommandContext<FabricClientCommandSource> context) {
        boolean newState = !PackCoreConfig.enableCustomMenu;
        return setMenuEnabled(context, newState);
    }

    private static int showMenuStatus(CommandContext<FabricClientCommandSource> context) {
        boolean enabled = PackCoreConfig.enableCustomMenu;
        String status = enabled ? "Enabled" : "Disabled";
        Formatting color = enabled ? Formatting.GREEN : Formatting.RED;

        context.getSource().sendFeedback(
                Text.literal("Custom Menu Status: ")
                        .formatted(Formatting.YELLOW)
                        .append(Text.literal(status).formatted(color))
        );

        context.getSource().sendFeedback(Text.literal("/packcore menu toggle")
                .formatted(Formatting.YELLOW));

        return 1;
    }
}