package com.github.kd_gaming1.packcore.util;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Manages importing configuration ZIP files with validation
 */
public class ConfigImportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigImportManager.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Validation result with details
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }

    /**
     * Open native file chooser to select config zip with foreground focus
     */
    public static CompletableFuture<Path> selectConfigFile() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Show warning about file dialog
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(
                                net.minecraft.text.Text.literal(
                                        "§eFile browser opening... It may appear behind Minecraft. Check your taskbar!"),
                                false
                        );
                    }
                });

                // Create file chooser with better configuration
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Config File to Import (.zip with metadata)");
                fileChooser.setFileFilter(
                        new FileNameExtensionFilter("Config Files (*.zip)", "zip"));
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setMultiSelectionEnabled(false);

                // Set initial directory to user's Downloads folder
                Path downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads");
                if (Files.exists(downloadsPath)) {
                    fileChooser.setCurrentDirectory(downloadsPath.toFile());
                }

                // Force dialog to front (platform-specific workarounds)
                JDialog dialog = createForegroundDialog(fileChooser);
                int result = fileChooser.showOpenDialog(dialog);

                if (result == JFileChooser.APPROVE_OPTION) {
                    Path selectedFile = fileChooser.getSelectedFile().toPath();
                    LOGGER.info("Selected file: {}", selectedFile);

                    if (!selectedFile.toString().toLowerCase().endsWith(".zip")) {
                        LOGGER.warn("Selected file is not a zip: {}", selectedFile);
                        showErrorDialog("Invalid File",
                                "Please select a .zip file containing a valid configuration.");
                        return null;
                    }

                    return selectedFile;
                }

                return null;

            } catch (Exception e) {
                LOGGER.error("Error opening file dialog", e);
                showErrorDialog("Error", "Failed to open file browser: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Creates a dialog that attempts to stay in foreground
     */
    private static JDialog createForegroundDialog(JFileChooser fileChooser) {
        JDialog dialog = new JDialog((Frame) null, "Select Config File", false);
        dialog.setAlwaysOnTop(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Platform-specific workarounds for bringing to front
        SwingUtilities.invokeLater(() -> {
            dialog.toFront();
            dialog.setVisible(true);
            dialog.requestFocus();

            // Additional workaround for some systems
            dialog.setAlwaysOnTop(false);
            dialog.toFront();
            dialog.requestFocus();
            dialog.setAlwaysOnTop(true);
        });

        return dialog;
    }

    /**
     * Shows an error dialog to the user
     */
    private static void showErrorDialog(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane optionPane = new JOptionPane(
                    message,
                    JOptionPane.ERROR_MESSAGE
            );
            JDialog dialog = optionPane.createDialog(title);
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
        });
    }

    /**
     * Preview config metadata without importing - includes validation
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

        // Validate before reading metadata
        ValidationResult validation = validateConfigZip(configPath);
        if (!validation.isValid) {
            LOGGER.error("Config validation failed: {}", validation.errorMessage);
            showErrorDialog("Invalid Config", validation.errorMessage);
            return null;
        }

        return ConfigFileUtils.readMetadataFromZip(configPath);
    }

    /**
     * Validates that a ZIP file is a valid config:
     * - Must contain packcore_metadata.json
     * - Must NOT contain any .jar files
     */
    public static ValidationResult validateConfigZip(Path zipPath) {
        if (zipPath == null || !Files.exists(zipPath)) {
            return ValidationResult.invalid("File does not exist");
        }

        if (!zipPath.toString().toLowerCase().endsWith(".zip")) {
            return ValidationResult.invalid("File must be a .zip file");
        }

        try {
            if (Files.size(zipPath) == 0) {
                return ValidationResult.invalid("File is empty");
            }
        } catch (IOException e) {
            return ValidationResult.invalid("Cannot read file size");
        }

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            boolean hasMetadata = false;
            boolean hasJarFiles = false;

            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // Check for metadata file
                if (entryName.equals(ConfigFileUtils.METADATA_FILE) ||
                        entryName.endsWith("/" + ConfigFileUtils.METADATA_FILE)) {
                    hasMetadata = true;
                }

                // Check for .jar files (forbidden)
                if (entryName.toLowerCase().endsWith(".jar")) {
                    hasJarFiles = true;
                    LOGGER.warn("Found .jar file in config: {}", entryName);
                }
            }

            if (!hasMetadata) {
                return ValidationResult.invalid(
                        "Invalid config file: Missing packcore_metadata.json\n\n" +
                                "This ZIP must contain configuration metadata to be imported."
                );
            }

            if (hasJarFiles) {
                return ValidationResult.invalid(
                        "Invalid config file: Contains .jar files\n\n" +
                                "Configuration files should not contain mod .jar files.\n" +
                                "Please use config files only."
                );
            }

            return ValidationResult.valid();

        } catch (IOException e) {
            LOGGER.error("Failed to validate config zip", e);
            return ValidationResult.invalid(
                    "Cannot read ZIP file: " + e.getMessage()
            );
        }
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

                // Perform comprehensive validation
                ValidationResult validation = validateConfigZip(sourceFile);
                if (!validation.isValid) {
                    finalCallback.onComplete(false, validation.errorMessage);
                    return;
                }

                finalCallback.onProgress("Reading metadata...", 30);
                ConfigMetadata metadata = ConfigFileUtils.readMetadataFromZip(sourceFile);
                if (metadata == null || !metadata.isValid()) {
                    finalCallback.onComplete(false,
                            "Could not read config metadata or metadata is invalid");
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