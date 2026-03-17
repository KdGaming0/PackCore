package com.github.kd_gaming1.packcore.configpack;

import java.time.Instant;
import java.util.List;

/**
 * Metadata written into pack.json inside a config pack zip.
 * Use {@link Builder} to construct instances.
 */
public final class ConfigPackMeta {

    // Required
    private final String version;
    private final int targetWidth;
    private final int targetHeight;
    private final int guiScale;

    // Optional
    private final String name;
    private final String description;
    private final String author;
    private final List<String> mods;

    // Auto-generated — caller never sets this
    private final String createdDate;

    private ConfigPackMeta(Builder builder) {
        this.version = builder.version;
        this.targetWidth = builder.targetWidth;
        this.targetHeight = builder.targetHeight;
        this.guiScale = builder.guiScale;
        this.name = builder.name;
        this.description = builder.description;
        this.author = builder.author;
        this.mods = builder.mods != null ? List.copyOf(builder.mods) : List.of();
        this.createdDate = Instant.now().toString();
    }

    public String version() { return version; }
    public int targetWidth() { return targetWidth; }
    public int targetHeight() { return targetHeight; }
    public int guiScale() { return guiScale; }
    public String name() { return name; }
    public String description() { return description; }
    public String author() { return author; }
    public List<String> mods() { return mods; }
    public String createdDate() { return createdDate; }

    public static Builder builder(String version, int targetWidth, int targetHeight, int guiScale) {
        return new Builder(version, targetWidth, targetHeight, guiScale);
    }

    public static final class Builder {

        // Required
        private final String version;
        private final int targetWidth;
        private final int targetHeight;
        private final int guiScale;

        // Optional
        private String name;
        private String description;
        private String author;
        private List<String> mods = List.of();

        private Builder(String version, int targetWidth, int targetHeight, int guiScale) {
            this.version = version;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            this.guiScale = guiScale;
        }

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder author(String author) { this.author = author; return this; }
        public Builder mods(List<String> mods) {
            this.mods = mods != null ? mods : List.of();
            return this;
        }

        public ConfigPackMeta build() {
            return new ConfigPackMeta(this);
        }
    }
}