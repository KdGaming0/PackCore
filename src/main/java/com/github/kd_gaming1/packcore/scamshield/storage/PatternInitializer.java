package com.github.kd_gaming1.packcore.scamshield.storage;

import com.github.kd_gaming1.packcore.PackCore;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PatternInitializer {
    private static final String DEFAULT_PATTERNS_RESOURCE = "/assets/packcore/scamshield/default-patterns.json";

    public static void initializePatterns() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path scamShieldDir = gameDir.resolve("packcore/scamshield");
        Path patternsFile = scamShieldDir.resolve("patterns.json");

        try {
            Files.createDirectories(scamShieldDir);

            if (!Files.exists(patternsFile)) {
                copyDefaultPatterns(patternsFile);
            }

            PackCore.LOGGER.info("[ScamShield] Pattern files initialized at: {}", scamShieldDir);
        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to initialize pattern files", e);
        }
    }

    private static void copyDefaultPatterns(Path targetFile) throws IOException {
        // Use class loader to get resource - works in dev and production
        try (InputStream in = PatternInitializer.class.getResourceAsStream(DEFAULT_PATTERNS_RESOURCE)) {
            if (in == null) {
                PackCore.LOGGER.error("[ScamShield] Default patterns resource not found at: {}",
                        DEFAULT_PATTERNS_RESOURCE);

                // Create an empty JSON array as fallback
                Files.writeString(targetFile, "[]");
                return;
            }

            Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            PackCore.LOGGER.info("[ScamShield] Copied default patterns to: {}", targetFile);
        }
    }

    public static Path getPatternsFilePath() {
        return FabricLoader.getInstance()
                .getGameDir()
                .resolve("packcore/scamshield/patterns.json");
    }
}