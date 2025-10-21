package com.github.kd_gaming1.packcore.scamshield.detector.language;

import com.github.kd_gaming1.packcore.PackCore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and manages the language pattern database from JSON.
 *
 * This class is responsible for:
 * 1. Loading the phishing-language.json file
 * 2. Parsing it into usable Java objects
 * 3. Providing access to tactics for analysis
 * 4. Hot-reloading when the file changes
 */
public class LanguageDatabase {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String LANGUAGE_FILE = "phishing-language.json";

    private final Path languageFile;
    private Map<String, TacticDefinition> tactics = new HashMap<>();
    private String version = "unknown";

    public LanguageDatabase() {
        // Determine where the language file should be
        Path gameDir = FabricLoader.getInstance().getGameDir();
        this.languageFile = gameDir.resolve("packcore/scamshield/" + LANGUAGE_FILE);

        // Initialize: copy default if missing, then load
        initializeFile();
        load();
    }

    /**
     * Initialize the language file by copying from resources if it doesn't exist.
     * This ensures users always have a working default configuration.
     */
    private void initializeFile() {
        try {
            // Create directory if needed
            Files.createDirectories(languageFile.getParent());

            // If file already exists, don't overwrite
            if (Files.exists(languageFile)) {
                PackCore.LOGGER.info("[ScamShield] Language database found at: {}", languageFile);
                return;
            }

            // Copy default from resources
            String resourcePath = "/assets/packcore/scamshield/" + LANGUAGE_FILE;
            try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    PackCore.LOGGER.error("[ScamShield] Default language file not found in resources: {}", resourcePath);
                    createEmptyFile();
                    return;
                }

                Files.copy(in, languageFile);
                PackCore.LOGGER.info("[ScamShield] Created language database from defaults");
            }
        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to initialize language file", e);
            createEmptyFile();
        }
    }

    /**
     * Create a minimal empty language file as fallback.
     */
    private void createEmptyFile() {
        try {
            String emptyJson = "{\"version\":\"1.0.0\",\"tactics\":{}}";
            Files.writeString(languageFile, emptyJson, StandardCharsets.UTF_8);
            PackCore.LOGGER.warn("[ScamShield] Created empty language database");
        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to create empty language file", e);
        }
    }

    /**
     * Load the language database from disk.
     * This parses the JSON and builds the tactics map.
     */
    public void load() {
        try {
            if (!Files.exists(languageFile)) {
                PackCore.LOGGER.warn("[ScamShield] Language file not found: {}", languageFile);
                return;
            }

            // Read the JSON file
            String json = Files.readString(languageFile, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(json, JsonObject.class);

            // Extract version for logging/debugging
            if (root.has("version")) {
                version = root.get("version").getAsString();
            }

            // Parse tactics object
            if (root.has("tactics")) {
                JsonObject tacticsObj = root.getAsJsonObject("tactics");
                tactics.clear();

                // Each key is a tactic ID (e.g., "urgency", "authority")
                for (String tacticId : tacticsObj.keySet()) {
                    TacticDefinition tactic = GSON.fromJson(
                            tacticsObj.get(tacticId),
                            TacticDefinition.class
                    );
                    tactics.put(tacticId, tactic);
                }
            }

            PackCore.LOGGER.info("[ScamShield] Loaded language database v{} with {} tactics",
                    version, tactics.size());

        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to load language database", e);
        } catch (Exception e) {
            PackCore.LOGGER.error("[ScamShield] Failed to parse language database", e);
        }
    }

    /**
     * Get all defined tactics.
     * Used by PhishingLanguageScam to analyze messages.
     */
    public Map<String, TacticDefinition> getTactics() {
        return new HashMap<>(tactics);
    }

    /**
     * Get a specific tactic by ID.
     */
    public TacticDefinition getTactic(String tacticId) {
        return tactics.get(tacticId);
    }

    /**
     * Reload the database from disk.
     * Call this when the file changes or via /scamshield reload command.
     */
    public void reload() {
        PackCore.LOGGER.info("[ScamShield] Reloading language database...");
        load();
    }

    public String getVersion() {
        return version;
    }

    public Path getLanguageFile() {
        return languageFile;
    }
}