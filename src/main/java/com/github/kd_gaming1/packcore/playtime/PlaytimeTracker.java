package com.github.kd_gaming1.packcore.playtime;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.configpack.BackupManager;
import com.github.kd_gaming1.packcore.gui.util.ToastHelper;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public final class PlaytimeTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/PlaytimeTracker");

    private static final Executor BACKUP_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private PlaytimeTracker() {}

    public static void onSessionStart() {
        long now = System.currentTimeMillis();

        if (!PackCoreConfig.autoBackupEnabled) {
            PackCoreConfig.lastSeenEpochMs = now;
            MidnightConfig.write(MOD_ID);
            return;
        }

        long intervalMs = PackCoreConfig.autoBackupIntervalDays * 24L * 60 * 60 * 1_000;
        long lastBackup = PackCoreConfig.lastBackupEpochMs;
        long lastSeen   = PackCoreConfig.lastSeenEpochMs;
        long msSinceBackup   = now - lastBackup;
        long msSinceLastSeen = now - lastSeen;

        boolean shouldAutoBackup = lastBackup > 0
                && msSinceBackup    >= intervalMs
                && msSinceLastSeen  <  intervalMs;

        PackCoreConfig.lastSeenEpochMs = now;
        if (shouldAutoBackup) {
            PackCoreConfig.lastBackupEpochMs = now;
        }
        MidnightConfig.write(MOD_ID);

        if (shouldAutoBackup) {
            triggerAutoBackupAsync();
        }
    }

    public static void onSessionEnd() {
        PackCoreConfig.lastSeenEpochMs = System.currentTimeMillis();
        MidnightConfig.write(MOD_ID);
    }

    private static void triggerAutoBackupAsync() {
        LOGGER.info("Scheduling automatic backup.");
        BACKUP_EXECUTOR.execute(() -> {
            try {
                BackupManager.createAutoBackup(FabricLoader.getInstance().getGameDir());
                Minecraft.getInstance().execute(() -> ToastHelper.showBackupCreated("Auto backup"));
            } catch (IOException e) {
                LOGGER.error("Automatic backup failed: {}", e.getMessage());
            }
        });
    }
}