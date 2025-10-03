package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.util.copysystem.UnzipFiles;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Manages config application on game restart.
 * Handles the pending config system for in-game config switching.
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
            Path gameDir = FabricLoader.getInstance().getGameDir();
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

            // Create backup using new backup manager
            Path backup = BackupManager.createAutoBackup();
            if (backup != null) {
                LOGGER.info("Created auto-backup before applying config: {}", backup);
            }

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

    /**
     * Apply a config by extracting its ZIP file
     */
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

    /**
     * Data class for pending config info
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