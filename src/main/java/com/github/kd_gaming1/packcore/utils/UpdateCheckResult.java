package com.github.kd_gaming1.packcore.utils;

import com.github.kd_gaming1.packcore.PackCore;

import static com.mojang.text2speech.Narrator.LOGGER;

public class UpdateCheckResult {

    private final boolean success;
    private final boolean updateAvailable;
    private final String versionNumber;
    private final String versionType;
    private final String changelog;
    private final String versionId;
    private final String errorMessage;

    // Constructor for successful update check
    public UpdateCheckResult(boolean updateAvailable, String versionNumber,
                             String versionType, String changelog, String versionId) {
        this.success = true;
        this.updateAvailable = updateAvailable;
        this.versionNumber = versionNumber;
        this.versionType = versionType;
        this.changelog = changelog;
        this.versionId = versionId;
        this.errorMessage = null;
    }

    // Constructor for error cases
    private UpdateCheckResult(String errorMessage) {
        this.success = false;
        this.updateAvailable = false;
        this.versionNumber = null;
        this.versionType = null;
        this.changelog = null;
        this.versionId = null;
        this.errorMessage = errorMessage;
    }

    // Static method to create error result
    public static UpdateCheckResult error(String errorMessage) {
        return new UpdateCheckResult(errorMessage);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public boolean isUpdateAvailable() { return updateAvailable; }
    public String getVersionNumber() { return versionNumber; }
    public String getVersionType() { return versionType; }
    public String getChangelog() { return changelog; }
    public String getVersionId() { return versionId; }
    public String getErrorMessage() { return errorMessage; }

    // Helper method to create Modrinth version URL
    public String getModrinthUrl() {
        ModpackInfo info = PackCore.getModpackInfo();
        if (info == null) {
            LOGGER.error("Update system not initialized properly. Cannot create URL");
            return null;
        }

        // Check if the configuration is valid
        if (!info.isConfigurationValid()) {
            LOGGER.warn("Configuration is invalid, cannot create URL for changelog: {}",
                    info.getValidationError());
            return null;
        }

        String projectId = info.getModrinthProjectId();

        if (versionId != null) {
            return "https://modrinth.com/project/" + projectId + "/version/" + versionId;
        }
        return null;
    }
}
