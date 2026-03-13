package com.github.kd_gaming1.packcore.playtime;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.configpack.BackupManager;
import com.github.kd_gaming1.packcore.gui.util.ToastHelper;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public final class PlaytimeTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/PlaytimeTracker");

    private static final long BACKUP_INTERVAL_MS = 3L * 24 * 60 * 60 * 1_000;

    private static final DateTimeFormatter BACKUP_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

    private PlaytimeTracker() {}

    public static void onSessionStart() {
        long now = System.currentTimeMillis();
        long lastBackup = PackCoreConfig.lastBackupEpochMs;
        long lastSeen = PackCoreConfig.lastSeenEpochMs;
        long msSinceBackup = now - lastBackup;
        long msSinceLastSeen = now - lastSeen;

        boolean shouldAutoBackup = lastBackup > 0
                && msSinceBackup >= BACKUP_INTERVAL_MS
                && msSinceLastSeen < BACKUP_INTERVAL_MS;

        PackCoreConfig.lastSeenEpochMs = now;
        if (shouldAutoBackup) {
            PackCoreConfig.lastBackupEpochMs = now;
        }
        MidnightConfig.write(MOD_ID);

        if (shouldAutoBackup) {
            triggerAutoBackup();
        }
    }

    public static void onSessionEnd() {
        PackCoreConfig.lastSeenEpochMs = System.currentTimeMillis();
        MidnightConfig.write(MOD_ID);
    }

    private static void triggerAutoBackup() {
        String backupName = "auto_" + BACKUP_NAME_FORMAT.format(Instant.now()) + ".zip";
        LOGGER.info("Creating automatic backup: {}", backupName);

        try {
            BackupManager.createBackup(FabricLoader.getInstance().getGameDir());
            ToastHelper.showBackupCreated(backupName);
        } catch (IOException e) {
            LOGGER.error("Automatic backup failed: {}", e.getMessage());
        }
    }
}