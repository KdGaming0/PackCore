package com.github.kdgaming0.packcore.tweaker;

import com.github.kdgaming0.packcore.copysystem.ZipSelectionDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * TestTweaker handles the initialization process during Minecraft startup.
 * It checks the configuration to determine whether to display the ZipSelectionDialog.
 */
public class PackConfigTweaker implements ITweaker {
    private static final Logger LOGGER = LogManager.getLogger("PackCore");
    private static final String CONFIG_FILENAME = "PackCore.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private File gameDir;
    private File configFile;
    private JsonObject configData;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.gameDir = gameDir;
        // Define the path to the configuration file within the 'config' subdirectory
        this.configFile = new File(new File(gameDir, "config"), CONFIG_FILENAME);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        @SuppressWarnings("unchecked")
        List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");
        tweakClasses.add("org.spongepowered.asm.launch.MixinTweaker");

        loadConfig();

        // Check if PromptSetDefaultConfig is true
        if (getPromptSetDefaultConfig()) {
            LOGGER.info("PromptSetDefaultConfig is enabled. Displaying ZipSelectionDialog.");
            // Show the ZipSelectionDialog to the user
            ZipSelectionDialog.showDialog(gameDir);

            // After the dialog operation, set PromptSetDefaultConfig to false
            setPromptSetDefaultConfig(false);
            saveConfig();
        } else {
            LOGGER.info("PromptSetDefaultConfig is disabled. Skipping ZipSelectionDialog.");
        }
    }

    @Override
    public String getLaunchTarget() {
        return null; // No specific launch target needed
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0]; // No additional launch arguments
    }

    /**
     * Loads the configuration from the PackCore.json file.
     * If the file doesn't exist, a default configuration is created.
     */
    private void loadConfig() {
        if (!configFile.exists()) {
            LOGGER.warn("Config file not found. Creating default configuration.");
            createDefaultConfig();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            configData = gson.fromJson(reader, JsonObject.class);
            LOGGER.info("Configuration loaded successfully from " + configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to load config file: " + configFile.getAbsolutePath(), e);
            // Create default config if reading fails
            createDefaultConfig();
        }
    }

    /**
     * Creates a default configuration with PromptSetDefaultConfig set to true.
     */
    private void createDefaultConfig() {
        // Ensure the 'config' directory exists
        File configDirectory = configFile.getParentFile();
        if (!configDirectory.exists()) {
            boolean dirCreated = configDirectory.mkdirs();
            if (dirCreated) {
                LOGGER.info("Config directory created at: " + configDirectory.getAbsolutePath());
            } else {
                LOGGER.error("Failed to create config directory at: " + configDirectory.getAbsolutePath());
                return;
            }
        }

        JsonObject defaultConfig = new JsonObject();
        defaultConfig.addProperty("PromptSetDefaultConfig", true);
        defaultConfig.addProperty("ShowOptifineGuide", true);

        configData = defaultConfig;
        saveConfig();
        LOGGER.info("Default configuration created and saved to " + configFile.getAbsolutePath());
    }

    /**
     * Saves the current configuration to the PackCore.json file.
     */
    private void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(configData, writer);
            LOGGER.info("Configuration saved successfully to " + configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to save config to: " + configFile.getAbsolutePath(), e);
        }
    }

    /**
     * Retrieves the value of PromptSetDefaultConfig from the configuration.
     *
     * @return true if the dialog should be shown; false otherwise.
     */
    private boolean getPromptSetDefaultConfig() {
        if (configData == null) {
            loadConfig();
        }
        return configData.has("PromptSetDefaultConfig") && configData.get("PromptSetDefaultConfig").getAsBoolean();
    }

    /**
     * Sets the value of PromptSetDefaultConfig in the configuration.
     *
     * @param value The new value to set.
     */
    private void setPromptSetDefaultConfig(boolean value) {
        if (configData == null) {
            loadConfig();
        }
        configData.addProperty("PromptSetDefaultConfig", value);
        LOGGER.info("PromptSetDefaultConfig set to " + value);
    }
}