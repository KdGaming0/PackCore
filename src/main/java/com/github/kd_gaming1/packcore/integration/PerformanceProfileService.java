package com.github.kd_gaming1.packcore.integration;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class PerformanceProfileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public enum PerformanceProfile {
        PERFORMANCE("maxfps", "Maximum Performance"),
        BALANCED("balanced", "Balanced"),
        QUALITY("quality", "Best Quality"),
        SHADERS_PERFORMANCE("shaders_balanced", "Shaders (Balanced)"),
        SHADERS_QUALITY("shaders_quality", "Shaders (Quality)");

        private final String id;
        private final String displayName;

        PerformanceProfile(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() { return id; }
        public String getDisplayName() { return displayName; }
    }

    public static boolean applyAll(PerformanceProfile profile) {
        LOGGER.info("Applying global performance profile: {}", profile.getDisplayName());

        boolean vanilla = MinecraftIntegration.applyProfile(profile);

        boolean sodium = true;
        if (FabricLoader.getInstance().isModLoaded("sodium")) {
            sodium = SodiumConfigurator.applyProfile(profile);
        }

        boolean moreculling = MoreCullingConfigurator.applyProfile(profile);

        boolean iris = true;
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            iris = switch (profile) {
                case SHADERS_PERFORMANCE -> IrisConfigurator.setShaderPack("MakeUp-UltraFast");
                case SHADERS_QUALITY -> IrisConfigurator.setShaderPack("ComplementaryUnbound");
                default -> IrisConfigurator.disableShaders();
            };
        }

        return vanilla && sodium && moreculling && iris;
    }
}