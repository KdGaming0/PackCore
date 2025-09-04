package com.github.kd_gaming1.packcore.wizard.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;

/**
 * Utility class for reading metadata from config ZIP files
 */
public final class ConfigMetadataReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigMetadataReader.class);
    private static final String METADATA_FILE = "packcore_metadata.json";
    private static final Gson GSON = new Gson();

    private ConfigMetadataReader() {}

    /**
     * Reads metadata from a ZIP file
     */
    public static Optional<ConfigMetadata> readMetadata(Path zipPath) {
        try (var zipFile = new ZipFile(zipPath.toFile())) {
            var entry = zipFile.getEntry(METADATA_FILE);
            if (entry == null) {
                LOGGER.debug("No metadata file found in {}", zipPath);
                return Optional.empty();
            }

            try (var inputStream = zipFile.getInputStream(entry)) {
                var json = new String(inputStream.readAllBytes());
                var metadata = GSON.fromJson(json, ConfigMetadata.class);
                return metadata != null ? Optional.of(metadata) : Optional.empty();
            }

        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to read metadata from {}: {}", zipPath, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Metadata for config files
     */
    public record ConfigMetadata(
            String name,
            String description,
            String version,
            String author,
            String createdDate,
            String targetResolution,
            List<String> features,
            List<String> requirements
    ) {}
}