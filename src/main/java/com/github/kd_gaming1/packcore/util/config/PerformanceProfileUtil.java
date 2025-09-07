package com.github.kd_gaming1.packcore.util.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Central utility class for managing performance profiles across different systems.
 * This class coordinates between Sodium, Minecraft vanilla settings, and Iris shaders.
 */
public class PerformanceProfileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String SODIUM_MOD_ID = "sodium";
    private static final String IRIS_MOD_ID = "iris";

    /**
     * Applies a performance profile to all available systems (Sodium + Vanilla + Iris)
     * @param profile The performance profile to apply
     * @return ProfileResult indicating what was applied and any failures
     */
    public static ProfileResult applyPerformanceProfile(PerformanceProfile profile) {
        LOGGER.info("Applying performance profile: {}", profile.getDisplayName());

        ProfileResult result = new ProfileResult();

        // Apply Sodium settings if available
        if (isSodiumAvailable()) {
            try {
                boolean sodiumSuccess = SodiumIntegration.applyProfile(profile);
                result.setSodiumApplied(sodiumSuccess);
                if (sodiumSuccess) {
                    LOGGER.info("Sodium profile applied successfully");
                } else {
                    LOGGER.warn("Failed to apply Sodium profile");
                }
            } catch (Throwable t) {
                LOGGER.error("Error applying Sodium profile", t);
                result.setSodiumApplied(false);
            }
        } else {
            LOGGER.debug("Sodium not available, skipping Sodium profile");
        }

        // Apply vanilla Minecraft settings
        try {
            boolean vanillaSuccess = MinecraftIntegration.applyProfile(profile);
            result.setVanillaApplied(vanillaSuccess);
            if (vanillaSuccess) {
                LOGGER.info("Vanilla Minecraft profile applied successfully");
            } else {
                LOGGER.warn("Failed to apply vanilla Minecraft profile");
            }
        } catch (Throwable t) {
            LOGGER.error("Error applying vanilla Minecraft profile", t);
            result.setVanillaApplied(false);
        }

        // Apply Iris shader settings if available and profile requires shaders
        if (isIrisAvailable()) {
            try {
                boolean irisSuccess = applyIrisSettings(profile);
                result.setIrisApplied(irisSuccess);
                if (irisSuccess) {
                    LOGGER.info("Iris shader settings applied successfully");
                } else {
                    LOGGER.warn("Failed to apply Iris shader settings");
                }
            } catch (Throwable t) {
                LOGGER.error("Error applying Iris shader settings", t);
                result.setIrisApplied(false);
            }
        } else if (profile == PerformanceProfile.SHADERS) {
            LOGGER.warn("Shaders profile selected but Iris is not available");
            result.setIrisApplied(false);
        }

        return result;
    }

    private static boolean applyIrisSettings(PerformanceProfile profile) {
        if (Objects.requireNonNull(profile) == PerformanceProfile.SHADERS) {
            return IrisIntegration.setShaderPack("ComplementaryUnbound");
        }
        return IrisIntegration.disableShaders();
    }

    /**
     * Restores default settings for all available systems
     * @return ProfileResult indicating what was restored
     */
    public static ProfileResult restoreDefaults() {
        LOGGER.info("Restoring default settings");

        ProfileResult result = new ProfileResult();

        // Restore Sodium defaults if available
        if (isSodiumAvailable()) {
            try {
                boolean sodiumSuccess = SodiumIntegration.restoreDefaults();
                result.setSodiumApplied(sodiumSuccess);
            } catch (Throwable t) {
                LOGGER.error("Error restoring Sodium defaults", t);
                result.setSodiumApplied(false);
            }
        }

        // Restore vanilla defaults
        try {
            boolean vanillaSuccess = MinecraftIntegration.restoreDefaults();
            result.setVanillaApplied(vanillaSuccess);
        } catch (Throwable t) {
            LOGGER.error("Error restoring vanilla defaults", t);
            result.setVanillaApplied(false);
        }

        // Restore Iris defaults if available
        if (isIrisAvailable()) {
            try {
                boolean irisSuccess = IrisIntegration.disableShaders(); // Default to shaders off
                result.setIrisApplied(irisSuccess);
            } catch (Throwable t) {
                LOGGER.error("Error restoring Iris defaults", t);
                result.setIrisApplied(false);
            }
        }

        return result;
    }

    /**
     * Gets information about available performance systems
     * @return SystemAvailability indicating what systems are present
     */
    public static SystemAvailability getSystemAvailability() {
        return new SystemAvailability(isSodiumAvailable(), true, isIrisAvailable());
    }

    private static boolean isSodiumAvailable() {
        return FabricLoader.getInstance().isModLoaded(SODIUM_MOD_ID);
    }

    private static boolean isIrisAvailable() {
        return FabricLoader.getInstance().isModLoaded(IRIS_MOD_ID);
    }

    public enum PerformanceProfile {
        PERFORMANCE("Maximum Performance", "Optimized for highest FPS"),
        BALANCED("Balanced", "Good balance between performance and quality"),
        QUALITY("Best Quality", "Optimized for visual quality"),
        SHADERS("Shaders", "Ultimate visual experience with shaders enabled");

        private final String displayName;
        private final String description;

        PerformanceProfile(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    /**
     * Result of applying a performance profile
     */
    public static class ProfileResult {
        private boolean sodiumApplied = false;
        private boolean vanillaApplied = false;
        private boolean irisApplied = false;
        private final boolean sodiumAvailable;
        private final boolean irisAvailable;

        public ProfileResult() {
            this.sodiumAvailable = isSodiumAvailable();
            this.irisAvailable = isIrisAvailable();
        }

        public boolean isSodiumApplied() { return sodiumApplied; }
        public boolean isVanillaApplied() { return vanillaApplied; }
        public boolean isIrisApplied() { return irisApplied; }
        public boolean isSodiumAvailable() { return sodiumAvailable; }
        public boolean isIrisAvailable() { return irisAvailable; }

        void setSodiumApplied(boolean applied) { this.sodiumApplied = applied; }
        void setVanillaApplied(boolean applied) { this.vanillaApplied = applied; }
        void setIrisApplied(boolean applied) { this.irisApplied = applied; }

        public boolean isFullySuccessful() {
            return vanillaApplied &&
                    (!sodiumAvailable || sodiumApplied) &&
                    (!irisAvailable || irisApplied);
        }

        public boolean hasAnyFailures() {
            return !vanillaApplied ||
                    (sodiumAvailable && !sodiumApplied) ||
                    (irisAvailable && !irisApplied);
        }
    }

    /**
     * Information about what performance systems are available
     */
    public static class SystemAvailability {
        private final boolean sodiumAvailable;
        private final boolean vanillaAvailable;
        private final boolean irisAvailable;

        public SystemAvailability(boolean sodiumAvailable, boolean vanillaAvailable, boolean irisAvailable) {
            this.sodiumAvailable = sodiumAvailable;
            this.vanillaAvailable = vanillaAvailable;
            this.irisAvailable = irisAvailable;
        }

        public boolean isSodiumAvailable() { return sodiumAvailable; }
        public boolean isVanillaAvailable() { return vanillaAvailable; }
        public boolean isIrisAvailable() { return irisAvailable; }
        public boolean hasAnySystem() { return sodiumAvailable || vanillaAvailable || irisAvailable; }
    }
}