package com.github.kd_gaming1.packcore.gui.help.guide.util;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class GuideUtil {
    private static final String GUIDES_FOLDER = "packcore/guides";
    private static final ConcurrentHashMap<String, GuideInfo> GUIDE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> CONTENT_CACHE = new ConcurrentHashMap<>();

    /**
     * Gets the guides directory path
     */
    public static Path getGuidesDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve(GUIDES_FOLDER);
    }

    /**
     * Loads all available guides from the guides folder
     */
    public static List<GuideInfo> loadAvailableGuides() {
        List<GuideInfo> guides = new ArrayList<>();
        Path guidesDir = getGuidesDirectory();

        if (!Files.exists(guidesDir)) {
            PackCore.LOGGER.warn("Guides directory doesn't exist: {}", guidesDir);
            return guides;
        }

        try (Stream<Path> files = Files.list(guidesDir)) {
            files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".md"))
                    .forEach(path -> {
                        try {
                            GuideInfo guide = loadGuideInfo(path);
                            if (guide != null) {
                                guides.add(guide);
                                GUIDE_CACHE.put(path.getFileName().toString(), guide);
                            }
                        } catch (IOException e) {
                            PackCore.LOGGER.error("Failed to load guide: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            PackCore.LOGGER.error("Failed to read guides directory: {}", guidesDir, e);
        }

        return guides;
    }

    /**
     * Loads guide information (title and preview) from a markdown file
     */
    private static GuideInfo loadGuideInfo(Path filePath) throws IOException {
        List<String> lines = Files.readAllLines(filePath);

        if (lines.isEmpty()) {
            return null;
        }

        // Extract title (first line, remove markdown heading syntax)
        String title = lines.getFirst().replaceFirst("^#+\\s*", "").trim();

        if (title.isEmpty()) {
            title = filePath.getFileName().toString().replace(".md", "");
        }

        // Extract preview - build up to 3 lines of visible text
        StringBuilder previewBuilder = new StringBuilder();
        int currentLine = 0;
        int maxLines = 3;

        for (int i = 1; i < lines.size() && currentLine < maxLines; i++) {
            String line = lines.get(i).trim();

            // Skip empty lines and headers
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Remove markdown formatting for cleaner preview
            String cleanLine = line.replaceAll("^[>\\-*+]\\s*", "") // Remove blockquote and list markers
                    .replaceAll("\\*\\*(.*?)\\*\\*", "$1") // Remove bold
                    .replaceAll("\\*(.*?)\\*", "$1")       // Remove italic
                    .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1") // Remove links, keep text
                    .replaceAll("`([^`]+)`", "$1");        // Remove inline code

            if (!cleanLine.isEmpty()) {
                if (currentLine > 0) {
                    previewBuilder.append(" ");
                }
                previewBuilder.append(cleanLine);
                currentLine++;
            }
        }

        String preview = previewBuilder.toString();

        // Truncate if too long and add ellipsis
        if (preview.length() > 200) {
            int lastSpace = preview.lastIndexOf(' ', 197);
            if (lastSpace > 150) {
                preview = preview.substring(0, lastSpace) + "...";
            } else {
                preview = preview.substring(0, 197) + "...";
            }
        } else if (currentLine == maxLines && !preview.endsWith("...")) {
            // If we hit the line limit, add ellipsis even if under character limit
            preview += "...";
        }

        return new GuideInfo(title, preview, filePath);
    }

    /**
     * Loads the full content of a guide (with caching)
     */
    public static void loadGuideContent(GuideInfo guide) {
        String fileName = guide.getFilePath().getFileName().toString();

        // Check cache first
        String cachedContent = CONTENT_CACHE.get(fileName);
        if (cachedContent != null) {
            guide.setFullContent(cachedContent);
            return;
        }

        // Load from file
        try {
            String content = Files.readString(guide.getFilePath());
            CONTENT_CACHE.put(fileName, content);
            guide.setFullContent(content);
        } catch (IOException e) {
            PackCore.LOGGER.error("Failed to load guide content: {}", guide.getFilePath(), e);
        }
    }

    /**
     * Clears the guide cache (useful for development or manual refresh)
     */
    public static void clearCache() {
        GUIDE_CACHE.clear();
        CONTENT_CACHE.clear();
    }

    /**
     * Ensures the guides directory exists
     */
    public static void ensureGuidesDirectory() {
        Path guidesDir = getGuidesDirectory();
        try {
            Files.createDirectories(guidesDir);
        } catch (IOException e) {
            PackCore.LOGGER.error("Failed to create guides directory: {}", guidesDir, e);
        }
    }
}