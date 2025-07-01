package com.kd_gaming1.commands;

import com.kd_gaming1.config.PackCoreConfig;
import com.kd_gaming1.copysystem.ZipArchiver;
import eu.midnightdust.lib.config.MidnightConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
 * Unified PackCore command system using MidnightLib for configuration management.
 * Combines all PackCore commands into a single organized system.
 */
public class PackCoreCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerMainCommand(dispatcher);
        });
    }

    private static void registerMainCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("packcore")
                // Main help command
                .executes(PackCoreCommands::showHelp)

                // Help subcommand
                .then(CommandManager.literal("help")
                        .executes(PackCoreCommands::showHelp))

                // Dialog commands
                .then(CommandManager.literal("dialog")
                        .executes(PackCoreCommands::showDialogStatus)
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(PackCoreCommands::executeDialogConfig)))

                // Menu commands
                .then(CommandManager.literal("menu")
                        .executes(PackCoreCommands::showMenuStatus)
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(PackCoreCommands::executeMenuConfig)))

                // Archive commands
                .then(CommandManager.literal("archive")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    // Auto-complete suggestions for presets
                                    String[] presets = {"vanilla-configs", "mod-configs", "all-configs"};
                                    for (String preset : presets) {
                                        builder.suggest(preset);
                                    }
                                    // Common folder suggestions
                                    String[] commonFolders = {"config", "resourcepacks", "shaderpacks", "screenshots"};
                                    for (String folder : commonFolders) {
                                        builder.suggest(folder);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> executeArchive(context, null))
                                .then(CommandManager.argument("filename", StringArgumentType.greedyString())
                                        .executes(context -> executeArchive(context,
                                                StringArgumentType.getString(context, "filename"))))))

                // Config commands
                .then(CommandManager.literal("config")
                        .executes(PackCoreCommands::showConfigStatus)
                        .then(CommandManager.literal("timeout")
                                .then(CommandManager.argument("minutes", IntegerArgumentType.integer(1, 60))
                                        .requires(source -> source.hasPermissionLevel(2)) // Operator permission
                                        .executes(PackCoreCommands::setDialogTimeout)))
                        .then(CommandManager.literal("reload")
                                .requires(source -> source.hasPermissionLevel(2)) // Operator permission
                                .executes(PackCoreCommands::reloadConfig))));
    }

    // ==================== HELP COMMANDS ====================

    private static int showHelp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("§6=== PackCore Commands ==="), false);
        source.sendFeedback(() -> Text.literal("§f/packcore help §7- Show this help message"), false);
        source.sendFeedback(() -> Text.literal(""), false);

        source.sendFeedback(() -> Text.literal("§e§lConfiguration:"), false);
        source.sendFeedback(() -> Text.literal("§f/packcore dialog [true|false] §7- Config selection dialog"), false);
        source.sendFeedback(() -> Text.literal("§f/packcore menu [true|false] §7- Custom main menu"), false);
        source.sendFeedback(() -> Text.literal("§f/packcore config §7- Show all config status"), false);
        source.sendFeedback(() -> Text.literal("§f/packcore config timeout <minutes> §7- Set dialog timeout"), false);
        source.sendFeedback(() -> Text.literal("§f/packcore config reload §7- Reload config from file"), false);
        source.sendFeedback(() -> Text.literal(""), false);

        source.sendFeedback(() -> Text.literal("§e§lArchiving:"), false);
        source.sendFeedback(() -> Text.literal("§f/packcore archive <target> [filename]"), false);
        source.sendFeedback(() -> Text.literal("§7  Archive presets:"), false);
        source.sendFeedback(() -> Text.literal("§7  • §fvanilla-configs §7- options.txt, servers.dat"), false);
        source.sendFeedback(() -> Text.literal("§7  • §fmod-configs §7- config folder only"), false);
        source.sendFeedback(() -> Text.literal("§7  • §fall-configs §7- vanilla + mod configs"), false);
        source.sendFeedback(() -> Text.literal("§7  Or specify any folder name (config, resourcepacks, etc.)"), false);
        source.sendFeedback(() -> Text.literal(""), false);

        source.sendFeedback(() -> Text.literal("§7Archives saved to: §fSkyblock Enhanced/CustomConfigs/"), false);
        source.sendFeedback(() -> Text.literal("§7Config GUI available through ModMenu"), false);

        return 1;
    }

    // ==================== DIALOG COMMANDS ====================

    private static int executeDialogConfig(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ServerCommandSource source = context.getSource();

        // Update the config using MidnightLib
        PackCoreConfig.promptSetDefaultConfig = enabled;
        MidnightConfig.write("packcore");

        // Send feedback
        String status = enabled ? "§aenabled" : "§cdisabled";
        source.sendFeedback(() -> Text.literal("§6Dialog window has been " + status + "§6."), false);

        if (enabled) {
            source.sendFeedback(() -> Text.literal("§7The selection dialog will now appear on next startup if multiple config files are found."), false);
        } else {
            source.sendFeedback(() -> Text.literal("§7The selection dialog will be skipped and auto-extract single configs."), false);
        }

        return 1;
    }

    private static int showDialogStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        boolean currentStatus = PackCoreConfig.promptSetDefaultConfig;

        String status = currentStatus ? "§aenabled" : "§cdisabled";
        source.sendFeedback(() -> Text.literal("§6Dialog window is currently " + status + "§6."), false);
        source.sendFeedback(() -> Text.literal("§7Use §f/packcore dialog <true|false> §7to change this setting."), false);

        return 1;
    }

    // ==================== MENU COMMANDS ====================

    private static int executeMenuConfig(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ServerCommandSource source = context.getSource();

        // Update the config using MidnightLib
        PackCoreConfig.enableCustomMenu = enabled;
        MidnightConfig.write("packcore");

        // Send feedback
        String status = enabled ? "§aenabled" : "§cdisabled";
        source.sendFeedback(() -> Text.literal("§6Custom menu has been " + status + "§6."), false);
        source.sendFeedback(() -> Text.literal("§7Changes will take effect after restarting the game."), false);

        return 1;
    }

    private static int showMenuStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        boolean currentStatus = PackCoreConfig.enableCustomMenu;

        String status = currentStatus ? "§aenabled" : "§cdisabled";
        source.sendFeedback(() -> Text.literal("§6Custom menu is currently " + status + "§6."), false);
        source.sendFeedback(() -> Text.literal("§7Use §f/packcore menu <true|false> §7to change this setting."), false);

        return 1;
    }

    // ==================== ARCHIVE COMMANDS ====================

    private static int executeArchive(CommandContext<ServerCommandSource> context, String customFilename) {
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

    // ==================== CONFIG COMMANDS ====================

    private static int showConfigStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("§6=== PackCore Configuration Status ==="), false);
        source.sendFeedback(() -> Text.literal("§7Dialog Window: " + (PackCoreConfig.promptSetDefaultConfig ? "§aEnabled" : "§cDisabled")), false);
        source.sendFeedback(() -> Text.literal("§7Custom Menu: " + (PackCoreConfig.enableCustomMenu ? "§aEnabled" : "§cDisabled")), false);
        source.sendFeedback(() -> Text.literal("§7Custom Panorama: " + (PackCoreConfig.enableCustomPanorama ? "§aEnabled" : "§cDisabled")), false);
        source.sendFeedback(() -> Text.literal("§7Dialog Timeout: §e" + PackCoreConfig.dialogTimeoutMinutes + " minutes"), false);
        source.sendFeedback(() -> Text.literal("§7Use §f/packcore help §7for more commands."), false);

        return 1;
    }

    private static int setDialogTimeout(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int minutes = IntegerArgumentType.getInteger(context, "minutes");
        ServerCommandSource source = context.getSource();

        PackCoreConfig.dialogTimeoutMinutes = minutes;
        MidnightConfig.write("packcore");

        source.sendFeedback(() -> Text.literal("§6Dialog timeout set to §e" + minutes + " minutes§6."), false);

        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        // Reload the config from file
        MidnightConfig.loadValuesFromJson("packcore");

        source.sendFeedback(() -> Text.literal("§6PackCore configuration reloaded from file."), false);

        return 1;
    }
}