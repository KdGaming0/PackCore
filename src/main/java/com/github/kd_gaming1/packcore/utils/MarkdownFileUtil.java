package com.github.kd_gaming1.packcore.utils;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class MarkdownFileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Path runDir = FabricLoader.getInstance().getGameDir();
    private static final Path INFO_HELP_DIR = runDir.resolve("packcore/info_help");

    public static String readMarkdownFile(String fileName) {
        Path filePath = INFO_HELP_DIR.resolve(fileName);
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to read markdown file: {}", fileName, e);
            return "Error loading content. " + e.getMessage();
        }
    }
}