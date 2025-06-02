package com.kd_gaming1.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader; // Changed from Forge Loader
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static com.kd_gaming1.PackCore.MOD_ID;

public class ModConfig {
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static final String CONFIG_FILENAME = "PackCore.json"; // Or use MOD_ID + ".json"

    // Updated to use Fabric's way of getting the config directory
    private static final Path configPath = FabricLoader.getInstance().getConfigDir();
    private static final File CONFIG_FILE = new File(configPath.toFile(), CONFIG_FILENAME);

    private static JsonObject configData;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            createDefaultConfig(); // This will also save the config, creating directories if needed
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            configData = gson.fromJson(reader, JsonObject.class);
            if (configData == null) { // Handle case where file is empty or invalid JSON
                LOGGER.warn("Config file was empty or invalid. Creating default config.");
                createDefaultConfig();
            } else {
                updateConfigWithDefaults();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config file: " + CONFIG_FILE.getAbsolutePath(), e);
            // Create default config if reading fails
            createDefaultConfig();
        }
    }

    private static void updateConfigWithDefaults() {
        boolean updated = false;

        if (!configData.has("PromptSetDefaultConfig")) {
            configData.addProperty("PromptSetDefaultConfig", true);
            updated = true;
        }
        if (!configData.has("EnableCustomMenu")) {
            configData.addProperty("EnableCustomMenu", true);
            updated = true;
        }

        if (updated) {
            saveConfig();
        }
    }

    private static void createDefaultConfig() {
        LOGGER.info("Creating default config file at: " + CONFIG_FILE.getAbsolutePath());
        JsonObject defaultConfig = new JsonObject();

        // Add your config values here
        defaultConfig.addProperty("PromptSetDefaultConfig", true);
        defaultConfig.addProperty("EnableCustomMenu", true);

        configData = defaultConfig; // Set the default config as current
        saveConfig(); // Save to file
    }

    // Getter methods
    public static boolean getPromptSetDefaultConfig() {
        if (configData == null) loadConfig();
        return configData.get("PromptSetDefaultConfig").getAsBoolean();
    }

    public static boolean getEnableCustomMenu() {
        if (configData == null) loadConfig();
        return configData.get("EnableCustomMenu").getAsBoolean();
    }

    // Setter methods
    public static void setPromptSetDefaultConfig(boolean value) {
        if (configData == null) loadConfig();
        configData.addProperty("PromptSetDefaultConfig", value);
        saveConfig();
    }

    public static void setEnableCustomMenu(boolean value) {
        if (configData == null) loadConfig();
        configData.addProperty("EnableCustomMenu", value);
        saveConfig();
    }

    // Save config to file
    public static void saveConfig() {
        if (configData == null) {
            LOGGER.warn("Attempted to save null config data. Aborting save.");
            return;
        }
        try {
            File parentDir = CONFIG_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    LOGGER.error("Could not create parent directories for config file: " + CONFIG_FILE.getAbsolutePath());
                    return; // Don't attempt to write if directory creation failed
                }
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                gson.toJson(configData, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config to: " + CONFIG_FILE.getAbsolutePath(), e);
        }
    }
}