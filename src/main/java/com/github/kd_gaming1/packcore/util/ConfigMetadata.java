package com.github.kd_gaming1.packcore.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Simplified metadata model for exported configs.
 * Contains only essential information needed by importers.
 */
public class ConfigMetadata {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String name;
    private String description;
    private String version;
    private String author;
    private String createdDate;
    private String targetResolution;
    private List<String> mods;
    private String source;  // "Official", "Community", or "System"

    // Default constructor for Gson
    public ConfigMetadata() {
        this.name = "Unknown Config";
        this.description = "";
        this.version = "1.0.0";
        this.author = "Unknown";
        this.createdDate = LocalDateTime.now().format(DATE_FORMAT);
        this.targetResolution = "Any";
        this.mods = new ArrayList<>();
        this.source = "Community";
    }

    // Builder pattern for cleaner creation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ConfigMetadata metadata = new ConfigMetadata();

        public Builder name(String name) {
            metadata.name = name != null && !name.isBlank() ? name : "Unknown Config";
            return this;
        }

        public Builder description(String description) {
            metadata.description = description != null ? description : "";
            return this;
        }

        public Builder version(String version) {
            metadata.version = version != null && !version.isBlank() ? version : "1.0.0";
            return this;
        }

        public Builder author(String author) {
            metadata.author = author != null && !author.isBlank() ? author : "Unknown";
            return this;
        }

        public Builder targetResolution(String resolution) {
            metadata.targetResolution = resolution != null && !resolution.isBlank() ? resolution : "Any";
            return this;
        }

        public Builder mods(List<String> mods) {
            metadata.mods = mods != null ? new ArrayList<>(mods) : new ArrayList<>();
            return this;
        }

        public Builder source(String source) {
            metadata.source = source != null ? source : "Community";
            return this;
        }

        public Builder createdNow() {
            metadata.createdDate = LocalDateTime.now().format(DATE_FORMAT);
            return this;
        }

        public ConfigMetadata build() {
            return metadata;
        }
    }

    // Getters only - make immutable after creation
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getCreatedDate() { return createdDate; }
    public String getTargetResolution() { return targetResolution; }
    public List<String> getMods() { return new ArrayList<>(mods); }  // Return copy for safety
    public String getSource() { return source; }

    // Convenience method for display
    public String getDisplayName() {
        return name + " v" + version;
    }

    // Validation helper
    public boolean isValid() {
        return name != null && !name.isBlank();
    }
}