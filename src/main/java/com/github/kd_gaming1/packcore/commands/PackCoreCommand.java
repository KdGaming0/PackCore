package com.github.kd_gaming1.packcore.commands;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.help.guide.BaseGuidePage;
import com.github.kd_gaming1.packcore.gui.configscreen.ModpackConfigMenuScreen;
import com.github.kd_gaming1.packcore.util.ConfigFileUtils;
import com.github.kd_gaming1.packcore.util.config.PerformanceProfileUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

public class PackCoreCommand {

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("packcore")
                .then(ClientCommandManager.literal("guide")
                        .executes(PackCoreCommand::openGuide))
                .then(ClientCommandManager.literal("configmanager")
                        .executes(PackCoreCommand::openConfig))
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
                        }))
                .then(ClientCommandManager.literal("performance")
                        .then(ClientCommandManager.literal("list")
                                .executes(PackCoreCommand::listPerformanceProfiles))
                        .then(ClientCommandManager.literal("apply")
                                .then(ClientCommandManager.argument("profile", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("performance");
                                            builder.suggest("balanced");
                                            builder.suggest("quality");
                                            builder.suggest("shaders");
                                            return builder.buildFuture();
                                        })
                                        .executes(PackCoreCommand::applyPerformanceProfile)))));
    }

    private static int openGuide(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();

        if (client == null) {
            context.getSource().sendError(Text.literal("Unable to access Minecraft client"));
            return 0;
        }

        /*
            After executing a command, the current screen will be closed (the chat hud).
            And if you open a new screen in a command, that new screen will be closed
            instantly along with the chat hud. Slightly delaying the opening of the
            screen fixes this issue.
         */
        client.send(() -> {
            try {
                client.setScreen(new BaseGuidePage());
            } catch (Exception e) {
                PackCore.LOGGER.error("Failed to open guide: " + e.getMessage());
            }
        });

        return 1;
    }

    private static int openConfig(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();

        if (client == null) {
            context.getSource().sendError(Text.literal("Unable to access Minecraft client"));
            return 0;
        }

        /*
            After executing a command, the current screen will be closed (the chat hud).
            And if you open a new screen in a command, that new screen will be closed
            instantly along with the chat hud. Slightly delaying the opening of the
            screen fixes this issue.
         */
        client.send(() -> {
            try {
                client.setScreen(new ModpackConfigMenuScreen());
            } catch (Exception e) {
                PackCore.LOGGER.error("Failed to open config: " + e.getMessage());
            }
        });

        return 1;
    }

    private static int applyPerformanceProfile(CommandContext<FabricClientCommandSource> context) {
        String profileName = StringArgumentType.getString(context, "profile").toLowerCase();

        PerformanceProfileUtil.PerformanceProfile profile;

        // Map string to enum
        switch (profileName) {
            case "performance" -> profile = PerformanceProfileUtil.PerformanceProfile.PERFORMANCE;
            case "balanced" -> profile = PerformanceProfileUtil.PerformanceProfile.BALANCED;
            case "quality" -> profile = PerformanceProfileUtil.PerformanceProfile.QUALITY;
            case "shaders" -> profile = PerformanceProfileUtil.PerformanceProfile.SHADERS;
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
                PerformanceProfileUtil.ProfileResult result = PerformanceProfileUtil.applyPerformanceProfile(profile);

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
                MinecraftClient.getInstance().execute(() -> {
                    context.getSource().sendError(Text.literal("✗ Failed to apply performance profile: " + e.getMessage())
                            .formatted(Formatting.RED));
                });
            }
        });

        return 1;
    }

    private static int listPerformanceProfiles(CommandContext<FabricClientCommandSource> context) {
        var availability = PerformanceProfileUtil.getSystemAvailability();

        context.getSource().sendFeedback(Text.literal("=== PackCore Performance Profiles ===")
                .formatted(Formatting.GOLD));

        // Show available systems
        context.getSource().sendFeedback(Text.literal("Available Systems:")
                .formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("  • Minecraft: ✓")
                .formatted(Formatting.GREEN));
        context.getSource().sendFeedback(Text.literal("  • Sodium: " + (availability.isSodiumAvailable() ? "✓" : "✗"))
                .formatted(availability.isSodiumAvailable() ? Formatting.GREEN : Formatting.RED));
        context.getSource().sendFeedback(Text.literal("  • Iris/Shaders: " + (availability.isIrisAvailable() ? "✓" : "✗"))
                .formatted(availability.isIrisAvailable() ? Formatting.GREEN : Formatting.RED));

        context.getSource().sendFeedback(Text.literal(""));

        // Show available profiles
        context.getSource().sendFeedback(Text.literal("Available Profiles:")
                .formatted(Formatting.YELLOW));

        for (PerformanceProfileUtil.PerformanceProfile profile : PerformanceProfileUtil.PerformanceProfile.values()) {
            String command = "/packcore performance apply " + profile.name().toLowerCase();
            context.getSource().sendFeedback(Text.literal("  • " + profile.getDisplayName() + " - " + profile.getDescription())
                    .formatted(Formatting.WHITE));
            context.getSource().sendFeedback(Text.literal("    Command: " + command)
                    .formatted(Formatting.GRAY));
        }

        return 1;
    }
}