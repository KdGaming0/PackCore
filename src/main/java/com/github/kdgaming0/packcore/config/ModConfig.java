package com.github.kdgaming0.packcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static com.github.kdgaming0.packcore.PackCore.MOD_ID;

public class ModConfig {
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static final String CONFIG_FILENAME = "PackCore.json";
    private static final File CONFIG_FILE = new File(Loader.instance().getConfigDir(), CONFIG_FILENAME);
    private static JsonObject configData;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            createDefaultConfig();
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            configData = gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            LOGGER.error("Failed to load config file: " + CONFIG_FILE.getAbsolutePath(), e);
            // Create default config if reading fails
            createDefaultConfig();
        }
    }

    private static void createDefaultConfig() {
        JsonObject defaultConfig = new JsonObject();

        // Add your config values here
        defaultConfig.addProperty("PromptSetDefaultConfig", true);
        defaultConfig.addProperty("ShowOptifineGuide", true);

        configData = defaultConfig; // Set the default config as current
        saveConfig(); // Save to file
    }

    // Getter methods
    public static boolean getPromptSetDefaultConfig() {
        if (configData == null) loadConfig();
        return configData.get("PromptSetDefaultConfig").getAsBoolean();
    }

    public static boolean getShowOptifineGuide() {
        if (configData == null) loadConfig();
        return configData.get("ShowOptifineGuide").getAsBoolean();
    }

    // Setter methods
    public static void setPromptSetDefaultConfig(boolean value) {
        if (configData == null) loadConfig();
        configData.addProperty("PromptSetDefaultConfig", value);
        saveConfig();
    }

    public static void setShowOptifineGuide(boolean value) {
        if (configData == null) loadConfig();
        configData.addProperty("ShowOptifineGuide", value);
        saveConfig();
    }

    // Save config to file
    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(configData, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config to: " + CONFIG_FILE.getAbsolutePath(), e);
        }
    }
}