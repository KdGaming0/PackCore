package com.github.kdgaming0.packcore.copysystem;

/**
 * Enum representing the type of configuration
 */
public enum ConfigType {
    OFFICIAL("OfficialConfigs"),
    CUSTOM("CustomConfigs");

    private final String folderName;

    ConfigType(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }
}
