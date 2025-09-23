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

        result.setSodiumApplied(applySystemProfile(() -> SodiumIntegration.applyProfile(profile), "Sodium", isSodiumAvailable()));
        result.setVanillaApplied(applySystemProfile(() -> MinecraftIntegration.applyProfile(profile), "Vanilla Minecraft", true));
        result.setIrisApplied(applySystemProfile(() -> applyIrisSettings(profile), "Iris shader", isIrisAvailable() || profile == PerformanceProfile.SHADERS));

        return result;
    }

    private static boolean applySystemProfile(SystemProfileApplier applier, String systemName, boolean shouldApply) {
        if (!shouldApply) {
            LOGGER.debug("{} not available, skipping profile", systemName);
            return true;
        }

        try {
            boolean success = applier.apply();
            if (success) {
                LOGGER.info("{} profile applied successfully", systemName);
            } else {
                LOGGER.warn("Failed to apply {} profile", systemName);
            }
            return success;
        } catch (Throwable t) {
            LOGGER.error("Error applying {} profile", systemName, t);
            return false;
        }
    }

    private static boolean applyIrisSettings(PerformanceProfile profile) {
        if (!isIrisAvailable() && profile == PerformanceProfile.SHADERS) {
            LOGGER.warn("Shaders profile selected but Iris is not available");
            return false;
        }

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

        result.setSodiumApplied(applySystemProfile(SodiumIntegration::restoreDefaults, "Sodium", isSodiumAvailable()));
        result.setVanillaApplied(applySystemProfile(MinecraftIntegration::restoreDefaults, "Vanilla", true));
        result.setIrisApplied(applySystemProfile(IrisIntegration::disableShaders, "Iris", isIrisAvailable()));

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

    @FunctionalInterface
    private interface SystemProfileApplier {
        boolean apply() throws Exception;
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