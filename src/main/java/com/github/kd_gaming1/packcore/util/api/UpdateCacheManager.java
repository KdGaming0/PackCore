package com.github.kd_gaming1.packcore.util.api;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.util.modpack.ModpackInfo;

import java.time.Instant;

public class UpdateCacheManager {
    private String cachedVersionNumber;
    private String cachedVersionType;
    private String cachedChangelog;
    private String cachedVersionId;
    private boolean updateAvailable;

    private String cachedModrinthProjectId;
    private String cachedUpdateChannel;
    private String cachedCurrentVersion;
    private String cachedMinecraftVersion;

    private Instant lastUpdateCheck;
    private static final long CACHE_DURATION_MINUTES = 15;

    private final ModrinthApiClient apiClient;

    public UpdateCacheManager() {
        this.apiClient = new ModrinthApiClient();
    }

    // Main method - this is what other classes call
    public UpdateCheckResult checkForUpdates(ModpackInfo modpackInfo) {
        // Validate configuration first
        if (modpackInfo.isConfigurationValid()) {
            String error = modpackInfo.getValidationError();
            PackCore.LOGGER.error("Invalid modpack configuration: {}", error);
            return UpdateCheckResult.error("Configuration error: " + error);
        }

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
        return lastUpdateCheck != null &&
                java.time.Duration.between(lastUpdateCheck, Instant.now()).toMinutes() < CACHE_DURATION_MINUTES &&
                configMatches(modpackInfo);
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
            ModrinthVersion latestVersion = apiClient.getLatestVersion(
                    modpackInfo.getModrinthProjectId(),
                    modpackInfo.getUpdateChannel(),
                    modpackInfo.getMinecraftVersion()
            );

            updateCacheConfig(modpackInfo);
            lastUpdateCheck = Instant.now();

            if (latestVersion == null) {
                updateAvailable = false;
                cachedVersionNumber = null;
                cachedVersionType = null;
                cachedChangelog = "No versions found matching your criteria";
                cachedVersionId = null;

                return new UpdateCheckResult(false, null, null,
                        "No versions found matching your criteria", null);
            }

            boolean isNewer = compareVersions(latestVersion.versionNumber(), modpackInfo.getVersion()) > 0;

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

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.replaceAll("[^0-9.]", "").split("\\.");
        String[] parts2 = v2.replaceAll("[^0-9.]", "").split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (p1 != p2) {
                return p1 - p2;
            }
        }

        return 0;
    }
}