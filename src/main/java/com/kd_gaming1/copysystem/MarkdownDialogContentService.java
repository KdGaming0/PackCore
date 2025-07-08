package com.kd_gaming1.copysystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for reading and processing markdown content for the configuration dialog.
 * Supports basic markdown formatting and converts it to HTML for display.
 */
public class MarkdownDialogContentService {
    private static final Logger LOGGER = LogManager.getLogger(MarkdownDialogContentService.class);

    private static final String DEFAULT_MARKDOWN_FILENAME = "dialog-content.md";
    private static final String FALLBACK_CONTENT = """
            # ModPack Configuration Manager
            
            Welcome! This dialog helps you manage your Minecraft mod configurations before the game starts.
            
            ## Configuration Types
            
            **📦 Official Configs**: Pre-made configurations that come with your modpack. These are tested setups created by the modpack authors to provide specific gameplay experiences.
            
            **🎨 Custom Configs**: Your personal configurations or ones shared by other players. These contain customized mod settings.
            
            ## What Happens When You:
            
            **Extract**: The selected configuration will overwrite your current mod settings with the chosen setup.
            
            **Skip**: Minecraft will start with whatever settings are currently in place (either default or previously applied configurations).
            
            ## Important Notes
            
            ⚠️ Extracting will **REPLACE** your current mod configurations - make a backup first if you want to keep them!
            
            ⏱️ After clicking Extract or Skip, please be patient! Minecraft may take 10-60 seconds to appear as the game loads in the background.
            
            ## Managing Configurations
            
            - Create your own backup: Use `/packcore archive` in-game to save your current settings
            - Share with friends: Custom config archives can be shared and imported by other players
            - Disable this dialog: Use `/packcore dialog false` if you don't want to see this anymore
            - Get help: Use `/packcore help` to see all available commands
            """;

    private final File skyblockFolder;

    public MarkdownDialogContentService(File skyblockFolder) {
        this.skyblockFolder = skyblockFolder;
    }

    /**
     * Reads the markdown content from the dialog-content.md file in the SkyBlock Enhanced folder.
     * Falls back to default content if file doesn't exist or can't be read.
     */
    public String getDialogContent() {
        File markdownFile = new File(skyblockFolder, DEFAULT_MARKDOWN_FILENAME);

        if (!markdownFile.exists()) {
            LOGGER.info("Dialog content file not found: {}. Using default content.", markdownFile.getAbsolutePath());
            return convertMarkdownToHtml(FALLBACK_CONTENT);
        }

        try {
            String content = Files.readString(markdownFile.toPath());
            LOGGER.info("Successfully loaded dialog content from: {}", markdownFile.getAbsolutePath());
            return convertMarkdownToHtml(content);
        } catch (IOException e) {
            LOGGER.error("Failed to read dialog content file: {}. Using default content.", markdownFile.getAbsolutePath(), e);
            return convertMarkdownToHtml(FALLBACK_CONTENT);
        }
    }

    /**
     * Checks if a custom markdown file exists
     */
    public boolean hasCustomContent() {
        File markdownFile = new File(skyblockFolder, DEFAULT_MARKDOWN_FILENAME);
        return markdownFile.exists();
    }

    /**
     * Extracts help links from the markdown content.
     * Looks for a special ## Links section or ## Help Links section.
     */
    public java.util.Map<String, String> getHelpLinks() {
        File markdownFile = new File(skyblockFolder, DEFAULT_MARKDOWN_FILENAME);
        java.util.Map<String, String> links = new java.util.LinkedHashMap<>();

        if (!markdownFile.exists()) {
            // Generic default links
            links.put("ModPack Documentation", "https://example.com/modpack-docs");
            links.put("Community Discord", "https://discord.gg/example");
            links.put("Report Issues", "https://github.com/example/modpack/issues");
            links.put("Configuration Guide", "https://example.com/config-guide");
            return links;
        }

        try {
            String content = Files.readString(markdownFile.toPath());

            // Look for links section
            String[] lines = content.split("\n");
            boolean inLinksSection = false;

            for (String line : lines) {
                line = line.trim();

                // Check if we're entering a links section
                if (line.toLowerCase().contains("## links") ||
                        line.toLowerCase().contains("## help links") ||
                        line.toLowerCase().contains("## useful links")) {
                    inLinksSection = true;
                    continue;
                }

                // Check if we're leaving the links section (another ## header)
                if (inLinksSection && line.startsWith("## ") &&
                        !line.toLowerCase().contains("links")) {
                    break;
                }

                // Parse links in the links section
                if (inLinksSection && line.startsWith("- ")) {
                    parseHelpLink(line.substring(2), links);
                }
            }

            // If no links section found, extract all links from the entire content
            if (links.isEmpty()) {
                for (String line : lines) {
                    if (line.trim().startsWith("- ")) {
                        parseHelpLink(line.trim().substring(2), links);
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.error("Failed to read markdown file for links", e);
        }

        // If still no links found, provide some defaults (only for custom files)
        if (links.isEmpty()) {
            links.put("Help & Support", "https://example.com/help");
            links.put("Community", "https://example.com/community");
        }

        return links;
    }

    private void parseHelpLink(String line, java.util.Map<String, String> links) {
        // Parse markdown links [text](url)
        java.util.regex.Pattern linkPattern = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
        java.util.regex.Matcher matcher = linkPattern.matcher(line);

        while (matcher.find()) {
            String text = matcher.group(1);
            String url = matcher.group(2);
            links.put(text, url);
        }
    }

    /**
     * Creates a sample markdown file for modpack creators to customize.
     */
    public boolean createSampleMarkdownFile() {
        File markdownFile = new File(skyblockFolder, DEFAULT_MARKDOWN_FILENAME);

        if (markdownFile.exists()) {
            LOGGER.info("Dialog content file already exists: {}", markdownFile.getAbsolutePath());
            return false;
        }

        String sampleContent = """
                # Your ModPack Name - Configuration Setup
                
                Welcome to **Your ModPack Name**! This dialog helps you choose the perfect configuration for your gameplay experience.
                
                ## 🎮 Configuration Types
                
                **📦 Official Configs**: These are carefully crafted configurations designed specifically for this modpack:
                - **Performance**: Optimized for lower-end systems and better performance
                - **Balanced**: Great mix of features and performance for most players
                - **Features**: All bells and whistles enabled for maximum experience
                - **Ultra**: Maximum quality settings for high-end systems
                
                **🎨 Custom Configs**: Community-created or your personal configurations that you've saved.
                
                ## ⚡ Quick Start Guide
                
                1. **First Time Players**: Choose the **Balanced** official config for the best experience
                2. **Performance Issues**: Select the **Performance** config to improve FPS
                3. **High-End PC**: Try the **Features** or **Ultra** config for maximum visual quality
                4. **Returning Players**: You can skip if you're happy with current settings
                
                ## 🔧 What Each Button Does
                
                **🚀 Extract**: Applies the selected configuration to your game (replaces current settings)
                
                **⏭️ Skip**: Continues with your existing settings (no changes made)
                
                **❓ Help**: Shows available commands and useful resources
                
                ## 🚨 Important Notes
                
                - Extracting a config will **overwrite** your current mod settings
                - Create a backup first using `/packcore archive` if you want to keep current settings
                - After selection, be patient - Minecraft takes time to load (10-60 seconds)
                - This dialog can be disabled with `/packcore dialog false`
                
                ## Help Links
                
                - [ModPack Discord](https://discord.gg/your-server)
                - [ModPack Wiki](https://wiki.yoursite.com)
                - [Report Issues](https://github.com/your-username/your-modpack/issues)
                - [Configuration Guide](https://wiki.yoursite.com/configuration)
                - [Troubleshooting](https://wiki.yoursite.com/troubleshooting)
                - [Video Tutorials](https://youtube.com/playlist/your-playlist)
                - [Community Configs](https://github.com/your-username/your-modpack/discussions)
                
                ## 🎯 Recommended Configurations
                
                - **New Players**: Start with **Balanced** configuration
                - **Performance Issues**: Use **Performance** configuration  
                - **Content Creators**: Try **Features** configuration for best visuals
                - **Competitive Players**: **Performance** config for maximum FPS
                """;

        try {
            // Ensure the directory exists
            if (!skyblockFolder.exists()) {
                skyblockFolder.mkdirs();
            }

            Files.writeString(markdownFile.toPath(), sampleContent);
            LOGGER.info("Created sample dialog content file: {}", markdownFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to create sample dialog content file: {}", markdownFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Converts basic markdown to HTML for display in JEditorPane.
     * Supports headers, bold, italic, lists, and links.
     */
    private String convertMarkdownToHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 12px; line-height: 1.6; margin: 10px; }");
        html.append("h1 { color: #2c3e50; font-size: 18px; margin-bottom: 10px; border-bottom: 2px solid #3498db; padding-bottom: 5px; }");
        html.append("h2 { color: #34495e; font-size: 15px; margin-top: 15px; margin-bottom: 8px; }");
        html.append("h3 { color: #34495e; font-size: 13px; margin-top: 12px; margin-bottom: 6px; }");
        html.append("p { margin-bottom: 8px; }");
        html.append("ul { margin-left: 20px; margin-bottom: 10px; }");
        html.append("li { margin-bottom: 4px; }");
        html.append("strong { color: #2c3e50; }");
        html.append("em { color: #7f8c8d; }");
        html.append("code { background-color: #ecf0f1; padding: 2px 4px; border-radius: 3px; font-family: 'Courier New', monospace; }");
        html.append("a { color: #3498db; text-decoration: none; }");
        html.append("a:hover { text-decoration: underline; }");
        html.append("hr { border: none; border-top: 1px solid #bdc3c7; margin: 15px 0; }");
        html.append("</style></head><body>");

        String[] lines = markdown.split("\n");
        boolean inList = false;

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                html.append("<br>");
                continue;
            }

            // Headers
            if (line.startsWith("### ")) {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<h3>").append(escapeHtml(line.substring(4))).append("</h3>");
            } else if (line.startsWith("## ")) {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<h2>").append(escapeHtml(line.substring(3))).append("</h2>");
            } else if (line.startsWith("# ")) {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<h1>").append(escapeHtml(line.substring(2))).append("</h1>");
            }
            // Horizontal rule
            else if (line.equals("---")) {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<hr>");
            }
            // List items
            else if (line.startsWith("- ")) {
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(processInlineMarkdown(line.substring(2))).append("</li>");
            }
            // Regular paragraphs
            else {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<p>").append(processInlineMarkdown(line)).append("</p>");
            }
        }

        if (inList) {
            html.append("</ul>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Processes inline markdown elements like bold, italic, code, and links.
     */
    private String processInlineMarkdown(String text) {
        // Bold text
        text = text.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");

        // Italic text
        text = text.replaceAll("\\*(.*?)\\*", "<em>$1</em>");

        // Inline code
        text = text.replaceAll("`(.*?)`", "<code>$1</code>");

        // Links [text](url)
        Pattern linkPattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
        Matcher linkMatcher = linkPattern.matcher(text);
        text = linkMatcher.replaceAll("<a href=\"$2\">$1</a>");

        return escapeHtml(text);
    }

    /**
     * Escapes HTML special characters, but preserves already processed HTML tags.
     */
    private String escapeHtml(String text) {
        // Only escape if it's not already processed HTML
        if (text.contains("<") && text.contains(">")) {
            return text; // Already contains HTML tags
        }

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public File getMarkdownFile() {
        return new File(skyblockFolder, DEFAULT_MARKDOWN_FILENAME);
    }
}