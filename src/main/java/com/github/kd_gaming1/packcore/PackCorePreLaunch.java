package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PackCorePreLaunch implements PreLaunchEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/PreLaunch");

    @Override
    public void onPreLaunch() {
        Path packcoreDir = PackCore.PACKCORE_DIR;
        Path configsDir  = packcoreDir.resolve("configs");

        MidnightConfig.init("packcore", PackCoreConfig.class);

        // ── Priority path: user explicitly chose a config from the wizard GUI ──
        // If pendingConfigPack is set, apply that pack unconditionally (bypasses
        // resolution auto-detection) and clear the flag, then return early.
        if (!PackCoreConfig.pendingConfigPack.isBlank()) {
            applyPendingConfig(packcoreDir, configsDir);
            return;
        }

        // ── Normal path: auto-detect the best resolution match ─────────────────
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

        extractIfNeeded(selectedPack, packcoreDir);
    }

    // ── Pending (user-selected) config ────────────────────────────────────────

    /**
     * Applies the pack whose filename is stored in {@link PackCoreConfig#pendingConfigPack}.
     *
     * Always uses {@link ConfigPackExtractor.OverwriteMode#REPLACE_EXISTING} because
     * the user explicitly asked to switch, so we want a clean application of the new pack.
     * The pending flag is cleared regardless of success or failure.
     */
    private void applyPendingConfig(Path packcoreDir, Path configsDir) {
        String pendingFile = PackCoreConfig.pendingConfigPack;
        LOGGER.info("Pending config switch requested: {}", pendingFile);

        // Locate the zip in the configs directory
        Path zipPath = configsDir.resolve(pendingFile);
        if (!Files.exists(zipPath)) {
            LOGGER.error("Pending config zip not found at: {}", zipPath);
            clearPending();
            return;
        }

        // Re-scan so we get the ConfigPackEntry (with the parsed JsonObject)
        ConfigPackScanner scanner = new ConfigPackScanner();
        List<ConfigPackEntry> packs;
        try {
            packs = scanner.scanFolder(configsDir);
        } catch (IOException e) {
            LOGGER.error("Failed to scan configs for pending pack: {}", e.getMessage());
            clearPending();
            return;
        }

        ConfigPackEntry entry = packs.stream()
                .filter(p -> p.zipPath().getFileName().toString().equals(pendingFile))
                .findFirst()
                .orElse(null);

        if (entry == null) {
            LOGGER.error("Could not locate pending config entry after scan: {}", pendingFile);
            clearPending();
            return;
        }

        // Extract — full replace, since the user intentionally chose this pack
        try {
            ConfigPackExtractor.extractAll(
                    entry.zipPath(), packcoreDir,
                    ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING
            );
        } catch (IOException e) {
            LOGGER.error("Failed to extract pending config pack '{}': {}", pendingFile, e.getMessage());
            clearPending();
            return;
        }

        JsonObject config      = entry.config();
        String     packVersion = config.has("version") ? config.get("version").getAsString() : "";

        PackCoreConfig.lastAppliedVersion  = packVersion;
        PackCoreConfig.lastAppliedPackFile = pendingFile;
        PackCoreConfig.pendingConfigPack   = "";
        MidnightConfig.write("packcore");

        LOGGER.info("Successfully applied pending config: {} (version: {})", pendingFile, packVersion);
    }

    /** Clears {@code pendingConfigPack} and persists the change. */
    private static void clearPending() {
        PackCoreConfig.pendingConfigPack = "";
        MidnightConfig.write("packcore");
    }

    // ── Auto-detect helpers ───────────────────────────────────────────────────

    /**
     * Extracts {@code selectedPack} if it is newer than the installed version,
     * or if no version has been applied yet.
     */
    private void extractIfNeeded(ConfigPackEntry selectedPack, Path packcoreDir) {
        JsonObject config           = selectedPack.config();
        String     packVersion      = config.has("version") ? config.get("version").getAsString() : "";
        String     installedVersion = PackCoreConfig.lastAppliedVersion;

        LOGGER.info("Best resolution match: {} (version: {})",
                selectedPack.zipPath().getFileName(), packVersion);

        try {
            if (installedVersion.isEmpty()) {
                LOGGER.info("No config applied yet — performing full extraction.");
                ConfigPackExtractor.extractAll(
                        selectedPack.zipPath(), packcoreDir,
                        ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING
                );
            } else if (UpdateChecker.isNewerVersion(packVersion, installedVersion)) {
                LOGGER.info("Newer config available ({} → {}), applying with SKIP_EXISTING.",
                        installedVersion, packVersion);
                ConfigPackExtractor.extractAll(
                        selectedPack.zipPath(), packcoreDir,
                        ConfigPackExtractor.OverwriteMode.SKIP_EXISTING
                );
            } else {
                LOGGER.info("Config up to date (version: {}), skipping.", installedVersion);
                return;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to extract config pack: {}", e.getMessage());
            return;
        }

        PackCoreConfig.lastAppliedVersion  = packVersion;
        PackCoreConfig.lastAppliedPackFile = selectedPack.zipPath().getFileName().toString();
        MidnightConfig.write("packcore");

        LOGGER.info("Successfully applied config version: {}", packVersion);
    }

    /**
     * Returns the pack whose target resolution is closest to the screen resolution
     * using squared Euclidean distance.
     */
    private ConfigPackEntry findBestResolutionMatch(List<ConfigPackEntry> packs,
                                                    int screenWidth, int screenHeight) {
        ConfigPackEntry selectedPack       = null;
        long            bestDistanceSquared = Long.MAX_VALUE;

        for (ConfigPackEntry pack : packs) {
            JsonObject config = pack.config();

            if (!config.has("targetWidth") || !config.has("targetHeight")) {
                LOGGER.warn("Pack missing resolution fields, skipping: {}", pack.zipPath().getFileName());
                continue;
            }

            long widthDiff  = config.get("targetWidth").getAsInt()  - screenWidth;
            long heightDiff = config.get("targetHeight").getAsInt() - screenHeight;
            long distSq     = widthDiff * widthDiff + heightDiff * heightDiff;

            if (distSq < bestDistanceSquared) {
                bestDistanceSquared = distSq;
                selectedPack        = pack;
            }
        }

        return selectedPack;
    }
}