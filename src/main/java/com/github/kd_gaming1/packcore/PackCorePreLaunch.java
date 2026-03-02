package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.util.*;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.github.kd_gaming1.packcore.configpack.ConfigPackScanner;
import com.github.kd_gaming1.packcore.configpack.ConfigPackExtractor;
import com.google.gson.JsonObject;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PackCorePreLaunch implements PreLaunchEntrypoint {


    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/PreLaunch");

    @Override
    public void onPreLaunch() {
        Path packcoreDir = PackCore.PACKCORE_DIR;
        Path configsDir = packcoreDir.resolve("configs");

        MidnightConfig.init("packcore", PackCoreConfig.class);

        ScreenResolution.ScreenSize screen = ScreenResolution.detect();

        ConfigPackScanner scanner = new ConfigPackScanner();
        List<ConfigPackEntry> scannedPacks;

        try {
            scannedPacks = scanner.scanFolder(configsDir);
        } catch (IOException e) {
            LOGGER.error("Failed to scan configs directory: {}", e.getMessage());
            return;
        }

        if (scannedPacks.isEmpty()) {
            LOGGER.warn("No valid config packs found in: {}", configsDir);
            return;
        }

        ConfigPackEntry selectedPack = findBestResolutionMatch(scannedPacks, screen.width(), screen.height());

        if (selectedPack == null) {
            LOGGER.warn("No packs had valid resolution fields, aborting.");
            return;
        }

        JsonObject config = selectedPack.config();
        String packVersion = config.has("version") ? config.get("version").getAsString() : "";
        String installedVersion = PackCoreConfig.lastAppliedVersion;

        LOGGER.info("Best match: {} (version: {})", selectedPack.zipPath().getFileName(), packVersion);

        try {
            if (installedVersion.isEmpty()) {
                LOGGER.info("No config applied yet, performing full extraction.");
                ConfigPackExtractor.extractAll(selectedPack.zipPath(), packcoreDir, ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING);
            } else if (isNewerVersion(packVersion, installedVersion)) {
                LOGGER.info("Newer config available ({} -> {}), applying with SKIP_EXISTING.", installedVersion, packVersion);
                ConfigPackExtractor.extractAll(selectedPack.zipPath(), packcoreDir, ConfigPackExtractor.OverwriteMode.SKIP_EXISTING);
            } else {
                LOGGER.info("Config is up to date (version: {}), skipping.", installedVersion);
                return;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to extract config pack: {}", e.getMessage());
            return;
        }

        PackCoreConfig.lastAppliedVersion = packVersion;
        MidnightConfig.write("packcore");

        LOGGER.info("Successfully applied config version: {}", packVersion);
    }

    /**
     * Returns the pack whose target resolution is closest to the screen resolution.
     * Uses squared Euclidean distance
     */
    private ConfigPackEntry findBestResolutionMatch(List<ConfigPackEntry> packs, int screenWidth, int screenHeight) {
        ConfigPackEntry selectedPack = null;
        long bestDistanceSquared = Long.MAX_VALUE;

        for (ConfigPackEntry pack : packs) {
            JsonObject config = pack.config();

            if (!config.has("targetWidth") || !config.has("targetHeight")) {
                LOGGER.warn("Pack missing resolution fields, skipping: {}", pack.zipPath().getFileName());
                continue;
            }

            int packWidth = config.get("targetWidth").getAsInt();
            int packHeight = config.get("targetHeight").getAsInt();

            long widthDiff = packWidth - screenWidth;
            long heightDiff = packHeight - screenHeight;
            long distanceSquared = (widthDiff * widthDiff) + (heightDiff * heightDiff);

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                selectedPack = pack;
            }
        }

        return selectedPack;
    }

    private boolean isNewerVersion(String available, String applied) {
        String[] availableParts = available.split("\\.");
        String[] appliedParts = applied.split("\\.");

        int segmentCount = Math.max(availableParts.length, appliedParts.length);

        for (int i = 0; i < segmentCount; i++) {
            int availableSegment = i < availableParts.length ? parseVersionSegment(availableParts[i]) : 0;
            int appliedSegment = i < appliedParts.length ? parseVersionSegment(appliedParts[i]) : 0;

            if (availableSegment != appliedSegment) {
                return availableSegment > appliedSegment;
            }
        }

        return false; // Versions are equal
    }

    private int parseVersionSegment(String segment) {
        try {
            return Integer.parseInt(segment.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("Unparseable version segment '{}', treating as 0", segment);
            return 0;
        }
    }
}