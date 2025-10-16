package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.util.copysystem.AsyncZipFiles;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enhanced backup manager with async operations and progress reporting
 */
public class BackupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final String BACKUPS_DIR = "packcore/backups";
    private static final String METADATA_FILE = "backup_metadata.json";

    // Async executor for background operations
    private static final ExecutorService BACKUP_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("BackupManager-" + thread.getId());
        thread.setDaemon(true);
        return thread;
    });

    // Config-related paths to backup
    private static final Set<String> CONFIG_PATHS = Set.of(
            "config",
            "options.txt",
            "servers.dat",
            "resourcepacks",
            "shaderpacks",
            "packcore/current_config.json"
    );

    // Excluded config subfolders
    private static final Set<String> EXCLUDED_CONFIG_SUBFOLDERS = Set.of(
            "firmament/profiles", "skyhanni/backup", "skyhanni/repo",
            "skyblocker/item-repo", "skyocean/data"
    );

    // Batch size for file operations
    private static final int BATCH_SIZE = 50;

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

    public record BackupInfo(String backupId, String timestamp, BackupType type, String configName,
                             String configVersion, long sizeBytes, String title, String description) {

        public String getDisplayName() {
                return String.format("[%s] %s - %s",
                        type.getDisplayName(),
                        title != null ? title : (configName != null ? configName : "Unknown Config"),
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
     * Create an automatic backup before config changes (async)
     */
    public static CompletableFuture<Path> createAutoBackupAsync(Consumer<String> progressCallback) {
        if (!PackCoreConfig.enableAutoBackups) {
            LOGGER.debug("Auto-backups are disabled");
            return CompletableFuture.completedFuture(null);
        }

        ConfigMetadata currentConfig = ConfigFileUtils.getCurrentConfig();
        String title = "Auto backup before applying: " +
                (currentConfig != null ? currentConfig.getName() : "Unknown Config");

        return createBackupAsync(BackupType.AUTO, title, null, progressCallback);
    }

    /**
     * Create an automatic backup (blocking fallback)
     */
    public static Path createAutoBackup() {
        try {
            return createAutoBackupAsync(msg -> {
            }).get();
        } catch (Exception e) {
            LOGGER.error("Failed to create auto backup", e);
            return null;
        }
    }

    /**
     * Create a manual backup (async with progress)
     */
    public static void createManualBackup(String title, String description) {
        createManualBackupAsync(title, description, msg -> LOGGER.info("Backup progress: {}", msg));
    }

    /**
     * Create a manual backup asynchronously
     */
    public static CompletableFuture<Path> createManualBackupAsync(
            String title, String description, Consumer<String> progressCallback) {
        return createBackupAsync(BackupType.MANUAL, title, description, progressCallback);
    }

    /**
     * Create a backup with metadata (async)
     */
    private static CompletableFuture<Path> createBackupAsync(
            BackupType type, String title, String description, Consumer<String> progressCallback) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                progressCallback.accept("Preparing backup...");

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
                    progressCallback.accept("Copying configuration files...");

                    // Copy config-related files asynchronously
                    copyConfigFilesAsync(gameDir, tempDir, progressCallback).join();

                    progressCallback.accept("Calculating backup size...");

                    // Get current config info
                    ConfigMetadata currentConfig = ConfigFileUtils.getCurrentConfig();

                    // Calculate size asynchronously
                    long size = calculateDirectorySizeAsync(tempDir).join();

                    // Create backup metadata
                    BackupInfo backupInfo = new BackupInfo(
                            backupId,
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            type,
                            currentConfig != null ? currentConfig.getName() : "Unknown",
                            currentConfig != null ? currentConfig.getVersion() : "1.0.0",
                            size,
                            title != null ? title : "Manual backup",
                            description
                    );

                    // Write backup metadata
                    Path metadataPath = tempDir.resolve(METADATA_FILE);
                    Files.writeString(metadataPath, GSON.toJson(backupInfo), StandardCharsets.UTF_8);

                    progressCallback.accept("Creating backup archive...");

                    // Create ZIP asynchronously
                    AsyncZipFiles zipFiles = new AsyncZipFiles();
                    zipFiles.zipDirectoryAsync(tempDir.toFile(), backupZip.toString(),
                            (bytesProcessed, totalBytes, percentage) -> progressCallback.accept(String.format("Zipping: %d%%", percentage))).join();

                    LOGGER.info("Created {} backup: {}", type.getDisplayName().toLowerCase(), backupZip);

                    // Clean up old backups in background
                    CompletableFuture.runAsync(() -> cleanupOldBackups(backupsDir), BACKUP_EXECUTOR);

                    progressCallback.accept("Backup complete!");
                    return backupZip;

                } finally {
                    // Clean up temp directory in background
                    CompletableFuture.runAsync(() -> {
                        try {
                            ConfigFileOperations.deleteDirectory(tempDir);
                        } catch (Exception e) {
                            LOGGER.warn("Failed to clean up temp directory", e);
                        }
                    }, BACKUP_EXECUTOR);
                }

            } catch (Exception e) {
                LOGGER.error("Failed to create backup", e);
                progressCallback.accept("Backup failed: " + e.getMessage());
                throw new RuntimeException("Backup creation failed", e);
            }
        }, BACKUP_EXECUTOR);
    }

    /**
     * Copy config-related files asynchronously with progress reporting
     */
    private static CompletableFuture<Void> copyConfigFilesAsync(
            Path gameDir, Path backupDir, Consumer<String> progressCallback) {

        return CompletableFuture.runAsync(() -> {
            try {
                int total = CONFIG_PATHS.size();
                int processed = 0;

                for (String configPath : CONFIG_PATHS) {
                    Path sourcePath = gameDir.resolve(configPath);
                    if (Files.exists(sourcePath)) {
                        Path targetPath = backupDir.resolve(configPath);

                        progressCallback.accept(String.format("Copying %s...", configPath));

                        if (Files.isDirectory(sourcePath)) {
                            copyDirectoryWithExclusionsAsync(sourcePath, targetPath).join();
                        } else {
                            Files.createDirectories(targetPath.getParent());
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }

                    processed++;
                    int percentage = (processed * 100) / total;
                    progressCallback.accept(String.format("Copying files: %d%%", percentage));
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy config files", e);
            }
        }, BACKUP_EXECUTOR);
    }

    /**
     * Copy directory asynchronously while excluding specified subfolders
     */
    private static CompletableFuture<Void> copyDirectoryWithExclusionsAsync(Path source, Path target) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(target);

                // Collect all paths first to avoid holding streams open
                List<Path> pathsToCopy;
                try (Stream<Path> paths = Files.walk(source)) {
                    pathsToCopy = paths.toList();
                }

                // Process in batches for better performance
                for (int i = 0; i < pathsToCopy.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, pathsToCopy.size());
                    List<Path> batch = pathsToCopy.subList(i, end);

                    // Process batch in parallel
                    batch.parallelStream().forEach(sourcePath -> {
                        try {
                            Path relativePath = source.relativize(sourcePath);
                            String relativePathStr = relativePath.toString().replace("\\", "/");

                            // Check if this path should be excluded
                            for (String excludedFolder : EXCLUDED_CONFIG_SUBFOLDERS) {
                                if (relativePathStr.startsWith(excludedFolder)) {
                                    return; // Skip this path
                                }
                            }

                            Path targetPath = target.resolve(relativePath);

                            if (Files.isDirectory(sourcePath)) {
                                Files.createDirectories(targetPath);
                            } else {
                                Files.createDirectories(targetPath.getParent());
                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            LOGGER.warn("Failed to copy file during backup: {}", sourcePath, e);
                        }
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy directory", e);
            }
        }, BACKUP_EXECUTOR);
    }

    /**
     * Calculate directory size asynchronously
     */
    private static CompletableFuture<Long> calculateDirectorySizeAsync(Path directory) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicLong totalSize = new AtomicLong(0);

            try (Stream<Path> paths = Files.walk(directory)) {
                paths.filter(Files::isRegularFile)
                        .parallel()
                        .forEach(path -> {
                            try {
                                totalSize.addAndGet(Files.size(path));
                            } catch (IOException e) {
                                LOGGER.debug("Could not get size for: {}", path);
                            }
                        });
            } catch (IOException e) {
                LOGGER.warn("Failed to calculate directory size", e);
            }

            return totalSize.get();
        }, BACKUP_EXECUTOR);
    }

    /**
     * Get list of all backups with metadata (async)
     */
    public static CompletableFuture<List<BackupInfo>> getBackupsAsync() {
        return CompletableFuture.supplyAsync(() -> {
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
        }, BACKUP_EXECUTOR);
    }

    /**
     * Get list of all backups (blocking)
     */
    public static List<BackupInfo> getBackups() {
        try {
            return getBackupsAsync().get();
        } catch (Exception e) {
            LOGGER.error("Failed to get backups", e);
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

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if (fileName.contains("backup_")) {
                try {
                    String timestampPart = fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf("."));
                    LocalDateTime dateTime = LocalDateTime.parse(timestampPart, TIMESTAMP_FORMAT);
                    timestamp = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception ignored) {
                }
            }

            long size = Files.size(backupZip);

            return new BackupInfo(
                    backupId,
                    timestamp,
                    BackupType.AUTO,
                    "Legacy Config",
                    "Unknown",
                    size,
                    "Legacy backup (no metadata)",
                    null
            );

        } catch (IOException e) {
            LOGGER.error("Failed to create legacy backup info", e);
            return null;
        }
    }

    /**
     * Restore a backup asynchronously with progress
     */
    public static CompletableFuture<Boolean> restoreBackupAsync(
            BackupInfo backupInfo, Consumer<String> progressCallback) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
                Path backupsDir = gameDir.resolve(BACKUPS_DIR);
                Path backupZip = backupsDir.resolve(backupInfo.backupId + ".zip");

                if (!Files.exists(backupZip)) {
                    LOGGER.error("Backup file not found: {}", backupZip);
                    progressCallback.accept("Error: Backup file not found");
                    return false;
                }

                LOGGER.info("Restoring backup: {}", backupInfo.getDisplayName());
                progressCallback.accept("Creating safety backup...");

                // Create a backup of current state before restoring
                createAutoBackupAsync(msg -> {
                }).join();

                progressCallback.accept("Extracting backup...");

                // Extract backup
                Path tempDir = Files.createTempDirectory("packcore_restore");
                try {
                    var unzipper = new com.github.kd_gaming1.packcore.util.copysystem.AsyncUnzipFiles();
                    unzipper.unzipAsync(backupZip.toString(), tempDir.toString(),
                            (bytesProcessed, totalBytes, percentage) ->
                                    progressCallback.accept(String.format("Extracting: %d%%", percentage))).join();

                    progressCallback.accept("Restoring files...");

                    // Copy restored files back (excluding metadata)
                    copyRestoredFilesAsync(tempDir, gameDir, progressCallback).join();

                    progressCallback.accept("Restore complete!");
                    LOGGER.info("Backup restored successfully");
                    return true;

                } finally {
                    // Clean up temp directory
                    CompletableFuture.runAsync(() -> {
                        try {
                            ConfigFileOperations.deleteDirectory(tempDir);
                        } catch (Exception e) {
                            LOGGER.warn("Failed to clean up temp directory", e);
                        }
                    }, BACKUP_EXECUTOR);
                }

            } catch (Exception e) {
                LOGGER.error("Failed to restore backup", e);
                progressCallback.accept("Restore failed: " + e.getMessage());
                return false;
            }
        }, BACKUP_EXECUTOR);
    }

    /**
     * Restore a backup (blocking)
     */
    public static boolean restoreBackup(BackupInfo backupInfo) {
        try {
            return restoreBackupAsync(backupInfo, msg -> {
            }).get();
        } catch (Exception e) {
            LOGGER.error("Failed to restore backup", e);
            return false;
        }
    }

    /**
     * Copy restored files back to game directory asynchronously
     */
    private static CompletableFuture<Void> copyRestoredFilesAsync(
            Path sourceDir, Path gameDir, Consumer<String> progressCallback) {

        return CompletableFuture.runAsync(() -> {
            try {
                // Collect all paths first
                List<Path> pathsToRestore;
                try (Stream<Path> paths = Files.walk(sourceDir)) {
                    pathsToRestore = paths
                            .filter(path -> !path.equals(sourceDir))
                            .filter(path -> !path.getFileName().toString().equals(METADATA_FILE))
                            .toList();
                }

                int total = pathsToRestore.size();
                AtomicLong processed = new AtomicLong(0);

                // Process in batches
                for (int i = 0; i < pathsToRestore.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, pathsToRestore.size());
                    List<Path> batch = pathsToRestore.subList(i, end);

                    batch.parallelStream().forEach(sourcePath -> {
                        try {
                            Path relativePath = sourceDir.relativize(sourcePath);
                            Path targetPath = gameDir.resolve(relativePath);

                            if (Files.isDirectory(sourcePath)) {
                                Files.createDirectories(targetPath);
                            } else {
                                Files.createDirectories(targetPath.getParent());
                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            }

                            long count = processed.incrementAndGet();
                            if (count % 100 == 0) {
                                int percentage = (int) ((count * 100) / total);
                                progressCallback.accept(String.format("Restoring files: %d%%", percentage));
                            }
                        } catch (IOException e) {
                            LOGGER.warn("Failed to restore file: {}", sourcePath, e);
                        }
                    });
                }

            } catch (IOException e) {
                throw new RuntimeException("Failed to restore files", e);
            }
        }, BACKUP_EXECUTOR);
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
                    .toList();

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
        CompletableFuture.runAsync(() -> {
            try {
                Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
                Path backupsDir = gameDir.resolve(BACKUPS_DIR);
                Files.createDirectories(backupsDir);

                java.awt.Desktop.getDesktop().open(backupsDir.toFile());
            } catch (Exception e) {
                LOGGER.error("Failed to open backups folder", e);
            }
        }, BACKUP_EXECUTOR);
    }

    /**
     * Shutdown the executor (call on game close)
     */
    public static void shutdown() {
        BACKUP_EXECUTOR.shutdown();
    }
}