package com.github.kd_gaming1.packcore.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class CheckForUpdatesold {
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static String[] checkForUpdates() {
        String[] versions = new String[3];
        if (ModpackInfoOld.shouldCheckForUpdates()) {
            String currentVersion = ModpackInfoOld.getCurrentVersion();
            String projectId = ModpackInfoOld.getModrinthProjectId();
            ModrinthAPICallerOld.VersionResponse latest = ModrinthAPICallerOld.getLatestVersion(projectId);

            if (!latest.success) {
                // Handle error case
                LOGGER.error(String.format("§c[Update Checker Error] §f%s", latest.errorMessage));
                return versions;
            }

            versions[0] = currentVersion;
            versions[1] = latest.version;
            versions[2] = latest.changelog;
        }
        return versions;
    }
}