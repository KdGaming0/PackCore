package com.github.kd_gaming1.packcore.util.config;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.config.IrisConfig;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Integration class for Iris-specific settings.
 * This class is only loaded when Iris is present.
 */
public class IrisIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Set a shader pack by finding the first pack that starts with the given name and ends with .zip
     * @param shaderPackPrefix The prefix to search for (e.g., "ComplementaryUnbound")
     * @return true if successful
     */
    public static boolean setShaderPack(String shaderPackPrefix) {
        try {
            // Find the shader pack
            String foundPack = findShaderPack(shaderPackPrefix);
            if (foundPack == null) {
                LOGGER.warn("No shader pack found starting with: {}", shaderPackPrefix);
                return false;
            }

            // Get the Iris configuration instance
            IrisConfig irisConfig = Iris.getIrisConfig();

            // Enable shaders and set the pack
            irisConfig.setShadersEnabled(true);
            irisConfig.setShaderPackName(foundPack);

            LOGGER.info("Setting shader pack in method setShaderPack to: {}", foundPack);

            // Save the configuration
            irisConfig.save();
            Thread.sleep(200); // Brief delay for file I/O

            // Schedule reload on main thread
            CompletableFuture<Boolean> reloadFuture = new CompletableFuture<>();
            MinecraftClient.getInstance().execute(() -> {
                try {
                    LOGGER.info("Reloading Iris with shader pack: {}", foundPack);
                    Iris.reload();
                    reloadFuture.complete(true);
                } catch (Exception e) {
                    LOGGER.error("Failed to reload Iris", e);
                    reloadFuture.complete(false);
                }
            });

            return reloadFuture.get(30, TimeUnit.SECONDS);

        } catch (Exception e) {
            LOGGER.error("Failed to set shader pack", e);
            return false;
        }
    }

    /**
     * Disable shaders
     * @return true if successful
     */
    public static boolean disableShaders() {
        try {
            return configureAndReload(null, false);
        } catch (Exception e) {
            LOGGER.error("Failed to disable shaders", e);
            return false;
        }
    }

    private static boolean configureAndReload(String shaderPack, boolean enabled) throws Exception {
        IrisConfig irisConfig = Iris.getIrisConfig();
        irisConfig.setShadersEnabled(enabled);

        if (enabled && shaderPack != null) {
            irisConfig.setShaderPackName(shaderPack);
            LOGGER.info("Setting shader pack in method configureAndReload to: {}", shaderPack);
        } else {
            LOGGER.info("Disabling Iris shaders");
        }

        irisConfig.save();
        Thread.sleep(enabled ? 200 : 100);

        CompletableFuture<Boolean> reloadFuture = new CompletableFuture<>();
        MinecraftClient.getInstance().execute(() -> {
            try {
                LOGGER.info("Reloading Iris with shaders {}", enabled ? "enabled" : "disabled");
                Iris.reload();
                reloadFuture.complete(true);
            } catch (Exception e) {
                LOGGER.error("Failed to reload Iris", e);
                reloadFuture.complete(false);
            }
        });

        return reloadFuture.get(30, TimeUnit.SECONDS);
    }

    /**
     * Find a shader pack that starts with the given prefix and ends with .zip
     * @param prefix The prefix to search for
     * @return The full shader pack filename, or null if not found
     */
    private static String findShaderPack(String prefix) {
        try {
            Path shaderPacksDir = MinecraftClient.getInstance()
                    .runDirectory.toPath().resolve("shaderpacks");

            if (!Files.exists(shaderPacksDir)) {
                LOGGER.warn("Shaderpacks directory does not exist: {}", shaderPacksDir);
                return null;
            }

            try (Stream<Path> paths = Files.list(shaderPacksDir)) {
                return paths
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase())
                                && name.toLowerCase().endsWith(".zip"))
                        .findFirst()
                        .orElse(null);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to search for shader pack with prefix: {}", prefix, e);
            return null;
        }
    }
}