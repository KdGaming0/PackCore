package com.github.kd_gaming1.packcore.gui.help;

import java.util.*;

/**
 * Manages wizard configuration data across all pages
 */
public class WizardDataManager {
    private static WizardDataManager instance;

    // CORRECTED: Separate optimization profiles from resource packs
    private String selectedOptimizationProfile = ""; // Performance settings profile
    private final List<String> selectedResourcePacksOrdered = new ArrayList<>(); // Visual resource packs
    private String selectedTabDesign = "";
    private final Set<String> selectedAdditionalSettings = new HashSet<>();
    private final Map<String, Object> customSettings = new HashMap<>();

    // NEW: Application state tracking
    private boolean configurationApplied = false;
    private boolean configurationApplying = false;
    private String configurationResult = ""; // "success", "failed", or ""
    private String configurationErrorMessage = "";

    private WizardDataManager() {}

    public static WizardDataManager getInstance() {
        if (instance == null) {
            instance = new WizardDataManager();
        }
        return instance;
    }

    // CORRECTED: Optimization Profile Management (Performance Settings)
    public void setOptimizationProfile(String profile) {
        this.selectedOptimizationProfile = profile;
    }

    public String getOptimizationProfile() {
        return selectedOptimizationProfile;
    }

    // Resource Pack Management (Visual Packs)
    public void setResourcePacksOrdered(List<String> packs) {
        selectedResourcePacksOrdered.clear();
        selectedResourcePacksOrdered.addAll(packs);
    }

    public List<String> getResourcePacksOrdered() {
        return new ArrayList<>(selectedResourcePacksOrdered);
    }

    public void addResourcePack(String packKey) {
        if (!selectedResourcePacksOrdered.contains(packKey)) {
            selectedResourcePacksOrdered.add(packKey);
        }
    }

    public void removeResourcePack(String packKey) {
        selectedResourcePacksOrdered.remove(packKey);
    }

    public void toggleResourcePack(String packKey) {
        if (selectedResourcePacksOrdered.contains(packKey)) {
            selectedResourcePacksOrdered.remove(packKey);
        } else {
            selectedResourcePacksOrdered.add(packKey);
        }
    }

    public boolean isResourcePackSelected(String packKey) {
        return selectedResourcePacksOrdered.contains(packKey);
    }

    // Tab Design (Page 2)
    public void setTabDesign(String design) {
        this.selectedTabDesign = design;
    }

    public String getTabDesign() {
        return selectedTabDesign;
    }

    // Additional Settings
    public void toggleAdditionalSetting(String setting) {
        if (selectedAdditionalSettings.contains(setting)) {
            selectedAdditionalSettings.remove(setting);
        } else {
            selectedAdditionalSettings.add(setting);
        }
    }

    public boolean isAdditionalSettingSelected(String setting) {
        return selectedAdditionalSettings.contains(setting);
    }

    public Set<String> getAdditionalSettings() {
        return new HashSet<>(selectedAdditionalSettings);
    }

    // Custom settings storage
    public void setCustomSetting(String key, Object value) {
        customSettings.put(key, value);
    }

    public Object getCustomSetting(String key) {
        return customSettings.get(key);
    }

    // NEW: Application state management
    public void setConfigurationApplied(boolean applied) {
        this.configurationApplied = applied;
    }

    public boolean isConfigurationApplied() {
        return configurationApplied;
    }

    public void setConfigurationApplying(boolean applying) {
        this.configurationApplying = applying;
    }

    public boolean isConfigurationApplying() {
        return configurationApplying;
    }

    public void setConfigurationResult(String result, String errorMessage) {
        this.configurationResult = result;
        this.configurationErrorMessage = errorMessage != null ? errorMessage : "";
    }

    public String getConfigurationResult() {
        return configurationResult;
    }

    public String getConfigurationErrorMessage() {
        return configurationErrorMessage;
    }

    public void resetConfigurationState() {
        this.configurationApplied = false;
        this.configurationApplying = false;
        this.configurationResult = "";
        this.configurationErrorMessage = "";
    }

    // Configuration summary
    public WizardConfiguration getConfiguration() {
        return new WizardConfiguration(
                selectedOptimizationProfile,
                new ArrayList<>(selectedResourcePacksOrdered),
                selectedTabDesign,
                new HashSet<>(selectedAdditionalSettings),
                new HashMap<>(customSettings)
        );
    }

    // Reset all data
    public void reset() {
        selectedOptimizationProfile = "";
        selectedResourcePacksOrdered.clear();
        selectedTabDesign = "";
        selectedAdditionalSettings.clear();
        customSettings.clear();
        resetConfigurationState();
    }

    // Check if configuration is complete
    public boolean isConfigurationComplete() {
        return !selectedOptimizationProfile.isEmpty() || !selectedResourcePacksOrdered.isEmpty();
    }

    // Get configuration summary text
    public String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Optimization Profile: ").append(selectedOptimizationProfile.isEmpty() ? "None" : selectedOptimizationProfile).append("\n");
        summary.append("Resource Packs: ").append(selectedResourcePacksOrdered.isEmpty() ? "None" : String.join(", ", selectedResourcePacksOrdered)).append("\n");
        summary.append("Tab Design: ").append(selectedTabDesign.isEmpty() ? "None" : selectedTabDesign).append("\n");
        summary.append("Additional Settings: ").append(selectedAdditionalSettings.isEmpty() ? "None" : String.join(", ", selectedAdditionalSettings));
        return summary.toString();
    }

    public static class WizardConfiguration {
        private final String optimizationProfile;
        private final List<String> resourcePacksOrdered;
        private final String tabDesign;
        private final Set<String> additionalSettings;
        private final Map<String, Object> customSettings;

        public WizardConfiguration(String optimizationProfile, List<String> resourcePacksOrdered,
                                   String tabDesign, Set<String> additionalSettings,
                                   Map<String, Object> customSettings) {
            this.optimizationProfile = optimizationProfile;
            this.resourcePacksOrdered = resourcePacksOrdered;
            this.tabDesign = tabDesign;
            this.additionalSettings = additionalSettings;
            this.customSettings = customSettings;
        }

        public String getOptimizationProfile() {
            return optimizationProfile;
        }

        public List<String> getResourcePacksOrdered() {
            return new ArrayList<>(resourcePacksOrdered);
        }

        public String getTabDesign() {
            return tabDesign;
        }

        public Set<String> getAdditionalSettings() {
            return new HashSet<>(additionalSettings);
        }

        public Map<String, Object> getCustomSettings() {
            return new HashMap<>(customSettings);
        }
    }
}