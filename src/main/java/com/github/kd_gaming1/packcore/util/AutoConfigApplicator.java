package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.util.copysystem.UnzipFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Automatically applies the best matching configuration based on screen resolution.
 * Used during first launch or when explicitly requested.
 */
public class AutoConfigApplicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoConfigApplicator.class);

    private static final Map<String, Integer> RESOLUTION_PRIORITY = Map.of(
            "4k", 4,
            "1440p", 3,
            "1080p", 2,
            "720p", 1,
            "any", 0
    );

    /**
     * Detect screen resolution and apply the best matching config
     * @param gameDir The game directory
     * @return true if a config was successfully applied
     */
    public static boolean applyBestMatchingConfig(Path gameDir) {
        LOGGER.info("Starting automatic config application...");

        // Detect screen resolution
        String detectedResolution = detectResolution();
        LOGGER.info("Detected screen resolution: {}", detectedResolution);

        // Get all available configs
        List<ConfigFileUtils.ConfigFile> allConfigs = ConfigFileUtils.getAllConfigs();

        if (allConfigs.isEmpty()) {
            LOGGER.warn("No configs available for automatic application");
            return false;
        }

        LOGGER.info("Found {} available configs", allConfigs.size());

        // Find best match
        ConfigFileUtils.ConfigFile bestMatch = findBestMatch(detectedResolution, allConfigs);

        if (bestMatch == null) {
            LOGGER.error("Could not find suitable config to apply");
            return false;
        }

        LOGGER.info("Selected config: {} ({})", bestMatch.getDisplayName(),
                bestMatch.isOfficial() ? "Official" : "Custom");

        // Apply the config using shared logic
        return applyConfigDirect(bestMatch, gameDir);
    }

    /**
     * Apply a config directly during pre-launch (no restart needed)
     * Uses the shared UnzipFiles utility and ConfigFileOperations
     */
    private static boolean applyConfigDirect(ConfigFileUtils.ConfigFile config, Path gameDir) {
        LOGGER.info("Applying config: {}", config.getDisplayName());

        try {
            // Create backup using shared utility
            Path backup = ConfigFileOperations.createBackup(gameDir);
            if (backup != null) {
                LOGGER.info("Backup created at: {}", backup);

                // Clean old backups, keep last 5
                ConfigFileOperations.cleanOldBackups(gameDir, 5);
            }

            // Extract config using existing UnzipFiles utility
            UnzipFiles unzipper = new UnzipFiles();
            unzipper.unzip(
                    config.getPath().toString(),
                    gameDir.toString(),
                    (bytesProcessed, totalBytes, percentage) -> {
                        if (percentage % 25 == 0) {
                            LOGGER.info("Extraction progress: {}%", percentage);
                        }
                    }
            );

            // Save metadata using ConfigFileUtils
            ConfigFileUtils.saveCurrentConfig(config.getMetadata());

            LOGGER.info("Config applied successfully: {}", config.getDisplayName());
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to apply config", e);
            return false;
        }
    }

    /**
     * Detect the current screen resolution category
     */
    private static String detectResolution() {
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension screenSize = toolkit.getScreenSize();
            int width = screenSize.width;
            int height = screenSize.height;

            LOGGER.debug("Screen dimensions: {}x{}", width, height);

            return categorizeResolution(width, height);
        } catch (Exception e) {
            LOGGER.error("Failed to detect resolution, defaulting to 1080p", e);
            return "1080p";
        }
    }

    private static String categorizeResolution(int width, int height) {
        if (height >= 2160) return "4k";
        else if (height >= 1440) return "1440p";
        else if (height >= 1080) return "1080p";
        else return "720p";
    }

    /**
     * Find the best matching config for the detected resolution
     */
    private static ConfigFileUtils.ConfigFile findBestMatch(
            String detectedResolution,
            List<ConfigFileUtils.ConfigFile> configs) {

        LOGGER.debug("Finding best match for resolution: {}", detectedResolution);

        return configs.stream()
                .sorted(createConfigComparator(detectedResolution))
                .peek(config -> LOGGER.debug("Evaluating: {} - resolution: {}, official: {}",
                        config.getDisplayName(),
                        config.getMetadata().getTargetResolution(),
                        config.isOfficial()))
                .findFirst()
                .orElse(null);
    }

    private static Comparator<ConfigFileUtils.ConfigFile> createConfigComparator(String targetResolution) {
        return Comparator
                // Prioritize official configs
                .comparing((ConfigFileUtils.ConfigFile c) -> !c.isOfficial())
                // Then by how close the resolution matches
                .thenComparing(c -> getResolutionDistance(
                        targetResolution,
                        c.getMetadata().getTargetResolution()))
                // Then by name for consistency
                .thenComparing(c -> c.getDisplayName());
    }

    /**
     * Calculate distance between two resolutions (lower is better)
     */
    private static int getResolutionDistance(String target, String candidate) {
        if (candidate == null) return 999;

        String candidateLower = candidate.toLowerCase();
        String targetLower = target.toLowerCase();

        // Exact match is best
        if (candidateLower.equals(targetLower)) return 0;

        // "any" is acceptable but not preferred
        if (candidateLower.equals("any")) return 10;

        // Calculate distance based on priority
        int targetPriority = RESOLUTION_PRIORITY.getOrDefault(targetLower, 0);
        int candidatePriority = RESOLUTION_PRIORITY.getOrDefault(candidateLower, 0);

        return Math.abs(targetPriority - candidatePriority);
    }
}