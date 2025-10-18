package com.github.kd_gaming1.packcore.command;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.ui.help.guide.GuideListScreen;
import com.github.kd_gaming1.packcore.ui.screen.configmanager.ConfigManagerScreen;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.github.kd_gaming1.packcore.integration.minecraft.PerformanceProfileService;
import com.github.kd_gaming1.packcore.integration.tabdesign.TabDesignManager;
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
                            var currentConfig = ConfigFileRepository.getCurrentConfig();

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
                                        .executes(PackCoreCommand::applyPerformanceProfile))))
                .then(ClientCommandManager.literal("tabdesign")
                        .then(ClientCommandManager.literal("list")
                                .executes(PackCoreCommand::listTabDesigns))
                        .then(ClientCommandManager.literal("apply")
                                .then(ClientCommandManager.argument("design", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("skyhanni");
                                            builder.suggest("skyblocker");
                                            return builder.buildFuture();
                                        })
                                        .executes(PackCoreCommand::applyTabDesign)))));
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
                client.setScreen(new GuideListScreen());
            } catch (Exception e) {
                PackCore.LOGGER.error("Failed to open guide: {}", e.getMessage());
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
                client.setScreen(new ConfigManagerScreen());
            } catch (Exception e) {
                PackCore.LOGGER.error("Failed to open config: {}", e.getMessage());
            }
        });

        return 1;
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
        var availability = PerformanceProfileService.getSystemAvailability();

        context.getSource().sendFeedback(Text.literal("=== PackCore Performance Profiles ===")
                .formatted(Formatting.GOLD));

        // Show available systems
        context.getSource().sendFeedback(Text.literal("Available Systems:")
                .formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("  • Minecraft: ✓")
                .formatted(Formatting.GREEN));
        context.getSource().sendFeedback(Text.literal("  • Sodium: " + (availability.sodiumAvailable() ? "✓" : "✗"))
                .formatted(availability.sodiumAvailable() ? Formatting.GREEN : Formatting.RED));
        context.getSource().sendFeedback(Text.literal("  • Iris/Shaders: " + (availability.irisAvailable() ? "✓" : "✗"))
                .formatted(availability.irisAvailable() ? Formatting.GREEN : Formatting.RED));

        context.getSource().sendFeedback(Text.literal(""));

        // Show available profiles
        context.getSource().sendFeedback(Text.literal("Available Profiles:")
                .formatted(Formatting.YELLOW));

        for (PerformanceProfileService.PerformanceProfile profile : PerformanceProfileService.PerformanceProfile.values()) {
            String command = "/packcore performance apply " + profile.name().toLowerCase();
            context.getSource().sendFeedback(Text.literal("  • " + profile.getDisplayName() + " - " + profile.getDescription())
                    .formatted(Formatting.WHITE));
            context.getSource().sendFeedback(Text.literal("    Command: " + command)
                    .formatted(Formatting.GRAY));
        }

        return 1;
    }

    private static int applyTabDesign(CommandContext<FabricClientCommandSource> context) {
        String designName = StringArgumentType.getString(context, "design").toLowerCase();

        // Validate the design name
        if (!designName.equals("skyhanni") && !designName.equals("skyblocker")) {
            context.getSource().sendError(Text.literal("Unknown tab design: " + designName)
                    .formatted(Formatting.RED));
            context.getSource().sendFeedback(Text.literal("Available designs: skyhanni, skyblocker")
                    .formatted(Formatting.YELLOW));
            return 0;
        }

        // Check mod availability
        TabDesignManager.TabDesignAvailability availability = TabDesignManager.getAvailability();

        if (designName.equals("skyhanni") && !availability.isSkyHanniAvailable()) {
            context.getSource().sendError(Text.literal("✗ SkyHanni mod is not installed!")
                    .formatted(Formatting.RED));
            return 0;
        }

        if (designName.equals("skyblocker") && !availability.isSkyblockerAvailable()) {
            context.getSource().sendError(Text.literal("✗ Skyblocker mod is not installed!")
                    .formatted(Formatting.RED));
            return 0;
        }

        String displayName = designName.equals("skyhanni") ? "SkyHanni Compact" : "Skyblocker Fancy";
        context.getSource().sendFeedback(Text.literal("Applying tab design: " + displayName + "...")
                .formatted(Formatting.YELLOW));

        // Apply the tab design asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                boolean success = TabDesignManager.applyTabDesign(designName);

                // Send feedback on main thread
                MinecraftClient.getInstance().execute(() -> {
                    if (success) {
                        context.getSource().sendFeedback(Text.literal("✓ Tab design '" + displayName + "' applied successfully!")
                                .formatted(Formatting.GREEN));

                        if (designName.equals("skyhanni")) {
                            context.getSource().sendFeedback(Text.literal("  ℹ SkyHanni Compact tab list is now active")
                                    .formatted(Formatting.GRAY));
                        } else {
                            context.getSource().sendFeedback(Text.literal("  ℹ Skyblocker Fancy tab HUD is now active")
                                    .formatted(Formatting.GRAY));
                        }
                    } else {
                        context.getSource().sendError(Text.literal("⚠ Failed to apply tab design")
                                .formatted(Formatting.YELLOW));
                        context.getSource().sendFeedback(Text.literal("  The mod may not be loaded properly")
                                .formatted(Formatting.GRAY));
                    }
                });

            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() ->
                        context.getSource().sendError(Text.literal("✗ Failed to apply tab design: " + e.getMessage())
                                .formatted(Formatting.RED)));
            }
        });

        return 1;
    }

    private static int listTabDesigns(CommandContext<FabricClientCommandSource> context) {
        TabDesignManager.TabDesignAvailability availability = TabDesignManager.getAvailability();

        context.getSource().sendFeedback(Text.literal("=== PackCore Tab Designs ===")
                .formatted(Formatting.GOLD));

        // Show available mods
        context.getSource().sendFeedback(Text.literal("Available Mods:")
                .formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("  • SkyHanni: " + (availability.isSkyHanniAvailable() ? "✓" : "✗"))
                .formatted(availability.isSkyHanniAvailable() ? Formatting.GREEN : Formatting.RED));
        context.getSource().sendFeedback(Text.literal("  • Skyblocker: " + (availability.isSkyblockerAvailable() ? "✓" : "✗"))
                .formatted(availability.isSkyblockerAvailable() ? Formatting.GREEN : Formatting.RED));

        context.getSource().sendFeedback(Text.literal(""));

        // Show available designs
        context.getSource().sendFeedback(Text.literal("Available Designs:")
                .formatted(Formatting.YELLOW));

        if (availability.isSkyHanniAvailable()) {
            context.getSource().sendFeedback(Text.literal("  • SkyHanni Compact - Minimalist compact tab list")
                    .formatted(Formatting.WHITE));
            context.getSource().sendFeedback(Text.literal("    Command: /packcore tabdesign apply skyhanni")
                    .formatted(Formatting.GRAY));
        }

        if (availability.isSkyblockerAvailable()) {
            context.getSource().sendFeedback(Text.literal("  • Skyblocker Fancy - Feature-rich tab HUD")
                    .formatted(Formatting.WHITE));
            context.getSource().sendFeedback(Text.literal("    Command: /packcore tabdesign apply skyblocker")
                    .formatted(Formatting.GRAY));
        }

        if (!availability.isSkyHanniAvailable() && !availability.isSkyblockerAvailable()) {
            context.getSource().sendFeedback(Text.literal("  ⚠ No compatible tab design mods found")
                    .formatted(Formatting.RED));
        }

        return 1;
    }
}