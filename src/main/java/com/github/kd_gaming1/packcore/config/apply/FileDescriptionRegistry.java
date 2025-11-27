package com.github.kd_gaming1.packcore.config.apply;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class FileDescriptionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Gson GSON = new Gson();
    private static final String DESCRIPTIONS_FILE = "file-descriptions.json";
    private static final String CONFIG_PREFIX = "config/";
    private static final String RESOURCEPACKS_PREFIX = "resourcepacks/";

    private static Map<String, FileDescription> masterDescriptions = new HashMap<>();

    static {
        loadMasterDescriptions();
    }

    public record FileDescription(
            String displayName,
            String description,
            String icon,
            boolean isImportant
    ) {}

    /**
     * Loads master descriptions from bundled resource.
     */
    private static void loadMasterDescriptions() {
        try {
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
            Identifier resourceId = Identifier.of(MOD_ID, DESCRIPTIONS_FILE);

            try (InputStream stream = resourceManager.getResource(resourceId)
                    .orElseThrow()
                    .getInputStream()) {

                Map<String, FileDescription> descriptions = parseDescriptionsJson(stream);
                masterDescriptions = descriptions;
                LOGGER.info("Loaded {} master file descriptions", descriptions.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load master descriptions", e);
            masterDescriptions = new HashMap<>();
        }
    }

    /**
     * Loads descriptions from a ZIP file (for config packages).
     * Falls back to master descriptions if not found.
     */
    public static Map<String, FileDescription> loadFromZip(ZipFile zipFile) {
        ZipEntry entry = zipFile.getEntry(DESCRIPTIONS_FILE);

        if (entry != null) {
            try (InputStream stream = zipFile.getInputStream(entry)) {
                Map<String, FileDescription> customDescriptions = parseDescriptionsJson(stream);
                LOGGER.info("Loaded {} custom file descriptions from ZIP", customDescriptions.size());

                // Merge with master (custom overrides master)
                Map<String, FileDescription> merged = new HashMap<>(masterDescriptions);
                merged.putAll(customDescriptions);
                return merged;
            } catch (IOException e) {
                LOGGER.warn("Failed to load descriptions from ZIP, using master", e);
            }
        }

        return new HashMap<>(masterDescriptions);
    }

    /**
     * Parses the descriptions JSON structure from an input stream.
     */
    private static Map<String, FileDescription> parseDescriptionsJson(InputStream stream) {
        JsonObject root = JsonParser.parseReader(
                new InputStreamReader(stream)
        ).getAsJsonObject();

        if (root.has("descriptions")) {
            Type typeToken = new TypeToken<Map<String, FileDescription>>(){}.getType();
            return GSON.fromJson(root.get("descriptions"), typeToken);
        }

        return new HashMap<>();
    }

    /**
     * Gets description from a specific description map.
     */
    public static Optional<FileDescription> getDescription(String path, Map<String, FileDescription> descriptions) {
        String normalizedPath = normalizePath(path);

        // Exact match
        FileDescription exact = descriptions.get(normalizedPath);
        if (exact != null) {
            return Optional.of(exact);
        }

        // Partial match
        for (Map.Entry<String, FileDescription> entry : descriptions.entrySet()) {
            if (normalizedPath.endsWith(entry.getKey().toLowerCase())) {
                return Optional.of(entry.getValue());
            }
        }

        return inferDescription(normalizedPath);
    }

    /**
     * Gets description using master registry (for general use).
     */
    public static Optional<FileDescription> getDescription(String path) {
        return getDescription(path, masterDescriptions);
    }

    /**
     * Normalizes a file path to lowercase with forward slashes.
     */
    private static String normalizePath(String path) {
        return path.replace("\\", "/").toLowerCase();
    }

    /**
     * Infers a description based on the file path.
     */
    private static Optional<FileDescription> inferDescription(String path) {
        if (path.startsWith(CONFIG_PREFIX)) {
            return inferConfigDescription(path);
        }

        if (path.startsWith(RESOURCEPACKS_PREFIX)) {
            return inferResourcePackDescription(path);
        }

        return Optional.empty();
    }

    /**
     * Infers description for configuration files.
     */
    private static Optional<FileDescription> inferConfigDescription(String path) {
        String fileName = path.substring(CONFIG_PREFIX.length());
        int slashIndex = fileName.indexOf('/');

        if (slashIndex > 0) {
            String folderName = fileName.substring(0, slashIndex);
            return Optional.of(new FileDescription(
                    formatModName(folderName) + " Configuration",
                    "Settings and options for " + formatModName(folderName),
                    "⚙",
                    false
            ));
        }

        String baseName = getBaseName(fileName);
        return Optional.of(new FileDescription(
                formatModName(baseName) + " Config",
                "Configuration file for " + formatModName(baseName),
                "⚙",
                false
        ));
    }

    /**
     * Infers description for resource pack files.
     */
    private static Optional<FileDescription> inferResourcePackDescription(String path) {
        String packName = path.substring(RESOURCEPACKS_PREFIX.length());
        int slashIndex = packName.indexOf('/');

        if (slashIndex > 0) {
            packName = packName.substring(0, slashIndex);
        }

        return Optional.of(new FileDescription(
                "Resource Pack: " + packName,
                "Texture and sound resources",
                "🎨",
                false
        ));
    }

    /**
     * Extracts the base name of a file (without extension).
     */
    private static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Formats a mod name by capitalizing words and replacing separators with spaces.
     */
    private static String formatModName(String modName) {
        modName = modName.replace("-", " ").replace("_", " ");
        String[] words = modName.split(" ");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return formatted.toString().trim();
    }

    /**
     * Checks if a file is marked as important.
     */
    public static boolean isImportantFile(String path) {
        return getDescription(path)
                .map(FileDescription::isImportant)
                .orElse(false);
    }

    /**
     * Gets the icon for a file path.
     */
    public static String getIcon(String path) {
        return getDescription(path)
                .map(FileDescription::icon)
                .orElse("📄");
    }
}