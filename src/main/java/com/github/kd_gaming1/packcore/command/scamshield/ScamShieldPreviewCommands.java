package com.github.kd_gaming1.packcore.command.scamshield;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.ScamWarningMessageBuilder;
import com.github.kd_gaming1.packcore.scamshield.ScamShieldScreenIntegration;
import com.github.kd_gaming1.packcore.scamshield.detector.ConfidenceLevel;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamCategory;
import com.github.kd_gaming1.packcore.ui.screen.scamshield.ScamWarningScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Commands for previewing ScamShield warnings and screens.
 */
public class ScamShieldPreviewCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("preview")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("Available severity: high, medium, low, screen")
                            .formatted(Formatting.YELLOW));
                    return 0;
                })

                .then(registerLowPreview())
                .then(registerMediumPreview())
                .then(registerHighPreview())
                .then(ClientCommandManager.literal("screen")
                        .executes(ScamShieldPreviewCommands::previewWarningScreen)
                );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> registerLowPreview() {
        return ClientCommandManager.literal("low")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("Available types: coop, giveaway, phishing, trade, generic")
                            .formatted(Formatting.YELLOW));
                    return 0;
                })
                .then(ClientCommandManager.literal("phishing")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.LOW, ScamCategory.PHISHING))
                )
                .then(ClientCommandManager.literal("giveaway")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.LOW, ScamCategory.FAKE_REWARD))
                )
                .then(ClientCommandManager.literal("coop")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.LOW, ScamCategory.ACCOUNT_THEFT))
                )
                .then(ClientCommandManager.literal("generic")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.LOW, ScamCategory.CUSTOM))
                );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> registerMediumPreview() {
        return ClientCommandManager.literal("medium")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("Available types: coop, giveaway, phishing, trade, generic")
                            .formatted(Formatting.YELLOW));
                    return 0;
                })
                .then(ClientCommandManager.literal("phishing")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.MEDIUM, ScamCategory.PHISHING))
                )
                .then(ClientCommandManager.literal("giveaway")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.MEDIUM, ScamCategory.FAKE_REWARD))
                )
                .then(ClientCommandManager.literal("coop")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.MEDIUM, ScamCategory.ACCOUNT_THEFT))
                )
                .then(ClientCommandManager.literal("trade")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.MEDIUM, ScamCategory.TRADE_MANIPULATION))
                )
                .then(ClientCommandManager.literal("generic")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.MEDIUM, ScamCategory.CUSTOM))
                );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> registerHighPreview() {
        return ClientCommandManager.literal("high")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("Available types: coop, giveaway, phishing, trade, generic")
                            .formatted(Formatting.YELLOW));
                    return 0;
                })
                .then(ClientCommandManager.literal("phishing")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.HIGH, ScamCategory.PHISHING))
                )
                .then(ClientCommandManager.literal("giveaway")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.HIGH, ScamCategory.FAKE_REWARD))
                )
                .then(ClientCommandManager.literal("coop")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.HIGH, ScamCategory.ACCOUNT_THEFT))
                )
                .then(ClientCommandManager.literal("trade")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.HIGH, ScamCategory.TRADE_MANIPULATION))
                )
                .then(ClientCommandManager.literal("generic")
                        .executes(ctx -> previewWarning(ctx, ConfidenceLevel.HIGH, ScamCategory.CUSTOM))
                );
    }

    private static int previewWarning(CommandContext<FabricClientCommandSource> context,
                                      ConfidenceLevel level, ScamCategory category) {
        FabricClientCommandSource source = context.getSource();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            source.sendError(Text.literal("§c[ScamShield] Must be in-game to preview warnings"));
            return 0;
        }

        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        source.sendFeedback(Text.literal("§e[Preview Mode] " + level.getDisplayName() + " Confidence - " + category.getDisplayName()));
        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        source.sendFeedback(Text.literal(""));

        // Create a mock detection result
        DetectionResult.Builder builder = new DetectionResult.Builder(
                "Example scam message for preview",
                "PreviewScammer",
                PackCoreConfig.scamShieldTriggerThreshold
        );

        // Add score to match the confidence level
        builder.addScamTypeContribution(category.getScamTypeId(), level.getMinScore());

        DetectionResult mockResult = builder.build();

        // Build and send the warning message
        Text warningMessage = ScamWarningMessageBuilder.buildWarningMessage(mockResult);
        client.player.sendMessage(warningMessage, false);

        source.sendFeedback(Text.literal(""));
        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        source.sendFeedback(Text.literal("§7This is a preview. Real detections will look like this."));

        return 1;
    }

    private static int previewWarningScreen(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            source.sendError(Text.literal("§c[ScamShield] Must be in-game to preview screen"));
            return 0;
        }

        source.sendFeedback(Text.literal("§e[ScamShield] §7Opening warning screen preview..."));

        // Create a mock HIGH confidence detection
        DetectionResult.Builder builder = new DetectionResult.Builder(
                "join my discord to verify your account and get free rewards!",
                "PreviewScammer",
                PackCoreConfig.scamShieldTriggerThreshold
        );

        builder.addScamTypeContribution("discord_verify_scam", 150);
        builder.addScamTypeContribution("credential_fishing", 100);

        DetectionResult mockResult = builder.build();

        // Open the warning screen
        try {
            ScamWarningScreen.ScamWarning warning =
                    ScamShieldScreenIntegration.convertToWarning(mockResult);

            ScamWarningScreen warningScreen =
                    new ScamWarningScreen(warning, () -> {
                        source.sendFeedback(Text.literal("§a[ScamShield] Preview screen dismissed"));
                    });

            client.send(() -> client.setScreen(warningScreen));

        } catch (Exception e) {
            source.sendError(Text.literal("§c[ScamShield] Failed to open preview screen: " + e.getMessage()));
            PackCore.LOGGER.error("[ScamShield] Preview screen error", e);
            return 0;
        }

        return 1;
    }
}