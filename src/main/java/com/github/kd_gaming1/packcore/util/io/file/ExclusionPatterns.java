package com.github.kd_gaming1.packcore.util.io.file;

import java.nio.file.Path;
import java.util.Set;

/**
 * Centralized exclusion patterns for file operations across the mod.
 * Prevents large mod cache/storage folders from slowing down backups and exports.
 */
public final class ExclusionPatterns {

    private ExclusionPatterns() {} // Utility class

    /** Mod storage/cache folders that should be excluded from backups and exports */
    public static final Set<String> EXCLUDED_CONFIG_SUBFOLDERS = Set.of(
            "firmament/profiles",
            "firmament/storage",
            "skyhanni/backup",
            "skyhanni/repo",
            "skyhanni/logs",
            "skyblocker/item-repo",
            "skyocean/data",
            "skyblocktweaks/repo",
            "skyblocker/reward-trackers",
            "skyblocker/garden_plots",
            "skyblocker/config_backups",
            "skyblocker/backpack-preview"
    );

    /** Folders to hide from the file tree UI */
    public static final Set<String> HIDDEN_FOLDERS = Set.of(
            "packcore", "logs", "crash-reports", "screenshots",
            ".git", ".minecraft", "saves", "assets", "mods", ".firmament"
    );

    /**
     * Check if a path should be excluded during backup/export operations.
     * @param relativePath Path relative to game directory (use forward slashes)
     */
    public static boolean shouldExclude(String relativePath) {
        String normalized = relativePath.replace("\\", "/");
        for (String excluded : EXCLUDED_CONFIG_SUBFOLDERS) {
            String configPath = "config/" + excluded;
            if (normalized.equals(configPath) || normalized.startsWith(configPath + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a path should be excluded (Path variant).
     * @param basePath The base directory (e.g., gameDir)
     * @param fullPath The full path to check
     */
    public static boolean shouldExclude(Path basePath, Path fullPath) {
        return shouldExclude(basePath.relativize(fullPath).toString());
    }
}