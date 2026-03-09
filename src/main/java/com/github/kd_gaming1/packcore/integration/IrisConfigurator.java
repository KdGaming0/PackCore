package com.github.kd_gaming1.packcore.integration;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.config.IrisConfig;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class IrisConfigurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean setShaderPack(String prefix) {
        String foundPack = findShaderPack(prefix);
        if (foundPack == null) {
            LOGGER.warn("Shader pack not found with prefix: {}", prefix);
            return false;
        }
        return applyAndReload(foundPack, true);
    }

    public static boolean disableShaders() {
        return applyAndReload(null, false);
    }

    private static boolean applyAndReload(String packName, boolean enabled) {
        try {
            IrisConfig config = Iris.getIrisConfig();
            config.setShadersEnabled(enabled);
            if (enabled && packName != null) {
                config.setShaderPackName(packName);
            }
            config.save();

            Minecraft.getInstance().execute(() -> {
                LOGGER.info("Iris: Reloading with shaders {}", enabled ? "ON (" + packName + ")" : "OFF");
                try {
                    Iris.reload();
                } catch (IOException e) {
                    LOGGER.error("Iris: Failed to reload shaders", e);
                }
            });
            return true;
        } catch (Exception e) {
            LOGGER.error("Iris: Failed to update configuration", e);
            return false;
        }
    }

    private static String findShaderPack(String prefix) {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("shaderpacks");
        if (!Files.exists(dir)) return null;

        try (Stream<Path> paths = Files.list(dir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()) && name.toLowerCase().endsWith(".zip"))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}