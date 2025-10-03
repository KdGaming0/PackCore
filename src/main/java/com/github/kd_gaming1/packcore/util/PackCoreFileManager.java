package com.github.kd_gaming1.packcore.util;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class PackCoreFileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackCoreFileManager.class);
    private static final Path RUN_DIR = FabricLoader.getInstance().getGameDir();

    private static boolean hasInitialized = false;

    /**
     * Initialize all required directories and default files.
     * Should be called during pre-launch on first startup.
     */
    public static void initializeFileStructure() {
        if (hasInitialized) {
            LOGGER.info("File structure already initialized, skipping...");
            return;
        }

        LOGGER.info("Initializing PackCore file structure...");

        try {
            // Create all required directories
            createDirectories();

            // Create default markdown files for in-game menus
            createDefaultMarkdownFiles();

            hasInitialized = true;
            LOGGER.info("PackCore file structure initialization complete");

        } catch (Exception e) {
            LOGGER.error("Failed to initialize PackCore file structure", e);
        }
    }

    /**
     * Create all required directories
     */
    private static void createDirectories() throws IOException {
        Map<String, String> directories = Map.of(
                "packcore/modpack_config/official_configs", "Official modpack configurations",
                "packcore/modpack_config/custom_configs", "Custom modpack configurations",
                "packcore/info_help", "Information and help markdown files",
                "packcore/guides", "User guides and documentation"
        );

        for (Map.Entry<String, String> entry : directories.entrySet()) {
            Path dirPath = RUN_DIR.resolve(entry.getKey());
            Files.createDirectories(dirPath);
            LOGGER.info("Created directory: {} - {}", dirPath, entry.getValue());
        }
    }

    /**
     * Create default markdown files with instructions
     */
    private static void createDefaultMarkdownFiles() {
        // Guide files
        createMarkdownFile("packcore/guides", "Getting Started.md", getGettingStartedContent());
        createMarkdownFile("packcore/guides", "FAQ.md", getFAQContent());
        createMarkdownFile("packcore/guides", "Troubleshooting.md", getTroubleshootingContent());

        // Info help files
        createMarkdownFile("packcore/info_help", "Welcome.md", getWelcomeContent());
        createMarkdownFile("packcore/info_help", "Optimisation.md", getOptimisationContent());
        createMarkdownFile("packcore/info_help", "ResourcePacks.md", getResourcePacksContent());
        createMarkdownFile("packcore/info_help", "UsefulInformation.md", getUsefulInformationContent());
    }

    /**
     * Create markdown file with user instructions
     */
    private static void createMarkdownFile(String directory, String fileName, String content) {
        Path filePath = RUN_DIR.resolve(directory).resolve(fileName);

        if (Files.exists(filePath)) {
            LOGGER.debug("Markdown file already exists, skipping: {}", filePath);
            return;
        }

        try {
            String fullContent = content + "\n\n---\n\n" +
                    "> **📝 Edit this file:** Navigate to `" + directory + "/" + fileName + "` in your game directory to customize this content.\n" +
                    "> **🔄 Refresh:** Restart the game or reopen the menu to see your changes.";

            Files.writeString(filePath, fullContent, StandardOpenOption.CREATE_NEW);
            LOGGER.info("Created markdown file with instructions: {}", filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to create markdown file: {}", filePath, e);
        }
    }

    /**
     * Check if a markdown file exists and provide fallback content
     */
    public static String getMarkdownContentSafe(String directory, String fileName, String fallbackContent) {
        Path filePath = RUN_DIR.resolve(directory).resolve(fileName);

        if (!Files.exists(filePath)) {
            LOGGER.warn("Markdown file not found: {}, using fallback content", filePath);
            return fallbackContent + "\n\n> **File not found:** Expected at `" + directory + "/" + fileName + "`";
        }

        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to read markdown file: {}", filePath, e);
            return fallbackContent + "\n\n> **Error reading file:** " + e.getMessage();
        }
    }

    // Default content methods for in-game help system
    private static String getGettingStartedContent() {
        return """
                # Getting Started with PackCore
                
                Welcome to PackCore! This guide will help you get familiar with the modpack.
                
                ## First Steps
                
                1. **Apply a Configuration** - Use the config manager to apply optimized settings
                2. **Check Your Keybinds** - Press `ESC > Options > Controls` to see all mod keybinds
                3. **Explore the Interface** - Many mods add new UI elements and features
                
                ## Key Features
                
                - **Optimized Performance** - Pre-configured settings for smooth gameplay
                - **Enhanced UI** - Improved interfaces and helpful overlays
                - **Quality of Life** - Many small improvements to make the game more enjoyable
                
                ## Need Help?
                
                - Press `F1` in-game for contextual help
                - Check the other guides in this menu
                - Join our Discord community for live support
                """;
    }

    private static String getFAQContent() {
        return """
                # Frequently Asked Questions
                
                ## General Questions
                
                **Q: How do I reset my settings?**
                A: Delete the `packcore` folder in your game directory and restart.
                
                **Q: Can I add my own mods?**
                A: Yes, but be careful about compatibility. Check mod requirements first.
                
                **Q: Why is my performance poor?**
                A: Try applying a lower resolution configuration profile.
                
                ## Technical Issues
                
                **Q: The game crashes on startup**
                A: Check your Java version and allocated memory. See the Troubleshooting guide.
                
                **Q: Mods aren't working properly**
                A: Try pressing F3+T to reload resources, or restart the game.
                
                ## Getting More Help
                
                If your question isn't answered here, check the Troubleshooting guide or join our Discord.
                """;
    }

    private static String getTroubleshootingContent() {
        return """
                # Troubleshooting Guide
                
                ## Common Issues and Solutions
                
                ### Game Won't Start
                
                1. **Check Java Version** - Ensure you're using Java 21 or newer
                2. **Memory Allocation** - Allocate at least 4GB RAM to Minecraft
                3. **Mod Conflicts** - Remove recently added mods one by one
                
                ### Performance Issues
                
                1. **Lower Settings** - Reduce render distance and graphics quality
                2. **Update Drivers** - Ensure your graphics drivers are current
                3. **Close Other Programs** - Free up system resources
                
                ### Visual Glitches
                
                1. **Reload Resources** - Press F3+T in-game
                2. **Check Resource Packs** - Disable resource packs temporarily
                3. **Update Graphics Drivers** - Especially important for shader support
                
                ## Still Need Help?
                
                1. **Check Logs** - Look in `.minecraft/logs/latest.log` for error messages
                2. **Discord Support** - Join our community for live help
                3. **GitHub Issues** - Report bugs on our GitHub repository
                
                ## System Requirements
                
                - **Java:** 21 or newer
                - **RAM:** 6GB minimum, 8GB recommended
                - **Graphics:** OpenGL 3.2 support required
                """;
    }

    private static String getWelcomeContent() {
        return """
                # 🎮 Welcome to PackCore!
                
                Thank you for choosing **PackCore**! This modpack provides an optimized experience for your gameplay.
                
                ## 🚀 Key Features
                
                - **🔍 Automatic Configuration** - Smart config detection and application on first launch
                - **💡 Optimized Performance** - Pre-configured settings for smooth gameplay
                - **⚙️ Config Manager** - Import, export, and apply configurations in-game
                - **🎯 Resolution Profiles** - Optimized settings for different screen resolutions
                
                ## 📋 Getting Started
                
                1. **First Launch** - The mod automatically detects your screen resolution and applies the best config
                2. **In-Game Config Manager** - Access it from the main menu or ESC menu to manage configurations
                3. **Import/Export** - Share configurations with friends or create your own
                
                ---
                
                ## 💡 About Configurations
                
                Each configuration package contains:
                - **Optimized game settings** for your resolution
                - **Mod interface layouts** positioned for best visibility
                - **Performance tweaks** to ensure smooth gameplay
                - **Resource pack selections** that complement your setup
                
                > **Need help?** Check the other guides in this menu or join our Discord community!
                """;
    }

    private static String getOptimisationContent() {
        return """
                # ⚡ Optimisation Tips
                
                Get the most out of your modpack with these optimization tips!
                
                ## Video Settings
                
                ## Performance Mods
                
                The modpack includes several performance-enhancing mods. Make sure they're properly configured in their respective settings.
                
                ## Memory Allocation
                
                - **Recommended:** 6-8GB for optimal performance
                - **Minimum:** 4GB for basic gameplay
                - Configure in your launcher settings
                
                """;
    }

    private static String getResourcePacksContent() {
        return """
                # 🎨 Resource Pack Selection
                
                Choose the visual style that best fits your preferences!
                
                ## Available Packs:
                
                ### **Hypixel Plus**
                A clean, mostly vanilla pack designed for Hypixel modes like SkyBlock. Updates items and icons for better clarity without changing the overall Minecraft feel.
                
                ### **FurfSky Overlay** 
                A comprehensive resource pack for Hypixel SkyBlock, offering textures for nearly every item in the game with special styled retextures for items only.
                
                ### **FurfSky Full**
                A comprehensive resource pack for Hypixel SkyBlock with full retextures for both items and menus in a special artistic style.
                
                ### **SkyBlock Dark UI**
                A sleek, dark-themed resource pack for Hypixel SkyBlock, enhancing all GUI elements including mod interfaces with a modern aesthetic.
                
                ### **Defrosted**
                Icy-themed 16x pack for Minecraft 1.21.5 with a frosty blue aesthetic across items and menus, maintaining minimalist clarity.
                
                ### **Looshy**
                A smooth, vanilla-like 16x resource pack with clean updates and subtle charm that keeps Minecraft's original style while offering refined textures.
                
                ## 💡 Tips:
                
                - You can select multiple packs - they'll be applied in order
                - Resource packs can be changed later in the game settings
                - Some packs work better together than others
                """;
    }

    private static String getUsefulInformationContent() {
        return """
                # ℹ️ Useful Information
                
                ## Config Management
                
                - **Import Config:** Import configurations from zip files
                - **Export Config:** Create and share your own configurations
                - **Apply Config:** Switch between different configuration profiles
                
                ## Backup & Restore
                
                Backups are automatically created before applying new configurations.
                Find them in: `packcore/backups/`
                
                ## Community
                
                Join our Discord for support, updates, and to share your configurations!
                """;
    }

    /**
     * Force re-initialization (useful for development)
     */
    public static void forceReinitialize() {
        hasInitialized = false;
        initializeFileStructure();
    }

    /**
     * Check if initialization has been completed
     */
    public static boolean isInitialized() {
        return hasInitialized;
    }
}