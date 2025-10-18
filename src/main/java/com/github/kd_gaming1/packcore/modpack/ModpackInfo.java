package com.github.kd_gaming1.packcore.modpack;

import com.github.kd_gaming1.packcore.util.GsonUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModpackInfo {
    // Using a special comment field that gets ignored by our code but helps users
    @SerializedName("_comment")
    private String comment = "Edit the values below for your modpack. Remove this _comment field when done.";

    @SerializedName("mod_name")
    private String modName = "YOUR_MODPACK_NAME_HERE";

    @SerializedName("mod_version")
    private String modVersion = "1.0.0";

    @SerializedName("minecraft_version")
    private String minecraftVersion = "1.21.+";

    @SerializedName("author")
    private String author = "YOUR_NAME_HERE";

    @SerializedName("description")
    private String description = "A brief description of your modpack";

    @SerializedName("modrinth_project_id")
    private String modrinthProjectId = "YOUR_PROJECT_ID_FROM_MODRINTH_URL";

    @SerializedName("update_channel")
    private String updateChannel = "release"; // release/beta/alpha

    @SerializedName("website")
    private String website = "https://your-website.com";

    @SerializedName("discord")
    private String discord = "https://discord.gg/your-invite";

    @SerializedName("issue_tracker")
    private String issueTracker = "https://github.com/yourname/yourmod/issues";

    @SerializedName("wiki")
    private String wiki = "https://your-wiki-url.com";

    // File handling
    private static final String CONFIG_FILE_NAME = "modpack-info.json";
    private static final Gson gson = GsonUtils.GSON;

    // File save location
    private static final Path runDir = FabricLoader.getInstance().getGameDir();
    private static final Path packcoreDir = runDir.resolve("packcore");

    // Constructor
    public ModpackInfo() {}

    // Save to file
    public void saveToFile(Path packcoreDir) throws IOException {
        Path filePath = packcoreDir.resolve(CONFIG_FILE_NAME);
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            gson.toJson(this, writer);
        }
    }

    // Load from file
    public static ModpackInfo loadFromFile(Path packcoreDir) throws IOException {
        Path filePath = packcoreDir.resolve(CONFIG_FILE_NAME);

        if (!Files.exists(filePath)) {
            // Create default file
            ModpackInfo defaultInfo = new ModpackInfo();
            defaultInfo.setConfigDirectory(packcoreDir);
            defaultInfo.saveToFile(packcoreDir);
            return defaultInfo;
        }

        try (FileReader reader = new FileReader(filePath.toFile())) {
            ModpackInfo info = gson.fromJson(reader, ModpackInfo.class);
            info.setConfigDirectory(packcoreDir);
            return info;
        }
    }

    // Store the config directory for auto-saving
    private transient Path configDirectory;

    // Method to set the config directory (call this after loading)
    public void setConfigDirectory(Path configDir) {
        this.configDirectory = configDir;
    }

    // Auto-save helper method
    private void autoSave() {
        if (configDirectory != null) {
            try {
                saveToFile(configDirectory);
            } catch (IOException e) {
                System.err.println("Failed to auto-save modpack info: " + e.getMessage());
            }
        }
    }

    public boolean isConfigurationValid() {
        return modrinthProjectId.equals("YOUR_PROJECT_ID_FROM_MODRINTH_URL") ||
                modrinthProjectId.trim().isEmpty() ||
                modName.equals("YOUR_MODPACK_NAME_HERE") ||
                author.equals("YOUR_NAME_HERE");
    }

    public String getValidationError() {
        if (modrinthProjectId.equals("YOUR_PROJECT_ID_FROM_MODRINTH_URL")) {
            return "Modrinth project ID is not configured. Please set a valid project ID.";
        }
        if (modrinthProjectId.trim().isEmpty()) {
            return "Modrinth project ID is empty.";
        }
        if (modName.equals("YOUR_MODPACK_NAME_HERE")) {
            return "Mod name is not configured.";
        }
        if (author.equals("YOUR_NAME_HERE")) {
            return "Author is not configured.";
        }
        return null;
    }

    // Getters
    public String getName() { return modName; }
    public String getVersion() { return modVersion; }
    public String getMinecraftVersion() { return minecraftVersion; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getModrinthProjectId() { return modrinthProjectId; }
    public String getUpdateChannel() { return updateChannel; }
    public String getWebsite() { return website; }
    public String getDiscord() { return discord; }
    public String getIssueTracker() { return issueTracker; }
    public String getWiki() { return wiki; }

    // Setters with auto-save
    public void setName(String name) { this.modName = name; autoSave(); }
    public void setVersion(String version) { this.modVersion = version; autoSave(); }
    public void setMinecraftVersion(String version) { this.minecraftVersion = version; autoSave(); }
    public void setAuthor(String author) { this.author = author; autoSave(); }
    public void setDescription(String description) { this.description = description; autoSave(); }
    public void setModrinthProjectId(String id) { this.modrinthProjectId = id; autoSave(); }
    public void setUpdateChannel(String channel) { this.updateChannel = channel; autoSave(); }
    public void setWebsite(String website) { this.website = website; autoSave(); }
    public void setDiscord(String discord) { this.discord = discord; autoSave(); }
    public void setIssueTracker(String tracker) { this.issueTracker = tracker; autoSave(); }
    public void setWiki(String wiki) { this.wiki = wiki; autoSave(); }
}
