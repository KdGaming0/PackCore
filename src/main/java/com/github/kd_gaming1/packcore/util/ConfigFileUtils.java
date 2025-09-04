package com.github.kd_gaming1.packcore.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.MinecraftClient;
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

public class ConfigFileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileUtils.class);
    private static final Gson GSON = new Gson();

    // Standardized metadata filename used inside zips and in the game directory
    public static final String OFFICIAL_CONFIGS_PATH = "packcore/modpack_config/official_configs";
    public static final String CUSTOM_CONFIGS_PATH = "packcore/modpack_config/custom_configs";
    public static final String METADATA_FILE = "packcore_metadata.json";

    public static class ConfigFile {
        private final String name;
        private final Path path;
        private final boolean isOfficial;
        private final ConfigMetadata metadata;

        public ConfigFile(String name, Path path, boolean isOfficial, ConfigMetadata metadata) {
            this.name = name;
            this.path = path;
            this.isOfficial = isOfficial;
            this.metadata = metadata != null ? metadata : new ConfigMetadata();
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            // Use metadata name if available, otherwise use filename without extension
            String metadataName = metadata.getName();
            if (!"Unknown".equals(metadataName)) {
                return metadataName;
            }
            return name.endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
        }

        public Path getPath() {
            return path;
        }

        public boolean isOfficial() {
            return isOfficial;
        }

        public ConfigMetadata getMetadata() {
            return metadata;
        }
    }

    /**
     * Gets the currently applied config by reading packcore_metadata.json from rundir
     */
    public static ConfigMetadata getCurrentConfig() {
        Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
        Path configInfoPath = gameDir.resolve(METADATA_FILE);

        if (!Files.exists(configInfoPath)) {
            // Return default config info if no config is applied
            ConfigMetadata defaultConfig = new ConfigMetadata();
            defaultConfig.setName("Default Configuration");
            defaultConfig.setSource("System");
            return defaultConfig;
        }

        try {
            String content = Files.readString(configInfoPath, StandardCharsets.UTF_8);
            ConfigMetadata metadata = GSON.fromJson(content, ConfigMetadata.class);
            if (metadata == null) {
                metadata = new ConfigMetadata();
                metadata.setName("Unknown Configuration");
                metadata.setSource("System");
            }
            return metadata;
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to read current config info", e);
            ConfigMetadata errorConfig = new ConfigMetadata();
            errorConfig.setName("Error Reading Config");
            errorConfig.setSource("System");
            return errorConfig;
        }
    }

    /**
     * Reads metadata from a zip file without extracting it.
     * Made public so other managers can use it (e.g. application manager).
     */
    public static ConfigMetadata readMetadataFromZip(Path zipPath, boolean isOfficial) {
        ConfigMetadata metadata = new ConfigMetadata();

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry metadataEntry = zipFile.getEntry(METADATA_FILE);

            if (metadataEntry != null) {
                try (InputStreamReader reader = new InputStreamReader(
                        zipFile.getInputStream(metadataEntry), StandardCharsets.UTF_8)) {
                    metadata = GSON.fromJson(reader, ConfigMetadata.class);
                    if (metadata == null) {
                        metadata = new ConfigMetadata();
                    }
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.debug("Could not read metadata from {}: {}", zipPath, e.getMessage());
            metadata = new ConfigMetadata();
        }

        // Set fallback values
        if ("Unknown".equals(metadata.getName())) {
            String fileName = zipPath.getFileName().toString();
            String displayName = fileName.endsWith(".zip") ? fileName.substring(0, fileName.length() - 4) : fileName;
            metadata.setName(displayName);
        }

        metadata.setSource(isOfficial ? "Official" : "Community");

        return metadata;
    }

    /**
     * Reads all config files from both official and custom directories
     */
    public static List<ConfigFile> getAllConfigs() {
        List<ConfigFile> configs = new ArrayList<>();

        // Get official configs
        configs.addAll(getConfigsFromDirectory(OFFICIAL_CONFIGS_PATH, true));

        // Get custom configs
        configs.addAll(getConfigsFromDirectory(CUSTOM_CONFIGS_PATH, false));

        return configs;
    }

    /**
     * Reads config files from official directory only
     */
    public static List<ConfigFile> getOfficialConfigs() {
        return getConfigsFromDirectory(OFFICIAL_CONFIGS_PATH, true);
    }

    /**
     * Reads config files from custom directory only
     */
    public static List<ConfigFile> getCustomConfigs() {
        return getConfigsFromDirectory(CUSTOM_CONFIGS_PATH, false);
    }

    private static List<ConfigFile> getConfigsFromDirectory(String relativePath, boolean isOfficial) {
        List<ConfigFile> configs = new ArrayList<>();

        try {
            // Get the game directory (rundir)
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path configDir = gameDir.resolve(relativePath);

            // Create directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                LOGGER.info("Created config directory: {}", configDir);
                return configs; // Return empty list for new directory
            }

            // Read all .zip files from the directory
            try (Stream<Path> files = Files.list(configDir)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().toLowerCase().endsWith(".zip"))
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            ConfigMetadata metadata = readMetadataFromZip(path, isOfficial);
                            configs.add(new ConfigFile(fileName, path, isOfficial, metadata));
                        });
            }

            LOGGER.debug("Found {} configs in {}", configs.size(), relativePath);

        } catch (IOException e) {
            LOGGER.error("Failed to read config directory: {}", relativePath, e);
        }

        return configs;
    }

    /**
     * Check if a config file exists in either directory
     */
    public static boolean configExists(String fileName) {
        if (!fileName.endsWith(".zip")) {
            fileName += ".zip";
        }

        Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
        Path officialPath = gameDir.resolve(OFFICIAL_CONFIGS_PATH).resolve(fileName);
        Path customPath = gameDir.resolve(CUSTOM_CONFIGS_PATH).resolve(fileName);

        return Files.exists(officialPath) || Files.exists(customPath);
    }
}