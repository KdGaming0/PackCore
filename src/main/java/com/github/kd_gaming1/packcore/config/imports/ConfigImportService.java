package com.github.kd_gaming1.packcore.config.imports;

import com.github.kd_gaming1.packcore.config.apply.ConfigApplyService;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.github.kd_gaming1.packcore.config.model.ConfigMetadata;
import com.github.kd_gaming1.packcore.util.task.ProgressListener;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Manages importing configuration ZIP files with validation.
 * Uses a folder-based approach to avoid Swing/AWT headless issues.
 */
public class ConfigImportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigImportService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static final String IMPORTS_FOLDER = "packcore/imports";

    /**
     * Validation result with details
     */
    public record ValidationResult(boolean isValid, String errorMessage) {

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }

    /**
     * Represents a file available for import
     */
    public record ImportableFile(
            Path path,
            String fileName,
            long fileSize,
            LocalDateTime lastModified,
            ConfigMetadata metadata,
            ValidationResult validation
    ) {
        public boolean isValid() {
            return validation.isValid();
        }

        public String getDisplayName() {
            if (metadata != null && metadata.getName() != null) {
                return metadata.getName();
            }
            return fileName;
        }

        public String getFileSizeFormatted() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * Get the imports folder path, creating it if necessary
     */
    public static Path getImportsFolder() {
        Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
        Path importsDir = gameDir.resolve(IMPORTS_FOLDER);

        try {
            Files.createDirectories(importsDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create imports directory", e);
        }

        return importsDir;
    }

    /**
     * Open the imports folder in the system file browser
     */
    public static CompletableFuture<Boolean> openImportsFolder() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path importsFolder = getImportsFolder();

                if (!Files.exists(importsFolder)) {
                    Files.createDirectories(importsFolder);
                }

                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.OPEN)) {
                        desktop.open(importsFolder.toFile());
                        LOGGER.info("Opened imports folder: {}", importsFolder);
                        return true;
                    }
                }

                LOGGER.warn("Desktop operations not supported on this system");
                return false;

            } catch (IOException e) {
                LOGGER.error("Failed to open imports folder", e);
                return false;
            }
        });
    }

    /**
     * Scan the imports folder for available config files
     */
    public static CompletableFuture<List<ImportableFile>> scanImportsFolder() {
        return CompletableFuture.supplyAsync(() -> {
            List<ImportableFile> importableFiles = new ArrayList<>();
            Path importsFolder = getImportsFolder();

            if (!Files.exists(importsFolder)) {
                LOGGER.warn("Imports folder does not exist: {}", importsFolder);
                return importableFiles;
            }

            try (Stream<Path> files = Files.list(importsFolder)) {
                files.filter(path -> path.toString().toLowerCase().endsWith(".zip"))
                        .forEach(path -> {
                            try {
                                // Get file info
                                long size = Files.size(path);
                                LocalDateTime modified = LocalDateTime.ofInstant(
                                        Files.getLastModifiedTime(path).toInstant(),
                                        java.time.ZoneId.systemDefault()
                                );

                                // Validate the file
                                ValidationResult validation = validateConfigZip(path);

                                // Try to read metadata (null if invalid)
                                ConfigMetadata metadata = null;
                                if (validation.isValid()) {
                                    metadata = ConfigFileRepository.readMetadataFromZip(path);
                                }

                                ImportableFile importableFile = new ImportableFile(
                                        path,
                                        path.getFileName().toString(),
                                        size,
                                        modified,
                                        metadata,
                                        validation
                                );

                                importableFiles.add(importableFile);

                            } catch (IOException e) {
                                LOGGER.error("Failed to process file: {}", path, e);
                            }
                        });

            } catch (IOException e) {
                LOGGER.error("Failed to scan imports folder", e);
            }

            // Sort by last modified (newest first)
            importableFiles.sort((a, b) -> b.lastModified().compareTo(a.lastModified()));

            return importableFiles;
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
            return null;
        }

        return ConfigFileRepository.readMetadataFromZip(configPath);
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
                if (entryName.equals(ConfigFileRepository.METADATA_FILE) ||
                        entryName.endsWith("/" + ConfigFileRepository.METADATA_FILE)) {
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
                        """
                                Invalid config file: Missing packcore_metadata.json
                                
                                This ZIP must contain configuration metadata to be imported."""
                );
            }

            if (hasJarFiles) {
                return ValidationResult.invalid(
                        """
                                Invalid config file: Contains .jar files
                                
                                Configuration files should not contain mod .jar files.
                                Please use config files only."""
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
                                    ProgressListener callback) {
        if (callback == null) {
            callback = new ProgressListener() {
                @Override
                public void onProgress(String msg, int pct) {}
                @Override
                public void onComplete(boolean success, String msg) {}
            };
        }

        final ProgressListener finalCallback = callback;

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
                ConfigMetadata metadata = ConfigFileRepository.readMetadataFromZip(sourceFile);
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
                    ConfigFileRepository.ConfigFile configFile =
                            new ConfigFileRepository.ConfigFile(
                                    destination.getFileName().toString(),
                                    destination,
                                    false,
                                    metadata
                            );

                    // Schedule application on restart
                    ConfigApplyService.scheduleConfigApplication(configFile);

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

    /**
     * Import and optionally delete the source file from imports folder
     */
    public static void importConfigAndCleanup(Path sourceFile, boolean applyImmediately,
                                              boolean deleteAfterImport, ProgressListener callback) {
        ProgressListener wrappedCallback = new ProgressListener() {
            @Override
            public void onProgress(String msg, int pct) {
                if (callback != null) callback.onProgress(msg, pct);
            }

            @Override
            public void onComplete(boolean success, String msg) {
                if (success && deleteAfterImport) {
                    try {
                        Files.deleteIfExists(sourceFile);
                        LOGGER.info("Deleted imported file from imports folder: {}", sourceFile);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to delete imported file: {}", sourceFile, e);
                    }
                }
                if (callback != null) callback.onComplete(success, msg);
            }
        };

        importConfig(sourceFile, applyImmediately, wrappedCallback);
    }

    private static Path copyToCustomConfigs(Path sourceFile, ConfigMetadata metadata) {
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path customConfigsDir = gameDir.resolve(ConfigFileRepository.CUSTOM_CONFIGS_PATH);
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

        return ConfigFileRepository.getAllConfigs().stream()
                .anyMatch(config -> config.fileName()
                        .toLowerCase()
                        .contains(sanitizedName));
    }

    /**
     * Delete a file from the imports folder
     */
    public static boolean deleteImportFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
            LOGGER.info("Deleted import file: {}", filePath);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to delete import file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Get the imports folder path as a string for display
     */
    public static String getImportsFolderPath() {
        return getImportsFolder().toAbsolutePath().toString();
    }
}