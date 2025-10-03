package com.github.kd_gaming1.packcore.util;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Manages importing configuration ZIP files
 */
public class ConfigImportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigImportManager.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Open native file chooser to select config zip
     */
    public static CompletableFuture<Path> selectConfigFile() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Config File to Import");
                fileChooser.setFileFilter(
                        new FileNameExtensionFilter("Config Files (*.zip)", "zip"));
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                // Set initial directory to user's Downloads folder
                Path downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads");
                if (Files.exists(downloadsPath)) {
                    fileChooser.setCurrentDirectory(downloadsPath.toFile());
                }

                int result = fileChooser.showOpenDialog(null);

                if (result == JFileChooser.APPROVE_OPTION) {
                    Path selectedFile = fileChooser.getSelectedFile().toPath();
                    LOGGER.info("Selected file: {}", selectedFile);

                    if (!selectedFile.toString().toLowerCase().endsWith(".zip")) {
                        LOGGER.warn("Selected file is not a zip: {}", selectedFile);
                        return null;
                    }

                    return selectedFile;
                }

                return null;

            } catch (Exception e) {
                LOGGER.error("Error opening file dialog", e);
                return null;
            }
        });
    }

    /**
     * Preview config metadata without importing
     */
    public static ConfigMetadata previewConfig(Path configPath) {
        if (configPath == null || !Files.exists(configPath)) {
            LOGGER.error("Config file does not exist: {}", configPath);
            return null;
        }

        if (!configPath.toString().toLowerCase().endsWith(".zip")) {
            LOGGER.error("File is not a zip: {}", configPath);
            return null;
        }

        return ConfigFileUtils.readMetadataFromZip(configPath);
    }

    /**
     * Import config file to custom configs directory
     */
    public static void importConfig(Path sourceFile, boolean applyImmediately,
                                    ImportCallback callback) {
        if (callback == null) {
            callback = new ImportCallback() {
                @Override
                public void onProgress(String msg, int pct) {}
                @Override
                public void onComplete(boolean success, String msg) {}
            };
        }

        final ImportCallback finalCallback = callback;

        CompletableFuture.runAsync(() -> {
            try {
                finalCallback.onProgress("Validating file...", 10);

                if (!validateConfigFile(sourceFile)) {
                    finalCallback.onComplete(false, "Invalid config file format");
                    return;
                }

                finalCallback.onProgress("Reading metadata...", 30);
                ConfigMetadata metadata = previewConfig(sourceFile);
                if (metadata == null) {
                    finalCallback.onComplete(false, "Could not read config metadata");
                    return;
                }

                finalCallback.onProgress("Copying file...", 50);
                Path destination = copyToCustomConfigs(sourceFile, metadata);

                if (destination == null) {
                    finalCallback.onComplete(false, "Failed to copy config file");
                    return;
                }

                finalCallback.onProgress("Import complete", 80);

                if (applyImmediately) {
                    finalCallback.onProgress("Scheduling restart...", 90);

                    // Create ConfigFile for application
                    ConfigFileUtils.ConfigFile configFile =
                            new ConfigFileUtils.ConfigFile(
                                    destination.getFileName().toString(),
                                    destination,
                                    false,
                                    metadata
                            );

                    // Schedule application on restart
                    ConfigApplicationManager.scheduleConfigApplication(configFile);

                    finalCallback.onComplete(true,
                            "Config imported and will be applied on restart.");
                } else {
                    finalCallback.onComplete(true,
                            "Config imported successfully: " + metadata.getName());
                }

            } catch (Exception e) {
                LOGGER.error("Failed to import config", e);
                finalCallback.onComplete(false, "Import failed: " + e.getMessage());
            }
        });
    }

    private static boolean validateConfigFile(Path configPath) {
        try {
            if (!Files.exists(configPath) ||
                    !configPath.toString().toLowerCase().endsWith(".zip") ||
                    Files.size(configPath) == 0) {
                return false;
            }

            // Verify it's a valid zip
            try (java.util.zip.ZipFile zipFile =
                         new java.util.zip.ZipFile(configPath.toFile())) {
                return zipFile.entries().hasMoreElements();
            }

        } catch (IOException e) {
            LOGGER.error("Failed to validate config file", e);
            return false;
        }
    }

    private static Path copyToCustomConfigs(Path sourceFile, ConfigMetadata metadata) {
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path customConfigsDir = gameDir.resolve(ConfigFileUtils.CUSTOM_CONFIGS_PATH);
            Files.createDirectories(customConfigsDir);

            // Generate unique filename
            String baseName = metadata.getName()
                    .replaceAll("[^a-zA-Z0-9\\-_]", "_");
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String fileName = baseName + "_" + timestamp + ".zip";

            Path destination = customConfigsDir.resolve(fileName);

            // Ensure unique name
            int counter = 1;
            while (Files.exists(destination)) {
                fileName = baseName + "_" + timestamp + "_" + counter + ".zip";
                destination = customConfigsDir.resolve(fileName);
                counter++;
            }

            // Copy file
            Files.copy(sourceFile, destination, StandardCopyOption.COPY_ATTRIBUTES);
            LOGGER.info("Config imported to: {}", destination);

            return destination;

        } catch (IOException e) {
            LOGGER.error("Failed to copy config file", e);
            return null;
        }
    }

    /**
     * Check if a config with similar name already exists
     */
    public static boolean configExists(String configName) {
        if (configName == null || configName.isBlank()) {
            return false;
        }

        String sanitizedName = configName
                .replaceAll("[^a-zA-Z0-9\\-_]", "_")
                .toLowerCase();

        return ConfigFileUtils.getAllConfigs().stream()
                .anyMatch(config -> config.getFileName()
                        .toLowerCase()
                        .contains(sanitizedName));
    }
}