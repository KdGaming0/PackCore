package com.github.kd_gaming1.packcore.config. backup;

import com.github. kd_gaming1.packcore.config.apply.SelectiveConfigApplyService.SelectableFile;
import com.github.kd_gaming1.packcore.config.apply.FileDescriptionRegistry;
import com.github.kd_gaming1.packcore.util.GsonUtils;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Service for selectively restoring specific files from a backup
 */
public class SelectiveBackupRestoreService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectiveBackupRestoreService.class);
    private static final Gson GSON = GsonUtils. GSON;
    private static final String PENDING_SELECTIVE_RESTORE_FILE = "packcore_pending_selective_restore.json";

    /**
     * Data class for pending selective restore info
     */
    private static class PendingSelectiveRestore {
        String backupId;
        String backupName;
        Set<String> selectedPaths;

        PendingSelectiveRestore(String backupId, String backupName, Set<String> selectedPaths) {
            this.backupId = backupId;
            this.backupName = backupName;
            this.selectedPaths = selectedPaths;
        }
    }

    /**
     * Scan a backup file and build a tree of restorable files
     */
    public static CompletableFuture<List<SelectableFile>> scanBackupFiles(
            BackupManager. BackupInfo backup) {
        return CompletableFuture. supplyAsync(() -> {
            List<SelectableFile> files = new ArrayList<>();

            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path backupsDir = gameDir.resolve("packcore/backups");
            Path backupZip = backupsDir.resolve(backup.backupId() + ".zip");

            if (! Files.exists(backupZip)) {
                LOGGER.error("Backup file not found: {}", backupZip);
                return files;
            }

            try (ZipFile zipFile = new ZipFile(backupZip.toFile())) {
                Enumeration<?  extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // Skip metadata file
                    if (entryName.equals("backup_metadata.json")) {
                        continue;
                    }

                    SelectableFile. FileType type = determineFileType(entryName);

                    // Get description from registry
                    var description = FileDescriptionRegistry.getDescription(entryName);
                    String displayName = description
                            .map(FileDescriptionRegistry. FileDescription::displayName)
                            .orElseGet(() -> getDisplayName(entryName));

                    String desc = description
                            .map(FileDescriptionRegistry.FileDescription::description)
                            .orElseGet(() -> getFileDescription(entryName, type));

                    files.add(new SelectableFile(
                            entryName,
                            displayName,
                            type,
                            entry.getSize(),
                            desc,
                            entry.isDirectory()
                    ));
                }

                // Sort by type and then name
                files.sort(Comparator
                        .comparing((SelectableFile f) -> f.type(). ordinal())
                        .thenComparing(f -> f.displayName()));

            } catch (IOException e) {
                LOGGER. error("Failed to scan backup files", e);
            }

            return files;
        });
    }

    /**
     * Schedule selected files to be restored on next game start
     */
    public static CompletableFuture<Boolean> scheduleSelectiveRestore(
            BackupManager.BackupInfo backup,
            Set<String> selectedPaths) {
        return CompletableFuture. supplyAsync(() -> {
            try {
                Path gameDir = FabricLoader.getInstance().getGameDir();
                Path pendingFile = gameDir.resolve(PENDING_SELECTIVE_RESTORE_FILE);

                // Create pending restore info
                PendingSelectiveRestore pending = new PendingSelectiveRestore(
                        backup.backupId(),
                        backup.getDisplayName(),
                        selectedPaths
                );

                // Write to file
                String json = GSON.toJson(pending);
                Files.writeString(pendingFile, json, StandardCharsets. UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                LOGGER.info("[Selective Backup Restore] Scheduled {} files for restoration from: {}",
                        selectedPaths.size(), backup.getDisplayName());

                // Schedule game shutdown
                MinecraftClient.getInstance().scheduleStop();

                return true;

            } catch (IOException e) {
                LOGGER.error("[Selective Backup Restore] Failed to schedule selective restore", e);
                return false;
            }
        });
    }

    /**
     * Check and apply pending selective restore during pre-launch
     *
     * @return true if a selective restore was performed
     */
    public static boolean checkAndApplyPendingSelectiveRestore(Path gameDir) {
        Path pendingFile = gameDir.resolve(PENDING_SELECTIVE_RESTORE_FILE);

        if (!Files.exists(pendingFile)) {
            return false;
        }

        try {
            // Read pending restore info
            String json = Files.readString(pendingFile, StandardCharsets.UTF_8);
            PendingSelectiveRestore pending = GSON.fromJson(json, PendingSelectiveRestore.class);

            if (pending == null || pending.backupId == null || pending.selectedPaths == null) {
                LOGGER.warn("[Selective Backup Restore] Invalid pending selective restore file");
                Files.deleteIfExists(pendingFile);
                return false;
            }

            LOGGER.info("[Selective Backup Restore] Found pending selective restore: {} ({} files)",
                    pending.backupName, pending.selectedPaths.size());

            // Get backup zip path
            Path backupsDir = gameDir.resolve("packcore/backups");
            Path backupZip = backupsDir.resolve(pending.backupId + ".zip");

            if (!Files.exists(backupZip)) {
                LOGGER.error("[Selective Backup Restore] Backup file not found: {}", backupZip);
                Files. deleteIfExists(pendingFile);
                return false;
            }

            // Create a safety backup before restoring
            LOGGER.info("[Selective Backup Restore] Creating safety backup before restore.. .");
            BackupManager.createBackupAsync(
                    gameDir,
                    BackupManager.BackupType.AUTO,
                    "Safety backup before selective restore",
                    "Auto backup created before restoring files from: " + pending.backupId,
                    msg -> {}
            ).join();

            // Restore the selected files
            boolean success = restoreSelectedFilesSync(
                    backupZip,
                    pending.selectedPaths,
                    gameDir,
                    msg -> LOGGER.info("[Selective Backup Restore] {}", msg)
            );

            if (success) {
                LOGGER.info("[Selective Backup Restore] Successfully restored {} selected files",
                        pending. selectedPaths.size());
            } else {
                LOGGER. error("[Selective Backup Restore] Failed to restore selected files");
            }

            // Clean up pending file
            Files.deleteIfExists(pendingFile);

            return success;

        } catch (Exception e) {
            LOGGER. error("[Selective Backup Restore] Error processing pending selective restore", e);
            try {
                Files.deleteIfExists(pendingFile);
            } catch (IOException ex) {
                LOGGER. warn("[Selective Backup Restore] Failed to clean up pending file", ex);
            }
            return false;
        }
    }

    /**
     * Restore selected files synchronously (for pre-launch)
     */
    private static boolean restoreSelectedFilesSync(
            Path backupZip,
            Set<String> selectedPaths,
            Path gameDir,
            Consumer<String> progressCallback) {

        try {
            progressCallback.accept("Extracting selected files...");

            // Extract only selected files
            Path tempDir = Files.createTempDirectory("packcore_selective_restore");
            try {
                extractSelectedFiles(backupZip, selectedPaths, tempDir, progressCallback);

                progressCallback.accept("Restoring files...");

                // Copy extracted files to game directory
                copyFilesToGameDir(tempDir, gameDir, progressCallback);

                LOGGER.info("[Selective Backup Restore] Successfully restored {} selected files",
                        selectedPaths.size());
                progressCallback.accept("Restore complete!");
                return true;

            } finally {
                // Cleanup temp directory
                deleteDirectory(tempDir);
            }

        } catch (Exception e) {
            LOGGER.error("[Selective Backup Restore] Failed to restore selected files", e);
            progressCallback.accept("Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extract only the selected files from the backup
     */
    private static void extractSelectedFiles(
            Path backupZip,
            Set<String> selectedPaths,
            Path tempDir,
            Consumer<String> progressCallback) throws IOException {

        try (ZipFile zipFile = new ZipFile(backupZip.toFile())) {
            int processed = 0;
            int total = selectedPaths.size();

            for (String selectedPath : selectedPaths) {
                ZipEntry entry = zipFile.getEntry(selectedPath);
                if (entry == null) {
                    LOGGER.warn("Entry not found in backup: {}", selectedPath);
                    continue;
                }

                Path targetPath = tempDir.resolve(selectedPath);

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);

                    // Extract all files in this directory
                    extractDirectory(zipFile, selectedPath, tempDir);
                } else {
                    Files.createDirectories(targetPath. getParent());

                    try (var is = zipFile.getInputStream(entry)) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                processed++;
                int percentage = (processed * 100) / total;
                progressCallback.accept(String. format("Extracting: %d%%", percentage));
            }
        }
    }

    /**
     * Extract all files in a directory from the backup
     */
    private static void extractDirectory(ZipFile zipFile, String dirPath, Path tempDir)
            throws IOException {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry. getName();

            if (entryName.startsWith(dirPath) && !entry.isDirectory()) {
                Path targetPath = tempDir.resolve(entryName);
                Files. createDirectories(targetPath. getParent());

                try (var is = zipFile.getInputStream(entry)) {
                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Copy extracted files to game directory
     */
    private static void copyFilesToGameDir(Path tempDir, Path gameDir, Consumer<String> progressCallback)
            throws IOException {
        List<Path> filesToCopy = new ArrayList<>();

        Files.walk(tempDir). forEach(filesToCopy::add);

        int total = filesToCopy.size();
        int processed = 0;

        for (Path sourcePath : filesToCopy) {
            if (Files.isRegularFile(sourcePath)) {
                Path relativePath = tempDir.relativize(sourcePath);
                Path targetPath = gameDir.resolve(relativePath);

                Files.createDirectories(targetPath.getParent());
                Files.copy(sourcePath, targetPath, StandardCopyOption. REPLACE_EXISTING);

                LOGGER.debug("Restored: {}", relativePath);
            }

            processed++;
            if (processed % 10 == 0) {
                int percentage = (processed * 100) / total;
                progressCallback. accept(String.format("Restoring files: %d%%", percentage));
            }
        }
    }

    /**
     * Delete directory recursively
     */
    private static void deleteDirectory(Path directory) {
        try {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            LOGGER.debug("Could not delete: {}", path);
                        }
                    });
        } catch (IOException e) {
            LOGGER.warn("Failed to delete temp directory: {}", directory);
        }
    }

    // Helper methods

    private static SelectableFile.FileType determineFileType(String path) {
        path = path.toLowerCase();

        if (path.equals("options.txt")) {
            return SelectableFile.FileType. GAME_OPTIONS;
        } else if (path.equals("servers.dat")) {
            return SelectableFile.FileType.SERVER_LIST;
        } else if (path.startsWith("config/")) {
            return SelectableFile.FileType.MOD_CONFIG;
        } else if (path.startsWith("resourcepacks/")) {
            return SelectableFile.FileType. RESOURCE_PACK;
        }

        return SelectableFile.FileType.OTHER;
    }

    private static String getDisplayName(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }

        return path;
    }

    private static String getFileDescription(String path, SelectableFile.FileType type) {
        // Extract mod name from config path
        if (path.startsWith("config/")) {
            String fileName = path.substring("config/".length());
            int dotIndex = fileName.indexOf('.');
            if (dotIndex > 0) {
                String modName = fileName.substring(0, dotIndex);
                modName = modName.substring(0, 1).toUpperCase() + modName.substring(1);
                return "Configuration for " + modName;
            }
        }

        return switch (type) {
            case MOD_CONFIG -> "Mod configuration file";
            case GAME_OPTIONS -> "Minecraft game settings";
            case KEYBINDINGS -> "Control bindings";
            case RESOURCE_PACK -> "Resource pack";
            case SERVER_LIST -> "Multiplayer server list";
            case OTHER -> "Configuration file";
        };
    }
}