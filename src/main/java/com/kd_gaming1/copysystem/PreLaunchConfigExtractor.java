package com.kd_gaming1.copysystem;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.FabricLoader;
import com.kd_gaming1.config.PackCoreConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Pre-launch entrypoint that handles configuration extraction before Minecraft starts.
 * This ensures all necessary configs are in place before the game initializes.
 */
public class PreLaunchConfigExtractor implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore-PreLaunch");

    @Override
    public void onPreLaunch() {
        LOGGER.info("Starting PackCore pre-launch configuration extraction...");

        try {
            // Initialize MidnightLib config early - this works in pre-launch!
            MidnightConfig.init("packcore", PackCoreConfig.class);

            File minecraftRoot = FabricLoader.getInstance().getGameDir().toFile();
            ConfigExtractionService extractionService = new ConfigExtractionService(minecraftRoot);

            // Check if we need to show the dialog using MidnightLib
            if (!PackCoreConfig.promptSetDefaultConfig) {
                LOGGER.info("Config dialog disabled, skipping user prompt");
                return;
            }

            ConfigSelectionResult result = extractionService.selectAndExtractConfig();

            if (result.shouldShowDialog()) {
                LOGGER.info("Multiple configs found, showing selection dialog");
                showConfigSelectionDialog(extractionService);
            } else if (result.hasAutoExtractConfig()) {
                LOGGER.info("Auto-extracting single config: {}", result.getConfigName());
                boolean success = extractionService.extractConfig(result.getConfigName(), result.getConfigType());

                if (success) {
                    // Disable the prompt for next time using MidnightLib
                    LOGGER.info("Auto-extraction successful, disabling config prompt for next launch");
                    PackCoreConfig.promptSetDefaultConfig = false;
                    PackCoreConfig.lastConfigApplied = result.getConfigName(); // Store last applied config
                    MidnightConfig.write("packcore"); // Save using MidnightLib

                    try {
                        // Short delay to ensure file write completes
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                LOGGER.info("No configs found, using default settings");
            }

        } catch (Exception e) {
            LOGGER.error("Error during pre-launch config extraction", e);
            // Continue startup even if config extraction fails
        }

        LOGGER.info("Pre-launch configuration extraction completed");
    }

    private void showConfigSelectionDialog(ConfigExtractionService extractionService) {
        try {
            // Use CompletableFuture to handle the dialog asynchronously but block the main thread
            CompletableFuture<Boolean> dialogResult = CompletableFuture.supplyAsync(() -> {
                ConfigSelectionDialog dialog = new ConfigSelectionDialog(extractionService);
                return dialog.showAndWait();
            });

            // Block until dialog completes or times out
            Boolean success = dialogResult.get(PackCoreConfig.dialogTimeoutMinutes, TimeUnit.MINUTES);

            if (success) {
                LOGGER.info("Config selection completed successfully");
            } else {
                LOGGER.warn("Config selection was cancelled or failed");
            }

        } catch (Exception e) {
            LOGGER.error("Error showing config selection dialog", e);
        }
    }
}