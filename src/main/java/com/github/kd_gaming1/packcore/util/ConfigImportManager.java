package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.gui.configscreen.ui.UITheme;
import com.google.gson.Gson;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class ConfigImportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigImportManager.class);
    private static final Gson GSON = new Gson();

    public interface ImportProgressCallback {
        void onProgress(String stage, int percentage);
        void onComplete(boolean success, String message);
        void onError(String error);
    }

    /**
     * Opens a native file dialog to select config zip files
     */
    public static CompletableFuture<Path> selectConfigFile() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return selectFileWithNativeDialog();
            } catch (Exception e) {
                LOGGER.error("Failed to open file dialog", e);
                return null;
            }
        });
    }

    private static Path selectFileWithNativeDialog() {
        try {
            // Get the Minecraft window for proper dialog parenting
            Frame parentFrame = null;
            try {
                // Try to get the Minecraft window as parent
                Window[] windows = Window.getWindows();
                for (Window window : windows) {
                    if (window instanceof Frame && window.isVisible()) {
                        parentFrame = (Frame) window;
                        break;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Could not find parent window, using null parent");
            }

            // Create a native file dialog with proper parent
            FileDialog fileDialog = new FileDialog(parentFrame, "Select Config File to Import", FileDialog.LOAD);

            // Set file filter for zip files - this makes only .zip files visible by default
            fileDialog.setFilenameFilter((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".zip");
            });

            // Set file pattern hint for better UX (on some systems this shows in the dialog)
            try {
                fileDialog.setFile("*.zip");
            } catch (Exception e) {
                // Some systems don't support this, ignore
                LOGGER.debug("Could not set file pattern hint");
            }

            // Set initial directory to Downloads or user home
            String userHome = System.getProperty("user.home");
            Path downloadsPath = Paths.get(userHome, "Downloads");
            if (Files.exists(downloadsPath)) {
                fileDialog.setDirectory(downloadsPath.toString());
            } else {
                fileDialog.setDirectory(userHome);
            }

            // Make sure dialog appears on top
            fileDialog.setAlwaysOnTop(true);

            // For additional focus handling, especially important for games
            if (parentFrame != null) {
                // Temporarily minimize or hide the game window to ensure dialog visibility
                boolean wasFullScreen = false;
                try {
                    // Check if we're in fullscreen mode
                    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                    wasFullScreen = gd.getFullScreenWindow() != null;

                    if (wasFullScreen) {
                        LOGGER.info("Game is in fullscreen, dialog may appear behind. Consider windowed mode for file operations.");
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not check fullscreen state");
                }
            }

            // Show the dialog - this blocks until user selects or cancels
            LOGGER.info("Opening file dialog for config selection...");
            fileDialog.setVisible(true);

            // Get the selected file
            String directory = fileDialog.getDirectory();
            String filename = fileDialog.getFile();

            // Dispose of the dialog to free resources
            fileDialog.dispose();

            if (directory != null && filename != null) {
                Path selectedPath = Paths.get(directory, filename);
                LOGGER.info("User selected config file: {}", selectedPath);

                // Verify it's actually a zip file
                if (!filename.toLowerCase().endsWith(".zip")) {
                    LOGGER.warn("Selected file is not a .zip file: {}", filename);
                    return null;
                }

                return selectedPath;
            } else {
                LOGGER.info("User cancelled file selection");
                return null;
            }

        } catch (Exception e) {
            LOGGER.error("Error with native file dialog", e);
            return null;
        }
    }


    /**
     * Validates and reads metadata from a selected config file
     */
    public static ConfigMetadata previewConfigMetadata(Path configPath) {
        if (configPath == null || !Files.exists(configPath)) {
            LOGGER.error("Config file does not exist: {}", configPath);
            return null;
        }

        if (!configPath.toString().toLowerCase().endsWith(".zip")) {
            LOGGER.error("Selected file is not a zip file: {}", configPath);
            return null;
        }

        try {
            return ConfigFileUtils.readMetadataFromZip(configPath, false);
        } catch (Exception e) {
            LOGGER.error("Failed to read config metadata", e);
            return null;
        }
    }

    /**
     * Imports a config file to the custom configs directory
     */
    public static void importConfig(Path sourceFile, boolean applyImmediately, ImportProgressCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                callback.onProgress("Validating file...", 10);

                if (!validateConfigFile(sourceFile)) {
                    callback.onError("Invalid config file format");
                    return;
                }

                callback.onProgress("Reading metadata...", 20);
                ConfigMetadata metadata = previewConfigMetadata(sourceFile);
                if (metadata == null) {
                    callback.onError("Could not read config metadata");
                    return;
                }

                callback.onProgress("Copying file...", 40);
                Path destinationPath = copyToCustomConfigs(sourceFile, metadata);

                if (destinationPath == null) {
                    callback.onError("Failed to copy config file");
                    return;
                }

                callback.onProgress("Import complete", 80);

                if (applyImmediately) {
                    callback.onProgress("Preparing to apply config...", 90);

                    // Create a ConfigFile object for the application manager
                    ConfigFileUtils.ConfigFile configFile = new ConfigFileUtils.ConfigFile(
                            destinationPath.getFileName().toString(),
                            destinationPath,
                            false,
                            metadata
                    );

                    // Apply the config - this will schedule restart
                    ConfigApplicationManager.applyConfigOnRestart(configFile);
                    callback.onComplete(true, "Config imported and will be applied on restart. Game will close shortly.");
                } else {
                    callback.onProgress("Complete", 100);
                    callback.onComplete(true, "Config imported successfully to: " + destinationPath.getFileName());
                }

            } catch (Exception e) {
                LOGGER.error("Failed to import config", e);
                callback.onError("Import failed: " + e.getMessage());
            }
        });
    }

    private static boolean validateConfigFile(Path configPath) {
        try {
            // Enhanced validation - check if it's a valid zip and has reasonable size
            if (!Files.exists(configPath) ||
                    !configPath.toString().toLowerCase().endsWith(".zip") ||
                    Files.size(configPath) == 0) {
                return false;
            }

            // Try to read the zip to ensure it's not corrupted
            try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(configPath.toFile())) {
                // Just opening and closing validates the zip structure
                return zipFile.entries().hasMoreElements();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to validate config file", e);
            return false;
        }
    }

    private static Path copyToCustomConfigs(Path sourceFile, ConfigMetadata metadata) {
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path customConfigsDir = gameDir.resolve(ConfigFileUtils.CUSTOM_CONFIGS_PATH);
            Files.createDirectories(customConfigsDir);

            // Generate a unique filename
            String baseName = metadata.getName().replaceAll("[^a-zA-Z0-9\\-_]", "_");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = baseName + "_imported_" + timestamp + ".zip";

            Path destinationPath = customConfigsDir.resolve(fileName);

            // Ensure we don't overwrite existing files
            int counter = 1;
            while (Files.exists(destinationPath)) {
                String nameWithCounter = baseName + "_imported_" + timestamp + "_" + counter + ".zip";
                destinationPath = customConfigsDir.resolve(nameWithCounter);
                counter++;
            }

            // Copy the file
            Files.copy(sourceFile, destinationPath, StandardCopyOption.COPY_ATTRIBUTES);

            LOGGER.info("Config imported to: {}", destinationPath);
            return destinationPath;

        } catch (IOException e) {
            LOGGER.error("Failed to copy config file", e);
            return null;
        }
    }

    /**
     * Creates a metadata preview component for the UI
     */
    public static FlowLayout createMetadataPreview(ConfigMetadata metadata) {
        FlowLayout previewPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        previewPanel.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        previewPanel.padding(Insets.of(12));
        previewPanel.gap(6);

        // Header
        LabelComponent headerLabel = Components.label(Text.literal("Config Preview"))
                .color(UITheme.color(UITheme.ACCENT_GOLD));
        previewPanel.child(headerLabel);

        // Basic info
        FlowLayout basicInfo = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        basicInfo.gap(3);

        basicInfo.child(createInfoRow("Name:", metadata.getName()));
        basicInfo.child(createInfoRow("Author:", metadata.getAuthor()));
        basicInfo.child(createInfoRow("Version:", metadata.getVersion()));
        basicInfo.child(createInfoRow("Resolution:", metadata.getTargetResolution()));

        if (metadata.getCreatedDate() != null && !metadata.getCreatedDate().isEmpty()) {
            basicInfo.child(createInfoRow("Created:", metadata.getCreatedDate()));
        }

        previewPanel.child(basicInfo);

        // Description
        if (metadata.getDescription() != null && !metadata.getDescription().equals("No description available")) {
            FlowLayout descSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            descSection.gap(3);
            descSection.margins(Insets.top(8));

            descSection.child(Components.label(Text.literal("Description:"))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));
            descSection.child(Components.label(Text.literal(metadata.getDescription()))
                    .color(UITheme.color(UITheme.TEXT_WHITE))
                    .sizing(Sizing.fill(95), Sizing.content()));

            previewPanel.child(descSection);
        }

        // Features
        if (metadata.getFeatures() != null && !metadata.getFeatures().isEmpty()) {
            FlowLayout featuresSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            featuresSection.gap(2);
            featuresSection.margins(Insets.top(8));

            featuresSection.child(Components.label(Text.literal("Features:"))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));

            for (String feature : metadata.getFeatures()) {
                featuresSection.child(Components.label(Text.literal("• " + feature))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }

            previewPanel.child(featuresSection);
        }

        // Requirements
        if (metadata.getRequirements() != null && !metadata.getRequirements().isEmpty()) {
            FlowLayout requirementsSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            requirementsSection.gap(2);
            requirementsSection.margins(Insets.top(8));

            requirementsSection.child(Components.label(Text.literal("Requirements:"))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));

            for (String requirement : metadata.getRequirements()) {
                requirementsSection.child(Components.label(Text.literal("• " + requirement))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }

            previewPanel.child(requirementsSection);
        }

        // Mods
        if (metadata.getMods() != null && !metadata.getMods().isEmpty()) {
            FlowLayout modsSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            modsSection.gap(2);
            modsSection.margins(Insets.top(8));

            modsSection.child(Components.label(Text.literal("Included Mods:"))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));

            // Limit display to first 10 mods to avoid UI clutter
            int maxMods = Math.min(metadata.getMods().size(), 10);
            for (int i = 0; i < maxMods; i++) {
                modsSection.child(Components.label(Text.literal("• " + metadata.getMods().get(i)))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }

            if (metadata.getMods().size() > maxMods) {
                modsSection.child(Components.label(Text.literal("... and " + (metadata.getMods().size() - maxMods) + " more"))
                        .color(UITheme.color(UITheme.TEXT_SECONDARY)));
            }

            previewPanel.child(modsSection);
        }

        return previewPanel;
    }

    private static FlowLayout createInfoRow(String label, String value) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(8);

        LabelComponent labelComp = (LabelComponent) Components.label(Text.literal(label))
                .color(UITheme.color(UITheme.TEXT_SECONDARY))
                .sizing(Sizing.fixed(80), Sizing.content());

        LabelComponent valueComp = (LabelComponent) Components.label(Text.literal(value))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .sizing(Sizing.expand(), Sizing.content());

        row.child(labelComp);
        row.child(valueComp);

        return row;
    }

    /**
     * Checks if a config with the same name already exists
     */
    public static boolean configWithNameExists(String configName) {
        String sanitizedName = configName.replaceAll("[^a-zA-Z0-9\\-_]", "_");

        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path customConfigsDir = gameDir.resolve(ConfigFileUtils.CUSTOM_CONFIGS_PATH);

            if (!Files.exists(customConfigsDir)) {
                return false;
            }

            try (var files = Files.list(customConfigsDir)) {
                return files.anyMatch(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.startsWith(sanitizedName.toLowerCase()) && fileName.endsWith(".zip");
                });
            }
        } catch (IOException e) {
            LOGGER.error("Error checking for existing configs", e);
            return false;
        }
    }
}