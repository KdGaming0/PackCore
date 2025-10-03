package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.util.copysystem.ZipFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enhanced backup manager with metadata and user control
 */
public class BackupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final String BACKUPS_DIR = "packcore/backups";
    private static final String METADATA_FILE = "backup_metadata.json";

    // Config-related paths to backup
    private static final Set<String> CONFIG_PATHS = Set.of(
            "config",
            "options.txt",
            "servers.dat",
            "resourcepacks",
            "shaderpacks",
            "packcore/current_config.json"
    );

    public enum BackupType {
        AUTO("Auto"),
        MANUAL("Manual");

        private final String displayName;

        BackupType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static class BackupInfo {
        public final String backupId;
        public final String timestamp;
        public final BackupType type;
        public final String configName;
        public final String configVersion;
        public final long sizeBytes;
        public final String description;

        public BackupInfo(String backupId, String timestamp, BackupType type,
                          String configName, String configVersion, long sizeBytes, String description) {
            this.backupId = backupId;
            this.timestamp = timestamp;
            this.type = type;
            this.configName = configName;
            this.configVersion = configVersion;
            this.sizeBytes = sizeBytes;
            this.description = description;
        }

        public String getDisplayName() {
            return String.format("[%s] %s - %s",
                    type.getDisplayName(),
                    configName != null ? configName : "Unknown Config",
                    formatTimestamp());
        }

        private String formatTimestamp() {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return dateTime.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
            } catch (Exception e) {
                return timestamp;
            }
        }
    }

    /**
     * Create an automatic backup before config changes
     */
    public static Path createAutoBackup() {
        if (!PackCoreConfig.enableAutoBackups) {
            LOGGER.debug("Auto-backups are disabled");
            return null;
        }

        ConfigMetadata currentConfig = ConfigFileUtils.getCurrentConfig();
        String description = "Automatic backup before applying: " +
                (currentConfig != null ? currentConfig.getName() : "Unknown Config");

        return createBackup(BackupType.AUTO, description);
    }

    /**
     * Create a manual backup
     */
    public static Path createManualBackup(String description) {
        return createBackup(BackupType.MANUAL, description);
    }

    /**
     * Create a backup with metadata
     */
    private static Path createBackup(BackupType type, String description) {
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path backupsDir = gameDir.resolve(BACKUPS_DIR);
            Files.createDirectories(backupsDir);

            // Generate backup ID
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String backupId = type.name().toLowerCase() + "_" + timestamp;
            Path backupZip = backupsDir.resolve(backupId + ".zip");

            // Create temporary directory for backup content
            Path tempDir = Files.createTempDirectory("packcore_backup");

            try {
                // Copy config-related files
                copyConfigFiles(gameDir, tempDir);

                // Get current config info
                ConfigMetadata currentConfig = ConfigFileUtils.getCurrentConfig();

                // Create backup metadata
                BackupInfo backupInfo = new BackupInfo(
                        backupId,
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        type,
                        currentConfig != null ? currentConfig.getName() : "Unknown",
                        currentConfig != null ? currentConfig.getVersion() : "1.0.0",
                        calculateDirectorySize(tempDir),
                        description != null ? description : "Manual backup"
                );

                // Write backup metadata
                Path metadataPath = tempDir.resolve(METADATA_FILE);
                Files.writeString(metadataPath, GSON.toJson(backupInfo), StandardCharsets.UTF_8);

                // Create ZIP
                ZipFiles zipFiles = new ZipFiles();
                zipFiles.zipDirectory(tempDir.toFile(), backupZip.toString(), null);

                LOGGER.info("Created {} backup: {}", type.getDisplayName().toLowerCase(), backupZip);

                // Clean up old backups
                cleanupOldBackups(backupsDir);

                return backupZip;

            } finally {
                ConfigFileOperations.deleteDirectory(tempDir);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to create backup", e);
            return null;
        }
    }

    /**
     * Copy config-related files to backup directory
     */
    private static void copyConfigFiles(Path gameDir, Path backupDir) throws IOException {
        for (String configPath : CONFIG_PATHS) {
            Path sourcePath = gameDir.resolve(configPath);
            if (Files.exists(sourcePath)) {
                Path targetPath = backupDir.resolve(configPath);

                if (Files.isDirectory(sourcePath)) {
                    ConfigFileOperations.copyDirectory(sourcePath, targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Get list of all backups with metadata
     */
    public static List<BackupInfo> getBackups() {
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path backupsDir = gameDir.resolve(BACKUPS_DIR);

            if (!Files.exists(backupsDir)) {
                return new ArrayList<>();
            }

            List<BackupInfo> backups = new ArrayList<>();

            try (Stream<Path> backupFiles = Files.list(backupsDir)) {
                backupFiles.filter(path -> path.toString().endsWith(".zip"))
                        .forEach(backupZip -> {
                            BackupInfo info = readBackupMetadata(backupZip);
                            if (info != null) {
                                backups.add(info);
                            }
                        });
            }

            // Sort by timestamp (newest first)
            backups.sort((a, b) -> b.timestamp.compareTo(a.timestamp));

            return backups;

        } catch (IOException e) {
            LOGGER.error("Failed to list backups", e);
            return new ArrayList<>();
        }
    }

    /**
     * Read backup metadata from ZIP file
     */
    private static BackupInfo readBackupMetadata(Path backupZip) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(backupZip.toFile())) {
            java.util.zip.ZipEntry metadataEntry = zip.getEntry(METADATA_FILE);
            if (metadataEntry == null) {
                // Legacy backup without metadata
                return createLegacyBackupInfo(backupZip);
            }

            try (var inputStream = zip.getInputStream(metadataEntry)) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                return GSON.fromJson(json, BackupInfo.class);
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to read backup metadata: {}", backupZip, e);
            return createLegacyBackupInfo(backupZip);
        }
    }

    /**
     * Create backup info for legacy backups without metadata
     */
    private static BackupInfo createLegacyBackupInfo(Path backupZip) {
        try {
            String fileName = backupZip.getFileName().toString();
            String backupId = fileName.replace(".zip", "");

            // Try to extract timestamp from filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if (fileName.contains("backup_")) {
                try {
                    String timestampPart = fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf("."));
                    LocalDateTime dateTime = LocalDateTime.parse(timestampPart, TIMESTAMP_FORMAT);
                    timestamp = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception ignored) {
                    // Use current time if parsing fails
                }
            }

            long size = Files.size(backupZip);

            return new BackupInfo(
                    backupId,
                    timestamp,
                    BackupType.AUTO, // Assume legacy backups are auto
                    "Legacy Config",
                    "Unknown",
                    size,
                    "Legacy backup (no metadata)"
            );

        } catch (IOException e) {
            LOGGER.error("Failed to create legacy backup info", e);
            return null;
        }
    }

    /**
     * Restore a backup
     */
    public static boolean restoreBackup(BackupInfo backupInfo) {
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path backupsDir = gameDir.resolve(BACKUPS_DIR);
            Path backupZip = backupsDir.resolve(backupInfo.backupId + ".zip");

            if (!Files.exists(backupZip)) {
                LOGGER.error("Backup file not found: {}", backupZip);
                return false;
            }

            LOGGER.info("Restoring backup: {}", backupInfo.getDisplayName());

            // Create a backup of current state before restoring
            createAutoBackup();

            // Extract backup
            Path tempDir = Files.createTempDirectory("packcore_restore");
            try {
                var unzipper = new com.github.kd_gaming1.packcore.util.copysystem.UnzipFiles();
                unzipper.unzip(backupZip.toString(), tempDir.toString(), null);

                // Copy restored files back (excluding metadata)
                copyRestoredFiles(tempDir, gameDir);

                LOGGER.info("Backup restored successfully");
                return true;

            } finally {
                ConfigFileOperations.deleteDirectory(tempDir);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to restore backup", e);
            return false;
        }
    }

    /**
     * Copy restored files back to game directory
     */
    private static void copyRestoredFiles(Path sourceDir, Path gameDir) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(path -> !path.equals(sourceDir))
                    .filter(path -> !path.getFileName().toString().equals(METADATA_FILE))
                    .forEach(sourcePath -> {
                        try {
                            Path relativePath = sourceDir.relativize(sourcePath);
                            Path targetPath = gameDir.resolve(relativePath);

                            if (Files.isDirectory(sourcePath)) {
                                Files.createDirectories(targetPath);
                            } else {
                                Files.createDirectories(targetPath.getParent());
                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            LOGGER.warn("Failed to restore file: {}", sourcePath, e);
                        }
                    });
        }
    }

    /**
     * Delete a backup
     */
    public static boolean deleteBackup(BackupInfo backupInfo) {
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path backupsDir = gameDir.resolve(BACKUPS_DIR);
            Path backupZip = backupsDir.resolve(backupInfo.backupId + ".zip");

            if (Files.exists(backupZip)) {
                Files.delete(backupZip);
                LOGGER.info("Deleted backup: {}", backupInfo.getDisplayName());
                return true;
            }

            return false;

        } catch (IOException e) {
            LOGGER.error("Failed to delete backup", e);
            return false;
        }
    }

    /**
     * Clean up old backups based on settings
     */
    private static void cleanupOldBackups(Path backupsDir) {
        try {
            List<BackupInfo> backups = getBackups();

            // Separate auto and manual backups
            List<BackupInfo> autoBackups = backups.stream()
                    .filter(backup -> backup.type == BackupType.AUTO)
                    .collect(Collectors.toList());

            // Only clean up auto backups, keep manual backups
            if (autoBackups.size() > PackCoreConfig.maxBackups) {
                List<BackupInfo> toDelete = autoBackups.subList(PackCoreConfig.maxBackups, autoBackups.size());

                for (BackupInfo backup : toDelete) {
                    deleteBackup(backup);
                }

                LOGGER.info("Cleaned up {} old auto backups", toDelete.size());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to cleanup old backups", e);
        }
    }

    /**
     * Open backups folder in file explorer
     */
    public static void openBackupsFolder() {
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path backupsDir = gameDir.resolve(BACKUPS_DIR);
            Files.createDirectories(backupsDir);

            java.awt.Desktop.getDesktop().open(backupsDir.toFile());
        } catch (Exception e) {
            LOGGER.error("Failed to open backups folder", e);
        }
    }

    /**
     * Calculate directory size
     */
    private static long calculateDirectorySize(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }
}