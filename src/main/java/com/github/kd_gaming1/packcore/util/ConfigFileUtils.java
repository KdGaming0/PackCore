package com.github.kd_gaming1.packcore.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

/**
 * Simplified config file utilities for managing config zips and metadata
 */
public class ConfigFileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileUtils.class);
    private static final Gson GSON = new Gson();

    // Standard paths and filenames
    public static final String METADATA_FILE = "packcore_metadata.json";
    public static final String OFFICIAL_CONFIGS_PATH = "packcore/modpack_config/official_configs";
    public static final String CUSTOM_CONFIGS_PATH = "packcore/modpack_config/custom_configs";

    /**
     * Represents a config file with its metadata
     */
    public static class ConfigFile {
        private final String fileName;
        private final Path path;
        private final boolean official;
        private final ConfigMetadata metadata;

        public ConfigFile(String fileName, Path path, boolean official, ConfigMetadata metadata) {
            this.fileName = fileName;
            this.path = path;
            this.official = official;
            this.metadata = metadata != null ? metadata : new ConfigMetadata();
        }

        public String getFileName() { return fileName; }
        public Path getPath() { return path; }
        public boolean isOfficial() { return official; }
        public ConfigMetadata getMetadata() { return metadata; }

        public String getDisplayName() {
            if (metadata != null && metadata.isValid()) {
                return metadata.getDisplayName();
            }
            // Fallback to filename without extension
            return fileName.endsWith(".zip")
                    ? fileName.substring(0, fileName.length() - 4)
                    : fileName;
        }
    }

    /**
     * Get the currently applied configuration metadata
     * @return Current config metadata or default if none applied
     */
    public static ConfigMetadata getCurrentConfig() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path metadataPath = gameDir.resolve(METADATA_FILE);

        if (!Files.exists(metadataPath)) {
            return createDefaultConfig();
        }

        try {
            String content = Files.readString(metadataPath, StandardCharsets.UTF_8);
            ConfigMetadata metadata = GSON.fromJson(content, ConfigMetadata.class);
            return metadata != null ? metadata : createDefaultConfig();
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to read current config metadata", e);
            return createDefaultConfig();
        }
    }

    private static ConfigMetadata createDefaultConfig() {
        return ConfigMetadata.builder()
                .name("Default Configuration")
                .description("Stock Minecraft configuration")
                .version("1.0.0")
                .author("System")
                .source("System")
                .targetResolution("Any")
                .build();
    }

    /**
     * Save current config metadata to game directory
     */
    public static void saveCurrentConfig(ConfigMetadata metadata) throws IOException {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path metadataPath = gameDir.resolve(METADATA_FILE);

        String json = GSON.toJson(metadata);
        Files.writeString(metadataPath, json, StandardCharsets.UTF_8);
    }

    /**
     * Read metadata from a zip file
     */
    public static ConfigMetadata readMetadataFromZip(Path zipPath) {
        if (!Files.exists(zipPath) || !zipPath.toString().endsWith(".zip")) {
            LOGGER.warn("Invalid zip path: {}", zipPath);
            return null;
        }

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry metadataEntry = zipFile.getEntry(METADATA_FILE);

            if (metadataEntry == null) {
                LOGGER.debug("No metadata found in zip: {}", zipPath);
                return createFallbackMetadata(zipPath);
            }

            try (InputStreamReader reader = new InputStreamReader(
                    zipFile.getInputStream(metadataEntry), StandardCharsets.UTF_8)) {
                ConfigMetadata metadata = GSON.fromJson(reader, ConfigMetadata.class);
                return metadata != null ? metadata : createFallbackMetadata(zipPath);
            }

        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to read metadata from zip: {}", zipPath, e);
            return createFallbackMetadata(zipPath);
        }
    }

    private static ConfigMetadata createFallbackMetadata(Path zipPath) {
        String fileName = zipPath.getFileName().toString();
        String displayName = fileName.endsWith(".zip")
                ? fileName.substring(0, fileName.length() - 4)
                : fileName;

        return ConfigMetadata.builder()
                .name(displayName)
                .description("No description available")
                .version("Unknown")
                .author("Unknown")
                .source("Unknown")
                .build();
    }

    /**
     * Get all available configs (official and custom)
     */
    public static List<ConfigFile> getAllConfigs() {
        List<ConfigFile> configs = new ArrayList<>();
        configs.addAll(getConfigs(OFFICIAL_CONFIGS_PATH, true));
        configs.addAll(getConfigs(CUSTOM_CONFIGS_PATH, false));
        return configs;
    }

    /**
     * Get official configs only
     */
    public static List<ConfigFile> getOfficialConfigs() {
        return getConfigs(OFFICIAL_CONFIGS_PATH, true);
    }

    /**
     * Get custom configs only
     */
    public static List<ConfigFile> getCustomConfigs() {
        return getConfigs(CUSTOM_CONFIGS_PATH, false);
    }

    private static List<ConfigFile> getConfigs(String relativePath, boolean official) {
        List<ConfigFile> configs = new ArrayList<>();
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path configDir = gameDir.resolve(relativePath);

        // Create directory if it doesn't exist
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
                LOGGER.info("Created config directory: {}", configDir);
            } catch (IOException e) {
                LOGGER.error("Failed to create config directory: {}", configDir, e);
            }
            return configs;
        }

        // Read all zip files from directory
        try (Stream<Path> files = Files.list(configDir)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".zip"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        ConfigMetadata metadata = readMetadataFromZip(path);

                        if (metadata != null && official) {
                            metadata = ConfigMetadata.builder()
                                    .name(metadata.getName())
                                    .description(metadata.getDescription())
                                    .version(metadata.getVersion())
                                    .author(metadata.getAuthor())
                                    .targetResolution(metadata.getTargetResolution())
                                    .mods(metadata.getMods())
                                    .source("Official")
                                    .build();
                        }

                        configs.add(new ConfigFile(fileName, path, official, metadata));
                    });

        } catch (IOException e) {
            LOGGER.error("Failed to read configs from: {}", configDir, e);
        }

        return configs;
    }

    /**
     * Check if a config file exists
     */
    public static boolean configExists(String fileName) {
        if (!fileName.endsWith(".zip")) {
            fileName += ".zip";
        }

        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path officialPath = gameDir.resolve(OFFICIAL_CONFIGS_PATH).resolve(fileName);
        Path customPath = gameDir.resolve(CUSTOM_CONFIGS_PATH).resolve(fileName);

        return Files.exists(officialPath) || Files.exists(customPath);
    }

    /**
     * Delete a config file
     */
    public static boolean deleteConfig(ConfigFile config) {
        if (config == null || config.isOfficial()) {
            return false;  // Don't delete official configs
        }

        try {
            Files.deleteIfExists(config.getPath());
            LOGGER.info("Deleted config: {}", config.getPath());
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to delete config: {}", config.getPath(), e);
            return false;
        }
    }
}