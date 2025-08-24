package com.github.kd_gaming1.packcore.wizard.util;

import java.util.Map;
import java.util.function.Supplier;

public class PageContentProviders {

    public static final Map<String, Supplier<String>> CONTENT_PROVIDERS = Map.of(
            "welcome_en_us.md", PageContentProviders::getWelcomeContent,
            "choose_config_en_us.md", PageContentProviders::getChooseConfig,
            "finished_en_us.md", PageContentProviders::getFinishedContent
    );

    public static final Map<String, Supplier<String>> CONFIG_DESCRIPTION_PROVIDERS = Map.of(
            "1080p_balanced.md", PageContentProviders::get1080pBalancedDescription,
            "1440p_quality.md", PageContentProviders::get1440pQualityDescription,
            "4k_ultra.md", PageContentProviders::get4kUltraDescription
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

    public static String get1080pBalancedDescription() {
        return """
                # 1080p Balanced Configuration

                Perfectly balanced configuration designed for Full HD displays and standard gaming setups.

                ## 🎯 Target Audience
                - **Display:** Full HD (1920x1080) monitors
                - **System:** Mid-range gaming PCs and laptops
                - **Focus:** Best balance of performance and visual quality

                ## ⚙️ Technical Specifications
                - **Render Distance:** 12 chunks (optimal for most systems)
                - **Graphics Settings:** Medium-High (balanced approach)
                - **UI Scaling:** 100% (standard scaling)
                - **Memory Usage:** Moderate (4-6GB recommended)

                ## 📊 Performance Expectations
                - **Target FPS:** 60+ on recommended hardware
                - **Compatibility:** Works well on most modern systems
                - **Resource Usage:** Balanced CPU and GPU utilization

                ## 🎮 What's Included
                - Optimized video settings for 1080p displays
                - Carefully positioned mod interfaces
                - Performance-friendly resource pack selections
                - Balanced render settings for smooth gameplay

                > **Perfect for:** Most Minecraft players with standard gaming setups
                """;
    }

    public static String get1440pQualityDescription() {
        return """
                # 1440p Quality Configuration

                High-quality configuration designed for QHD displays and enthusiast gaming systems.

                ## 🎯 Target Audience
                - **Display:** QHD (2560x1440) monitors
                - **System:** Mid-to-high-end gaming PCs
                - **Focus:** Enhanced visual quality with good performance

                ## ⚙️ Technical Specifications
                - **Render Distance:** 16 chunks (enhanced view distance)
                - **Graphics Settings:** High with selective Ultra options
                - **UI Scaling:** 125% (improved readability on QHD)
                - **Memory Usage:** Higher (6-8GB recommended)

                ## 📊 Performance Expectations
                - **Target FPS:** 45-60+ on recommended hardware
                - **Compatibility:** Requires dedicated graphics card
                - **Resource Usage:** Balanced with slight GPU preference

                ## 🎮 What's Included
                - Enhanced texture quality and visual effects
                - Larger, more readable UI elements
                - High-quality resource pack integration
                - Optimized for 1440p screen real estate

                ## 💡 Recommendations
                - **GPU:** GTX 1660 / RX 580 or better recommended
                - **RAM:** 8GB+ system memory
                - **Storage:** SSD recommended for faster loading

                > **Perfect for:** Gamers with QHD displays who want enhanced visuals
                """;
    }

    public static String get4kUltraDescription() {
        return """
                # 4K Ultra Configuration

                Maximum quality configuration designed for 4K displays and high-end gaming systems.

                ## 🎯 Target Audience
                - **Display:** 4K (3840x2160) and higher resolutions
                - **System:** High-end gaming PCs with powerful GPUs
                - **Focus:** Maximum visual fidelity and immersion

                ## ⚙️ Technical Specifications
                - **Render Distance:** 20+ chunks (maximum view distance)
                - **Graphics Settings:** Ultra across all categories
                - **UI Scaling:** 150-200% (4K clarity optimization)
                - **Memory Usage:** High (8-12GB recommended)

                ## 📊 Performance Expectations
                - **Target FPS:** 30-60 depending on hardware
                - **Compatibility:** Requires high-end dedicated graphics
                - **Resource Usage:** GPU-intensive configuration

                ## 🎮 What's Included
                - Ultra-high resolution textures and effects
                - Maximum quality shaders and lighting
                - Large, crystal-clear UI elements for 4K
                - Premium resource pack selections
                - Advanced visual enhancement mods

                ## ⚠️ System Requirements
                - **GPU:** RTX 3070 / RX 6700 XT or better strongly recommended
                - **RAM:** 16GB+ system memory
                - **Storage:** NVMe SSD highly recommended
                - **CPU:** Modern 6+ core processor

                ## 🎨 Visual Enhancements
                - Ray tracing support (if available)
                - Advanced particle effects
                - High-resolution shadow mapping
                - Enhanced water and glass reflections

                > **Perfect for:** Enthusiasts with 4K displays and powerful gaming rigs
                """;
    }
}