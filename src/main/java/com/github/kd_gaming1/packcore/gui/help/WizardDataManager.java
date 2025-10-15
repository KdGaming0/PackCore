package com.github.kd_gaming1.packcore.gui.help;

import com.github.kd_gaming1.packcore.PackCore;
import java.util.*;

/**
 * Singleton class to manage wizard data and state.
 * This class stores user selections from the wizard pages,
 * tracks application state, and provides methods to retrieve
 * a summary of the configuration.
 */
public class WizardDataManager {
    private static WizardDataManager instance;

    // Core selections from wizard pages
    private String optimizationProfile = "";
    private final List<String> resourcePacksOrdered = new ArrayList<>();
    private String tabDesign = "";

    // Application tracking
    private boolean configurationApplied = false;
    private boolean configurationApplying = false;
    private String lastError = "";

    private WizardDataManager() {}

    public static WizardDataManager getInstance() {
        if (instance == null) {
            instance = new WizardDataManager();
        }
        return instance;
    }

    // ===== Optimization Profile (Page 1) =====

    public void setOptimizationProfile(String profile) {
        this.optimizationProfile = profile;
        PackCore.LOGGER.debug("Set optimization profile: {}", profile);
    }

    public String getOptimizationProfile() {
        return optimizationProfile;
    }

    // ===== Resource Packs (Page 3) =====

    public void setResourcePacksOrdered(List<String> packs) {
        this.resourcePacksOrdered.clear();
        this.resourcePacksOrdered.addAll(packs);
        PackCore.LOGGER.debug("Set resource packs: {}", packs);
    }

    public List<String> getResourcePacksOrdered() {
        return new ArrayList<>(resourcePacksOrdered);
    }

    // ===== Tab Design (Page 2) =====

    public void setTabDesign(String design) {
        this.tabDesign = design;
        PackCore.LOGGER.debug("Set tab design: {}", design);
    }

    public String getTabDesign() {
        return tabDesign;
    }

    // ===== Application State =====

    public void setConfigurationApplying(boolean applying) {
        this.configurationApplying = applying;
    }

    public boolean isConfigurationApplying() {
        return configurationApplying;
    }

    public void setConfigurationApplied(boolean applied) {
        this.configurationApplied = applied;
    }

    public boolean isConfigurationApplied() {
        return configurationApplied;
    }

    public void setConfigurationResult(String result, String errorMessage) {
        this.lastError = errorMessage != null ? errorMessage : "";
        PackCore.LOGGER.debug("Configuration result: {} - {}", result, errorMessage);
    }

    public String getConfigurationResult() {
        return configurationApplied ? "success" : (!lastError.isEmpty() ? "failed" : "");
    }

    public String getConfigurationErrorMessage() {
        return lastError;
    }

    // ===== Additional Settings (unused but kept for compatibility) =====

    public Set<String> getAdditionalSettings() {
        return new HashSet<>(); // Return empty set for compatibility
    }

    // ===== Configuration Summary =====

    public WizardConfiguration getConfiguration() {
        return new WizardConfiguration(
                optimizationProfile,
                new ArrayList<>(resourcePacksOrdered),
                tabDesign
        );
    }

    public boolean isConfigurationComplete() {
        // At minimum we need an optimization profile
        return !optimizationProfile.isEmpty();
    }

    // ===== Reset =====

    public void reset() {
        optimizationProfile = "";
        resourcePacksOrdered.clear();
        tabDesign = "";
        configurationApplied = false;
        configurationApplying = false;
        lastError = "";
        PackCore.LOGGER.info("Wizard data reset");
    }

    public static void clearInstance() {
        instance = null;
    }

    // ===== Data Class =====

    public record WizardConfiguration(String optimizationProfile, List<String> resourcePacksOrdered, String tabDesign) {

        @Override
        public List<String> resourcePacksOrdered() {
                return new ArrayList<>(resourcePacksOrdered);
            }

            // For compatibility with existing code
            public Set<String> getAdditionalSettings() {
                return new HashSet<>();
            }

            public Map<String, Object> getCustomSettings() {
                return new HashMap<>();
            }
        }
}