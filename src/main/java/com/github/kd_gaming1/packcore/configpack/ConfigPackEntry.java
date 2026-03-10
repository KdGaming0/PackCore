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
}