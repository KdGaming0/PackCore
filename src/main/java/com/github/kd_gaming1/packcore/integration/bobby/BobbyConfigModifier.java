package com.github.kd_gaming1.packcore.integration.bobby;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BobbyConfigModifier {

    /**
     * Enables Bobby's dynamicMultiWorld option by modifying its config file.
     * If Bobby is not installed or the config file is missing, it does nothing.
     */
    public static void enableDynamicMultiWorld() {
        if (!FabricLoader.getInstance().isModLoaded("bobby")) {
            PackCore.LOGGER.info("Bobby mod not found, skipping config modification");
            return;
        }

        try {
            // Bobby stores its config in config/bobby.conf
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("bobby.conf");

            if (!Files.exists(configPath)) {
                PackCore.LOGGER.info("Bobby config not found yet, skipping modification (will use defaults)");
                return;
            }

            // Read the config file
            List<String> lines = Files.readAllLines(configPath);
            List<String> newLines = new ArrayList<>();
            boolean modified = false;
            boolean foundDynamicMultiWorld = false;

            // Process each line
            for (String line : lines) {
                String trimmed = line.trim();

                // Check if this is the dynamic-multi-world setting
                if (trimmed.startsWith("dynamic-multi-world=") || trimmed.startsWith("dynamic-multi-world ")) {
                    foundDynamicMultiWorld = true;

                    // Check if it's already set to true
                    if (trimmed.contains("=true")) {
                        PackCore.LOGGER.info("Bobby's dynamicMultiWorld is already enabled");
                        return; // Already set, no need to modify
                    }

                    // Replace with true, preserving any indentation
                    String indentation = line.substring(0, line.indexOf(trimmed));
                    newLines.add(indentation + "dynamic-multi-world=true");
                    modified = true;
                } else {
                    newLines.add(line);
                }
            }

            if (!foundDynamicMultiWorld) {
                PackCore.LOGGER.warn("Could not find dynamic-multi-world setting in Bobby config");
                return;
            }

            if (modified) {
                Files.write(configPath, newLines);
                PackCore.LOGGER.info("Successfully enabled Bobby's dynamicMultiWorld");
            }

        } catch (Exception e) {
            PackCore.LOGGER.error("Failed to modify Bobby config", e);
        }
    }
}