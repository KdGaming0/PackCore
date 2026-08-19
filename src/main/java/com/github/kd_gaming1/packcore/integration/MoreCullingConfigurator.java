package com.github.kd_gaming1.packcore.integration;

import ca.fxco.moreculling.MoreCulling;
import ca.fxco.moreculling.config.option.LeavesCullingMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class MoreCullingConfigurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean applyProfile(PerformanceProfileService.PerformanceProfile profile) {
        if (!FabricLoader.getInstance().isModLoaded("moreculling")) {
            LOGGER.debug("MoreCulling not loaded, skipping");
            return true;
        }

        try {
            // Updated mapping per your request
            LeavesCullingMode mode = switch (profile) {
                case PERFORMANCE, BALANCED, SHADERS_PERFORMANCE -> LeavesCullingMode.FAST;
                case QUALITY, SHADERS_QUALITY -> LeavesCullingMode.CHECK;
            };

            MoreCulling.CONFIG.leavesCullingMode = mode;

            // Save via reflection — AutoConfig is loaded at runtime by MoreCulling
            boolean saved = saveViaReflection();
            if (!saved) {
                saveViaGson(); // fallback: write JSON directly
            }

            LOGGER.info("MoreCulling: Set leavesCullingMode to {} for profile {}", mode, profile);
            return true;
        } catch (Exception e) {
            LOGGER.error("MoreCulling: Failed to apply profile {}", profile, e);
            return false;
        }
    }

    /**
     * Attempts to call AutoConfig.getConfigHolder(MoreCullingConfig.class).save() via reflection.
     * AutoConfig is present at runtime because MoreCulling bundles it, but not on compile classpath.
     */
    private static boolean saveViaReflection() {
        try {
            Class<?> autoConfigClass = Class.forName("me.shedaniel.autoconfig.AutoConfig");
            Class<?> moreCullingConfigClass = Class.forName("ca.fxco.moreculling.config.MoreCullingConfig");

            Method getConfigHolder = autoConfigClass.getMethod("getConfigHolder", Class.class);
            Object holder = getConfigHolder.invoke(null, moreCullingConfigClass);

            Method save = holder.getClass().getMethod("save");
            save.invoke(holder);

            LOGGER.debug("MoreCulling config saved via AutoConfig reflection");
            return true;
        } catch (Exception e) {
            LOGGER.warn("MoreCulling: AutoConfig reflection save failed, will use Gson fallback", e);
            return false;
        }
    }

    /**
     * Fallback: writes the MoreCulling config directly to its JSON file using Gson.
     */
    private static void saveViaGson() {
        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("moreculling.json");

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(MoreCulling.CONFIG, writer);
            LOGGER.debug("MoreCulling config saved via Gson to {}", configPath);
        } catch (IOException e) {
            LOGGER.error("MoreCulling: Failed to save config via Gson", e);
        }
    }
}