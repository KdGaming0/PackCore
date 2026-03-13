package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.github.kd_gaming1.packcore.configpack.ConfigPackExtractor;
import com.github.kd_gaming1.packcore.configpack.ConfigPackScanner;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.util.ScreenResolution;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class PackCorePreLaunch implements PreLaunchEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/PreLaunch");
    private static final String PACK_META_FILE = "pack.json";

    @Override
    public void onPreLaunch() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path packcoreDir = PackCore.PACKCORE_DIR;
        Path configsDir = packcoreDir.resolve("configs");

        MidnightConfig.init("packcore", PackCoreConfig.class);

        if (!PackCoreConfig.pendingRestoreBackup.isBlank()) {
            applyPendingRestore(packcoreDir, gameDir);
            return;
        }

        if (!PackCoreConfig.pendingConfigPack.isBlank()) {
            applyPendingConfig(gameDir, configsDir);
            return;
        }

        ScreenResolution.ScreenSize screen = ScreenResolution.detect();

        List<ConfigPackEntry> scannedPacks;
        try {
            scannedPacks = new ConfigPackScanner().scanFolder(configsDir);
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

        if (isUpgradeFromV3()) {
            migrateFromV3(selectedPack, gameDir);
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
        String pendingFileName = PackCoreConfig.pendingConfigPack;
        Path zipPath = configsDir.resolve(pendingFileName);

        LOGGER.info("Pending config switch requested: {}", pendingFileName);

        if (!Files.isRegularFile(zipPath)) {
            LOGGER.error("Pending config zip not found at: {}", zipPath);
            clearPending();
            return;
        }

        Optional<JsonObject> configOptional = readPackConfig(zipPath);
        if (configOptional.isEmpty()) {
            LOGGER.error("Pending config zip is missing a valid {}: {}", PACK_META_FILE, zipPath);
            clearPending();
            return;
        }

        try {
            ConfigPackExtractor.extractAll(
                    zipPath,
                    gameDir,
                    ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING
            );
        } catch (IOException e) {
            LOGGER.error("Failed to extract pending config pack '{}': {}", pendingFileName, e.getMessage());
            clearPending();
            return;
        }

        JsonObject config = configOptional.get();
        String packVersion = config.has("version") ? config.get("version").getAsString() : "";

        PackCoreConfig.lastAppliedVersion = packVersion;
        PackCoreConfig.lastAppliedPackFile = pendingFileName;
        PackCoreConfig.pendingConfigPack = "";
        MidnightConfig.write(MOD_ID);

        LOGGER.info("Successfully applied pending config: {} (version: {})", pendingFileName, packVersion);
    }

    private static Optional<JsonObject> readPackConfig(Path zipPath) {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry metaEntry = zipFile.getEntry(PACK_META_FILE);
            if (metaEntry == null) {
                return Optional.empty();
            }

            try (InputStream stream = zipFile.getInputStream(metaEntry);
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return Optional.of(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            LOGGER.warn("Failed to read {} from '{}': {}", PACK_META_FILE, zipPath.getFileName(), e.getMessage());
            return Optional.empty();
        }
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
                    backupPath,
                    gameDir,
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
     * - No version has been applied yet, or
     * - The selected pack's filename matches the last applied file and its version is newer.
     * If filenames differ, extraction is skipped to preserve user-selected config.
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
                LOGGER.info("No config applied yet, performing full extraction.");
                ConfigPackExtractor.extractAll(
                        selectedPack.zipPath(),
                        gameDir,
                        ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING
                );
            } else if (!installedPackFile.equals(selectedPackFile)) {
                LOGGER.info(
                        "Selected pack '{}' differs from last applied '{}', skipping.",
                        selectedPackFile,
                        installedPackFile
                );
                return;
            } else if (UpdateChecker.isNewerVersion(packVersion, installedVersion)) {
                LOGGER.info(
                        "Newer config available ({} -> {}), applying with SKIP_EXISTING.",
                        installedVersion,
                        packVersion
                );
                ConfigPackExtractor.extractAll(
                        selectedPack.zipPath(),
                        gameDir,
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

    private void migrateFromV3(ConfigPackEntry selectedPack, Path gameDir) {
        PackCore.migratedFromV3 = true;

        JsonObject config = selectedPack.config();
        String selectedPackFile = selectedPack.zipPath().getFileName().toString();
        String packVersion = readPackVersionOrFallback(config);

        LOGGER.info(
                "Detected v3 upgrade. Backfilling PackCore v4 metadata using '{}' (version: {}) with SKIP_EXISTING.",
                selectedPackFile,
                packVersion
        );

        try {
            ConfigPackExtractor.extractAll(
                    selectedPack.zipPath(),
                    gameDir,
                    ConfigPackExtractor.OverwriteMode.SKIP_EXISTING
            );
        } catch (IOException e) {
            LOGGER.error("Failed to migrate v3 install using '{}': {}", selectedPackFile, e.getMessage());
            return;
        }

        PackCoreConfig.lastAppliedVersion = packVersion;
        PackCoreConfig.lastAppliedPackFile = selectedPackFile;
        PackCoreConfig.isFirstStartup = false;
        MidnightConfig.write(MOD_ID);

        LOGGER.info(
                "Successfully migrated v3 install. Stored applied pack '{}' at version '{}'.",
                selectedPackFile,
                packVersion
        );
    }


    /**
     * Returns the pack whose target resolution is closest to the current screen
     * using squared Euclidean distance. Ties are resolved by higher guiScale.
     */
    private ConfigPackEntry findBestMatch(List<ConfigPackEntry> packs, int screenWidth, int screenHeight) {
        ConfigPackEntry bestPack = null;
        long bestDistanceSquared = Long.MAX_VALUE;
        int bestGuiScale = -1;

        for (ConfigPackEntry pack : packs) {
            JsonObject config = pack.config();

            if (!config.has("targetWidth") || !config.has("targetHeight")) {
                LOGGER.warn("Pack missing resolution fields, skipping: {}", pack.zipPath().getFileName());
                continue;
            }

            long widthDiff = config.get("targetWidth").getAsInt() - screenWidth;
            long heightDiff = config.get("targetHeight").getAsInt() - screenHeight;
            long distanceSquared = widthDiff * widthDiff + heightDiff * heightDiff;
            int guiScale = config.has("guiScale") ? config.get("guiScale").getAsInt() : 0;

            if (distanceSquared < bestDistanceSquared
                    || (distanceSquared == bestDistanceSquared && guiScale > bestGuiScale)) {
                bestPack = pack;
                bestDistanceSquared = distanceSquared;
                bestGuiScale = guiScale;
            }
        }

        return bestPack;
    }

    private static boolean isUpgradeFromV3() {
        return !PackCoreConfig.isFirstStartup
                && PackCoreConfig.lastAppliedPackFile.isBlank()
                && PackCoreConfig.lastAppliedVersion.isBlank();
    }

    private static String readPackVersionOrFallback(JsonObject config) {
        if (config.has("version")) {
            String version = config.get("version").getAsString();
            if (!version.isBlank()) {
                return version;
            }
        }
        return "0.0.0";
    }

}