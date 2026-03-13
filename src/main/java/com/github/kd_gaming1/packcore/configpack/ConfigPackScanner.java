package com.github.kd_gaming1.packcore.configpack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ConfigPackScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ConfigPackScanner");
    private static final String CONFIG_FILE_NAME = "pack.json";

    public List<ConfigPackEntry> scanFolder(Path folderPath) throws IOException {
        if (!Files.exists(folderPath)) {
            Files.createDirectories(folderPath);
        }

        if (!Files.isDirectory(folderPath)) {
            throw new IllegalArgumentException("Expected a directory: " + folderPath);
        }

        List<ConfigPackEntry> results = new ArrayList<>();

        try (Stream<Path> entries = Files.list(folderPath)) {
            entries.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".zip"))
                    .forEach(zipPath ->
                            readConfigFromZip(zipPath).ifPresent(json ->
                                    results.add(new ConfigPackEntry(zipPath, json))
                            ));
        }

        return results;
    }

    private Optional<JsonObject> readConfigFromZip(Path zipPath) {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry configEntry = zipFile.getEntry(CONFIG_FILE_NAME);
            if (configEntry == null) {
                return Optional.empty();
            }

            try (InputStream stream = zipFile.getInputStream(configEntry);
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return Optional.of(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Failed to read zip: {} - {}", zipPath, e.getMessage());
            return Optional.empty();
        }
    }
}