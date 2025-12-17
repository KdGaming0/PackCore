package com.github.kd_gaming1.packcore.command.packcore;

import com.github.kd_gaming1.packcore.integration.minecraft.PerformanceProfileService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

import static com.github.kd_gaming1.packcore.command.CommandHelper.sendCopyCommand;

public class PerformanceCommand {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("performance")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("Available types: list, apply")
                            .formatted(Formatting.YELLOW));
                    return 0;
                })
                .then(ClientCommandManager.literal("list")
                        .executes(PerformanceCommand::listPerformanceProfiles))
                .then(ClientCommandManager.literal("apply")
                        .executes(context -> {
                            context.getSource().sendFeedback(Text.literal("Available performance options: performance, balanced, quality, shaders")
                                    .formatted(Formatting.YELLOW));
                            return 0;
                        })
                        .then(ClientCommandManager.argument("profile", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("performance");
                                    builder.suggest("balanced");
                                    builder.suggest("quality");
                                    builder.suggest("shaders");
                                    return builder.buildFuture();
                                })
                                .executes(PerformanceCommand::applyPerformanceProfile)));
    }

    private static int applyPerformanceProfile(CommandContext<FabricClientCommandSource> context) {
        String profileName = StringArgumentType.getString(context, "profile").toLowerCase();

        PerformanceProfileService.PerformanceProfile profile;

        // Map string to enum
        switch (profileName) {
            case "performance" -> profile = PerformanceProfileService.PerformanceProfile.PERFORMANCE;
            case "balanced" -> profile = PerformanceProfileService.PerformanceProfile.BALANCED;
            case "quality" -> profile = PerformanceProfileService.PerformanceProfile.QUALITY;
            case "shaders" -> profile = PerformanceProfileService.PerformanceProfile.SHADERS;
            default -> {
                context.getSource().sendError(Text.literal("Unknown performance profile: " + profileName)
                        .formatted(Formatting.RED));
                context.getSource().sendFeedback(Text.literal("Available profiles: performance, balanced, quality, shaders")
                        .formatted(Formatting.YELLOW));
                return 0;
            }
        }

        context.getSource().sendFeedback(Text.literal("Applying performance profile: " + profile.getDisplayName() + "...")
                .formatted(Formatting.YELLOW));

        // Apply the profile asynchronously to avoid blocking the main thread
        CompletableFuture.runAsync(() -> {
            try {
                PerformanceProfileService.ProfileResult result = PerformanceProfileService.applyPerformanceProfile(profile);

                // Send feedback on main thread
                MinecraftClient.getInstance().execute(() -> {
                    if (result.isFullySuccessful()) {
                        context.getSource().sendFeedback(Text.literal("✓ Performance profile '" + profile.getDisplayName() + "' applied successfully!")
                                .formatted(Formatting.GREEN));

                        // Show what was applied
                        if (result.isVanillaApplied()) {
                            context.getSource().sendFeedback(Text.literal("  ✓ Minecraft settings applied")
                                    .formatted(Formatting.GRAY));
                        }
                        if (result.isSodiumAvailable() && result.isSodiumApplied()) {
                            context.getSource().sendFeedback(Text.literal("  ✓ Sodium settings applied")
                                    .formatted(Formatting.GRAY));
                        }
                        if (result.isIrisAvailable() && result.isIrisApplied()) {
                            context.getSource().sendFeedback(Text.literal("  ✓ Iris/Shader settings applied")
                                    .formatted(Formatting.GRAY));
                        }
                    } else {
                        context.getSource().sendError(Text.literal("⚠ Performance profile applied with some issues:")
                                .formatted(Formatting.YELLOW));

                        if (!result.isVanillaApplied()) {
                            context.getSource().sendError(Text.literal("  ✗ Failed to apply Minecraft settings")
                                    .formatted(Formatting.RED));
                        }
                        if (result.isSodiumAvailable() && !result.isSodiumApplied()) {
                            context.getSource().sendError(Text.literal("  ✗ Failed to apply Sodium settings")
                                    .formatted(Formatting.RED));
                        }
                        if (result.isIrisAvailable() && !result.isIrisApplied()) {
                            context.getSource().sendError(Text.literal("  ✗ Failed to apply Iris/Shader settings")
                                    .formatted(Formatting.RED));
                        }
                    }
                });

            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() ->
                        context.getSource().sendError(Text.literal("✗ Failed to apply performance profile: " + e.getMessage())
                                .formatted(Formatting.RED)));
            }
        });

        return 1;
    }

    private static int listPerformanceProfiles(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();
        var availability = PerformanceProfileService.getSystemAvailability();

        source.sendFeedback(Text.literal("=== PackCore Performance Profiles ===")
                .formatted(Formatting.GOLD));

        // Available systems
        source.sendFeedback(Text.literal("Available Systems:")
                .formatted(Formatting.YELLOW));
        source.sendFeedback(Text.literal("  • Minecraft: ✓")
                .formatted(Formatting.GREEN));
        source.sendFeedback(Text.literal("  • Sodium: " + (availability.sodiumAvailable() ? "✓" : "✗"))
                .formatted(availability.sodiumAvailable() ? Formatting.GREEN : Formatting.RED));
        source.sendFeedback(Text.literal("  • Iris/Shaders: " + (availability.irisAvailable() ? "✓" : "✗"))
                .formatted(availability.irisAvailable() ? Formatting.GREEN : Formatting.RED));

        source.sendFeedback(Text.literal(""));

        // Profiles
        source.sendFeedback(Text.literal("Available Profiles:")
                .formatted(Formatting.YELLOW));

        for (PerformanceProfileService.PerformanceProfile profile
                : PerformanceProfileService.PerformanceProfile.values()) {

            String command = "/packcore performance apply " + profile.name().toLowerCase();

            source.sendFeedback(Text.literal("  • " + profile.getDisplayName())
                    .formatted(Formatting.WHITE, Formatting.BOLD));
            source.sendFeedback(Text.literal("    " + profile.getDescription())
                    .formatted(Formatting.GRAY));

            sendCopyCommand(
                    source,
                    "    §a" + command + " §7- Apply this profile",
                    command
            );
        }

        return 1;
    }

}
