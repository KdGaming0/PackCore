package com.github.kd_gaming1.packcore.util.config;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Integration class for Sodium-specific settings.
 * This class is only loaded when Sodium is present.
 */
public class SodiumIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean applyProfile(PerformanceProfileUtil.PerformanceProfile profile) {
        try {
            SodiumGameOptions options = SodiumClientMod.options();

            if (options.isReadOnly()) {
                LOGGER.warn("Sodium config is read-only, cannot apply profile");
                return false;
            }

            switch (profile) {
                case PERFORMANCE -> applyPerformanceSettings(options);
                case BALANCED -> applyBalancedSettings(options);
                case QUALITY -> applyQualitySettings(options);
                default -> {
                    LOGGER.warn("Unknown profile: {}", profile);
                    return false;
                }
            }

            SodiumGameOptions.writeToDisk(options);
            LOGGER.debug("Sodium profile '{}' applied successfully", profile.name());
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to save Sodium configuration", e);
            return false;
        } catch (Exception e) {
            LOGGER.error("Unexpected error applying Sodium profile", e);
            return false;
        }
    }

    public static boolean restoreDefaults() {
        try {
            SodiumClientMod.restoreDefaultOptions();
            LOGGER.debug("Sodium default settings restored");
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to restore Sodium defaults", e);
            return false;
        }
    }

    private static void applyPerformanceSettings(SodiumGameOptions options) {
        // Performance profile settings from sodium-options.json

        // Quality settings
        options.quality.weatherQuality = SodiumGameOptions.GraphicsQuality.FAST;
        options.quality.leavesQuality = SodiumGameOptions.GraphicsQuality.FAST;
        options.quality.enableVignette = true;

        // Advanced settings
        options.advanced.enableMemoryTracing = false;
        options.advanced.useAdvancedStagingBuffers = true;
        options.advanced.cpuRenderAheadLimit = 3;

        // Performance settings
        options.performance.chunkBuilderThreads = 0; // 0 = auto
        options.performance.alwaysDeferChunkUpdates = true;
        options.performance.animateOnlyVisibleTextures = true;
        options.performance.useEntityCulling = true;
        options.performance.useFogOcclusion = true;
        options.performance.useBlockFaceCulling = true;
        options.performance.useNoErrorGLContext = true;

        LOGGER.debug("Applied Sodium performance settings");
    }

    private static void applyBalancedSettings(SodiumGameOptions options) {
        // Balanced profile settings from sodium-options.json

        // Quality settings
        options.quality.weatherQuality = SodiumGameOptions.GraphicsQuality.DEFAULT;
        options.quality.leavesQuality = SodiumGameOptions.GraphicsQuality.DEFAULT;
        options.quality.enableVignette = true;

        // Advanced settings
        options.advanced.enableMemoryTracing = false;
        options.advanced.useAdvancedStagingBuffers = true;
        options.advanced.cpuRenderAheadLimit = 3;

        // Performance settings
        options.performance.chunkBuilderThreads = 0; // 0 = auto
        options.performance.alwaysDeferChunkUpdates = true;
        options.performance.animateOnlyVisibleTextures = true;
        options.performance.useEntityCulling = true;
        options.performance.useFogOcclusion = true;
        options.performance.useBlockFaceCulling = true;
        options.performance.useNoErrorGLContext = true;

        LOGGER.debug("Applied Sodium balanced settings");
    }

    private static void applyQualitySettings(SodiumGameOptions options) {
        // Quality profile settings from sodium-options.json

        // Quality settings
        options.quality.weatherQuality = SodiumGameOptions.GraphicsQuality.FANCY;
        options.quality.leavesQuality = SodiumGameOptions.GraphicsQuality.FANCY;
        options.quality.enableVignette = true;

        // Advanced settings
        options.advanced.enableMemoryTracing = false;
        options.advanced.useAdvancedStagingBuffers = true;
        options.advanced.cpuRenderAheadLimit = 3;

        // Performance settings
        options.performance.chunkBuilderThreads = 0; // 0 = auto
        options.performance.alwaysDeferChunkUpdates = true;
        options.performance.animateOnlyVisibleTextures = true;
        options.performance.useEntityCulling = true;
        options.performance.useFogOcclusion = true;
        options.performance.useBlockFaceCulling = true;
        options.performance.useNoErrorGLContext = true;

        LOGGER.debug("Applied Sodium quality settings");
    }
}