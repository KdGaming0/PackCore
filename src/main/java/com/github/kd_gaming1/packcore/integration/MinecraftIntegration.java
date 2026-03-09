package com.github.kd_gaming1.packcore.integration;

import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.client.CloudStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class MinecraftIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean applyProfile(PerformanceProfileService.PerformanceProfile profile) {
        try {
            Options options = getOptions();
            if (options == null) return false;

            applyCommonSettings(options);

            switch (profile) {
                case PERFORMANCE -> {
                    options.graphicsPreset().set(GraphicsPreset.FAST);
                    options.particles().set(ParticleStatus.DECREASED);
                    options.cloudStatus().set(CloudStatus.FAST);
                    options.renderDistance().set(10);
                    options.entityShadows().set(false);
                }
                case BALANCED, SHADERS_PERFORMANCE -> {
                    options.graphicsPreset().set(GraphicsPreset.FANCY);
                    options.particles().set(ParticleStatus.ALL);
                    options.cloudStatus().set(CloudStatus.FANCY);
                    options.renderDistance().set(16);
                    options.entityShadows().set(true);
                }
                case QUALITY, SHADERS_QUALITY -> {
                    options.graphicsPreset().set(GraphicsPreset.FABULOUS);
                    options.particles().set(ParticleStatus.ALL);
                    options.cloudStatus().set(CloudStatus.FANCY);
                    options.renderDistance().set(20);
                    options.entityShadows().set(true);
                    options.entityDistanceScaling().set(1.25);
                }
            }

            options.save();
            return true;
        } catch (Exception e) {
            LOGGER.error("Vanilla: Failed to apply profile", e);
            return false;
        }
    }

    private static void applyCommonSettings(Options options) {
        options.ambientOcclusion().set(true);
        options.biomeBlendRadius().set(2);
        options.enableVsync().set(false);
        options.framerateLimit().set(260);
        options.mipmapLevels().set(4);
        options.simulationDistance().set(12);
        options.entityDistanceScaling().set(1.0);
    }

    private static Options getOptions() {
        return Minecraft.getInstance().options;
    }
}