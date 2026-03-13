package com.github.kd_gaming1.packcore.config.apply;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.config.backup.BackupManager;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.github.kd_gaming1.packcore.util.io.zip.UnzipService;
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
public class ConfigAutoApplier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigAutoApplier.class);

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
        List<ConfigFileRepository.ConfigFile> allConfigs = ConfigFileRepository.getAllConfigs();

        if (allConfigs.isEmpty()) {
            LOGGER.warn("No configs available for automatic application");
            return false;
        }

        LOGGER.info("Found {} available configs", allConfigs.size());

        // Find best match
        ConfigFileRepository.ConfigFile bestMatch = findBestMatch(screenSize, allConfigs);

        if (bestMatch == null) {
            LOGGER.error("Could not find suitable config to apply");
            return false;
        }

        LOGGER.info("Selected config: {} ({})", bestMatch.getDisplayName(),
                bestMatch.official() ? "Official" : "Custom");

        // Apply the config using shared logic
        return applyConfigDirect(bestMatch, gameDir);
    }

    private static boolean applyConfigDirect(ConfigFileRepository.ConfigFile config, Path gameDir) {
        LOGGER.info("Applying config: {}", config.getDisplayName());

        try {
            // Create zip backup (pre-launch) with a filename that reflects why it's made
            if (PackCoreConfig.enableAutoBackups) {
                String title = "Auto backup before pre-launch auto-apply: " + config.getDisplayName();
                String description = "Safety backup created before automatically applying a config based on screen resolution.";
                String backupIdHint = "prelaunch_auto_apply_" + config.getDisplayName();

                Path backupZip = BackupManager.createBackupAsync(
                        gameDir,
                        BackupManager.BackupType.AUTO,
                        title,
                        description,
                        backupIdHint,
                        msg -> {}
                ).join();

                if (backupZip != null) {
                    LOGGER.info("Backup ZIP created at: {}", backupZip);
                }
            } else {
                LOGGER.debug("Auto-backups disabled; skipping pre-launch safety backup");
            }

            // Extract config using existing UnzipFiles utility
            UnzipService unzipper = new UnzipService();
            unzipper.unzip(
                    config.path().toString(),
                    gameDir.toString(),
                    (bytesProcessed, totalBytes, percentage) -> {
                        if (percentage % 25 == 0) {
                            LOGGER.info("Extraction progress: {}%", percentage);
                        }
                    }
            );

            ConfigFileRepository.saveCurrentConfig(config.metadata());

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
        } catch (Throwable e) {
            LOGGER.error("Failed to detect resolution", e);
            return null;
        }
    }

    /**
     * Find the best matching config for the detected resolution
     * Uses actual pixel distance calculation
     */
    private static ConfigFileRepository.ConfigFile findBestMatch(
            Dimension detectedResolution,
            List<ConfigFileRepository.ConfigFile> configs) {

        LOGGER.info("Available configs:");
        for (ConfigFileRepository.ConfigFile config : configs) {
            if (config.metadata() == null) {
                LOGGER.warn("Config {} has no metadata, skipping", config.getDisplayName());
                continue;
            }
            String targetRes = config.metadata().getTargetResolution();
            Dimension configRes = parseResolution(targetRes);
            double distance = calculateDistance(detectedResolution, configRes);

            LOGGER.info("  - {} | Resolution: {} | Official: {} | Distance: {}",
                    config.getDisplayName(),
                    targetRes,
                    config.official(),
                    configRes != null ? String.format("%.0f", distance) : "N/A");
        }

        ConfigFileRepository.ConfigFile selected = configs.stream()
                .filter(c -> c.metadata() != null)
                .min(createConfigComparator(detectedResolution))
                .orElse(null);

        if (selected != null) {
            Dimension selectedRes = parseResolution(selected.metadata().getTargetResolution());
            double distance = calculateDistance(detectedResolution, selectedRes);
            LOGGER.info("Best match selected: {} (distance: {} pixels)",
                    selected.getDisplayName(), String.format("%.0f", distance));
        }

        return selected;
    }

    /**
     * Creates a comparator that prioritizes configs by:
     * 1. Official status (official configs first)
     * 2. Pixel distance from target resolution (closer is better)
     * 3. Name (alphabetically for consistency)
     */
    private static Comparator<ConfigFileRepository.ConfigFile> createConfigComparator(Dimension targetResolution) {
        return Comparator
                // Prioritize official configs
                .comparing((ConfigFileRepository.ConfigFile c) -> !c.official())
                // Then by how close the resolution matches (pixel distance)
                .thenComparing(c -> {
                    Dimension configRes = parseResolution(c.metadata().getTargetResolution());
                    return calculateDistance(targetResolution, configRes);
                })
                // Then by name for consistency
                .thenComparing(ConfigFileRepository.ConfigFile::getDisplayName);
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
