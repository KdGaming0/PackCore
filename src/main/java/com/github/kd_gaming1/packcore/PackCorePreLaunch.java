package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.util.*;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.github.kd_gaming1.packcore.configpack.ConfigPackScanner;
import com.github.kd_gaming1.packcore.configpack.ConfigPackExtractor;
import com.google.gson.JsonObject;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class PackCorePreLaunch implements PreLaunchEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/PreLaunch");

    @Override
    public void onPreLaunch() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path packcoreDir = PackCore.PACKCORE_DIR;
        Path configsDir = packcoreDir.resolve("configs");

        MidnightConfig.init("packcore", PackCoreConfig.class);

        // Highest priority: restore a backup if one is pending
        if (!PackCoreConfig.pendingRestoreBackup.isBlank()) {
            applyPendingRestore(packcoreDir, gameDir);
            return;
        }

        // Next priority: the user explicitly chose a config from the wizard GUI
        if (!PackCoreConfig.pendingConfigPack.isBlank()) {
            applyPendingConfig(gameDir, configsDir);
            return;
        }

        // Normal path: auto-detect the best resolution + GUI scale match
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

        ConfigPackEntry selectedPack = findBestMatch(scannedPacks, screen.width(), screen.height());

        if (selectedPack == null) {
            LOGGER.warn("No packs had valid resolution fields, aborting.");
            return;
        }

        extractIfNeeded(selectedPack, gameDir);
    }

    /**
     * Applies the pack whose filename is stored in {@link PackCoreConfig#pendingConfigPack}.
     * Always uses REPLACE_EXISTING because the user explicitly asked to switch.
     * The pending flag is cleared regardless of success or failure.
     */
    private void applyPendingConfig(Path gameDir, Path configsDir) {
        String pendingFile = PackCoreConfig.pendingConfigPack;
        LOGGER.info("Pending config switch requested: {}", pendingFile);

        Path zipPath = configsDir.resolve(pendingFile);
        if (!Files.exists(zipPath)) {
            LOGGER.error("Pending config zip not found at: {}", zipPath);
            clearPending();
            return;
        }

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

        try {
            ConfigPackExtractor.extractAll(
                    entry.zipPath(), gameDir,
                    ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING
            );
        } catch (IOException e) {
            LOGGER.error("Failed to extract pending config pack '{}': {}", pendingFile, e.getMessage());
            clearPending();
            return;
        }

        JsonObject config = entry.config();
        String packVersion = config.has("version") ? config.get("version").getAsString() : "";

        PackCoreConfig.lastAppliedVersion = packVersion;
        PackCoreConfig.lastAppliedPackFile = pendingFile;
        PackCoreConfig.pendingConfigPack = "";
        MidnightConfig.write(MOD_ID);

        LOGGER.info("Successfully applied pending config: {} (version: {})", pendingFile, packVersion);
    }

    private void applyPendingRestore(Path packcoreDir, Path gameDir) {
        String backupFile = PackCoreConfig.pendingRestoreBackup;
        Path backupPath = packcoreDir.resolve("backups").resolve(backupFile);

        LOGGER.info("Pending backup restore requested: {}", backupFile);

        if (!Files.exists(backupPath)) {
            LOGGER.error("Pending restore backup not found: {}", backupPath);
            clearPendingRestore();
            return;
        }

        try {
            ConfigPackExtractor.extractAll(
                    backupPath, gameDir,
                    ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING
            );
            LOGGER.info("Successfully restored backup: {}", backupFile);
        } catch (IOException e) {
            LOGGER.error("Failed to restore backup '{}': {}", backupFile, e.getMessage());
        }

        clearPendingRestore();
    }

    private static void clearPendingRestore() {
        PackCoreConfig.pendingRestoreBackup = "";
        MidnightConfig.write(MOD_ID);
    }

    private static void clearPending() {
        PackCoreConfig.pendingConfigPack = "";
        MidnightConfig.write(MOD_ID);
    }

    /**
     * Extracts {@code selectedPack} into the game directory if:
     * <ul>
     *   <li>No version has been applied yet, or</li>
     *   <li>The selected pack's filename matches the last applied file AND its version is newer.</li>
     * </ul>
     * If the filenames differ, extraction is skipped to preserve what the user has installed.
     */
    private void extractIfNeeded(ConfigPackEntry selectedPack, Path gameDir) {
        JsonObject config = selectedPack.config();
        String packVersion = config.has("version") ? config.get("version").getAsString() : "";
        String installedVersion = PackCoreConfig.lastAppliedVersion;
        String installedPackFile = PackCoreConfig.lastAppliedPackFile;
        String selectedPackFile = selectedPack.zipPath().getFileName().toString();

        LOGGER.info("Best match: {} (version: {})", selectedPackFile, packVersion);

        try {
            if (installedVersion.isEmpty()) {
                LOGGER.info("No config applied yet — performing full extraction.");
                ConfigPackExtractor.extractAll(
                        selectedPack.zipPath(), gameDir,
                        ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING
                );
            } else if (!installedPackFile.equals(selectedPackFile)) {
                // Selected pack differs from last applied — keep what's installed.
                LOGGER.info("Selected pack '{}' differs from last applied '{}', skipping.",
                        selectedPackFile, installedPackFile);
                return;
            } else if (UpdateChecker.isNewerVersion(packVersion, installedVersion)) {
                LOGGER.info("Newer config available ({} → {}), applying with SKIP_EXISTING.",
                        installedVersion, packVersion);
                ConfigPackExtractor.extractAll(
                        selectedPack.zipPath(), gameDir,
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

        PackCoreConfig.lastAppliedVersion = packVersion;
        PackCoreConfig.lastAppliedPackFile = selectedPackFile;
        MidnightConfig.write(MOD_ID);

        LOGGER.info("Successfully applied config version: {}", packVersion);
    }

    /**
     * Returns the pack whose target resolution is closest to the screen resolution
     * using squared Euclidean distance. When two packs tie on distance, the one
     * with the higher guiScale wins.
     */
    private ConfigPackEntry findBestMatch(List<ConfigPackEntry> packs, int screenWidth, int screenHeight) {
        ConfigPackEntry best = null;
        long bestDistSq = Long.MAX_VALUE;
        int bestGuiScale = -1;

        for (ConfigPackEntry pack : packs) {
            JsonObject config = pack.config();

            if (!config.has("targetWidth") || !config.has("targetHeight")) {
                LOGGER.warn("Pack missing resolution fields, skipping: {}", pack.zipPath().getFileName());
                continue;
            }

            long widthDiff = config.get("targetWidth").getAsInt() - screenWidth;
            long heightDiff = config.get("targetHeight").getAsInt() - screenHeight;
            long distSq = widthDiff * widthDiff + heightDiff * heightDiff;
            int guiScale = config.has("guiScale") ? config.get("guiScale").getAsInt() : 0;

            if (distSq < bestDistSq || (distSq == bestDistSq && guiScale > bestGuiScale)) {
                bestDistSq = distSq;
                bestGuiScale = guiScale;
                best = pack;
            }
        }

        return best;
    }
}