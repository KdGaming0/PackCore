package com.github.kd_gaming1.packcore.config.backup;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.util.GsonUtils;
import com.github.kd_gaming1.packcore.util.io.file.ExclusionPatterns;
import com.github.kd_gaming1.packcore.util.io.file.FileUtils;
import com.github.kd_gaming1.packcore.util.io.zip.UnzipAsyncTask;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.github.kd_gaming1.packcore.config.model.ConfigMetadata;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Enhanced backup manager with async operations and progress reporting.
 * Optimized to stream directly to ZIP to avoid double-copy overhead.
 */
public class BackupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);
    private static final Gson GSON = GsonUtils.GSON;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final String BACKUPS_DIR = "packcore/backups";
    private static final String METADATA_FILE = "backup_metadata.json";

    // Async executor for background operations
    private static final ExecutorService BACKUP_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("BackupManager-" + thread.threadId());
        thread.setDaemon(true);
        return thread;
    });

    // Config-related paths to backup
    private static final Set<String> CONFIG_PATHS = Set.of(
            "config",
            "options.txt",
            "servers.dat",
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

        ConfigMetadata currentConfig = ConfigFileRepository.getCurrentConfig();
        String title = "Auto backup before applying: " +
                (currentConfig != null ? currentConfig.getName() : "Unknown Config");

        return createBackupAsync(BackupType.AUTO, title, null, progressCallback);
    }

    /**
     * Create an automatic backup (blocking fallback)
     */
    public static Path createAutoBackup() {
        try {
            return createAutoBackupAsync(msg -> {}).get();
        } catch (Exception e) {
            LOGGER.error("Failed to create auto backup", e);
            return null;
        }
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
    static CompletableFuture<Path> createBackupAsync(
            BackupType type, String title, String description, Consumer<String> progressCallback) {
        return createBackupAsync(type, title, description, null, progressCallback);
    }

    /**
     * Create a backup with metadata (async) with an optional hint that becomes part of the zip filename.
     */
    static CompletableFuture<Path> createBackupAsync(
            BackupType type, String title, String description, String backupIdHint, Consumer<String> progressCallback) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                progressCallback.accept("Preparing backup...");
                Path gameDir = getGameDirectory();
                return createBackupAsyncInternal(gameDir, type, title, description, backupIdHint, progressCallback).join();
            } catch (Exception e) {
                LOGGER.error("Failed to create backup", e);
                progressCallback.accept("Backup failed: " + e.getMessage());
                throw new RuntimeException("Backup creation failed", e);
            }
        }, BACKUP_EXECUTOR);
    }

    /**
     * Create a backup with explicit game directory
     */
    public static CompletableFuture<Path> createBackupAsync(
            Path gameDir, BackupType type, String title, String description, Consumer<String> progressCallback) {
        return createBackupAsyncInternal(gameDir, type, title, description, null, progressCallback);
    }

    public static CompletableFuture<Path> createBackupAsync(
            Path gameDir, BackupType type, String title, String description, String backupIdHint, Consumer<String> progressCallback) {
        return createBackupAsyncInternal(gameDir, type, title, description, backupIdHint, progressCallback);
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        double value = bytes / Math.pow(1024, digitGroups);
        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(value) + " " + units[digitGroups];
    }

    /**
     * Internal backup creation method.
     * OPTIMIZED: Uses direct streaming to avoid temporary files.
     */
    private static CompletableFuture<Path> createBackupAsyncInternal(
            Path gameDir, BackupType type, String title, String description, String backupIdHint, Consumer<String> progressCallback) {

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            boolean debug = PackCoreConfig.enableBackupDebugLogging;

            try {
                if (debug) {
                    LOGGER.info("╔══════════════════════════════════════════════════════════════╗");
                    LOGGER.info("║              BACKUP STARTED (Direct Streaming Mode)          ║");
                    LOGGER.info("╚══════════════════════════════════════════════════════════════╝");
                }
                progressCallback.accept("Preparing backup...");

                // Phase 1: Create directories
                long phaseStart = System.currentTimeMillis();
                if (debug) LOGGER.info("[Backup] Phase 1: Creating backup directory...");
                Path backupsDir = gameDir.resolve(BACKUPS_DIR);
                Files.createDirectories(backupsDir);
                if (debug) LOGGER.info("[Backup] Phase 1 complete: {}ms", System.currentTimeMillis() - phaseStart);

                String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
                String backupId = (sanitizeForBackupId(backupIdHint) != null ? backupIdHint : type.name().toLowerCase()) + "_" + timestamp;
                Path backupZip = backupsDir.resolve(backupId + ".zip");

                // Prepare Metadata
                ConfigMetadata currentConfig = ConfigFileRepository.getCurrentConfig();
                BackupInfo backupInfo = new BackupInfo(
                        backupId,
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        type,
                        currentConfig != null ? currentConfig.getName() : "Unknown",
                        currentConfig != null ? currentConfig.getVersion() : "1.0.0",
                        -1, // Size calculated later or on read
                        title != null ? title : "Manual backup",
                        description
                );
                String metadataJson = GSON.toJson(backupInfo);

                // Phase 2: Create ZIP archive directly from source
                phaseStart = System.currentTimeMillis();
                if (debug) LOGGER.info("[Backup] Phase 2: Streaming files directly to ZIP");
                progressCallback.accept("Creating backup archive...");

                try (FileOutputStream fos = new FileOutputStream(backupZip.toFile());
                     BufferedOutputStream bos = new BufferedOutputStream(fos);
                     ZipOutputStream zos = new ZipOutputStream(bos)) {

                    zos.setLevel(3); // Moderate compression for speed

                    // 1. Write Metadata
                    ZipEntry metaEntry = new ZipEntry(METADATA_FILE);
                    zos.putNextEntry(metaEntry);
                    zos.write(metadataJson.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();

                    // 2. Stream config files directly
                    byte[] buffer = new byte[32768];
                    int pathsProcessed = 0;

                    for (String configPath : CONFIG_PATHS) {
                        Path sourcePath = gameDir.resolve(configPath);
                        if (!Files.exists(sourcePath)) continue;

                        progressCallback.accept("Backing up: " + configPath);

                        if (Files.isDirectory(sourcePath)) {
                            // Walk directory and zip
                            Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    if (ExclusionPatterns.shouldExclude(gameDir, file)) return FileVisitResult.CONTINUE;

                                    // Relativize path for ZIP entry
                                    String relative = gameDir.relativize(file).toString().replace(File.separatorChar, '/');
                                    ZipEntry entry = new ZipEntry(relative);
                                    entry.setTime(attrs.lastModifiedTime().toMillis());
                                    zos.putNextEntry(entry);

                                    try (InputStream is = Files.newInputStream(file)) {
                                        int read;
                                        while ((read = is.read(buffer)) != -1) {
                                            zos.write(buffer, 0, read);
                                        }
                                    }
                                    zos.closeEntry();
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } else {
                            // Zip single file
                            String relative = configPath.replace(File.separatorChar, '/');
                            ZipEntry entry = new ZipEntry(relative);
                            entry.setTime(Files.getLastModifiedTime(sourcePath).toMillis());
                            zos.putNextEntry(entry);
                            try (InputStream is = Files.newInputStream(sourcePath)) {
                                int read;
                                while ((read = is.read(buffer)) != -1) {
                                    zos.write(buffer, 0, read);
                                }
                            }
                            zos.closeEntry();
                        }
                        pathsProcessed++;
                        int percentage = (pathsProcessed * 100) / CONFIG_PATHS.size();
                        progressCallback.accept(String.format("Zipping: %d%%", percentage));
                    }
                }

                if (debug) LOGGER.info("[Backup] Phase 2 complete: {}ms", System.currentTimeMillis() - phaseStart);

                long totalTime = System.currentTimeMillis() - startTime;
                if (debug) {
                    long zipSize = Files.size(backupZip);
                    LOGGER.info("╔══════════════════════════════════════════════════════════════╗");
                    LOGGER.info("║ BACKUP COMPLETE (Direct streaming: 2 phases)                 ║");
                    LOGGER.info("║ Total time: {}ms - Size: {}", totalTime, formatBytes(zipSize));
                    LOGGER.info("╚══════════════════════════════════════════════════════════════╝");
                } else {
                    LOGGER.info("Backup created in {}ms: {}", totalTime, backupZip.getFileName());
                }

                CompletableFuture.runAsync(() -> cleanupOldBackups(backupsDir), BACKUP_EXECUTOR);

                progressCallback.accept("Backup complete!");
                return backupZip;

            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - startTime;
                LOGGER.error("[Backup] FAILED after {}ms: {}", totalTime, e.getMessage(), e);
                progressCallback.accept("Backup failed: " + e.getMessage());
                throw new RuntimeException("Backup creation failed", e);
            }
        }, BACKUP_EXECUTOR);
    }

    private static String sanitizeForBackupId(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return null;
        s = s.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (s.isEmpty()) return null;
        int maxLen = 40;
        if (s.length() > maxLen) s = s.substring(0, maxLen).replaceAll("_+$", "");
        return s.isEmpty() ? null : s;
    }

    /**
     * Get list of all backups with metadata (async)
     */
    public static CompletableFuture<List<BackupInfo>> getBackupsAsync(Path gameDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path backupsDir = gameDir.resolve(BACKUPS_DIR);
                if (!Files.exists(backupsDir)) return new ArrayList<>();
                List<BackupInfo> backups = new ArrayList<>();
                try (Stream<Path> backupFiles = Files.list(backupsDir)) {
                    backupFiles.filter(path -> path.toString().endsWith(".zip"))
                            .forEach(backupZip -> {
                                BackupInfo info = readBackupMetadata(backupZip);
                                if (info != null) backups.add(info);
                            });
                }
                backups.sort((a, b) -> b.timestamp.compareTo(a.timestamp));
                return backups;
            } catch (IOException e) {
                LOGGER.error("Failed to list backups", e);
                return new ArrayList<>();
            }
        }, BACKUP_EXECUTOR);
    }

    public static CompletableFuture<List<BackupInfo>> getBackupsAsync() {
        return getBackupsAsync(getGameDirectory());
    }

    public static List<BackupInfo> getBackups() {
        try {
            return getBackupsAsync().get();
        } catch (Exception e) {
            LOGGER.error("Failed to get backups", e);
            return new ArrayList<>();
        }
    }

    private static BackupInfo readBackupMetadata(Path backupZip) {
        try (ZipFile zip = new ZipFile(backupZip.toFile())) {
            ZipEntry metadataEntry = zip.getEntry(METADATA_FILE);
            if (metadataEntry == null) return createLegacyBackupInfo(backupZip);

            try (InputStream inputStream = zip.getInputStream(metadataEntry)) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                BackupInfo info = GSON.fromJson(json, BackupInfo.class);
                if (info != null && info.sizeBytes() <= 0) {
                    long zipSize = Files.size(backupZip);
                    return new BackupInfo(info.backupId(), info.timestamp(), info.type(), info.configName(), info.configVersion(), zipSize, info.title(), info.description());
                }
                return info;
            }
        } catch (Exception e) {
            return createLegacyBackupInfo(backupZip);
        }
    }

    private static BackupInfo createLegacyBackupInfo(Path backupZip) {
        try {
            String fileName = backupZip.getFileName().toString();
            String backupId = fileName.replace(".zip", "");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            // Primitive parsing attempt
            if (fileName.contains("auto_")) timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long size = Files.size(backupZip);
            return new BackupInfo(backupId, timestamp, BackupType.MANUAL, "Legacy", "Unknown", size, "Legacy Backup", "No metadata");
        } catch (IOException e) {
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
                Path gameDir = getGameDirectory();
                Path backupsDir = gameDir.resolve(BACKUPS_DIR);
                Path backupZip = backupsDir.resolve(backupInfo.backupId + ".zip");

                if (!Files.exists(backupZip)) {
                    throw new FileNotFoundException("Backup file not found: " + backupInfo.backupId);
                }

                // Safety backup before restore
                createBackupAsync(BackupType.AUTO, "Pre-restore safety backup", "Created before restoring " + backupInfo.backupId, progressCallback).join();

                progressCallback.accept("Extracting backup...");

                // Extract backup to temp
                Path tempDir = Files.createTempDirectory("packcore_restore");
                try {
                    UnzipAsyncTask unzipTask = new UnzipAsyncTask();
                    unzipTask.unzipAsync(backupZip.toString(), tempDir.toString(), (p, t, percent) ->
                            progressCallback.accept("Extracting: " + percent + "%")
                    ).join();

                    progressCallback.accept("Applying restored files...");
                    copyRestoredFilesAsync(tempDir, gameDir, progressCallback).join();

                    return true;
                } finally {
                    CompletableFuture.runAsync(() -> {
                        try {
                            FileUtils.deleteDirectory(tempDir);
                        } catch (Exception ignored) {}
                    }, BACKUP_EXECUTOR);
                }

            } catch (Exception e) {
                LOGGER.error("Failed to restore backup", e);
                progressCallback.accept("Restore failed: " + e.getMessage());
                return false;
            }
        }, BACKUP_EXECUTOR);
    }

    public static boolean restoreBackup(BackupInfo backupInfo) {
        try {
            return restoreBackupAsync(backupInfo, msg -> {}).get();
        } catch (Exception e) {
            LOGGER.error("Failed to restore backup", e);
            return false;
        }
    }

    private static CompletableFuture<Void> copyRestoredFilesAsync(
            Path sourceDir, Path gameDir, Consumer<String> progressCallback) {

        return CompletableFuture.runAsync(() -> {
            try {
                List<Path> pathsToRestore;
                try (Stream<Path> paths = Files.walk(sourceDir)) {
                    pathsToRestore = paths.filter(Files::isRegularFile)
                            .filter(p -> !p.getFileName().toString().equals(METADATA_FILE))
                            .toList();
                }

                int total = pathsToRestore.size();
                AtomicLong processed = new AtomicLong(0);

                for (Path sourcePath : pathsToRestore) {
                    Path relativePath = sourceDir.relativize(sourcePath);
                    Path targetPath = gameDir.resolve(relativePath);

                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    long current = processed.incrementAndGet();
                    if (current % Math.max(1, total / 100) == 0 || current == total) {
                        int percent = (int) ((current * 100) / total);
                        progressCallback.accept("Restoring files: " + percent + "%");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to restore files", e);
            }
        }, BACKUP_EXECUTOR);
    }

    public static boolean deleteBackup(BackupInfo backupInfo) {
        try {
            Path gameDir = getGameDirectory();
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

    private static void cleanupOldBackups(Path backupsDir) {
        try {
            Path gameDir = backupsDir.getParent().getParent();
            List<BackupInfo> backups = getBackupsAsync(gameDir).get();
            List<BackupInfo> autoBackups = backups.stream()
                    .filter(backup -> backup.type == BackupType.AUTO)
                    .toList();

            if (autoBackups.size() > PackCoreConfig.maxBackups) {
                List<BackupInfo> toDelete = autoBackups.subList(PackCoreConfig.maxBackups, autoBackups.size());
                for (BackupInfo backup : toDelete) {
                    try {
                        Path backupZip = backupsDir.resolve(backup.backupId + ".zip");
                        if (Files.exists(backupZip)) {
                            Files.delete(backupZip);
                            LOGGER.info("Deleted old auto backup: {}", backup.backupId);
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Failed to delete clean up backup", e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to cleanup old backups", e);
        }
    }

    public static void openBackupsFolder() {
        CompletableFuture.runAsync(() -> {
            try {
                Path backupsDir = getGameDirectory().resolve(BACKUPS_DIR);
                Files.createDirectories(backupsDir);
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(backupsDir.toFile());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to open backups folder", e);
            }
        }, BACKUP_EXECUTOR);
    }

    private static Path getGameDirectory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.runDirectory != null) {
            return client.runDirectory.toPath();
        }
        return FabricLoader.getInstance().getGameDir();
    }

    public static void shutdown() {
        ScheduledBackupManager.shutdown();
        BACKUP_EXECUTOR.shutdown();
    }
}