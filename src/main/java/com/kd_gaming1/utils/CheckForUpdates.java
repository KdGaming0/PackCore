package com.kd_gaming1.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.kd_gaming1.PackCore.MOD_ID;

public class CheckForUpdates {
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static String[] checkForUpdates() {
        String[] versions = new String[3];
        if (ModpackInfo.shouldCheckForUpdates()) {
            String currentVersion = ModpackInfo.getCurrentVersion();
            String projectId = ModpackInfo.getModrinthProjectId();
            ModrinthAPICaller.VersionResponse latest = ModrinthAPICaller.getLatestVersion(projectId);

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