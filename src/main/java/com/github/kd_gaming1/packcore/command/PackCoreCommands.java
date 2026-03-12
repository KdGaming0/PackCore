package com.github.kd_gaming1.packcore.command;

import com.github.kd_gaming1.packcore.integration.ItemBackgroundManager;
import com.github.kd_gaming1.packcore.integration.PerformanceProfileService;
import com.github.kd_gaming1.packcore.integration.StorageDesignManager;
import com.github.kd_gaming1.packcore.integration.TabDesignManager;
import com.github.kd_gaming1.packcore.update.UpdateCache;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.update.UpdateStatus;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class PackCoreCommands {

    private PackCoreCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("packcore")
                        .then(literal("update")
                                .then(literal("check").executes(ctx -> {
                                    checkUpdate(ctx.getSource());
                                    return 1;
                                }))
                                .then(literal("reset").executes(ctx -> {
                                    resetUpdateCache(ctx.getSource());
                                    return 1;
                                }))
                        )
                        .then(literal("performance")
                                .then(argument("profile", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            Arrays.stream(PerformanceProfileService.PerformanceProfile.values())
                                                    .map(PerformanceProfileService.PerformanceProfile::id)
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "profile");
                                            applyPerformanceProfile(ctx.getSource(), id);
                                            return 1;
                                        })
                                )
                        )
                        .then(literal("tabdesign")
                                .then(literal("compact").executes(ctx -> {
                                    applyTabDesign(ctx.getSource(), TabDesignManager.TabDesign.COMPACT);
                                    return 1;
                                }))
                                .then(literal("fancy").executes(ctx -> {
                                    applyTabDesign(ctx.getSource(), TabDesignManager.TabDesign.FANCY);
                                    return 1;
                                }))
                        )
                        .then(literal("itembg")
                                .then(literal("none").executes(ctx -> {
                                    applyItemBackground(ctx.getSource(), ItemBackgroundManager.ItemBackground.NONE);
                                    return 1;
                                }))
                                .then(literal("circle").executes(ctx -> {
                                    applyItemBackground(ctx.getSource(), ItemBackgroundManager.ItemBackground.CIRCLE);
                                    return 1;
                                }))
                                .then(literal("square").executes(ctx -> {
                                    applyItemBackground(ctx.getSource(), ItemBackgroundManager.ItemBackground.SQUARE);
                                    return 1;
                                }))
                        )
                        .then(literal("storagedesign")
                                .then(literal("overlay").executes(ctx -> {
                                    applyStorageDesign(ctx.getSource(), StorageDesignManager.StorageDesign.OVERLAY);
                                    return 1;
                                }))
                                .then(literal("vanilla").executes(ctx -> {
                                    applyStorageDesign(ctx.getSource(), StorageDesignManager.StorageDesign.VANILLA);
                                    return 1;
                                }))
                        )
        );
    }

    private static void checkUpdate(FabricClientCommandSource source) {
        send(source, "Checking for updates...");

        CompletableFuture<UpdateStatus> future = UpdateChecker.checkAsync();

        future.thenAccept(status -> Minecraft.getInstance().execute(() -> {
            switch (status.state()) {
                case UP_TO_DATE -> send(source, "You are up to date! Version: " + status.installedVersion());
                case UPDATE_AVAILABLE -> {
                    send(source, "Update available!");
                    send(source, "Installed: " + status.installedVersion());
                    send(source, "Latest: " + status.latestVersion());

                    if (status.changelog() != null && !status.changelog().isBlank()) {
                        send(source, "Changelog:");
                        send(source, status.changelog());
                    }
                }
                case UNKNOWN -> sendError(source, "Could not determine update status.");
            }
        }));
    }

    private static void resetUpdateCache(FabricClientCommandSource source) {
        UpdateCache.invalidate();
        send(source, "Update cache cleared. Next check will fetch from Modrinth.");
    }

    private static void applyStorageDesign(FabricClientCommandSource source, StorageDesignManager.StorageDesign design) {
        send(source, "Applying storage design: " + design.name().toLowerCase() + "...");
        boolean success = StorageDesignManager.apply(design);
        if (success) {
            send(source, "Storage design applied: " + design.name().toLowerCase()
                    + ". If not in a world yet, the change will take effect on next world join.");
        } else {
            sendError(source, "Failed to apply storage design: " + design.name().toLowerCase() + ". Firmament may not be loaded — check logs.");
        }
    }

    private static void applyItemBackground(FabricClientCommandSource source, ItemBackgroundManager.ItemBackground background) {
        send(source, "Applying item background: " + background.name().toLowerCase() + "...");
        boolean success = ItemBackgroundManager.apply(background);
        if (success) {
            send(source, "Item background applied: " + background.name().toLowerCase());
        } else {
            sendError(source, "Failed to apply item background: " + background.name().toLowerCase() + ". Skyblocker may not be loaded — check logs.");
        }
    }

    private static void applyTabDesign(FabricClientCommandSource source, TabDesignManager.TabDesign design) {
        send(source, "Applying tab design: " + design.name().toLowerCase() + "...");
        boolean success = TabDesignManager.apply(design);
        if (success) {
            send(source, "Tab design applied: " + design.name().toLowerCase());
        } else {
            sendError(source, "Failed to apply tab design: " + design.name().toLowerCase() + ". Check logs for details.");
        }
    }

    private static void applyPerformanceProfile(FabricClientCommandSource source, String id) {
        PerformanceProfileService.PerformanceProfile profile = Arrays.stream(PerformanceProfileService.PerformanceProfile.values())
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElse(null);

        if (profile == null) {
            sendError(source, "Unknown performance profile: \"" + id + "\". Valid options: "
                    + Arrays.stream(PerformanceProfileService.PerformanceProfile.values())
                    .map(PerformanceProfileService.PerformanceProfile::id)
                    .reduce((a, b) -> a + ", " + b).orElse(""));
            return;
        }

        send(source, "Applying performance profile: " + profile.getDisplayName() + "...");
        boolean success = PerformanceProfileService.applyAll(profile);

        if (success) {
            send(source, "Performance profile applied: " + profile.getDisplayName());
        } else {
            sendError(source, "One or more integrations failed for profile: " + profile.getDisplayName() + ". Check logs for details.");
        }
    }

    private static void send(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal("[PackCore] " + message));
    }

    private static void sendError(FabricClientCommandSource source, String message) {
        source.sendError(Component.literal("[PackCore] " + message));
    }
}