package com.github.kdgaming0.packcore.mixin;

import com.github.kdgaming0.packcore.config.ModConfig;
import com.github.kdgaming0.packcore.copysystem.ConfigExtractionService;
import com.github.kdgaming0.packcore.copysystem.ConfigSelectionResult;
import com.github.kdgaming0.packcore.copysystem.ConfigSelectionDialog;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mixin(Minecraft.class)
public class PackCoreShowConfigMixin {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("PackCore-PreLaunch");
    @Unique
    private static final int DIALOG_TIMEOUT_MINUTES = 10;

    @Inject(method = "startGame", at = @At("HEAD"))
    private void onGameStart(CallbackInfo ci) {
        LOGGER.info("Starting PackCore pre-launch configuration extraction...");

        try {
            // Load configuration first
            ModConfig.loadConfig();

            // Get Minecraft instance and game directory
            Minecraft mc = (Minecraft)(Object)this;
            File minecraftRoot = mc.mcDataDir;
            ConfigExtractionService extractionService = new ConfigExtractionService(minecraftRoot);

            // Check if we need to show the dialog
            if (!ModConfig.getPromptSetDefaultConfig()) {
                LOGGER.info("Config dialog disabled, skipping user prompt");
                return;
            }

            ConfigSelectionResult result = extractionService.selectAndExtractConfig();

            if (result.shouldShowDialog()) {
                LOGGER.info("Multiple configs found, showing selection dialog");
                packCore$showConfigSelectionDialog(extractionService);
            } else if (result.hasAutoExtractConfig()) {
                LOGGER.info("Auto-extracting single config: {}", result.getConfigName());
                boolean success = extractionService.extractConfig(result.getConfigName(), result.getConfigType());

                if (success) {
                    // Disable the prompt for next time, just like in the dialog flow
                    LOGGER.info("Auto-extraction successful, disabling config prompt for next launch");
                    ModConfig.setPromptSetDefaultConfig(false);
                    ModConfig.saveConfig(); // Explicitly save the config

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

    @Unique
    private void packCore$showConfigSelectionDialog(ConfigExtractionService extractionService) {
        try {
            // Use CompletableFuture to handle the dialog asynchronously but block the main thread
            CompletableFuture<Boolean> dialogResult = CompletableFuture.supplyAsync(() -> {
                ConfigSelectionDialog dialog = new ConfigSelectionDialog(extractionService);
                return dialog.showAndWait();
            });

            // Block until dialog completes or times out
            Boolean success = dialogResult.get(DIALOG_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            if (success != null && success) {
                LOGGER.info("Config selection completed successfully");
            } else {
                LOGGER.warn("Config selection was cancelled or failed");
            }

        } catch (Exception e) {
            LOGGER.error("Error showing config selection dialog", e);
        }
    }
}