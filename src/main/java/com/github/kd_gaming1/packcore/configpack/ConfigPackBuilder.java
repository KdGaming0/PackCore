package com.github.kd_gaming1.packcore.configpack;

import com.github.kd_gaming1.packcore.PackCore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ConfigPackBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ConfigPackBuilder");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path OUTPUT_DIR = PackCore.PACKCORE_DIR.resolve("user_configs");

    private ConfigPackBuilder() {}

    /**
     * Zips an entire folder into a new zip, including a generated pack.json.
     *
     * @param sourceFolder  The folder to zip.
     * @param outputZipName The name of the zip file to create (e.g. "my_config.zip").
     * @param meta          Metadata to write as pack.json inside the zip.
     */
    public static void zipFolder(Path sourceFolder, String outputZipName, ConfigPackMeta meta) throws IOException {
        if (!Files.isDirectory(sourceFolder)) {
            throw new IllegalArgumentException("Expected a directory: " + sourceFolder);
        }

        List<Path> files;
        try (var stream = Files.walk(sourceFolder)) {
            files = stream.filter(Files::isRegularFile).toList();
        }

        Path outputZip = prepareOutputPath(outputZipName);

        try (ZipOutputStream zip = openZip(outputZip)) {
            writeMetaEntry(zip, meta);

            for (Path file : files) {
                Path entryName = sourceFolder.relativize(file);
                writeEntry(zip, file, entryName.toString());
            }
        }

        LOGGER.info("Created zip '{}' from folder '{}'", outputZip.getFileName(), sourceFolder.getFileName());
    }

    /**
     * Zips specific files (relative to a root) into a new zip, including a generated pack.json.
     *
     * @param root          The root folder all relative paths are relative to.
     * @param relativePaths Relative file paths to include (e.g. "options.txt", "mods/sodium.json").
     * @param outputZipName The name of the zip file to create.
     * @param meta          Metadata to write as pack.json inside the zip.
     */
    public static void zipFiles(Path root, Collection<String> relativePaths, String outputZipName, ConfigPackMeta meta) throws IOException {
        Path outputZip = prepareOutputPath(outputZipName);

        try (ZipOutputStream zip = openZip(outputZip)) {
            writeMetaEntry(zip, meta);

            for (String relative : relativePaths) {
                Path file = root.resolve(relative);

                if (!Files.isRegularFile(file)) {
                    LOGGER.warn("Skipping '{}' — not a regular file or does not exist", relative);
                    continue;
                }

                writeEntry(zip, file, relative);
            }
        }

        LOGGER.info("Created zip '{}' with {} file(s)", outputZip.getFileName(), relativePaths.size());
    }

    /** Builds pack.json from the metadata and writes it as an in-memory zip entry. */
    private static void writeMetaEntry(ZipOutputStream zip, ConfigPackMeta meta) throws IOException {
        JsonObject json = new JsonObject();

        json.addProperty("version",      meta.version());
        json.addProperty("targetWidth",  meta.targetWidth());
        json.addProperty("targetHeight", meta.targetHeight());
        json.addProperty("createdDate",  meta.createdDate());

        // Optional fields — only written if provided
        if (meta.name() != null) json.addProperty("name", meta.name());
        if (meta.description() != null) json.addProperty("description", meta.description());
        if (meta.author() != null) json.addProperty("author", meta.author());

        if (meta.mods() != null && !meta.mods().isEmpty()) {
            JsonArray modsArray = new JsonArray();
            meta.mods().forEach(modsArray::add);
            json.add("mods", modsArray);
        }

        byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);

        zip.putNextEntry(new ZipEntry("pack.json"));
        zip.write(bytes);
        zip.closeEntry();
    }

    /** Ensures the output directory exists and returns the full output path. */
    private static Path prepareOutputPath(String zipName) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        return OUTPUT_DIR.resolve(zipName);
    }

    private static ZipOutputStream openZip(Path zipPath) throws IOException {
        OutputStream fileOut = Files.newOutputStream(zipPath);
        return new ZipOutputStream(fileOut);
    }

    /** Writes one file from disk into the open ZipOutputStream. */
    private static void writeEntry(ZipOutputStream zip, Path file, String entryName) throws IOException {
        // Zip spec requires forward slashes
        String normalisedName = entryName.replace('\\', '/');

        zip.putNextEntry(new ZipEntry(normalisedName));
        Files.copy(file, zip);
        zip.closeEntry();
    }
}