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
        return readMarkdownFile(fileName, getDefaultContent(fileName));
    }

    public static String readMarkdownFile(String fileName, String fallbackContent) {
        // Ensure directory exists
        ensureDirectoryExists();

        Path filePath = INFO_HELP_DIR.resolve(fileName);

        if (!Files.exists(filePath)) {
            LOGGER.warn("Could not find {}, using fallback content", fileName);
            return createMissingFileContent(fileName, fallbackContent);
        }

        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to read markdown file: {}", fileName, e);
            return createErrorContent(fileName, e.getMessage());
        }
    }

    private static void ensureDirectoryExists() {
        try {
            if (!Files.exists(INFO_HELP_DIR)) {
                Files.createDirectories(INFO_HELP_DIR);
                LOGGER.info("Created directory: {}", INFO_HELP_DIR);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create directory: {}", INFO_HELP_DIR, e);
        }
    }

    private static String createMissingFileContent(String fileName, String fallbackContent) {
        return """
                # Missing File: %s
                
                > **File not found** in the `packcore/info_help` folder.
                > 
                > Expected location: `%s`
                
                ## Default Content:
                
                %s
                
                ---
                
                > **To fix this:** Create the file `%s` in your game directory at `packcore/info_help/%s`
                """.formatted(fileName, INFO_HELP_DIR.resolve(fileName), fallbackContent, fileName, fileName);
    }

    private static String createErrorContent(String fileName, String errorMessage) {
        return """
                # Error Loading: %s
                
                > **Error occurred** while reading the file.
                
                **Error message:** %s
                
                ---
                
                > **To fix this:** Check that the file `packcore/info_help/%s` exists and is readable.
                """.formatted(fileName, errorMessage, fileName);
    }

    private static String getDefaultContent(String fileName) {
        return switch (fileName.toLowerCase()) {
            case "welcome.md" -> """
                    # Welcome
                    
                    Welcome to PackCore! This is the default welcome content.
                    """;
            case "resourcepacks.md" -> """
                    # Resource Packs
                    
                    Information about available resource packs will be shown here.
                    """;
            default -> """
                    # %s
                    
                    This is placeholder content for %s.
                    """.formatted(fileName.replace(".md", ""), fileName);
        };
    }

    /**
     * Safe method to get markdown content from any directory
     */
    public static String readMarkdownFileSafe(String directory, String fileName, String fallbackContent) {
        return PackCoreFileManager.getMarkdownContentSafe(directory, fileName, fallbackContent);
    }
}