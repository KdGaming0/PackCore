package com.kd_gaming1.commands;

import com.kd_gaming1.copysystem.ZipArchiver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Command to create ZIP archives of selected Minecraft files and folders.
 * Usage: /packcore archive <preset|folder_name> [filename]
 * Presets: vanilla-configs, mod-configs, all-configs
 */
public class ArchiveCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("packcore")
                .then(CommandManager.literal("archive")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    // Auto-complete suggestions for presets
                                    String[] presets = {"vanilla-configs", "mod-configs", "all-configs"};
                                    for (String preset : presets) {
                                        builder.suggest(preset);
                                    }
                                    // Could also suggest common folder names
                                    String[] commonFolders = {"config", "resourcepacks", "shaderpacks", "screenshots"};
                                    for (String folder : commonFolders) {
                                        builder.suggest(folder);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> executeArchive(context, null))
                                .then(CommandManager.argument("filename", StringArgumentType.greedyString())
                                        .executes(context -> executeArchive(context,
                                                StringArgumentType.getString(context, "filename")))))));
    }

    private static int executeArchive(CommandContext<ServerCommandSource> context, String customFilename)
            throws CommandSyntaxException {

        String target = StringArgumentType.getString(context, "target");
        ServerCommandSource source = context.getSource();

        // Send initial message
        source.sendFeedback(() -> Text.literal("§6Starting archive creation for: §e" + target), false);

        // Run archive creation asynchronously to avoid blocking the game thread
        CompletableFuture.runAsync(() -> {
            try {
                createArchive(source, target, customFilename);
            } catch (Exception e) {
                source.sendFeedback(() -> Text.literal("§cError creating archive: " + e.getMessage()), false);
                e.printStackTrace();
            }
        });

        return 1;
    }

    private static void createArchive(ServerCommandSource source, String target, String customFilename) {
        File minecraftRoot = FabricLoader.getInstance().getGameDir().toFile();
        File skyblockFolder = new File(minecraftRoot, "Skyblock Enhanced");
        File customConfigFolder = new File(skyblockFolder, "CustomConfigs");

        // Ensure CustomConfigs folder exists
        if (!customConfigFolder.exists()) {
            customConfigFolder.mkdirs();
        }

        // Generate filename if not provided
        String filename = customFilename != null ? customFilename : generateFilename(target);
        if (!filename.endsWith(".zip")) {
            filename += ".zip";
        }

        // Get paths based on target (preset or folder name)
        List<Path> pathsToArchive = getPathsForTarget(target, minecraftRoot, source);

        if (pathsToArchive.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§cNo valid paths found for: " + target), false);
            return;
        }

        // Create the archive
        source.sendFeedback(() -> Text.literal("§6Creating archive with " + pathsToArchive.size() + " items..."), false);

        boolean success = ZipArchiver.createZipArchive(
                filename,
                customConfigFolder.getAbsolutePath(),
                pathsToArchive,
                minecraftRoot,
                new ZipArchiver.ArchiveProgressListener() {
                    private int lastReportedProgress = 0;

                    @Override
                    public void onProgress(int percentComplete) {
                        // Report progress every 20%
                        if (percentComplete >= lastReportedProgress + 20) {
                            lastReportedProgress = percentComplete;
                            source.sendFeedback(() -> Text.literal("§6Archive progress: §e" + percentComplete + "%"), false);
                        }
                    }
                }
        );

        if (success) {
            String finalFilename = filename;
            source.sendFeedback(() -> Text.literal("§aArchive created successfully: §f" + finalFilename), false);
            source.sendFeedback(() -> Text.literal("§aLocation: §f" + customConfigFolder.getAbsolutePath()), false);
        } else {
            source.sendFeedback(() -> Text.literal("§cFailed to create archive. Check console for errors."), false);
        }
    }

    private static List<Path> getPathsForTarget(String target, File minecraftRoot, ServerCommandSource source) {
        List<Path> paths = new ArrayList<>();
        Path rootPath = minecraftRoot.toPath();

        switch (target.toLowerCase()) {
            case "vanilla-configs":
                // Vanilla Minecraft configuration files
                addIfExists(paths, rootPath.resolve("options.txt"), source);
                addIfExists(paths, rootPath.resolve("servers.dat"), source);
                break;

            case "mod-configs":
                // Only the config folder (mod configurations)
                addIfExists(paths, rootPath.resolve("config"), source);
                break;

            case "all-configs":
                // Both vanilla and mod configs
                addIfExists(paths, rootPath.resolve("options.txt"), source);
                addIfExists(paths, rootPath.resolve("servers.dat"), source);
                addIfExists(paths, rootPath.resolve("config"), source);
                break;

            default:
                // Treat as folder name
                Path targetPath = rootPath.resolve(target);
                if (targetPath.toFile().exists()) {
                    paths.add(targetPath);
                    source.sendFeedback(() -> Text.literal("§6Found folder: §e" + target), false);
                } else {
                    source.sendFeedback(() -> Text.literal("§cFolder not found: §e" + target), false);
                }
                break;
        }

        return paths;
    }

    private static void addIfExists(List<Path> paths, Path path, ServerCommandSource source) {
        if (path.toFile().exists()) {
            paths.add(path);
            source.sendFeedback(() -> Text.literal("§6Found: §e" + path.getFileName()), false);
        } else {
            source.sendFeedback(() -> Text.literal("§7Skipped (not found): §e" + path.getFileName()), false);
        }
    }

    private static String generateFilename(String target) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return target.replace("-", "_") + "_backup_" + timestamp;
    }
}