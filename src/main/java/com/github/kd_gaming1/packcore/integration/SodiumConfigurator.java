package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.integration.PerformanceProfileService.PerformanceProfile;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.DeferMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class SodiumConfigurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean applyProfile(PerformanceProfile profile) {
        try {
            SodiumOptions options = SodiumClientMod.options();

            if (options.isReadOnly()) return false;

            applyCommonSettings(options);

            SodiumOptions.writeToDisk(options);

            LOGGER.info("Sodium: Applied {} profile", profile);
            return true;
        } catch (Exception e) {
            LOGGER.error("Sodium: Failed to apply profile", e);
            return false;
        }
    }

    private static void applyCommonSettings(SodiumOptions options) {
        options.advanced.useAdvancedStagingBuffers = true;
        options.advanced.cpuRenderAheadLimit = 3;

        options.performance.chunkBuilderThreads = 0;
        options.performance.chunkBuildDeferMode = DeferMode.ALWAYS;
        options.performance.animateOnlyVisibleTextures = true;
        options.performance.useEntityCulling = true;
        options.performance.useFogOcclusion = true;
        options.performance.useBlockFaceCulling = true;
    }
}