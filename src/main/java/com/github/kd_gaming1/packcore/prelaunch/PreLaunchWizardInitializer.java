package com.github.kd_gaming1.packcore.prelaunch;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.config.apply.ConfigAutoApplier;
import com.github.kd_gaming1.packcore.config.apply.ConfigApplyService;
import com.github.kd_gaming1.packcore.util.io.file.FileLayoutInitializer;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class PreLaunchWizardInitializer implements PreLaunchEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreLaunchWizardInitializer.class);

    @Override
    public void onPreLaunch() {
        LOGGER.info("PackCore pre-launch initializer started");

        // Initialize MidnightConfig to load existing settings
        MidnightConfig.init("packcore", PackCoreConfig.class);

        Path runDir = FabricLoader.getInstance().getGameDir();

        // Always create necessary directories and files
        FileLayoutInitializer.initializeFileStructure();

        // CHECK FOR PENDING CONFIG APPLICATION FIRST
        boolean configApplied = ConfigApplyService.checkAndApplyPendingConfig(runDir);
        if (configApplied) {
            LOGGER.info("Applied pending config during pre-launch");
            PackCoreConfig.defaultConfigSuccessfullyApplied = true;
            PackCoreConfig.write(MOD_ID);
        }

        // Check if automatic config application is needed
        if (isUpgradeFromOldVersion(runDir)) {
            // If it's an upgrade, mark it as no longer first startup to prevent future auto-applies
            LOGGER.info("Upgrade from old version detected - marking as not first startup");
            PackCoreConfig.isFirstStartup = false;
            PackCoreConfig.write(MOD_ID);
        } else if (shouldApplyConfigAutomatically()) {
            LOGGER.info("First launch detected - applying config automatically...");
            boolean success = ConfigAutoApplier.applyBestMatchingConfig(runDir);

            if (success) {
                LOGGER.info("Config applied successfully on first launch");
                PackCoreConfig.defaultConfigSuccessfullyApplied = true;
                PackCoreConfig.isFirstStartup = false;
                PackCoreConfig.write(MOD_ID);
            } else {
                LOGGER.warn("Failed to apply config automatically - will use defaults");
            }
        }

        LOGGER.info("PackCore pre-launch initialization complete");
    }

    private boolean shouldApplyConfigAutomatically() {
        return PackCoreConfig.isFirstStartup &&
                !PackCoreConfig.defaultConfigSuccessfullyApplied;
    }

    /**
     * Checks if this is an upgrade from the old version by detecting the
     * "SkyBlock Enhanced" folder in the root game directory.
     *
     * @param gameDir The game directory
     * @return true if "SkyBlock Enhanced" folder exists (old install), false otherwise
     */
    private boolean isUpgradeFromOldVersion(Path gameDir) {
        Path oldFolder = gameDir.resolve("SkyBlock Enhanced");
        boolean exists = Files.exists(oldFolder) && Files.isDirectory(oldFolder);

        if (exists) {
            LOGGER.info("Detected 'SkyBlock Enhanced' folder - this is an upgrade from old version");
        }

        return exists;
    }
}