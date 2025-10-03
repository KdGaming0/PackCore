package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.util.modpack.ModpackInfo;
import com.github.kd_gaming1.packcore.util.api.UpdateCacheManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class PackCore implements ModInitializer {
	public static final String MOD_ID = "packcore";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModpackInfo modpackInfo;
    private static UpdateCacheManager updateManager;
    private static final Path runDir = FabricLoader.getInstance().getGameDir();
    private static final Path packcoreDir = runDir.resolve("packcore");

    @Override
	public void onInitialize() {
        LOGGER.info("PackCore initialized!");

        try {
            // Load modpack info at startup
            modpackInfo = ModpackInfo.loadFromFile(packcoreDir);

            // Create an update manager
            updateManager = new UpdateCacheManager();

            LOGGER.info("Loaded modpack info for: {}", modpackInfo.getName());

        } catch (Exception e) {
            LOGGER.error("Failed to load modpack info: {}", e.getMessage());
        }
	}

    public static ModpackInfo getModpackInfo() {
        return modpackInfo;
    }

    public static UpdateCacheManager getUpdateManager() {
        return updateManager;
    }
}