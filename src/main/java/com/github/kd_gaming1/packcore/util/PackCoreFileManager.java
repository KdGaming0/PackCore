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

            // Create default markdown files
            createDefaultMarkdownFiles();

            // Create language files
            createLanguageFiles();

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
                "packcore/lang", "Language files for wizard content",
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
     * Create language files from existing system
     */
    private static void createLanguageFiles() {
        Map<String, String> langFiles = Map.of(
                "welcome_en_us.md", getWizardWelcomeContent(),
                "choose_config_en_us.md", getWizardChooseConfigContent(),
                "finished_en_us.md", getWizardFinishedContent()
        );

        for (Map.Entry<String, String> entry : langFiles.entrySet()) {
            createFile("packcore/lang", entry.getKey(), entry.getValue());
        }
    }

    /**
     * Generic file creation method
     */
    private static void createFile(String directory, String fileName, String content) {
        Path filePath = RUN_DIR.resolve(directory).resolve(fileName);

        if (Files.exists(filePath)) {
            LOGGER.debug("File already exists, skipping: {}", filePath);
            return;
        }

        try {
            Files.writeString(filePath, content, StandardOpenOption.CREATE_NEW);
            LOGGER.info("Created file: {}", filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to create file: {}", filePath, e);
        }
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

    // Default content methods
    private static String getGettingStartedContent() {
        return """
                # Getting Started with PackCore
                
                Welcome to PackCore! This guide will help you get familiar with the modpack.
                
                ## First Steps
                
                1. **Complete the Setup Wizard** - If you haven't already, run through the initial setup
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
                A: Try running the setup wizard again and choosing a lower resolution profile.
                
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
                # 🎮 Welcome to Your Modpack Setup!
                
                Thank you for choosing **PackCore**! This wizard will help you configure your modpack for the best possible experience.
                
                ## 🚀 What This Wizard Does:
                
                - **🔍 Detects Your Setup** - Automatically identifies your screen resolution and system capabilities
                - **💡 Smart Recommendations** - Suggests the optimal configuration for your hardware
                - **⚙️ Easy Installation** - Applies your chosen settings with just a few clicks
                - **🎯 Optimized Experience** - Ensures smooth gameplay tailored to your system
                
                ## 📋 What You'll Choose:
                
                1. **Configuration Package** - Pre-made settings optimized for different screen resolutions
                2. **Review & Apply** - Confirm your choices and let the wizard do the work
                3. **Ready to Play!** - Launch Minecraft with your perfectly configured modpack
                
                ---
                
                ## 💡 About Configurations
                
                Each configuration package contains:
                - **Optimized game settings** for your resolution
                - **Mod interface layouts** positioned for best visibility
                - **Performance tweaks** to ensure smooth gameplay
                - **Resource pack selections** that complement your setup
                
                > **First time with mods?** Don't worry! The wizard will explain everything as we go, and you'll get an in-game tutorial when you first launch Minecraft.
                
                ---
                
                **Ready to get started?** Click **Next** to begin!
                """;
    }

    private static String getOptimisationContent() {
        return """
            # ⚡ Optimisation

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
                
                """;
    }

    // Wizard content (from existing system)
    private static String getWizardWelcomeContent() {
        return """
                # 🎮 Welcome to Your Modpack Setup!
                
                Thank you for choosing **PackCore**! This wizard will help you configure your modpack for the best possible experience.
                
                ## 🚀 What This Wizard Does:
                
                - **🔍 Detects Your Setup** - Automatically identifies your screen resolution and system capabilities
                - **💡 Smart Recommendations** - Suggests the optimal configuration for your hardware
                - **⚙️ Easy Installation** - Applies your chosen settings with just a few clicks
                - **🎯 Optimized Experience** - Ensures smooth gameplay tailored to your system
                
                ## 📋 What You'll Choose:
                
                1. **Configuration Package** - Pre-made settings optimized for different screen resolutions
                2. **Review & Apply** - Confirm your choices and let the wizard do the work
                3. **Ready to Play!** - Launch Minecraft with your perfectly configured modpack
                
                ---
                
                ## 💡 About Configurations
                
                Each configuration package contains:
                - **Optimized game settings** for your resolution
                - **Mod interface layouts** positioned for best visibility
                - **Performance tweaks** to ensure smooth gameplay
                - **Resource pack selections** that complement your setup
                
                > **First time with mods?** Don't worry! The wizard will explain everything as we go, and you'll get an in-game tutorial when you first launch Minecraft.
                
                ---
                
                **Ready to get started?** Click **Next** to begin!
                """;
    }

    private static String getWizardChooseConfigContent() {
        return """
                # 🎯 Choose Your Configuration
                
                Select the configuration that best matches your setup from the list on the right.
                
                ## 🖥️ About Configuration Types:
                
                - **4K Configurations** - For ultra-high resolution displays (3840x2160+)
                - **1440p Configurations** - Perfect for QHD displays (2560x1440)  
                - **1080p Configurations** - Optimized for Full HD displays (1920x1080)
                - **720p Configurations** - Best for HD displays and performance-focused setups
                
                ## 💡 How We Choose:
                
                The wizard automatically detects your screen resolution and recommends the best configuration. You can see your detected resolution at the top of this page.
                
                ## ⚙️ What's Included:
                
                Each configuration contains pre-configured:
                - Video settings optimized for your resolution
                - Mod interface positions and scaling
                - Performance tweaks and optimizations
                - Resource pack selections
                
                > **Don't see your exact resolution?** Choose the closest match - you can fine-tune individual settings in-game later!
                """;
    }

    private static String getWizardFinishedContent() {
        return """
                ## 🚀 Launch Minecraft
                Your modpack is now ready to play!
                
                • Open your Minecraft launcher
                • Select your modpack profile
                • Click 'Play' to start your adventure
                • Your optimized settings are already applied
                
                ## 🎯 In-Game Tutorial
                Learn about your new mods and features!
                
                • Tutorial starts automatically on first launch
                • Learn mod controls and keybinds
                • Discover new gameplay mechanics
                • Get tips for optimal performance
                
                ## ⚙️ Customize Further
                Fine-tune your experience as needed!
                
                • Most mod interfaces can be moved and resized
                • Adjust video settings in Options menu
                • Check Controls for new mod keybinds
                • All changes are saved automatically
                
                ## 💡 Getting Help
                Support resources are available!
                
                • Press F1 in-game for mod help
                • Check modpack documentation
                • Visit community forums and Discord
                • Use in-game tooltips for guidance
                
                ## 🔧 Troubleshooting
                Common solutions for issues!
                
                • Press F3 + T to reload textures
                • Restart Minecraft if mods act strange
                • Check Java memory allocation
                • Rerun this wizard by deleting config folder
                
                ## 📖 Advanced Tips
                Get the most out of your modpack!
                
                • Explore mod configuration files
                • Try different resource packs
                • Join multiplayer servers with same mods
                • Share your configurations with friends
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