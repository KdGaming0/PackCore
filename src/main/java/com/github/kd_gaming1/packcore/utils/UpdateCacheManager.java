package com.github.kd_gaming1.packcore.utils;

import com.github.kd_gaming1.packcore.PackCore;

import java.time.Instant;

public class UpdateCacheManager {

    // Cached data from API response
    private String cachedVersionNumber;
    private String cachedVersionType;
    private String cachedChangelog;
    private String cachedVersionId;
    private boolean updateAvailable;

    // Configuration data we used for the cached API call
    private String cachedModrinthProjectId;
    private String cachedUpdateChannel;
    private String cachedCurrentVersion;
    private String cachedMinecraftVersion;

    // Cache timing
    private Instant lastUpdateCheck;
    private static final long CACHE_DURATION_MINUTES = 15; // Cache for 15 minutes

    // API caller instance
    private final ModrinthApiClient apiClient;

    public UpdateCacheManager() {
        this.apiClient = new ModrinthApiClient();
    }

    // Main method - this is what other classes call
    public UpdateCheckResult checkForUpdates(ModpackInfo modpackInfo) {
        // Validate configuration first
        if (!modpackInfo.isConfigurationValid()) {
            String error = modpackInfo.getValidationError();
            PackCore.LOGGER.error("Invalid modpack configuration: {}", error);
            return UpdateCheckResult.error("Configuration error: " + error);
        }
        // First, check if we can use cached data
        if (isCacheValid(modpackInfo)) {
            return createResultFromCache();
        }

        // Cache is invalid, fetch fresh data
        try {
            return fetchAndCacheUpdates(modpackInfo);
        } catch (Exception e) {
            return UpdateCheckResult.error("Failed to check for updates: " + e.getMessage());
        }
    }

    private boolean isCacheValid(ModpackInfo modpackInfo) {
        // No cached data yet
        if (lastUpdateCheck == null) {
            return false;
        }

        // Cache expired?
        long minutesSinceLastCheck = java.time.Duration.between(lastUpdateCheck, Instant.now()).toMinutes();
        if (minutesSinceLastCheck >= CACHE_DURATION_MINUTES) {
            return false;
        }

        // Configuration changed?
        if (!configMatches(modpackInfo)) {
            return false;
        }

        return true;
    }

    private boolean configMatches(ModpackInfo modpackInfo) {
        return modpackInfo.getModrinthProjectId().equals(cachedModrinthProjectId) &&
                modpackInfo.getUpdateChannel().equals(cachedUpdateChannel) &&
                modpackInfo.getVersion().equals(cachedCurrentVersion) &&
                modpackInfo.getMinecraftVersion().equals(cachedMinecraftVersion);
    }

    private UpdateCheckResult createResultFromCache() {
        return new UpdateCheckResult(updateAvailable, cachedVersionNumber,
                cachedVersionType, cachedChangelog, cachedVersionId);
    }

    private UpdateCheckResult fetchAndCacheUpdates(ModpackInfo modpackInfo) {
        try {
            // Call Modrinth API
            ModrinthVersion latestVersion = apiClient.getLatestVersion(
                    modpackInfo.getModrinthProjectId(),
                    modpackInfo.getUpdateChannel(),
                    modpackInfo.getMinecraftVersion()
            );

            // Update cache with current config
            updateCacheConfig(modpackInfo);
            lastUpdateCheck = Instant.now();

            if (latestVersion == null) {
                // No suitable version found
                updateAvailable = false;
                cachedVersionNumber = null;
                cachedVersionType = null;
                cachedChangelog = "No versions found matching your criteria";
                cachedVersionId = null;

                return new UpdateCheckResult(false, null, null,
                        "No versions found matching your criteria", null);
            }

            // Check if this is actually newer than current version
            boolean isNewer = isVersionNewer(modpackInfo.getVersion(), latestVersion.versionNumber());

            // Update cache
            updateAvailable = isNewer;
            cachedVersionNumber = latestVersion.versionNumber();
            cachedVersionType = latestVersion.versionType();
            cachedChangelog = latestVersion.changelog();
            cachedVersionId = latestVersion.versionId();

            return new UpdateCheckResult(isNewer, cachedVersionNumber,
                    cachedVersionType, cachedChangelog, cachedVersionId);

        } catch (Exception e) {
            return UpdateCheckResult.error("Failed to check for updates: " + e.getMessage());
        }
    }

    private void updateCacheConfig(ModpackInfo modpackInfo) {
        cachedModrinthProjectId = modpackInfo.getModrinthProjectId();
        cachedUpdateChannel = modpackInfo.getUpdateChannel();
        cachedCurrentVersion = modpackInfo.getVersion();
        cachedMinecraftVersion = modpackInfo.getMinecraftVersion();
    }

    // Simple version comparison - you might want to use a proper version comparison library
    private boolean isVersionNewer(String currentVersion, String latestVersion) {
        // For now, just do string comparison
        // In a real implementation, you'd want proper semantic versioning comparison
        return !currentVersion.equals(latestVersion);
    }
}