package com.kd_gaming1.copysystem;

import java.util.List;

/**
 * Result object that encapsulates the decision logic for config selection
 */
public class ConfigSelectionResult {
    private final boolean showDialog;
    private final boolean hasAutoExtract;
    private final String configName;
    private final ConfigType configType;
    private final List<ConfigInfo> officialConfigs;
    private final List<ConfigInfo> customConfigs;

    private ConfigSelectionResult(boolean showDialog, boolean hasAutoExtract, String configName,
                                  ConfigType configType, List<ConfigInfo> officialConfigs, List<ConfigInfo> customConfigs) {
        this.showDialog = showDialog;
        this.hasAutoExtract = hasAutoExtract;
        this.configName = configName;
        this.configType = configType;
        this.officialConfigs = officialConfigs;
        this.customConfigs = customConfigs;
    }

    public static ConfigSelectionResult showDialog(List<ConfigInfo> officialConfigs, List<ConfigInfo> customConfigs) {
        return new ConfigSelectionResult(true, false, null, null, officialConfigs, customConfigs);
    }

    public static ConfigSelectionResult autoExtract(String configName, ConfigType configType) {
        return new ConfigSelectionResult(false, true, configName, configType, null, null);
    }

    public static ConfigSelectionResult noConfigs() {
        return new ConfigSelectionResult(false, false, null, null, null, null);
    }

    public boolean shouldShowDialog() { return showDialog; }
    public boolean hasAutoExtractConfig() { return hasAutoExtract; }
    public String getConfigName() { return configName; }
    public ConfigType getConfigType() { return configType; }
    public List<ConfigInfo> getOfficialConfigs() { return officialConfigs; }
    public List<ConfigInfo> getCustomConfigs() { return customConfigs; }
}