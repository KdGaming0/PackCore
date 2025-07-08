package com.kd_gaming1.copysystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service class that handles the business logic for config detection and extraction.
 * Separates the UI concerns from the actual extraction logic.
 * Enhanced with markdown content support.
 */
public class ConfigExtractionService {
    private static final Logger LOGGER = LogManager.getLogger(ConfigExtractionService.class);

    private final File minecraftRoot;
    private final File skyblockFolder;
    private final ConfigExtractor extractor;
    private final MarkdownDialogContentService markdownService;

    public ConfigExtractionService(File minecraftRoot) {
        this.minecraftRoot = minecraftRoot;
        this.skyblockFolder = new File(minecraftRoot, "SkyBlock Enhanced");
        this.extractor = new ConfigExtractor();
        this.markdownService = new MarkdownDialogContentService(skyblockFolder);
    }

    /**
     * Analyzes available configs and determines what action to take
     */
    public ConfigSelectionResult selectAndExtractConfig() {
        List<ConfigInfo> officialConfigs = scanForConfigs("OfficialConfigs");
        List<ConfigInfo> customConfigs = scanForConfigs("CustomConfigs");

        // If custom configs exist, always show dialog for user choice
        if (!customConfigs.isEmpty()) {
            return ConfigSelectionResult.showDialog(officialConfigs, customConfigs);
        }

        // No custom configs, check official configs
        if (officialConfigs.isEmpty()) {
            return ConfigSelectionResult.noConfigs();
        } else if (officialConfigs.size() == 1) {
            ConfigInfo singleConfig = officialConfigs.get(0);
            return ConfigSelectionResult.autoExtract(singleConfig.getName(), ConfigType.OFFICIAL);
        } else {
            return ConfigSelectionResult.showDialog(officialConfigs, customConfigs);
        }
    }

    /**
     * Scans a specific folder for config files
     */
    private List<ConfigInfo> scanForConfigs(String folderName) {
        List<ConfigInfo> configs = new ArrayList<>();
        File folder = new File(skyblockFolder, folderName);

        if (!folder.exists() || !folder.isDirectory()) {
            return configs;
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
                    configs.add(new ConfigInfo(file.getName(), file.length()));
                }
            }
        }

        return configs;
    }

    /**
     * Extracts a specific config file
     */
    public boolean extractConfig(String configName, ConfigType configType) {
        return extractConfig(configName, configType, progress -> {
            LOGGER.debug("Extraction progress: {}%", progress);
        });
    }

    /**
     * Extracts a specific config file with progress callback
     */
    public boolean extractConfig(String configName, ConfigType configType, Consumer<Integer> progressCallback) {
        String subfolderName = configType == ConfigType.OFFICIAL ? "OfficialConfigs" : "CustomConfigs";
        File configFile = new File(new File(skyblockFolder, subfolderName), configName);

        if (!configFile.exists()) {
            LOGGER.error("Config file not found: {}", configFile.getAbsolutePath());
            return false;
        }

        try {
            return extractor.extractZipToDirectory(configFile, minecraftRoot, progressCallback);
        } catch (IOException e) {
            LOGGER.error("Failed to extract config: {}", configName, e);
            return false;
        }
    }

    public List<ConfigInfo> getOfficialConfigs() {
        return scanForConfigs("OfficialConfigs");
    }

    public List<ConfigInfo> getCustomConfigs() {
        return scanForConfigs("CustomConfigs");
    }

    /**
     * Gets the dialog content from markdown file or default content
     */
    public String getDialogContent() {
        return markdownService.getDialogContent();
    }

    /**
     * Creates a sample markdown file for modpack creators
     */
    public boolean createSampleMarkdownFile() {
        return markdownService.createSampleMarkdownFile();
    }

    /**
     * Gets the markdown file location
     */
    public File getMarkdownFile() {
        return markdownService.getMarkdownFile();
    }
    /**
     * Gets help links from the markdown content
     */
    public java.util.Map<String, String> getHelpLinks() {
        return markdownService.getHelpLinks();
    }

    /**
     * Checks if custom markdown content exists
     */
    public boolean hasCustomContent() {
        return markdownService.hasCustomContent();
    }

}