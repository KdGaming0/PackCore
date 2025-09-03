package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.wizard.copysystem.UnzipFiles;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class ConfigApplicationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String PENDING_CONFIG_FLAG = "packcore_pending_config.txt";

    /**
     * Marks a config for application on next startup and shuts down the game
     */
    public static void applyConfigOnRestart(ConfigFileUtils.ConfigFile config) {
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path flagFile = gameDir.resolve(PENDING_CONFIG_FLAG);

            // Write the config path to the flag file
            String configData = config.getPath().toString() + "\n" + config.getDisplayName();
            Files.writeString(flagFile, configData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            LOGGER.info("Marked config for application on restart: {}", config.getDisplayName());

            // Close the game
            MinecraftClient.getInstance().scheduleStop();

        } catch (IOException e) {
            LOGGER.error("Failed to mark config for application", e);
            throw new RuntimeException("Failed to prepare config application", e);
        }
    }

    /**
     * Checks if there's a pending config to apply and applies it
     * Should be called during pre-launch
     */
    public static boolean checkAndApplyPendingConfig(Path gameDir) {
        Path flagFile = gameDir.resolve(PENDING_CONFIG_FLAG);

        if (!Files.exists(flagFile)) {
            return false;
        }

        try {
            String content = Files.readString(flagFile);
            String[] lines = content.split("\n");
            if (lines.length < 2) {
                LOGGER.warn("Invalid pending config flag file format");
                Files.deleteIfExists(flagFile);
                return false;
            }

            String configPath = lines[0];
            String configName = lines[1];

            LOGGER.info("Found pending config application: {}", configName);

            // Apply the config
            boolean success = extractConfigToGameDir(Path.of(configPath), gameDir);

            if (success) {
                // Update the current config metadata
                updateCurrentConfigMetadata(configPath, configName, gameDir);
                LOGGER.info("Successfully applied config: {}", configName);
            }

            // Remove the flag file
            Files.deleteIfExists(flagFile);

            return success;

        } catch (IOException e) {
            LOGGER.error("Failed to process pending config", e);
            try {
                Files.deleteIfExists(flagFile);
            } catch (IOException ex) {
                LOGGER.warn("Failed to clean up flag file", ex);
            }
            return false;
        }
    }

    private static boolean extractConfigToGameDir(Path configZipPath, Path gameDir) {
        try {
            if (!Files.exists(configZipPath)) {
                LOGGER.error("Config file not found: {}", configZipPath);
                return false;
            }

            // Create backup of current config if needed
            createConfigBackup(gameDir);

            // Extract the config
            UnzipFiles unzipper = new UnzipFiles();
            unzipper.unzip(configZipPath.toString(), gameDir.toString(),
                    (bytesProcessed, totalBytes, percentage) -> {
                        if (percentage % 25 == 0) {
                            LOGGER.info("Config extraction progress: {}%", percentage);
                        }
                    });

            LOGGER.info("Config extraction completed successfully");
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to extract config", e);
            return false;
        }
    }

    private static void createConfigBackup(Path gameDir) {
        try {
            Path backupDir = gameDir.resolve("packcore/config_backups");
            Files.createDirectories(backupDir);

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path backupPath = backupDir.resolve("backup_" + timestamp);

            // You could implement a backup system here if needed
            LOGGER.info("Config backup location prepared: {}", backupPath);

        } catch (IOException e) {
            LOGGER.warn("Failed to prepare config backup", e);
        }
    }

    private static void updateCurrentConfigMetadata(String configPath, String configName, Path gameDir) {
        try {
            // Read metadata from the applied config if available
            ConfigFileUtils.ConfigFile appliedConfig = new ConfigFileUtils.ConfigFile(
                    configName, Path.of(configPath), true, new ConfigFileUtils.ConfigMetadata());

            // Create/update the current config info file
            Path configInfoPath = gameDir.resolve(ConfigFileUtils.METADATA_FILE);
            String metadataJson = new com.google.gson.Gson().toJson(appliedConfig.getMetadata());
            Files.writeString(configInfoPath, metadataJson, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            LOGGER.error("Failed to update current config metadata", e);
        }
    }
}