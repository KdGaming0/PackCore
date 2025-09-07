package com.github.kd_gaming1.packcore.util;

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
        try {
            if (!Files.exists(INFO_HELP_DIR)) {
                Files.createDirectories(INFO_HELP_DIR);
                LOGGER.info("Created directory: {}", INFO_HELP_DIR);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create directory: {}", INFO_HELP_DIR, e);
            return "> **Error:** Could not create info directory.\n>\n> " + e.getMessage();
        }

        Path filePath = INFO_HELP_DIR.resolve(fileName);

        if (!Files.exists(filePath)) {
            String message = "# Missing File\n" +
                    "> **Could not find** `" + fileName + "` in the `info_help` folder.\n" +
                    "> Please create it to display help content.";
            LOGGER.warn("Could not find {}, please make it in the info_help folder", fileName);
            return message;
        }

        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to read markdown file: {}", fileName, e);
            return "> **Error loading content:**\n>\n> " + e.getMessage();
        }
    }

}