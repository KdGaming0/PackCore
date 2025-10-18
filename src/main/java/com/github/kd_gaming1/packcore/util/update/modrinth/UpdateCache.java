package com.github.kd_gaming1.packcore.util.update.modrinth;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.ui.screen.title.SBEStyledTitleScreen;
import com.github.kd_gaming1.packcore.util.update.UpdateResult;
import com.github.kd_gaming1.packcore.modpack.ModpackInfo;

import java.time.Instant;

public class UpdateCache {
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

    private final ModrinthClient apiClient;

    public UpdateCache() {
        this.apiClient = new ModrinthClient();
    }

    // Main method - this is what other classes call
    public UpdateResult checkForUpdates(ModpackInfo modpackInfo) {
        // Validate configuration first
        if (modpackInfo.isConfigurationValid()) {
            String error = modpackInfo.getValidationError();
            PackCore.LOGGER.error("Invalid modpack configuration: {}", error);
            return UpdateResult.error("Configuration error: " + error);
        }

        if (isCacheValid(modpackInfo)) {
            return createResultFromCache();
        }

        // Cache is invalid, fetch fresh data
        try {
            return fetchAndCacheUpdates(modpackInfo);
        } catch (Exception e) {
            return UpdateResult.error("Failed to check for updates: " + e.getMessage());
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

    private UpdateResult createResultFromCache() {
        return new UpdateResult(updateAvailable, cachedVersionNumber,
                cachedVersionType, cachedChangelog, cachedVersionId);
    }

    private UpdateResult fetchAndCacheUpdates(ModpackInfo modpackInfo) {
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

                return new UpdateResult(false, null, null,
                        "No versions found matching your criteria", null);
            }

            boolean isNewer = compareVersions(latestVersion.versionNumber(), modpackInfo.getVersion()) > 0;

            updateAvailable = isNewer;
            cachedVersionNumber = latestVersion.versionNumber();
            cachedVersionType = latestVersion.versionType();
            cachedChangelog = latestVersion.changelog();
            cachedVersionId = latestVersion.versionId();

            return new UpdateResult(isNewer, cachedVersionNumber,
                    cachedVersionType, cachedChangelog, cachedVersionId);

        } catch (Exception e) {
            return UpdateResult.error("Failed to check for updates: " + e.getMessage());
        }
    }

    private void updateCacheConfig(ModpackInfo modpackInfo) {
        cachedModrinthProjectId = modpackInfo.getModrinthProjectId();
        cachedUpdateChannel = modpackInfo.getUpdateChannel();
        cachedCurrentVersion = modpackInfo.getVersion();
        cachedMinecraftVersion = modpackInfo.getMinecraftVersion();
    }

    private int compareVersions(String v1, String v2) {
        return SBEStyledTitleScreen.compareVersions(v1, v2);
    }
}