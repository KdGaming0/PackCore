package com.github.kd_gaming1.packcore.util.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central utility class for managing performance profiles across different systems.
 * This class coordinates between Sodium and Minecraft vanilla settings.
 */
public class PerformanceProfileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("YourMod-PerformanceProfile");
    private static final String SODIUM_MOD_ID = "sodium";

    /**
     * Applies a performance profile to all available systems (Sodium + Vanilla)
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

        return result;
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

        return result;
    }

    /**
     * Gets information about available performance systems
     * @return SystemAvailability indicating what systems are present
     */
    public static SystemAvailability getSystemAvailability() {
        return new SystemAvailability(isSodiumAvailable(), true); // Vanilla is always available
    }

    private static boolean isSodiumAvailable() {
        return FabricLoader.getInstance().isModLoaded(SODIUM_MOD_ID);
    }

    public enum PerformanceProfile {
        PERFORMANCE("Maximum Performance", "Optimized for highest FPS"),
        BALANCED("Balanced", "Good balance between performance and quality"),
        QUALITY("Best Quality", "Optimized for visual quality");

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
        private boolean sodiumAvailable = false;

        public ProfileResult() {
            this.sodiumAvailable = isSodiumAvailable();
        }

        public boolean isSodiumApplied() { return sodiumApplied; }
        public boolean isVanillaApplied() { return vanillaApplied; }
        public boolean isSodiumAvailable() { return sodiumAvailable; }

        void setSodiumApplied(boolean applied) { this.sodiumApplied = applied; }
        void setVanillaApplied(boolean applied) { this.vanillaApplied = applied; }

        public boolean isFullySuccessful() {
            return vanillaApplied && (!sodiumAvailable || sodiumApplied);
        }

        public boolean hasAnyFailures() {
            return !vanillaApplied || (sodiumAvailable && !sodiumApplied);
        }
    }

    /**
     * Information about what performance systems are available
     */
    public static class SystemAvailability {
        private final boolean sodiumAvailable;
        private final boolean vanillaAvailable;

        public SystemAvailability(boolean sodiumAvailable, boolean vanillaAvailable) {
            this.sodiumAvailable = sodiumAvailable;
            this.vanillaAvailable = vanillaAvailable;
        }

        public boolean isSodiumAvailable() { return sodiumAvailable; }
        public boolean isVanillaAvailable() { return vanillaAvailable; }
        public boolean hasAnySystem() { return sodiumAvailable || vanillaAvailable; }
    }
}
