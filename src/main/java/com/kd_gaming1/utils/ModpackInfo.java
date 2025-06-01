package com.kd_gaming1.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.kd_gaming1.PackCore.MOD_ID;

public class ModpackInfo {
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static final String MODPACK_INFO_PATH = "SkyBlock Enhanced/modpack_info.json";
    private static JsonObject modpackData;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void loadModpackInfo() {
        File modpackFile = new File(MODPACK_INFO_PATH);

        modpackFile.getParentFile().mkdirs();

        if (!modpackFile.exists()) {
            createDefaultConfig(modpackFile);
        }

        try (FileReader reader = new FileReader(modpackFile)) {
            modpackData = gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            LOGGER.error("Failed to load modpack info", e);
        }
    }

    private static void createDefaultConfig(File file) {
        JsonObject defaultConfig = new JsonObject();
        String currentTime = LocalDateTime.now().format(dateFormatter);

        // Modpack section
        JsonObject modpack = new JsonObject();
        modpack.addProperty("name", "SkyBlock Enhanced");
        modpack.addProperty("version", "1.0.0");
        modpack.addProperty("minecraft_version", "1.8.9");
        modpack.addProperty("author", "KdGaming0");
        modpack.addProperty("created_at", currentTime);
        modpack.addProperty("last_updated", currentTime);
        modpack.addProperty("description", "A enhanced SkyBlock experience for Minecraft 1.8.9");

        // Update section
        JsonObject update = new JsonObject();
        update.addProperty("modrinth_project_id", "");
        update.addProperty("check_for_updates", true);
        update.addProperty("update_channel", "release");

        // Social section
        JsonObject social = new JsonObject();
        social.addProperty("website", "");
        social.addProperty("discord", "");
        social.addProperty("issue_tracker", "");
        social.addProperty("wiki", "");

        // How to install Optifine
        JsonObject optifine = new JsonObject();
        optifine.addProperty("Guide", "1.8.9_HD_U_L5");

        // Add all sections
        defaultConfig.add("modpack", modpack);
        defaultConfig.add("update", update);
        defaultConfig.add("social", social);
        defaultConfig.add("optifine", optifine);

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(defaultConfig, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to create default config", e);
        }
    }

    // Getter methods
    public static String getCurrentVersion() {
        if (modpackData == null) loadModpackInfo();
        return modpackData.getAsJsonObject("modpack").get("version").getAsString();
    }

    public static String getModrinthProjectId() {
        if (modpackData == null) loadModpackInfo();
        return modpackData.getAsJsonObject("update").get("modrinth_project_id").getAsString();
    }

    public static boolean shouldCheckForUpdates() {
        if (modpackData == null) loadModpackInfo();
        return modpackData.getAsJsonObject("update").get("check_for_updates").getAsBoolean();
    }

    public static String getOptifineGuide() {
        if (modpackData == null) loadModpackInfo();
        return modpackData.getAsJsonObject("optifine").get("Guide").getAsString();
    }
}