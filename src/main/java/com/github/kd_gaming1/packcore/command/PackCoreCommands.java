package com.github.kd_gaming1.packcore.command;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.configpack.ConfigPackBuilder;
import com.github.kd_gaming1.packcore.configpack.ConfigPackExtractor;
import com.github.kd_gaming1.packcore.configpack.ConfigPackMeta;
import com.github.kd_gaming1.packcore.configpack.ConfigPackScanner;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.github.kd_gaming1.packcore.update.UpdateCache;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.update.UpdateStatus;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class PackCoreCommands {

    private PackCoreCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("packcore")
                        .then(literal("build").executes(ctx -> {
                            build(ctx.getSource());
                            return 1;
                        }))
                        .then(literal("extract").executes(ctx -> {
                            extract(ctx.getSource());
                            return 1;
                        }))
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
        );
    }

    /** Creates a test zip in packcore/user_configs/ from packcore/test_input/. */
    private static void build(FabricClientCommandSource source) {
        Path testInputDir = PackCore.PACKCORE_DIR.resolve("test_input");

        ConfigPackMeta meta = ConfigPackMeta.builder("1.0.0", 1920, 1080)
                .name("Test Pack")
                .author("PackCore Dev")
                .build();

        try {
            ConfigPackBuilder.zipFolder(testInputDir, "test_pack.zip", meta);
            send(source, "Built test_pack.zip from packcore/test_input/");
        } catch (Exception e) {
            sendError(source, "Build failed: " + e.getMessage());
        }
    }

    /** Scans packcore/user_configs/ and extracts the best resolution match. */
    private static void extract(FabricClientCommandSource source) {
        Path userConfigsDir = PackCore.PACKCORE_DIR.resolve("user_configs");

        try {
            List<ConfigPackEntry> packs = new ConfigPackScanner().scanFolder(userConfigsDir);

            if (packs.isEmpty()) {
                sendError(source, "No valid packs found in packcore/user_configs/");
                return;
            }

            // Just extract the first one found for testing purposes
            ConfigPackEntry pack = packs.getFirst();
            ConfigPackExtractor.extractAll(pack.zipPath(), PackCore.PACKCORE_DIR, ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING);

            send(source, "Extracted: " + pack.zipPath().getFileName());
        } catch (Exception e) {
            sendError(source, "Extract failed: " + e.getMessage());
        }
    }

    private static void checkUpdate(FabricClientCommandSource source) {
        send(source, "Checking for updates...");

        CompletableFuture<UpdateStatus> future = UpdateChecker.checkAsync();

        future.thenAccept(status -> {
            Minecraft.getInstance().execute(() -> {
                switch (status.state()) {
                    case UP_TO_DATE -> {
                        send(source, "You are up to date! Version: " + status.installedVersion());
                    }
                    case UPDATE_AVAILABLE -> {
                        send(source, "Update available!");
                        send(source, "Installed: " + status.installedVersion());
                        send(source, "Latest: " + status.latestVersion());

                        if (status.changelog() != null && !status.changelog().isBlank()) {
                            send(source, "Changelog:");
                            send(source, status.changelog());
                        }
                    }
                    case UNKNOWN -> {
                        sendError(source, "Could not determine update status.");
                    }
                }
            });
        });
    }

    private static void resetUpdateCache(FabricClientCommandSource source) {
        UpdateCache.invalidate();
        send(source, "Update cache cleared. Next check will fetch from Modrinth.");
    }

    private static void send(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal("[PackCore] " + message));
    }

    private static void sendError(FabricClientCommandSource source, String message) {
        source.sendError(Component.literal("[PackCore] " + message));
    }
}