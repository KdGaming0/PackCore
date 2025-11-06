package com.github.kd_gaming1.packcore.util.wizard;

import com.github.kd_gaming1.packcore.PackCore;
import java.util.*;

/**
 * Manages wizard configuration data and application state.
 * Uses a simple singleton pattern for data persistence across wizard pages.
 */
public class WizardDataStore {

    private static final WizardDataStore INSTANCE = new WizardDataStore();

    // Configuration selections
    private final WizardData data = new WizardData();

    // Application state
    private ApplicationState appState = new ApplicationState();

    private WizardDataStore() {}

    /**
     * Get the singleton instance
     */
    public static WizardDataStore getInstance() {
        return INSTANCE;
    }

    // ===== Configuration Getters/Setters =====

    public void setOptimizationProfile(String profile) {
        data.optimizationProfile = profile;
        logUpdate("optimization profile", profile);
    }

    public String getOptimizationProfile() {
        return data.optimizationProfile;
    }

    public void setTabDesign(String design) {
        data.tabDesign = design;
        logUpdate("tab design", design);
    }

    public String getTabDesign() {
        return data.tabDesign;
    }

    public void setItemBackground(String background) {
        data.itemBackground = background;
        logUpdate("item background", background);
    }

    public String getItemBackground() {
        return data.itemBackground;
    }

    public void setResourcePacksOrdered(List<String> packs) {
        data.resourcePacksOrdered.clear();
        data.resourcePacksOrdered.addAll(packs);
        logUpdate("resource packs", packs.toString());
    }

    public List<String> getResourcePacksOrdered() {
        return new ArrayList<>(data.resourcePacksOrdered);
    }

    public Set<String> getAdditionalSettings() {
        return new HashSet<>(); // Placeholder for compatibility
    }

    // ===== Application State =====

    public void setApplicationState(boolean applying, boolean applied, String error) {
        appState = new ApplicationState(applying, applied, error);
    }

    public boolean isConfigurationApplying() {
        return appState.applying;
    }

    public void setConfigurationApplying(boolean applying) {
        appState = appState.withApplying(applying);
    }

    public boolean isConfigurationApplied() {
        return appState.applied;
    }

    public void setConfigurationApplied(boolean applied) {
        appState = appState.withApplied(applied);
    }

    public void setConfigurationResult(String result, String errorMessage) {
        appState = appState.withError(errorMessage != null ? errorMessage : "");
        logUpdate("configuration result", result + " - " + errorMessage);
    }

    public String getConfigurationResult() {
        return appState.applied ? "success" : (!appState.error.isEmpty() ? "failed" : "");
    }

    public String getConfigurationErrorMessage() {
        return appState.error;
    }

    // ===== Summary Methods =====

    /**
     * Get the complete configuration
     */
    public WizardConfiguration getConfiguration() {
        return new WizardConfiguration(
                data.optimizationProfile,
                new ArrayList<>(data.resourcePacksOrdered),
                data.tabDesign,
                data.itemBackground
        );
    }

    /**
     * Check if minimum configuration is complete
     */
    public boolean isConfigurationComplete() {
        return !data.optimizationProfile.isEmpty();
    }

    /**
     * Reset all data to defaults
     */
    public void reset() {
        data.clear();
        appState = new ApplicationState();
        PackCore.LOGGER.info("Wizard data reset");
    }

    /**
     * Get a human-readable summary of the configuration
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        if (!data.optimizationProfile.isEmpty()) {
            sb.append("Performance: ").append(data.optimizationProfile);
        }

        if (!data.tabDesign.isEmpty()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("Tab: ").append(data.tabDesign);
        }

        if (!data.itemBackground.isEmpty()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("Items: ").append(data.itemBackground);
        }

        if (!data.resourcePacksOrdered.isEmpty()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("Packs: ").append(String.join(", ", data.resourcePacksOrdered));
        }

        return sb.toString();
    }

    private void logUpdate(String field, String value) {
        PackCore.LOGGER.debug("Set {}: {}", field, value);
    }

    /**
     * Internal data storage class
     */
    private static class WizardData {
        String optimizationProfile = "";
        String tabDesign = "";
        String itemBackground = "";
        final List<String> resourcePacksOrdered = new ArrayList<>();

        void clear() {
            optimizationProfile = "";
            tabDesign = "";
            itemBackground = "";
            resourcePacksOrdered.clear();
        }
    }

    /**
     * Immutable application state
     */
    private record ApplicationState(
            boolean applying,
            boolean applied,
            String error
    ) {
        ApplicationState() {
            this(false, false, "");
        }

        ApplicationState withApplying(boolean applying) {
            return new ApplicationState(applying, this.applied, this.error);
        }

        ApplicationState withApplied(boolean applied) {
            return new ApplicationState(this.applying, applied, this.error);
        }

        ApplicationState withError(String error) {
            return new ApplicationState(this.applying, this.applied, error);
        }
    }

    /**
     * Configuration record for external use
     */
    public record WizardConfiguration(
            String optimizationProfile,
            List<String> resourcePacksOrdered,
            String tabDesign,
            String itemBackground
    ) {

        @Override
        public List<String> resourcePacksOrdered() {
            return new ArrayList<>(resourcePacksOrdered);
        }

        // Compatibility methods
        public Set<String> getAdditionalSettings() {
            return new HashSet<>();
        }

        public Map<String, Object> getCustomSettings() {
            return new HashMap<>();
        }
    }
}