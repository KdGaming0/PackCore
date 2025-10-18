package com.github.kd_gaming1.packcore.util.markdown;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MarkdownService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarkdownService.class);

    private final Path infoHelpDir;
    private final String assetsNamespace; // e.g. "packcore"

    public MarkdownService() {
        this(FabricLoader.getInstance().getGameDir().resolve("packcore/wizard_markdown_content"), "packcore");
    }

    public MarkdownService(Path infoHelpDir, String assetsNamespace) {
        this.infoHelpDir = infoHelpDir;
        this.assetsNamespace = assetsNamespace;
    }

    public MarkdownLoadResult load(String fileName) {
        final String sanitized = sanitize(fileName);
        if (sanitized == null) {
            return new MarkdownLoadResult.Error(fileName, "Invalid file name", null);
        }

        // 1) Try filesystem (user-editable)
        Path file = infoHelpDir.resolve(sanitized);
        if (Files.isRegularFile(file)) {
            try {
                return new MarkdownLoadResult.Success(Files.readString(file));
            } catch (IOException e) {
                LOGGER.error("Failed to read markdown file: {}", file, e);
                return new MarkdownLoadResult.Error(sanitized, "Failed to read file", e);
            }
        }

        // 2) Try classpath defaults under assets/<ns>/markdown/<file>
        String resourcePath = "/assets/" + assetsNamespace + "/markdown/" + sanitized;
        try (InputStream in = MarkdownService.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                byte[] bytes = in.readAllBytes();
                return new MarkdownLoadResult.Success(new String(bytes, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read classpath markdown: {}", resourcePath, e);
            return new MarkdownLoadResult.Error(sanitized, "Failed to read classpath resource", e);
        }

        // 3) Not found anywhere
        LOGGER.warn("Markdown not found: {} (fs: {}, classpath: {})", sanitized, file, resourcePath);
        return new MarkdownLoadResult.NotFound(sanitized);
    }

    // Convenience for callers that want a fallback string
    public String getOrDefault(String fileName, String fallback) {
        var result = load(fileName);
        return switch (result) {
            case MarkdownLoadResult.Success s -> s.content();
            case MarkdownLoadResult.NotFound __ -> fallback;
            case MarkdownLoadResult.Error e -> fallback;
        };
    }

    // Only allow simple "<name>.md" files; no separators or traversal
    private static String sanitize(String name) {
        if (name == null) return null;
        String n = name.trim();
        if (n.isEmpty() || n.contains("..") || n.contains("/") || n.contains("\\"))
            return null;
        if (!n.endsWith(".md")) n = n + ".md";
        return n;
    }
}