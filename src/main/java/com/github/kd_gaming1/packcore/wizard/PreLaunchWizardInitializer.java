package com.github.kd_gaming1.packcore.wizard;

import com.github.kd_gaming1.packcore.wizard.util.PageContentProviders;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class PreLaunchWizardInitializer implements PreLaunchEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreLaunchWizardInitializer.class);

    @Override
    public void onPreLaunch() {
        Path runDir = FabricLoader.getInstance().getGameDir();
        Path langDir = runDir.resolve("packcore/lang");
        Path officialConfigDir = runDir.resolve("packcore/modpack_config/official_configs");
        Path customConfigDir = runDir.resolve("packcore/modpack_config/custom_configs");
        Path configDescriptionsDir = runDir.resolve("packcore/config_descriptions");

        // Create lang files
        try {
            Files.createDirectories(langDir);
            for (var entry : PageContentProviders.CONTENT_PROVIDERS.entrySet()) {
                Path file = langDir.resolve(entry.getKey());
                try {
                    Files.writeString(
                            file,
                            entry.getValue().get(),
                            StandardOpenOption.CREATE_NEW
                    );
                    LOGGER.info("Created file at {}", file.toAbsolutePath());
                } catch (IOException e) {
                    if (!Files.exists(file)) {
                        LOGGER.error("Failed to create file: {}", file, e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create lang directory", e);
        }

        // Create config directories
        try {
            Files.createDirectories(officialConfigDir);
            Files.createDirectories(customConfigDir);
            Files.createDirectories(configDescriptionsDir);

            // Create example config description markdown files
            createExampleConfigDescriptions(configDescriptionsDir);

            LOGGER.info("Created modpack data directories");
        } catch (IOException e) {
            LOGGER.error("Failed to create modpack data directories", e);
        }
    }

    private void createExampleConfigDescriptions(Path configDescriptionsDir) {
        for (var entry : PageContentProviders.CONFIG_DESCRIPTION_PROVIDERS.entrySet()) {
            Path exampleFile = configDescriptionsDir.resolve(entry.getKey());
            try {
                Files.writeString(exampleFile, entry.getValue().get(), StandardOpenOption.CREATE_NEW);
                LOGGER.info("Created example config description: {}", entry.getKey());
            } catch (IOException e) {
                if (!Files.exists(exampleFile)) {
                    LOGGER.warn("Failed to create example config description: {}", entry.getKey());
                }
            }
        }
    }
}