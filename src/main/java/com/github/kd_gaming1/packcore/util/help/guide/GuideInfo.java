package com.github.kd_gaming1.packcore.util.help.guide;

import java.nio.file.Path;

public class GuideInfo {
    private final String title;
    private final String preview;
    private final Path filePath;
    private String fullContent;

    public GuideInfo(String title, String preview, Path filePath) {
        this.title = title;
        this.preview = preview;
        this.filePath = filePath;
        this.fullContent = null; // Lazy loaded
    }

    public String getTitle() {
        return title;
    }

    public String getPreview() {
        return preview;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getFullContent() {
        return fullContent;
    }

    public void setFullContent(String fullContent) {
        this.fullContent = fullContent;
    }

    public boolean isContentLoaded() {
        return fullContent != null;
    }
}