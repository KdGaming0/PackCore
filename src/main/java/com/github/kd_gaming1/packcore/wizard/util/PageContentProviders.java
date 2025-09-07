package com.github.kd_gaming1.packcore.wizard.util;

import java.util.Map;
import java.util.function.Supplier;

public class PageContentProviders {

    public static final Map<String, Supplier<String>> CONTENT_PROVIDERS = Map.of(
            "welcome_en_us.md", PageContentProviders::getWelcomeContent,
            "choose_config_en_us.md", PageContentProviders::getChooseConfig,
            "finished_en_us.md", PageContentProviders::getFinishedContent
    );

    public static String getWelcomeContent() {
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

    public static String getChooseConfig() {
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

    public static String getFinishedContent() {
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
}