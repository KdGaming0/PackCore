package com.github.kd_gaming1.packcore.metadata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModpackMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ModpackMetadata");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonObject DEFAULTS = buildDefaults();
    private static final String FILE_NAME = "modpack.json";
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getGameDir().resolve("packcore").resolve(FILE_NAME);

    private static final class Holder {
        static final ModpackMetadata INSTANCE = new ModpackMetadata();
    }

    public static ModpackMetadata getInstance() {
        return Holder.INSTANCE;
    }

    private final String modpackName;
    private final String modpackVersion;
    private final String minecraftVersion;
    private final String author;
    private final String description;
    private final String modrinthProjectId;
    private final String websiteUrl;
    private final String discordUrl;
    private final String issueTrackerUrl;
    private final String wikiUrl;

    private ModpackMetadata() {
        ensureFileExists();

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            modpackName = get(json, "modpackName");
            modpackVersion = get(json, "modpackVersion");
            minecraftVersion = get(json, "minecraftVersion");
            author = get(json, "author");
            description = get(json, "description");
            modrinthProjectId = get(json, "modrinthProjectId");
            websiteUrl = get(json, "websiteUrl");
            discordUrl = get(json, "discordUrl");
            issueTrackerUrl = get(json, "issueTrackerUrl");
            wikiUrl = get(json, "wikiUrl");

        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + FILE_NAME, e);
        }
    }

    private void ensureFileExists() {
        if (Files.exists(CONFIG_PATH)) return;

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(DEFAULTS, writer);
            }

            LOGGER.info("Created {} with default values", FILE_NAME);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create " + FILE_NAME, e);
        }
    }

    private static JsonObject buildDefaults() {
        JsonObject json = new JsonObject();
        json.addProperty("modpackName", "Unknown Modpack");
        json.addProperty("modpackVersion", "Unknown");
        json.addProperty("minecraftVersion", "Unknown");
        json.addProperty("author", "Unknown Author");
        json.addProperty("description", "Unknown Description");
        json.addProperty("modrinthProjectId", "Unknown Modrinth ID");
        json.addProperty("websiteUrl", "Unknown Website URL");
        json.addProperty("discordUrl", "Unknown Discord URL");
        json.addProperty("issueTrackerUrl", "Unknown Issue URL");
        json.addProperty("wikiUrl", "Unknown Wiki URL");
        return json;
    }

    /** Returns the value for the given key, falling back to the default if missing. */
    private String get(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        LOGGER.warn("{} missing field '{}', using default", FILE_NAME, key);
        return DEFAULTS.get(key).getAsString();
    }

    public String getModpackName()      { return modpackName; }
    public String getModpackVersion()   { return modpackVersion; }
    public String getMinecraftVersion() { return minecraftVersion; }
    public String getAuthor()           { return author; }
    public String getDescription()      { return description; }
    public String getModrinthProjectId(){ return modrinthProjectId; }
    public String getWebsiteUrl()       { return websiteUrl; }
    public String getDiscordUrl()       { return discordUrl; }
    public String getIssueTrackerUrl()  { return issueTrackerUrl; }
    public String getWikiUrl()          { return wikiUrl; }
}