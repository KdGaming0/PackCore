package com.github.kd_gaming1.packcore.gui.help;

import java.util.*;

/**
 * Manages wizard configuration data across all pages
 */
public class WizardDataManager {
    private static WizardDataManager instance;

    // Configuration data storage
    private String selectedOptimizationProfile = "";
    private String selectedTabDesign = "";
    private final Set<String> selectedMiscSettings = new HashSet<>();
    private final Map<String, Object> customSettings = new HashMap<>();

    // NEW: Ordered resource pack storage
    private final List<String> selectedResourcePacksOrdered = new ArrayList<>();

    private WizardDataManager() {}

    public static WizardDataManager getInstance() {
        if (instance == null) {
            instance = new WizardDataManager();
        }
        return instance;
    }

    // Optimization Profile (Page 1) - DEPRECATED for resource packs, kept for compatibility
    public void setOptimizationProfile(String profile) {
        this.selectedOptimizationProfile = profile;
    }

    public String getOptimizationProfile() {
        return selectedOptimizationProfile;
    }

    // NEW: Resource Pack Management (Ordered)
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

    public Set<String> getResourcePacksAsSet() {
        return new LinkedHashSet<>(selectedResourcePacksOrdered);
    }

    public String getResourcePacksAsString() {
        return String.join(",", selectedResourcePacksOrdered);
    }

    // Tab Design (Page 2)
    public void setTabDesign(String design) {
        this.selectedTabDesign = design;
    }

    public String getTabDesign() {
        return selectedTabDesign;
    }

    // Miscellaneous Settings (Page 3)
    public void toggleMiscSetting(String setting) {
        if (selectedMiscSettings.contains(setting)) {
            selectedMiscSettings.remove(setting);
        } else {
            selectedMiscSettings.add(setting);
        }
    }

    public boolean isMiscSettingSelected(String setting) {
        return selectedMiscSettings.contains(setting);
    }

    public Set<String> getMiscSettings() {
        return new HashSet<>(selectedMiscSettings);
    }

    // Custom settings storage
    public void setCustomSetting(String key, Object value) {
        customSettings.put(key, value);
    }

    public Object getCustomSetting(String key) {
        return customSettings.get(key);
    }

    // Configuration summary for final page
    public WizardConfiguration getConfiguration() {
        return new WizardConfiguration(
                new ArrayList<>(selectedResourcePacksOrdered), // Use ordered list instead
                selectedTabDesign,
                new HashSet<>(selectedMiscSettings),
                new HashMap<>(customSettings)
        );
    }

    // Reset all data
    public void reset() {
        selectedOptimizationProfile = "";
        selectedTabDesign = "";
        selectedMiscSettings.clear();
        customSettings.clear();
        selectedResourcePacksOrdered.clear(); // NEW: Clear resource packs
    }

    // Check if configuration is complete
    public boolean isConfigurationComplete() {
        return !selectedResourcePacksOrdered.isEmpty() && !selectedTabDesign.isEmpty();
    }

    // Get configuration summary text
    public String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Resource Packs: ").append(selectedResourcePacksOrdered.isEmpty() ? "None" : String.join(", ", selectedResourcePacksOrdered)).append("\n");
        summary.append("Tab Design: ").append(selectedTabDesign.isEmpty() ? "None" : selectedTabDesign).append("\n");
        summary.append("Misc Settings: ").append(selectedMiscSettings.isEmpty() ? "None" : String.join(", ", selectedMiscSettings));
        return summary.toString();
    }

    public static class WizardConfiguration {
        private final List<String> resourcePacksOrdered; // CHANGED: Now a List instead of String
        private final String tabDesign;
        private final Set<String> miscSettings;
        private final Map<String, Object> customSettings;

        public WizardConfiguration(List<String> resourcePacksOrdered, String tabDesign,
                                   Set<String> miscSettings, Map<String, Object> customSettings) {
            this.resourcePacksOrdered = resourcePacksOrdered;
            this.tabDesign = tabDesign;
            this.miscSettings = miscSettings;
            this.customSettings = customSettings;
        }

        // DEPRECATED: Keep for backward compatibility
        @Deprecated
        public String getOptimizationProfile() {
            return String.join(",", resourcePacksOrdered);
        }

        // NEW: Proper method names
        public List<String> getResourcePacksOrdered() {
            return new ArrayList<>(resourcePacksOrdered);
        }

        public String getTabDesign() { return tabDesign; }
        public Set<String> getMiscSettings() { return miscSettings; }
        public Map<String, Object> getCustomSettings() { return customSettings; }
    }
}