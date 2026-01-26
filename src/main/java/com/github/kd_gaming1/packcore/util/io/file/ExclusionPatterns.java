package com.github.kd_gaming1.packcore.util.io.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
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

    /**
     * Generic heavy folder names often used for caches/logs/repos/backups.
     * Applied ONLY under config/.
     */
    private static final Set<String> GENERIC_HEAVY_FOLDER_NAMES = Set.of(
            "cache", "caches",
            "log", "logs",
            "backup", "backups",
            "repo", "repos", "repository", "repositories",
            "storage", "profiles",
            "downloads"
    );

    /**
     * If a file under config/ is huge, it's very likely a cache/database rather than a setting.
     * This protects users on slow disks from multi-minute backups.
     */
    private static final long MAX_CONFIG_FILE_SIZE_BYTES = 50L * 1024L * 1024L; // 50 MB

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
        String lower = normalized.toLowerCase(Locale.ROOT);

        // Only apply these exclusions inside config/
        if (lower.startsWith("config/")) {
            // 1) Specific known-heavy subfolders
            for (String excluded : EXCLUDED_CONFIG_SUBFOLDERS) {
                String configPath = "config/" + excluded;
                if (lower.equals(configPath) || lower.startsWith(configPath + "/")) {
                    return true;
                }
            }

            // 2) Generic heavy folder names anywhere under config/
            // Example: config/somemod/cache/** or config/somemod/profiles/**
            String afterConfig = lower.substring("config/".length());
            String[] parts = afterConfig.split("/");
            for (String part : parts) {
                if (GENERIC_HEAVY_FOLDER_NAMES.contains(part)) {
                    return true;
                }
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
        String rel = basePath.relativize(fullPath).toString();
        if (shouldExclude(rel)) {
            return true;
        }

        // Extra guard: skip huge files under config/
        try {
            String lower = rel.replace("\\", "/").toLowerCase(Locale.ROOT);
            if (lower.startsWith("config/") && Files.isRegularFile(fullPath)) {
                long size = Files.size(fullPath);
                return size > MAX_CONFIG_FILE_SIZE_BYTES;
            }
        } catch (Exception ignored) {
            // If we can't stat it, don't exclude based on size.
        }

        return false;
    }
}