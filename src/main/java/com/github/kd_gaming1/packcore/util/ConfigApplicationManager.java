package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.wizard.copysystem.UnzipFiles;
import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simplified config application manager
 * Handles applying configs on game restart
 */
public class ConfigApplicationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigApplicationManager.class);
    private static final String PENDING_CONFIG_FILE = "packcore_pending_config.json";
    private static final Gson GSON = new Gson();

    /**
     * Schedule a config to be applied on next game start
     */
    public static void scheduleConfigApplication(ConfigFileUtils.ConfigFile config) {
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path pendingFile = gameDir.resolve(PENDING_CONFIG_FILE);

            // Create pending config info
            PendingConfig pending = new PendingConfig(
                    config.getPath().toString(),
                    config.getDisplayName(),
                    config.getMetadata()
            );

            // Write to file
            String json = GSON.toJson(pending);
            Files.writeString(pendingFile, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            LOGGER.info("Scheduled config for application: {}", config.getDisplayName());

            // Schedule game shutdown
            MinecraftClient.getInstance().scheduleStop();

        } catch (IOException e) {
            LOGGER.error("Failed to schedule config application", e);
            throw new RuntimeException("Failed to prepare config application", e);
        }
    }

    /**
     * Check and apply pending config during pre-launch
     * @return true if a config was applied
     */
    public static boolean checkAndApplyPendingConfig(Path gameDir) {
        Path pendingFile = gameDir.resolve(PENDING_CONFIG_FILE);

        if (!Files.exists(pendingFile)) {
            return false;
        }

        try {
            // Read pending config info
            String json = Files.readString(pendingFile, StandardCharsets.UTF_8);
            PendingConfig pending = GSON.fromJson(json, PendingConfig.class);

            if (pending == null || pending.configPath == null) {
                LOGGER.warn("Invalid pending config file");
                Files.deleteIfExists(pendingFile);
                return false;
            }

            LOGGER.info("Found pending config: {}", pending.configName);

            // Create backup before applying
            createBackup(gameDir);

            // Apply the config
            boolean success = applyConfig(Path.of(pending.configPath), gameDir);

            if (success) {
                // Save the metadata as current config
                ConfigFileUtils.saveCurrentConfig(pending.metadata);
                LOGGER.info("Successfully applied config: {}", pending.configName);
            } else {
                LOGGER.error("Failed to apply config: {}", pending.configName);
            }

            // Clean up pending file
            Files.deleteIfExists(pendingFile);

            return success;

        } catch (Exception e) {
            LOGGER.error("Error processing pending config", e);
            try {
                Files.deleteIfExists(pendingFile);
            } catch (IOException ex) {
                LOGGER.warn("Failed to clean up pending file", ex);
            }
            return false;
        }
    }

    private static boolean applyConfig(Path configZipPath, Path gameDir) {
        try {
            if (!Files.exists(configZipPath)) {
                LOGGER.error("Config file not found: {}", configZipPath);
                return false;
            }

            // Extract config zip to game directory
            UnzipFiles unzipper = new UnzipFiles();
            unzipper.unzip(
                    configZipPath.toString(),
                    gameDir.toString(),
                    (bytesProcessed, totalBytes, percentage) -> {
                        if (percentage % 25 == 0) {
                            LOGGER.info("Extraction progress: {}%", percentage);
                        }
                    }
            );

            LOGGER.info("Config extraction completed");
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to extract config", e);
            return false;
        }
    }

    private static void createBackup(Path gameDir) {
        try {
            Path backupDir = gameDir.resolve("packcore/backups");
            Files.createDirectories(backupDir);

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            Path backupPath = backupDir.resolve("config_backup_" + timestamp);
            Files.createDirectories(backupPath);

            // Backup key configuration files and folders
            backupIfExists(gameDir.resolve("config"), backupPath.resolve("config"));
            backupIfExists(gameDir.resolve("options.txt"), backupPath.resolve("options.txt"));
            backupIfExists(gameDir.resolve("servers.dat"), backupPath.resolve("servers.dat"));

            // Also backup current metadata if it exists
            Path currentMetadata = gameDir.resolve(ConfigFileUtils.METADATA_FILE);
            if (Files.exists(currentMetadata)) {
                Files.copy(currentMetadata, backupPath.resolve(ConfigFileUtils.METADATA_FILE),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            LOGGER.info("Created backup at: {}", backupPath);

        } catch (IOException e) {
            LOGGER.warn("Failed to create full backup, continuing anyway", e);
        }
    }

    private static void backupIfExists(Path source, Path target) {
        try {
            if (Files.exists(source)) {
                if (Files.isDirectory(source)) {
                    copyDirectoryRecursively(source, target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Could not backup: {}", source);
        }
    }

    private static void copyDirectoryRecursively(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to copy: {}", sourcePath);
            }
        });
    }

    /**
     * Simple data class for pending config info
     */
    private static class PendingConfig {
        String configPath;
        String configName;
        ConfigMetadata metadata;

        PendingConfig(String configPath, String configName, ConfigMetadata metadata) {
            this.configPath = configPath;
            this.configName = configName;
            this.metadata = metadata;
        }
    }
}