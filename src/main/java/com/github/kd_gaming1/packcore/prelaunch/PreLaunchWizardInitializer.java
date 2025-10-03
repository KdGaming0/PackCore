package com.github.kd_gaming1.packcore.prelaunch;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.util.AutoConfigApplicator;
import com.github.kd_gaming1.packcore.util.ConfigApplicationManager;
import com.github.kd_gaming1.packcore.util.PackCoreFileManager;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class PreLaunchWizardInitializer implements PreLaunchEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreLaunchWizardInitializer.class);

    @Override
    public void onPreLaunch() {
        LOGGER.info("PackCore pre-launch initializer started");

        // Initialize MidnightConfig to load existing settings
        MidnightConfig.init("packcore", PackCoreConfig.class);

        Path runDir = FabricLoader.getInstance().getGameDir();

        // Always create necessary directories and files
        PackCoreFileManager.initializeFileStructure();

        // CHECK FOR PENDING CONFIG APPLICATION FIRST
        boolean configApplied = ConfigApplicationManager.checkAndApplyPendingConfig(runDir);
        if (configApplied) {
            LOGGER.info("Applied pending config during pre-launch");
            PackCoreConfig.defaultConfigSuccessfullyApplied = true;
            MidnightConfig.write("packcore");
        }

        // Check if automatic config application is needed
        if (shouldApplyConfigAutomatically()) {
            LOGGER.info("First launch detected - applying config automatically...");
            boolean success = AutoConfigApplicator.applyBestMatchingConfig(runDir);

            if (success) {
                LOGGER.info("Config applied successfully on first launch");
                PackCoreConfig.defaultConfigSuccessfullyApplied = true;
                PackCoreConfig.isFirstStartup = false;
                MidnightConfig.write("packcore");
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
}