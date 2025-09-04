package com.github.kd_gaming1.packcore.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified metadata model for exported configs.
 *
 * Fields:
 * - name
 * - description
 * - version
 * - author
 * - createdDate (ISO_LOCAL_DATE_TIME recommended)
 * - targetResolution
 * - features (quick highlight features list)
 * - requirements (recommended requirements: MC version or mods etc)
 * - mods (list of mods that are installed/required by this config)
 * - source (Official / Community / System)
 */
public class ConfigMetadata {
    private String name;
    private String description;
    private String version;
    private String author;
    private String createdDate;
    private String targetResolution;
    private List<String> features;
    private List<String> requirements;
    private List<String> mods;
    private String source;

    // Default constructor for Gson
    public ConfigMetadata() {
        this.name = "Unknown";
        this.description = "No description available";
        this.version = "1.0";
        this.author = "Unknown";
        this.createdDate = "";
        this.targetResolution = "Any";
        this.features = new ArrayList<>();
        this.requirements = new ArrayList<>();
        this.mods = new ArrayList<>();
        this.source = "Unknown";
    }

    // Full constructor convenience
    public ConfigMetadata(String name, String description, String version, String author,
                          String createdDate, String targetResolution,
                          List<String> features, List<String> requirements, List<String> mods) {
        this.name = name != null ? name : "Unknown";
        this.description = description != null ? description : "No description available";
        this.version = version != null ? version : "1.0";
        this.author = author != null ? author : "Unknown";
        this.createdDate = createdDate != null ? createdDate : "";
        this.targetResolution = targetResolution != null ? targetResolution : "Any";
        this.features = features != null ? features : new ArrayList<>();
        this.requirements = requirements != null ? requirements : new ArrayList<>();
        this.mods = mods != null ? mods : new ArrayList<>();
        this.source = "Unknown";
    }

    // Getters with safe fallbacks
    public String getName() { return name != null ? name : "Unknown"; }
    public String getDescription() { return description != null ? description : "No description available"; }
    public String getVersion() { return version != null ? version : "1.0"; }
    public String getAuthor() { return author != null ? author : "Unknown"; }
    public String getCreatedDate() { return createdDate != null ? createdDate : ""; }
    public String getTargetResolution() { return targetResolution != null ? targetResolution : "Any"; }
    public List<String> getFeatures() { return features != null ? features : new ArrayList<>(); }
    public List<String> getRequirements() { return requirements != null ? requirements : new ArrayList<>(); }
    public List<String> getMods() { return mods != null ? mods : new ArrayList<>(); }
    public String getSource() { return source != null ? source : "Unknown"; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setVersion(String version) { this.version = version; }
    public void setAuthor(String author) { this.author = author; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
    public void setTargetResolution(String targetResolution) { this.targetResolution = targetResolution; }
    public void setFeatures(List<String> features) { this.features = features; }
    public void setRequirements(List<String> requirements) { this.requirements = requirements; }
    public void setMods(List<String> mods) { this.mods = mods; }
    public void setSource(String source) { this.source = source; }
}