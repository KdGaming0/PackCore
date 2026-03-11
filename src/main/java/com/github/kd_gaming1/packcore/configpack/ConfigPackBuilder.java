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

    public static void zipFolder(Path sourceFolder, String outputZipName, ConfigPackMeta meta) throws IOException {
        if (!Files.isDirectory(sourceFolder)) {
            throw new IllegalArgumentException("Expected a directory: " + sourceFolder);
        }

        List<Path> files;
        try (var stream = Files.walk(sourceFolder)) {
            files = stream.filter(Files::isRegularFile).toList();
        }

        Files.createDirectories(OUTPUT_DIR);
        Path outputZip = OUTPUT_DIR.resolve(outputZipName);

        try (ZipOutputStream zip = openZip(outputZip)) {
            writeMetaEntry(zip, meta);
            for (Path file : files) {
                writeEntry(zip, file, sourceFolder.relativize(file).toString());
            }
        }

        LOGGER.info("Created zip '{}' from folder '{}'", outputZip.getFileName(), sourceFolder.getFileName());
    }

    /** Writes selected files to a zip in the default user_configs output directory. */
    public static void zipFiles(Path root, Collection<String> relativePaths, String outputZipName, ConfigPackMeta meta) throws IOException {
        zipFiles(root, relativePaths, OUTPUT_DIR, outputZipName, meta);
    }

    /** Writes selected files to a zip in the given output directory. */
    public static void zipFiles(Path root, Collection<String> relativePaths, Path outputDir, String outputZipName, ConfigPackMeta meta) throws IOException {
        Files.createDirectories(outputDir);
        Path outputZip = outputDir.resolve(outputZipName);

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

    private static void writeMetaEntry(ZipOutputStream zip, ConfigPackMeta meta) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("version", meta.version());
        json.addProperty("targetWidth", meta.targetWidth());
        json.addProperty("targetHeight", meta.targetHeight());
        json.addProperty("createdDate", meta.createdDate());

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

    private static ZipOutputStream openZip(Path zipPath) throws IOException {
        OutputStream fileOut = Files.newOutputStream(zipPath);
        return new ZipOutputStream(fileOut);
    }

    private static void writeEntry(ZipOutputStream zip, Path file, String entryName) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName.replace('\\', '/')));
        Files.copy(file, zip);
        zip.closeEntry();
    }
}