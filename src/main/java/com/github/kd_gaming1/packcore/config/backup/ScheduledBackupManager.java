package com.github.kd_gaming1.packcore.config.backup;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.ui.toast.PackCoreToast;
import com.github.kd_gaming1.packcore.util.GsonUtils;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages automatic scheduled backups every 3 days when user is active
 */
public class ScheduledBackupManager {
    private static final Gson GSON = GsonUtils.GSON;

    private static final String LAST_BACKUP_FILE = "packcore/last_auto_backup.json";
    private static final String LAST_ACTIVE_FILE = "packcore/last_active.json";
    private static final long THREE_DAYS_MILLIS = 3 * 24 * 60 * 60 * 1000L;

    private static ScheduledExecutorService scheduledExecutor;

    /**
     * Tracking data for scheduled backups
     */
    private record BackupTracking(long lastBackupTimestamp, long lastActiveTimestamp) {
        public static BackupTracking loadOrCreate(Path filePath) {
            try {
                if (Files.exists(filePath)) {
                    String json = Files.readString(filePath, StandardCharsets.UTF_8);
                    return GSON.fromJson(json, BackupTracking.class);
                }
            } catch (Exception e) {
                PackCore.LOGGER.warn("[ScheduledBackupManager] Failed to load backup tracking", e);
            }
            return new BackupTracking(0, 0);
        }

        public void save(Path filePath) {
            try {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, GSON.toJson(this), StandardCharsets.UTF_8);
            } catch (IOException e) {
                PackCore.LOGGER.error("[ScheduledBackupManager] Failed to save backup tracking", e);
            }
        }
    }

    /**
     * Initialize the scheduled backup system
     * Call this when the game starts
     */
    public static void initialize() {
        if (scheduledExecutor != null) {
            PackCore.LOGGER.debug("[ScheduledBackupManager] Scheduled backup manager already initialized");
            return;
        }

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("ScheduledBackupManager");
            thread.setDaemon(true);
            return thread;
        });

        // Update last active timestamp on startup
        updateLastActive();

        // Schedule periodic backup check (check every hour)
        scheduledExecutor.scheduleAtFixedRate(
                ScheduledBackupManager::checkAndPerformScheduledBackup,
                1, // Initial delay: 1 hour
                1, // Period: 1 hour
                TimeUnit.HOURS
        );

        PackCore.LOGGER.info("[ScheduledBackupManager] Scheduled backup manager initialized (checks every hour, backups every 3 days)");
    }

    /**
     * Update the last active timestamp
     * Call this periodically (e.g., on game startup)
     */
    public static void updateLastActive() {
        try {
            Path gameDir = getGameDirectorySafe();
            Path activeFile = gameDir.resolve(LAST_ACTIVE_FILE);

            long currentTime = System.currentTimeMillis();
            Files.createDirectories(activeFile.getParent());
            Files.writeString(activeFile, String.valueOf(currentTime), StandardCharsets.UTF_8);

            PackCore.LOGGER.debug("[ScheduledBackupManager] Updated last active timestamp: {}", currentTime);
        } catch (Exception e) {
            PackCore. LOGGER.error("[ScheduledBackupManager] Failed to update last active timestamp", e);
        }
    }

    /**
     * Check if a scheduled backup should be performed and execute it
     */
    private static void checkAndPerformScheduledBackup() {
        if (! PackCoreConfig.enableAutoBackups) {
            PackCore.LOGGER.debug("[ScheduledBackupManager] Auto backups disabled, skipping scheduled backup check");
            return;
        }

        try {
            Path gameDir = getGameDirectorySafe();
            Path trackingFile = gameDir.resolve(LAST_BACKUP_FILE);

            BackupTracking tracking = BackupTracking.loadOrCreate(trackingFile);
            long currentTime = System.currentTimeMillis();
            long timeSinceLastBackup = currentTime - tracking.lastBackupTimestamp;

            // Check if 3 days have passed since last backup
            if (timeSinceLastBackup < THREE_DAYS_MILLIS) {
                long hoursRemaining = (THREE_DAYS_MILLIS - timeSinceLastBackup) / (1000 * 60 * 60);
                PackCore.LOGGER.debug("[ScheduledBackupManager] Not time for scheduled backup yet.  Hours remaining: {}", hoursRemaining);
                return;
            }

            // Check if user has been active in the past 3 days
            if (! hasBeenActiveInPastThreeDays()) {
                PackCore.LOGGER.debug("[ScheduledBackupManager] User has not been active in past 3 days, skipping scheduled backup");
                return;
            }

            PackCore.LOGGER.info("[ScheduledBackupManager] Performing scheduled automatic backup");

            String title = "Scheduled backup - " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String description = "Automatic backup created after 3 days of activity";

            BackupManager.createBackupAsync(
                    gameDir,
                    BackupManager.BackupType.AUTO,
                    title,
                    description,
                    msg -> PackCore.LOGGER.debug("[ScheduledBackupManager] Scheduled backup: {}", msg)
            ).thenAccept(backupPath -> {
                if (backupPath != null) {
                    // Update tracking with new backup timestamp
                    BackupTracking newTracking = new BackupTracking(
                            System.currentTimeMillis(),
                            tracking.lastActiveTimestamp
                    );
                    newTracking.save(trackingFile);
                    PackCore.LOGGER.info("[ScheduledBackupManager] Scheduled backup completed: {}", backupPath.getFileName());

                    // Show toast notification on the main thread (only if client is available)
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) {
                        client.execute(() -> {
                            PackCoreToast.showBackupComplete(
                                    "Scheduled Config Backup",
                                    backupPath.getFileName().toString(),
                                    false
                            );
                        });
                    }
                }
            }). exceptionally(ex -> {
                PackCore.LOGGER.error("[ScheduledBackupManager] Scheduled backup failed", ex);

                // Show error toast on the main thread (only if client is available)
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.execute(() -> {
                        PackCoreToast.showError(
                                "Scheduled Config Backup Failed",
                                "Check logs for details"
                        );
                    });
                }
                return null;
            });

        } catch (Exception e) {
            PackCore.LOGGER. error("[ScheduledBackupManager] Failed to check/perform scheduled backup", e);
        }
    }

    /**
     * Check if user has been active in the past 3 days
     */
    private static boolean hasBeenActiveInPastThreeDays() {
        try {
            Path gameDir = getGameDirectorySafe();
            Path activeFile = gameDir.resolve(LAST_ACTIVE_FILE);

            if (!Files.exists(activeFile)) {
                PackCore.LOGGER.debug("[ScheduledBackupManager] No activity file found, considering user active");
                return true;
            }

            String content = Files.readString(activeFile, StandardCharsets.UTF_8);
            long lastActive = Long.parseLong(content. trim());
            long currentTime = System.currentTimeMillis();
            long daysSinceActive = (currentTime - lastActive) / (1000 * 60 * 60 * 24);

            PackCore.LOGGER.debug("[ScheduledBackupManager] Days since last active: {}", daysSinceActive);
            return daysSinceActive <= 3;

        } catch (Exception e) {
            PackCore.LOGGER.warn("[ScheduledBackupManager] Failed to check activity, assuming user is active", e);
            return true;
        }
    }

    /**
     * Get time until next scheduled backup (in milliseconds)
     * Returns -1 if no backup is scheduled
     */
    public static long getTimeUntilNextBackup() {
        try {
            Path gameDir = getGameDirectorySafe();
            Path trackingFile = gameDir.resolve(LAST_BACKUP_FILE);

            BackupTracking tracking = BackupTracking.loadOrCreate(trackingFile);
            long currentTime = System.currentTimeMillis();
            long timeSinceLastBackup = currentTime - tracking.lastBackupTimestamp;

            if (timeSinceLastBackup >= THREE_DAYS_MILLIS) {
                return 0; // Backup is due now
            }

            return THREE_DAYS_MILLIS - timeSinceLastBackup;

        } catch (Exception e) {
            PackCore. LOGGER.warn("[ScheduledBackupManager] Failed to calculate time until next backup", e);
            return -1;
        }
    }

    /**
     * Safely get game directory - works during pre-launch and post-launch
     */
    private static Path getGameDirectorySafe() {
        // Try to get from MinecraftClient first (post-launch)
        MinecraftClient client = MinecraftClient. getInstance();
        if (client != null && client.runDirectory != null) {
            return client.runDirectory. toPath();
        }

        // Fallback to FabricLoader for pre-launch
        return FabricLoader.getInstance().getGameDir();
    }

    /**
     * Shutdown the scheduled backup manager
     * Call this when the game closes
     */
    public static void shutdown() {
        if (scheduledExecutor != null) {
            PackCore.LOGGER.info("[ScheduledBackupManager] Shutting down scheduled backup manager");
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduledExecutor = null;
        }
    }
}