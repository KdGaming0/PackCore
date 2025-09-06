package com.github.kd_gaming1.packcore.gui.help;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.util.config.ResourcePackUtil;
import com.github.kd_gaming1.packcore.util.config.PerformanceProfileUtil;
import com.github.kd_gaming1.packcore.util.config.TabDesignUtil;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for applying all wizard configurations
 */
public class ConfigurationApplicationService {

    public interface ProgressCallback {
        void updateProgress(String stepKey, String status, String errorMessage);
    }

    public static class ConfigurationResult {
        private final boolean overallSuccess;
        private final Map<String, String> failedSteps;
        private final Map<String, String> successfulSteps;

        public ConfigurationResult(boolean overallSuccess, Map<String, String> failedSteps, Map<String, String> successfulSteps) {
            this.overallSuccess = overallSuccess;
            this.failedSteps = failedSteps;
            this.successfulSteps = successfulSteps;
        }

        public boolean isOverallSuccess() { return overallSuccess; }
        public Map<String, String> getFailedSteps() { return failedSteps; }
        public Map<String, String> getSuccessfulSteps() { return successfulSteps; }
    }

    public static CompletableFuture<Boolean> applyAllConfigurations() {
        return applyAllConfigurationsWithProgress(null).thenApply(ConfigurationResult::isOverallSuccess);
    }

    public static CompletableFuture<ConfigurationResult> applyAllConfigurationsWithProgress(ProgressCallback progressCallback) {
        WizardDataManager dataManager = WizardDataManager.getInstance();

        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> failedSteps = new LinkedHashMap<>();
            Map<String, String> successfulSteps = new LinkedHashMap<>();

            try {
                PackCore.LOGGER.info("Starting comprehensive configuration application");

                // Step 1: Apply Performance Profile (Optimization Profile)
                String optimizationProfile = dataManager.getOptimizationProfile();
                if (!optimizationProfile.isEmpty()) {
                    if (progressCallback != null) {
                        progressCallback.updateProgress("performance", "running", null);
                    }

                    boolean performanceApplied = applyPerformanceProfile(optimizationProfile);
                    if (!performanceApplied) {
                        String error = "Failed to apply performance profile: " + optimizationProfile;
                        PackCore.LOGGER.warn(error);
                        failedSteps.put("Performance Profile", error);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("performance", "error", "Application failed");
                        }
                    } else {
                        String success = "Successfully applied performance profile: " + optimizationProfile;
                        PackCore.LOGGER.info(success);
                        successfulSteps.put("Performance Profile", success);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("performance", "success", null);
                        }
                    }
                }

                // Step 2: Apply Resource Packs (separately from performance)
                List<String> resourcePacks = dataManager.getResourcePacksOrdered();
                if (!resourcePacks.isEmpty()) {
                    if (progressCallback != null) {
                        progressCallback.updateProgress("resourcepacks", "running", null);
                    }

                    try {
                        boolean resourcePacksApplied = ResourcePackUtil.applyResourcePacksOrdered(resourcePacks)
                                .exceptionally(ex -> {
                                    PackCore.LOGGER.error("Exception while applying resource packs", ex);
                                    failedSteps.put("Resource Packs", "Exception: " + ex.getMessage());
                                    return false;
                                }).join();

                        if (!resourcePacksApplied) {
                            String error = "Failed to apply resource packs: " + resourcePacks;
                            PackCore.LOGGER.warn(error);
                            failedSteps.put("Resource Packs", error);
                            if (progressCallback != null) {
                                progressCallback.updateProgress("resourcepacks", "error", "Application failed");
                            }
                        } else {
                            String success = "Successfully applied resource packs: " + resourcePacks;
                            PackCore.LOGGER.info(success);
                            successfulSteps.put("Resource Packs", success);
                            if (progressCallback != null) {
                                progressCallback.updateProgress("resourcepacks", "success", null);
                            }
                        }
                    } catch (Exception e) {
                        String error = "Exception while applying resource packs: " + e.getMessage();
                        PackCore.LOGGER.error(error, e);
                        failedSteps.put("Resource Packs", error);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("resourcepacks", "error", "Exception occurred");
                        }
                    }
                }

                // Step 3: Apply Tab Design
                String tabDesign = dataManager.getTabDesign();
                if (!tabDesign.isEmpty()) {
                    if (progressCallback != null) {
                        progressCallback.updateProgress("tabdesign", "running", null);
                    }

                    boolean tabDesignApplied = TabDesignUtil.applyTabDesignFromWizard();
                    if (!tabDesignApplied) {
                        String error = "Failed to apply tab design: " + tabDesign;
                        PackCore.LOGGER.warn(error);
                        failedSteps.put("Tab Design", error);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("tabdesign", "error", "Application failed");
                        }
                    } else {
                        String success = "Successfully applied tab design: " + tabDesign;
                        PackCore.LOGGER.info(success);
                        successfulSteps.put("Tab Design", success);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("tabdesign", "success", null);
                        }
                    }
                }

                // Step 4: Apply Additional Settings
                Set<String> additionalSettings = dataManager.getAdditionalSettings();
                if (!additionalSettings.isEmpty()) {
                    if (progressCallback != null) {
                        progressCallback.updateProgress("additional", "running", null);
                    }

                    boolean additionalSettingsApplied = applyAdditionalSettings(additionalSettings);
                    if (!additionalSettingsApplied) {
                        String error = "Failed to apply some additional settings: " + additionalSettings;
                        PackCore.LOGGER.warn(error);
                        failedSteps.put("Additional Settings", error);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("additional", "error", "Some settings failed");
                        }
                    } else {
                        String success = "Successfully applied additional settings: " + additionalSettings;
                        PackCore.LOGGER.info(success);
                        successfulSteps.put("Additional Settings", success);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("additional", "success", null);
                        }
                    }
                }

                boolean overallSuccess = failedSteps.isEmpty();
                PackCore.LOGGER.info("Configuration application completed with overall success: {}", overallSuccess);

                if (!overallSuccess) {
                    PackCore.LOGGER.warn("Failed steps: {}", failedSteps);
                }

                return new ConfigurationResult(overallSuccess, failedSteps, successfulSteps);

            } catch (Exception e) {
                PackCore.LOGGER.error("Fatal error during configuration application", e);
                failedSteps.put("Fatal Error", "Unexpected exception: " + e.getMessage());
                return new ConfigurationResult(false, failedSteps, successfulSteps);
            }
        });
    }

    private static boolean applyPerformanceProfile(String optimizationProfile) {
        try {
            PerformanceProfileUtil.PerformanceProfile profile = mapToPerformanceProfile(optimizationProfile);
            if (profile != null) {
                PerformanceProfileUtil.ProfileResult result = PerformanceProfileUtil.applyPerformanceProfile(profile);
                return result.isFullySuccessful();
            } else {
                PackCore.LOGGER.warn("Unknown optimization profile: {}", optimizationProfile);
                return false;
            }
        } catch (Exception e) {
            PackCore.LOGGER.error("Failed to apply performance profile: " + optimizationProfile, e);
            return false;
        }
    }

    private static boolean applyAdditionalSettings(Set<String> additionalSettings) {
        try {
            boolean allSuccessful = true;
            for (String setting : additionalSettings) {
                boolean applied = applySingleAdditionalSetting(setting);
                if (!applied) {
                    PackCore.LOGGER.warn("Failed to apply additional setting: {}", setting);
                    allSuccessful = false;
                } else {
                    PackCore.LOGGER.info("Applied additional setting: {}", setting);
                }
            }
            return allSuccessful;
        } catch (Exception e) {
            PackCore.LOGGER.error("Failed to apply additional settings", e);
            return false;
        }
    }

    private static boolean applySingleAdditionalSetting(String setting) {
        // Implement specific setting application logic here
        // For now, just log and return true
        PackCore.LOGGER.info("Applying additional setting: {}", setting);

        // Example implementations:
        switch (setting.toLowerCase()) {
            case "enable_chat_timestamps" -> {
                // Apply chat timestamp setting
                return true;
            }
            case "auto_reconnect" -> {
                // Apply auto-reconnect setting
                return true;
            }
            case "performance_mode" -> {
                // Apply performance mode setting
                return true;
            }
            default -> {
                PackCore.LOGGER.warn("Unknown additional setting: {}", setting);
                return false;
            }
        }
    }

    private static PerformanceProfileUtil.PerformanceProfile mapToPerformanceProfile(String optimizationProfile) {
        return switch (optimizationProfile.toLowerCase()) {
            case "max fps" -> PerformanceProfileUtil.PerformanceProfile.PERFORMANCE;
            case "balanced" -> PerformanceProfileUtil.PerformanceProfile.BALANCED;
            case "quality", "shaders" -> PerformanceProfileUtil.PerformanceProfile.QUALITY;
            default -> null;
        };
    }
}