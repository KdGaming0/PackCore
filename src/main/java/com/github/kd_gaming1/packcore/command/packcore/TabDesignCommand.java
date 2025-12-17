package com.github.kd_gaming1.packcore.command.packcore;

import com.github.kd_gaming1.packcore.integration.tabdesign.TabDesignManager;
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

public class TabDesignCommand {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("tabdesign")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("Available types: list, apply")
                            .formatted(Formatting.YELLOW));
                    return 0;
                })
                .then(ClientCommandManager.literal("list")
                        .executes(TabDesignCommand::listTabDesigns))
                .then(ClientCommandManager.literal("apply")
                        .executes(context -> {
                            context.getSource().sendFeedback(Text.literal("Available designs: skyhanni, skyblocker")
                                    .formatted(Formatting.YELLOW));
                            return 0;
                        })
                        .then(ClientCommandManager.argument("design", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("skyhanni");
                                    builder.suggest("skyblocker");
                                    return builder.buildFuture();
                                })
                                .executes(TabDesignCommand::applyTabDesign)));
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
