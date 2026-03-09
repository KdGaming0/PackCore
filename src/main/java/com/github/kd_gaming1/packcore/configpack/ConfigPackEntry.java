package com.github.kd_gaming1.packcore.configpack;

import com.google.gson.JsonObject;

import java.nio.file.Path;

/**
 * Holds the result of a successful scan for a single config pack zip.
 *
 * @param zipPath  Absolute path to the zip file on disk.
 * @param config   Parsed contents of pack.json found inside the zip.
 */
public record ConfigPackEntry(Path zipPath, JsonObject config) {

    /**
     * Builds a human-readable resolution string from this pack's config.
     * Appends GUI scale when the "guiScale" key is present.
     * <p>
     * Examples: "2560×1440", "1920×1080  ·  GUI Scale: 2", "Unknown resolution"
     */
    public String buildResolutionLine() {
        if (!config.has("targetWidth") || !config.has("targetHeight")) {
            return "Unknown resolution";
        }

        String res = config.get("targetWidth").getAsInt() + "×" + config.get("targetHeight").getAsInt();

        if (config.has("guiScale")) {
            res += "  ·  GUI Scale: " + config.get("guiScale").getAsString();
        }

        return res;
    }
}