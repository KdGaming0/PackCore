package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.util.copysystem.UnzipFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Automatically applies the best matching configuration based on screen resolution.
 * Uses actual pixel-based distance calculation to find the closest matching config.
 */
public class AutoConfigApplicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoConfigApplicator.class);

    /**
     * Detect screen resolution and apply the best matching config
     *
     * @param gameDir The game directory
     * @return true if a config was successfully applied
     */
    public static boolean applyBestMatchingConfig(Path gameDir) {
        LOGGER.info("Starting automatic config application...");

        // Detect screen resolution
        Dimension screenSize = detectResolution();
        if (screenSize == null) {
            LOGGER.error("Failed to detect screen resolution");
            return false;
        }

        LOGGER.info("Detected screen resolution: {}x{}", screenSize.width, screenSize.height);

        // Get all available configs
        List<ConfigFileUtils.ConfigFile> allConfigs = ConfigFileUtils.getAllConfigs();

        if (allConfigs.isEmpty()) {
            LOGGER.warn("No configs available for automatic application");
            return false;
        }

        LOGGER.info("Found {} available configs", allConfigs.size());

        // Find best match
        ConfigFileUtils.ConfigFile bestMatch = findBestMatch(screenSize, allConfigs);

        if (bestMatch == null) {
            LOGGER.error("Could not find suitable config to apply");
            return false;
        }

        LOGGER.info("Selected config: {} ({})", bestMatch.getDisplayName(),
                bestMatch.official() ? "Official" : "Custom");

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
                    config.path().toString(),
                    gameDir.toString(),
                    (bytesProcessed, totalBytes, percentage) -> {
                        if (percentage % 25 == 0) {
                            LOGGER.info("Extraction progress: {}%", percentage);
                        }
                    }
            );

            // Save metadata using ConfigFileUtils
            ConfigFileUtils.saveCurrentConfig(config.metadata());

            LOGGER.info("Config applied successfully: {}", config.getDisplayName());
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to apply config", e);
            return false;
        }
    }

    /**
     * Detect the current screen resolution
     *
     * @return Dimension with screen width and height, or null if detection fails
     */
    private static Dimension detectResolution() {
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension screenSize = toolkit.getScreenSize();
            LOGGER.debug("Screen dimensions: {}x{}", screenSize.width, screenSize.height);
            return screenSize;
        } catch (Exception e) {
            LOGGER.error("Failed to detect resolution", e);
            return null;
        }
    }

    /**
     * Find the best matching config for the detected resolution
     * Uses actual pixel distance calculation
     */
    private static ConfigFileUtils.ConfigFile findBestMatch(
            Dimension detectedResolution,
            List<ConfigFileUtils.ConfigFile> configs) {

        LOGGER.info("Available configs:");
        for (ConfigFileUtils.ConfigFile config : configs) {
            String targetRes = config.metadata().getTargetResolution();
            Dimension configRes = parseResolution(targetRes);
            double distance = calculateDistance(detectedResolution, configRes);

            LOGGER.info("  - {} | Resolution: {} | Official: {} | Distance: {}",
                    config.getDisplayName(),
                    targetRes,
                    config.official(),
                    configRes != null ? String.format("%.0f", distance) : "N/A");
        }

        ConfigFileUtils.ConfigFile selected = configs.stream()
                .min(createConfigComparator(detectedResolution))
                .orElse(null);

        if (selected != null) {
            Dimension selectedRes = parseResolution(selected.metadata().getTargetResolution());
            double distance = calculateDistance(detectedResolution, selectedRes);
            LOGGER.info("Best match selected: {} (distance: {:.0f} pixels)",
                    selected.getDisplayName(), distance);
        }

        return selected;
    }

    /**
     * Creates a comparator that prioritizes configs by:
     * 1. Official status (official configs first)
     * 2. Pixel distance from target resolution (closer is better)
     * 3. Name (alphabetically for consistency)
     */
    private static Comparator<ConfigFileUtils.ConfigFile> createConfigComparator(Dimension targetResolution) {
        return Comparator
                // Prioritize official configs
                .comparing((ConfigFileUtils.ConfigFile c) -> !c.official())
                // Then by how close the resolution matches (pixel distance)
                .thenComparing(c -> {
                    Dimension configRes = parseResolution(c.metadata().getTargetResolution());
                    return calculateDistance(targetResolution, configRes);
                })
                // Then by name for consistency
                .thenComparing(c -> c.getDisplayName());
    }

    /**
     * Calculate Euclidean distance between two resolutions in pixels
     *
     * @param target    The target resolution
     * @param candidate The candidate resolution
     * @return Distance in pixels, or Double.MAX_VALUE if either is null
     */
    private static double calculateDistance(Dimension target, Dimension candidate) {
        if (target == null || candidate == null) {
            return Double.MAX_VALUE;
        }

        double widthDiff = target.width - candidate.width;
        double heightDiff = target.height - candidate.height;

        return Math.sqrt(widthDiff * widthDiff + heightDiff * heightDiff);
    }

    /**
     * Parse resolution strings to extract width and height
     * Handles formats like "1920x1080", "2560×1440", "1920 x 1080", etc.
     * Supports both 'x' and '×' (multiplication sign) as separators
     *
     * @param resolution Resolution string to parse
     * @return Dimension with width and height, or null if parsing fails
     */
    private static Dimension parseResolution(String resolution) {
        if (resolution == null || resolution.trim().isEmpty()) {
            return null;
        }

        String cleaned = resolution.trim().toLowerCase();

        // Replace multiplication sign (×) with regular x for consistent parsing
        cleaned = cleaned.replace('×', 'x');

        // Match pattern: digits, optional whitespace, x, optional whitespace, digits
        // Example: "1920x1080", "1920 x 1080", "2560x1440"
        if (cleaned.matches("\\d+\\s*x\\s*\\d+")) {
            String[] parts = cleaned.split("x");
            try {
                int width = Integer.parseInt(parts[0].trim());
                int height = Integer.parseInt(parts[1].trim());
                return new Dimension(width, height);
            } catch (NumberFormatException e) {
                LOGGER.warn("Could not parse resolution numbers: {}", resolution);
                return null;
            }
        }

        LOGGER.warn("Resolution format not recognized: {}", resolution);
        return null;
    }
}