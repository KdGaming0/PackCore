package com.github.kd_gaming1.packcore.config.update;

import com.github.kd_gaming1.packcore.config.backup.BackupManager;
import com.github.kd_gaming1.packcore.util.GsonUtils;
import com.github.kd_gaming1.packcore.util.io.zip.UnzipService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Manages automatic config updates from the modpack update distribution folder.
 * This system is separate from user imports and handles developer-controlled config updates.
 */
public class ConfigUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUpdateService.class);
    private static final Gson GSON = GsonUtils.GSON;

    // Update system paths
    private static final String UPDATES_FOLDER = "packcore/updates";
    private static final String APPLIED_UPDATES_FILE = "packcore/applied_updates.json";
    private static final String UPDATE_MANIFEST_FILE = "update_manifest.json";

    /**
     * Represents metadata for a config update package
     */
    public record UpdateManifest(
            String updateId,           // Unique ID for this update (e.g., "1.2.0_coolnewmod")
            String version,            // Version string (e.g., "1.2.0")
            String description,        // What this update contains
            String configFileName,     // Name of the zip file containing configs
            boolean createBackup,      // Whether to create backup before applying
            List<String> affectedMods  // List of mods this update affects
    ) {
        public UpdateManifest {
            if (updateId == null || updateId.isBlank()) {
                throw new IllegalArgumentException("updateId cannot be null or blank");
            }
            if (configFileName == null || configFileName.isBlank()) {
                throw new IllegalArgumentException("configFileName cannot be null or blank");
            }
            affectedMods = affectedMods != null ? new ArrayList<>(affectedMods) : new ArrayList<>();
        }

        public boolean isValid() {
            return updateId != null && !updateId.isBlank() &&
                    configFileName != null && !configFileName.isBlank();
        }
    }

    /**
     * Tracks which updates have been applied
     */
    private record AppliedUpdatesRecord(
            List<String> appliedUpdateIds,
            String lastAppliedVersion,
            String lastAppliedDate
    ) {
        public AppliedUpdatesRecord {
            appliedUpdateIds = appliedUpdateIds != null ? new ArrayList<>(appliedUpdateIds) : new ArrayList<>();
        }

        public static AppliedUpdatesRecord empty() {
            return new AppliedUpdatesRecord(new ArrayList<>(), null, null);
        }

        public boolean hasApplied(String updateId) {
            return appliedUpdateIds.contains(updateId);
        }

        public AppliedUpdatesRecord withNewUpdate(String updateId, String version) {
            List<String> newList = new ArrayList<>(appliedUpdateIds);
            if (!newList.contains(updateId)) {
                newList.add(updateId);
            }
            String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return new AppliedUpdatesRecord(newList, version, date);
        }
    }

    /**
     * Check for and apply any pending config updates
     *
     * @param gameDir The game directory
     */
    public static void checkAndApplyUpdates(Path gameDir) {
        LOGGER.info("Checking for config updates...");

        Path updatesFolder = gameDir.resolve(UPDATES_FOLDER);

        // Create updates folder if it doesn't exist
        if (!Files.exists(updatesFolder)) {
            try {
                Files.createDirectories(updatesFolder);
                LOGGER.info("Created updates folder: {}", updatesFolder);
                createReadmeFile(updatesFolder);
            } catch (IOException e) {
                LOGGER.error("Failed to create updates folder", e);
            }
            return;
        }

        // Load record of applied updates
        AppliedUpdatesRecord appliedRecord = loadAppliedUpdates(gameDir);

        // Scan for available updates
        List<UpdatePackage> availableUpdates = scanForUpdates(updatesFolder);

        if (availableUpdates.isEmpty()) {
            LOGGER.info("No config updates found");
            return;
        }

        LOGGER.info("Found {} update package(s)", availableUpdates.size());

        // Filter out already applied updates
        List<UpdatePackage> pendingUpdates = availableUpdates.stream()
                .filter(pkg -> !appliedRecord.hasApplied(pkg.manifest.updateId()))
                .toList();

        if (pendingUpdates.isEmpty()) {
            LOGGER.info("All updates have already been applied");
            return;
        }

        LOGGER.info("Found {} pending update(s) to apply", pendingUpdates.size());

        // Apply each pending update
        boolean anyApplied = false;
        AppliedUpdatesRecord currentRecord = appliedRecord;

        for (UpdatePackage updatePkg : pendingUpdates) {
            LOGGER.info("Applying update: {} ({})", updatePkg.manifest.updateId(), updatePkg.manifest.description());

            boolean success = applyUpdate(gameDir, updatePkg);

            if (success) {
                // Mark as applied
                currentRecord = currentRecord.withNewUpdate(
                        updatePkg.manifest.updateId(),
                        updatePkg.manifest.version()
                );
                anyApplied = true;
                LOGGER.info("Successfully applied update: {}", updatePkg.manifest.updateId());

                // Archive the update package
                archiveUpdate(updatePkg);
            } else {
                LOGGER.error("Failed to apply update: {}", updatePkg.manifest.updateId());
            }
        }

        // Save updated record
        if (anyApplied) {
            saveAppliedUpdates(gameDir, currentRecord);
            LOGGER.info("Config updates applied successfully. Last version: {}", currentRecord.lastAppliedVersion);
        }
    }

    /**
     * Represents an update package ready to be applied
     */
    private record UpdatePackage(
            Path manifestPath,
            Path configZipPath,
            UpdateManifest manifest
    ) {}

    /**
     * Scan the updates folder for available update packages
     */
    private static List<UpdatePackage> scanForUpdates(Path updatesFolder) {
        List<UpdatePackage> updates = new ArrayList<>();

        try (Stream<Path> files = Files.list(updatesFolder)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(UPDATE_MANIFEST_FILE))
                    .forEach(manifestPath -> {
                        try {
                            // Read manifest
                            String json = Files.readString(manifestPath, StandardCharsets.UTF_8);
                            UpdateManifest manifest = GSON.fromJson(json, UpdateManifest.class);

                            if (manifest == null || !manifest.isValid()) {
                                LOGGER.warn("Invalid manifest at: {}", manifestPath);
                                return;
                            }

                            // Find corresponding config zip
                            Path configZip = manifestPath.getParent().resolve(manifest.configFileName());

                            if (!Files.exists(configZip)) {
                                LOGGER.warn("Config file not found for manifest: {} (expected: {})",
                                        manifestPath, manifest.configFileName());
                                return;
                            }

                            updates.add(new UpdatePackage(manifestPath, configZip, manifest));
                            LOGGER.debug("Found update package: {} at {}", manifest.updateId(), manifestPath.getParent());

                        } catch (IOException | JsonSyntaxException e) {
                            LOGGER.error("Failed to read manifest: {}", manifestPath, e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Failed to scan updates folder", e);
        }

        return updates;
    }

    /**
     * Apply an update package to the game directory
     */
    private static boolean applyUpdate(Path gameDir, UpdatePackage updatePkg) {
        try {
            UpdateManifest manifest = updatePkg. manifest;

            // Create backup if requested
            if (manifest.createBackup()) {
                LOGGER.info("Creating backup before applying update.. .");

                // Use pre-launch safe backup method with explicit gameDir
                Path backup = BackupManager.createBackupAsync(
                        gameDir,
                        BackupManager.BackupType.AUTO,
                        "Auto backup before update: " + manifest.updateId(),
                        "Backup created before applying config update",
                        msg -> LOGGER.debug("Backup progress: {}", msg)
                ).join();

                if (backup != null) {
                    LOGGER.info("Backup created: {}", backup. getFileName());
                } else {
                    LOGGER.warn("Failed to create backup, but continuing with update");
                }
            }

            // Extract the config zip to game directory
            LOGGER.info("Extracting update configs from: {}", updatePkg.configZipPath. getFileName());

            UnzipService unzipper = new UnzipService();
            unzipper.unzip(
                    updatePkg.configZipPath.toString(),
                    gameDir.toString(),
                    (bytesProcessed, totalBytes, percentage) -> {
                        if (percentage % 25 == 0) {
                            LOGGER.info("Update extraction progress: {}%", percentage);
                        }
                    }
            );

            LOGGER.info("Update configs extracted successfully");
            return true;

        } catch (IOException e) {
            LOGGER. error("Failed to apply update", e);
            return false;
        }
    }

    /**
     * Archive an applied update by moving it to an archive subfolder
     */
    private static void archiveUpdate(UpdatePackage updatePkg) {
        try {
            Path archiveFolder = updatePkg.manifestPath.getParent().resolve("applied");
            Files.createDirectories(archiveFolder);

            // Create timestamped archive folder
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path archiveSubfolder = archiveFolder.resolve(updatePkg.manifest.updateId() + "_" + timestamp);
            Files.createDirectories(archiveSubfolder);

            // Move manifest
            Path archivedManifest = archiveSubfolder.resolve(updatePkg.manifestPath.getFileName());
            Files.move(updatePkg.manifestPath, archivedManifest, StandardCopyOption.REPLACE_EXISTING);

            // Move config zip
            Path archivedZip = archiveSubfolder.resolve(updatePkg.configZipPath.getFileName());
            Files.move(updatePkg.configZipPath, archivedZip, StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info("Archived update to: {}", archiveSubfolder);

        } catch (IOException e) {
            LOGGER.warn("Failed to archive update package", e);
            // Non-critical error, continue
        }
    }

    /**
     * Load the record of applied updates
     */
    private static AppliedUpdatesRecord loadAppliedUpdates(Path gameDir) {
        Path recordPath = gameDir.resolve(APPLIED_UPDATES_FILE);

        if (!Files.exists(recordPath)) {
            return AppliedUpdatesRecord.empty();
        }

        try {
            String json = Files.readString(recordPath, StandardCharsets.UTF_8);
            AppliedUpdatesRecord record = GSON.fromJson(json, AppliedUpdatesRecord.class);
            return record != null ? record : AppliedUpdatesRecord.empty();
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to load applied updates record", e);
            return AppliedUpdatesRecord.empty();
        }
    }

    /**
     * Save the record of applied updates
     */
    private static void saveAppliedUpdates(Path gameDir, AppliedUpdatesRecord record) {
        Path recordPath = gameDir.resolve(APPLIED_UPDATES_FILE);

        try {
            Files.createDirectories(recordPath.getParent());
            String json = GSON.toJson(record);
            Files.writeString(recordPath, json, StandardCharsets.UTF_8);
            LOGGER.info("Saved applied updates record");
        } catch (IOException e) {
            LOGGER.error("Failed to save applied updates record", e);
        }
    }

    /**
     * Create a helpful README in the updates folder
     */
    private static void createReadmeFile(Path updatesFolder) {
        Path readmePath = updatesFolder.resolve("README.txt");

        try {
            String content = """
                ═══════════════════════════════════════════════════════════
                PackCore Automatic Config Updates Folder
                ═══════════════════════════════════════════════════════════
                
                📦 What is this folder?
                
                This folder is used for automatic config updates when you
                release a modpack update. It allows you to ship config files
                for new mods without overwriting users' existing configs.
                
                ═══════════════════════════════════════════════════════════
                🔧 How to use (for modpack developers):
                ═══════════════════════════════════════════════════════════
                
                1. Create your config update zip file
                   - Include ONLY the new/changed config files
                   - Example: config/newmod/settings.json
                
                2. Create update_manifest.json with this structure:
                   {
                     "updateId": "1.2.0_newmod",
                     "version": "1.2.0",
                     "description": "Added NewMod configuration",
                     "configFileName": "newmod_config.zip",
                     "createBackup": true,
                     "affectedMods": ["newmod"]
                   }
                
                3. Place both files in this folder for distribution
                
                4. When users update the modpack, configs apply automatically
                
                ═══════════════════════════════════════════════════════════
                📋 Update Manifest Fields:
                ═══════════════════════════════════════════════════════════
                
                updateId:       Unique identifier (prevents re-applying)
                version:        Modpack version (e.g., "1.2.0")
                description:    What this update contains
                configFileName: Name of the zip file to extract
                createBackup:   Whether to backup before applying
                affectedMods:   List of affected mod names
                
                ═══════════════════════════════════════════════════════════
                ⚠️ Important Notes:
                ═══════════════════════════════════════════════════════════
                
                - Each updateId can only be applied once
                - Applied updates are moved to the 'applied' subfolder
                - Users on fresh installs get full configs (updates skipped)
                - Only existing users will receive these updates
                
                ═══════════════════════════════════════════════════════════
                📂 Folder Structure After Updates:
                ═══════════════════════════════════════════════════════════
                
                packcore/updates/
                ├── update_manifest.json       ← Pending update
                ├── newmod_config.zip          ← Pending update
                ├── README.txt                 ← This file
                └── applied/                   ← Archive of applied updates
                    └── 1.2.0_newmod_20250101_120000/
                        ├── update_manifest.json
                        └── newmod_config.zip
                
                ═══════════════════════════════════════════════════════════
                """;

            Files.writeString(readmePath, content);
            LOGGER.info("Created updates README: {}", readmePath);
        } catch (IOException e) {
            LOGGER.error("Failed to create updates README", e);
        }
    }

    /**
     * Manually trigger update check (for debugging/testing)
     */
    public static void forceUpdateCheck(Path gameDir) {
        LOGGER.info("Forcing update check...");
        checkAndApplyUpdates(gameDir);
    }

    /**
     * Get list of applied updates (for UI display)
     */
    public static List<String> getAppliedUpdateIds(Path gameDir) {
        AppliedUpdatesRecord record = loadAppliedUpdates(gameDir);
        return new ArrayList<>(record.appliedUpdateIds);
    }

    /**
     * Reset applied updates (for testing - use with caution!)
     */
    public static void resetAppliedUpdates(Path gameDir) {
        Path recordPath = gameDir.resolve(APPLIED_UPDATES_FILE);
        try {
            Files.deleteIfExists(recordPath);
            LOGGER.warn("Reset applied updates record");
        } catch (IOException e) {
            LOGGER.error("Failed to reset applied updates", e);
        }
    }
}