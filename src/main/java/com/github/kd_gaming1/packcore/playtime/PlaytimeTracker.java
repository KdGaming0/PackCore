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

/**
 * Triggers an automatic backup every 3 real-world days, but only when the player
 * was also active in that window — returning after a long absence does not trigger one.
 *
 * <p>On session start it checks whether a backup is due. It will only fire if:
 * <ul>
 *   <li>At least {@value BACKUP_INTERVAL_MS} ms have passed since the last backup, and</li>
 *   <li>The player was last seen within that same interval (i.e. they played recently).</li>
 * </ul>
 *
 * <p>Call {@link #onSessionStart()} at client init and {@link #onSessionEnd()} on disconnect.
 */
public final class PlaytimeTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/PlaytimeTracker");

    /** 3 days in milliseconds. */
    private static final long BACKUP_INTERVAL_MS = 3L * 24 * 60 * 60 * 1_000;

    private static final DateTimeFormatter BACKUP_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

    private PlaytimeTracker() {}

    /**
     * Called once when the client starts. Checks whether a backup is due and fires
     * one if the player was active within the last 3 days.
     */
    public static void onSessionStart() {
        long now            = System.currentTimeMillis();
        long lastBackup     = PackCoreConfig.lastBackupEpochMs;
        long lastSeen       = PackCoreConfig.lastSeenEpochMs;
        long msSinceBackup  = now - lastBackup;
        long msSinceLastSeen = now - lastSeen;

        // Only back up if 3 days have passed AND the player was here within those 3 days.
        // If they were away longer than the interval, skip — they haven't been playing.
        if (lastBackup > 0
                && msSinceBackup  >= BACKUP_INTERVAL_MS
                && msSinceLastSeen < BACKUP_INTERVAL_MS) {
            triggerAutoBackup();
        }

        // Record this session start as "last seen"
        PackCoreConfig.lastSeenEpochMs = now;
        MidnightConfig.write(MOD_ID);
    }

    /** Call on disconnect to record the player's last-seen timestamp. */
    public static void onSessionEnd() {
        PackCoreConfig.lastSeenEpochMs = System.currentTimeMillis();
        MidnightConfig.write(MOD_ID);
    }

    private static void triggerAutoBackup() {
        PackCoreConfig.lastBackupEpochMs = System.currentTimeMillis();
        MidnightConfig.write(MOD_ID);

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