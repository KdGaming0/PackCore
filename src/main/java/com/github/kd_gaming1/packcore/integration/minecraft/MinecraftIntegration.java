package com.github.kd_gaming1.packcore.integration.minecraft;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.particle.ParticlesMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Integration class for vanilla Minecraft settings.
 * Handles GameOptions modifications for performance profiles.
 */
public class MinecraftIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean applyProfile(PerformanceProfileService.PerformanceProfile profile) {
        try {
            GameOptions options = getValidatedOptions();
            if (options == null) return false;

            switch (profile) {
                case PERFORMANCE -> applyPerformanceSettings(options);
                case BALANCED -> applyBalancedSettings(options);
                case QUALITY, SHADERS -> applyQualitySettings(options);
                default -> {
                    LOGGER.warn("Unknown profile: {}", profile);
                    return false;
                }
            }

            options.write();
            LOGGER.debug("Minecraft profile '{}' applied successfully", profile.name());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to apply Minecraft profile", e);
            return false;
        }
    }

    public static boolean restoreDefaults() {
        try {
            GameOptions options = getValidatedOptions();
            if (options == null) return false;

            // Reset to default values using getter methods
            options.getGraphicsMode().setValue(GraphicsMode.FANCY);
            options.getViewDistance().setValue(12);
            options.getSimulationDistance().setValue(12);
            options.getEntityDistanceScaling().setValue(1.0);
            options.getMaxFps().setValue(120);
            options.getEnableVsync().setValue(false);
            options.getBiomeBlendRadius().setValue(2);
            options.getEntityShadows().setValue(true);
            options.getCloudRenderMode().setValue(CloudRenderMode.FANCY);
            options.getAo().setValue(true); // Ambient occlusion

            options.write();
            LOGGER.debug("Minecraft default settings restored");
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to restore Minecraft defaults", e);
            return false;
        }
    }

    private static GameOptions getValidatedOptions() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            LOGGER.warn("MinecraftClient is null, cannot apply profile");
            return null;
        }

        GameOptions options = client.options;
        if (options == null) {
            LOGGER.warn("GameOptions is null, cannot apply profile");
            return null;
        }

        return options;
    }

    private static void applyPerformanceSettings(GameOptions options) {
        // Performance profile settings from options.txt
        options.getAo().setValue(true);
        options.getBiomeBlendRadius().setValue(2);
        options.getEnableVsync().setValue(false);
        options.getEntityDistanceScaling().setValue(1.0);
        options.getEntityShadows().setValue(false);
        options.getGraphicsMode().setValue(GraphicsMode.FAST); // graphicsMode:0 = FAST
        options.getMaxFps().setValue(260);
        options.getMipmapLevels().setValue(2);
        options.getParticles().setValue(ParticlesMode.MINIMAL); // particles:2 = MINIMAL
        options.getCloudRenderMode().setValue(CloudRenderMode.FAST); // renderClouds:"fast"
        options.getViewDistance().setValue(10); // renderDistance:10
        options.getSimulationDistance().setValue(10);

        LOGGER.debug("Applied Minecraft performance settings");
    }

    private static void applyBalancedSettings(GameOptions options) {
        // Balanced profile settings from options.txt
        options.getAo().setValue(true);
        options.getBiomeBlendRadius().setValue(2);
        options.getEnableVsync().setValue(false);
        options.getEntityDistanceScaling().setValue(1.0);
        options.getEntityShadows().setValue(true);
        options.getGraphicsMode().setValue(GraphicsMode.FANCY); // graphicsMode:1 = FANCY
        options.getMaxFps().setValue(260);
        options.getMipmapLevels().setValue(4);
        options.getParticles().setValue(ParticlesMode.ALL); // particles:0 = ALL
        options.getCloudRenderMode().setValue(CloudRenderMode.FANCY); // renderClouds:"true"
        options.getViewDistance().setValue(14); // renderDistance:14
        options.getSimulationDistance().setValue(12);

        LOGGER.debug("Applied Minecraft balanced settings");
    }

    private static void applyQualitySettings(GameOptions options) {
        // Quality profile settings from options.txt
        options.getAo().setValue(true);
        options.getBiomeBlendRadius().setValue(2);
        options.getEnableVsync().setValue(false);
        options.getEntityDistanceScaling().setValue(1.25);
        options.getEntityShadows().setValue(true);
        options.getGraphicsMode().setValue(GraphicsMode.FABULOUS); // graphicsMode:2 = FABULOUS
        options.getMaxFps().setValue(260);
        options.getMipmapLevels().setValue(4);
        options.getParticles().setValue(ParticlesMode.ALL); // particles:0 = ALL
        options.getCloudRenderMode().setValue(CloudRenderMode.FANCY); // renderClouds:"true"
        //? if >=1.21.8 {
        
        options.getViewDistance().setValue(20); // renderDistance:20
         
        //?} else {
        /*options.getViewDistance().setValue(16); // renderDistance:16
        *///?}
        options.getSimulationDistance().setValue(12);

        LOGGER.debug("Applied Minecraft quality settings");
    }
}