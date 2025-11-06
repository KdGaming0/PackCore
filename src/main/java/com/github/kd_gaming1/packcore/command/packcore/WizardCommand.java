package com.github.kd_gaming1.packcore.command.packcore;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.ui.screen.wizard.pages.WelcomeWizardPage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WizardCommand {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("wizard")
                .executes(WizardCommand::openWizard);
    }

    private static int openWizard(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();

        if (client == null) {
            context.getSource().sendError(Text.literal("Unable to access Minecraft client"));
            return 0;
        }

        context.getSource().sendFeedback(Text.literal("Opening setup wizard...")
                .formatted(Formatting.GREEN));

        client.send(() -> {
            try {
                client.setScreen(new WelcomeWizardPage());
            } catch (Exception e) {
                PackCore.LOGGER.error("Failed to open wizard: {}", e.getMessage());
            }
        });

        return 1;
    }
}