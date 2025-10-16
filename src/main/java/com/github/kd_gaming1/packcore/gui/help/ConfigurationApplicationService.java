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

    public record ConfigurationResult(boolean overallSuccess, Map<String, String> failedSteps,
                                      Map<String, String> successfulSteps) {
    }

    public static CompletableFuture<Boolean> applyAllConfigurations() {
        return applyAllConfigurationsWithProgress(null).thenApply(ConfigurationResult::overallSuccess);
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
                        String error = "Could not apply the '" + optimizationProfile + "' performance profile. The settings may be incompatible or already active.";
                        PackCore.LOGGER.warn(error);
                        failedSteps.put("Performance Settings", error);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("performance", "error", "Setup failed");
                        }
                    } else {
                        String success = "Successfully configured performance to '" + optimizationProfile + "' profile";
                        PackCore.LOGGER.info(success);
                        successfulSteps.put("Performance Settings", success);
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
                                    String userFriendlyError = "Failed to enable resource packs. They may be missing or corrupted.";
                                    failedSteps.put("Resource Packs", userFriendlyError);
                                    return false;
                                }).join();

                        if (!resourcePacksApplied) {
                            String error = "Could not enable the selected resource packs: " + String.join(", ", resourcePacks) + ". They may already be active or unavailable.";
                            PackCore.LOGGER.warn(error);
                            failedSteps.put("Resource Packs", error);
                            if (progressCallback != null) {
                                progressCallback.updateProgress("resourcepacks", "error", "Activation failed");
                            }
                        } else {
                            String success = "Successfully enabled " + resourcePacks.size() + " resource pack(s)";
                            PackCore.LOGGER.info(success);
                            successfulSteps.put("Resource Packs", success);
                            if (progressCallback != null) {
                                progressCallback.updateProgress("resourcepacks", "success", null);
                            }
                        }
                    } catch (Exception e) {
                        String error = "An error occurred while trying to enable resource packs. Check that they're properly installed.";
                        PackCore.LOGGER.error(error, e);
                        failedSteps.put("Resource Packs", error);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("resourcepacks", "error", "Error occurred");
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
                        String error = "Could not apply the '" + tabDesign + "' tab menu style. The theme may be unavailable.";
                        PackCore.LOGGER.warn(error);
                        failedSteps.put("Tab Menu Style", error);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("tabdesign", "error", "Theme unavailable");
                        }
                    } else {
                        String success = "Successfully applied '" + tabDesign + "' tab menu style";
                        PackCore.LOGGER.info(success);
                        successfulSteps.put("Tab Menu Style", success);
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
                        String error = "Some extra settings could not be applied. They may conflict with existing settings.";
                        PackCore.LOGGER.warn(error);
                        failedSteps.put("Extra Settings", error);
                        if (progressCallback != null) {
                            progressCallback.updateProgress("additional", "error", "Some failed");
                        }
                    } else {
                        String success = "Successfully applied all extra settings";
                        PackCore.LOGGER.info(success);
                        successfulSteps.put("Extra Settings", success);
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
                failedSteps.put("Setup Error", "An unexpected problem occurred during setup. Please try again or skip this step.");
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
                PackCore.LOGGER.warn("Unrecognized performance profile: {}", optimizationProfile);
                return false;
            }
        } catch (Exception e) {
            PackCore.LOGGER.error("Failed to apply performance profile: {}", optimizationProfile, e);
            return false;
        }
    }

    private static boolean applyAdditionalSettings(Set<String> additionalSettings) {
        try {
            boolean allSuccessful = true;
            for (String setting : additionalSettings) {
                boolean applied = applySingleAdditionalSetting(setting);
                if (!applied) {
                    PackCore.LOGGER.warn("Could not apply extra setting: {}", setting);
                    allSuccessful = false;
                } else {
                    PackCore.LOGGER.info("Successfully applied extra setting: {}", setting);
                }
            }
            return allSuccessful;
        } catch (Exception e) {
            PackCore.LOGGER.error("Error while applying extra settings", e);
            return false;
        }
    }

    private static boolean applySingleAdditionalSetting(String setting) {
        // Implement specific setting application logic here
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
                PackCore.LOGGER.warn("Unknown extra setting: {}", setting);
                return false;
            }
        }
    }

    private static PerformanceProfileUtil.PerformanceProfile mapToPerformanceProfile(String optimizationProfile) {
        return switch (optimizationProfile.toLowerCase()) {
            case "max fps" -> PerformanceProfileUtil.PerformanceProfile.PERFORMANCE;
            case "balanced" -> PerformanceProfileUtil.PerformanceProfile.BALANCED;
            case "quality" -> PerformanceProfileUtil.PerformanceProfile.QUALITY;
            case "shaders" -> PerformanceProfileUtil.PerformanceProfile.SHADERS;
            default -> null;
        };
    }
}