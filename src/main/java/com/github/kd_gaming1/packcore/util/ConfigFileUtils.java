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
 * Utility class for managing configuration files and their metadata.
 * Handles reading, writing, and listing config zip files and their associated metadata.
 */
public class ConfigFileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileUtils.class);
    private static final Gson GSON = new Gson();

    // Standard paths and filenames
    /** Name of the metadata file stored in config zips and game directory. */
    public static final String METADATA_FILE = "packcore_metadata.json";
    /** Path to official config zips relative to the game directory. */
    public static final String OFFICIAL_CONFIGS_PATH = "packcore/modpack_config/official_configs";
    /** Path to custom config zips relative to the game directory. */
    public static final String CUSTOM_CONFIGS_PATH = "packcore/modpack_config/custom_configs";

    /**
     * Represents a config file with its metadata.
     */
    public static class ConfigFile {
        private final String fileName;
        private final Path path;
        private final boolean official;
        private final ConfigMetadata metadata;

        /**
         * Constructs a ConfigFile instance.
         *
         * @param fileName Name of the config file.
         * @param path Path to the config file.
         * @param official Whether the config is official.
         * @param metadata Metadata associated with the config.
         */
        public ConfigFile(String fileName, Path path, boolean official, ConfigMetadata metadata) {
            this.fileName = fileName;
            this.path = path;
            this.official = official;
            this.metadata = metadata != null ? metadata : new ConfigMetadata();
        }

        /** @return The file name of the config. */
        public String getFileName() { return fileName; }
        /** @return The path to the config file. */
        public Path getPath() { return path; }
        /** @return True if the config is official, false otherwise. */
        public boolean isOfficial() { return official; }
        /** @return The metadata associated with the config. */
        public ConfigMetadata getMetadata() { return metadata; }

        /**
         * Gets a display name for the config, using metadata if available.
         * Falls back to the file name without extension.
         *
         * @return Display name for the config.
         */
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
     * Gets the currently applied configuration metadata.
     *
     * @return Current config metadata, or a default if none is applied.
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

    /**
     * Creates a default configuration metadata instance.
     *
     * @return Default ConfigMetadata.
     */
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
     * Saves the current config metadata to the game directory.
     *
     * @param metadata The metadata to save.
     * @throws IOException If writing fails.
     */
    public static void saveCurrentConfig(ConfigMetadata metadata) throws IOException {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path metadataPath = gameDir.resolve(METADATA_FILE);

        String json = GSON.toJson(metadata);
        Files.writeString(metadataPath, json, StandardCharsets.UTF_8);
    }

    /**
     * Reads metadata from a zip file.
     *
     * @param zipPath Path to the zip file.
     * @return ConfigMetadata if found, otherwise a fallback metadata.
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

    /**
     * Creates fallback metadata for a config zip if no metadata is found.
     *
     * @param zipPath Path to the zip file.
     * @return Fallback ConfigMetadata.
     */
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
     * Gets all available configs (official and custom).
     *
     * @return List of all ConfigFile instances.
     */
    public static List<ConfigFile> getAllConfigs() {
        List<ConfigFile> configs = new ArrayList<>();
        configs.addAll(getConfigs(OFFICIAL_CONFIGS_PATH, true));
        configs.addAll(getConfigs(CUSTOM_CONFIGS_PATH, false));
        return configs;
    }

    /**
     * Gets official configs only.
     *
     * @return List of official ConfigFile instances.
     */
    public static List<ConfigFile> getOfficialConfigs() {
        return getConfigs(OFFICIAL_CONFIGS_PATH, true);
    }

    /**
     * Gets custom configs only.
     *
     * @return List of custom ConfigFile instances.
     */
    public static List<ConfigFile> getCustomConfigs() {
        return getConfigs(CUSTOM_CONFIGS_PATH, false);
    }

    /**
     * Gets configs from a specific directory.
     *
     * @param relativePath Relative path to the config directory.
     * @param official Whether the configs are official.
     * @return List of ConfigFile instances found in the directory.
     */
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
     * Checks if a config file exists in either official or custom directories.
     *
     * @param fileName Name of the config file (with or without .zip extension).
     * @return True if the config exists, false otherwise.
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
     * Deletes a config file. Only custom configs can be deleted.
     *
     * @param config The ConfigFile to delete.
     * @return True if deletion was successful, false otherwise.
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
