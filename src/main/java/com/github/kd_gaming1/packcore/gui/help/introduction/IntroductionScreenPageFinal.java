package com.github.kd_gaming1.packcore.gui.help.introduction;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.help.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.help.ConfigurationApplicationService;
import com.github.kd_gaming1.packcore.gui.help.WizardDataManager;
import com.github.kd_gaming1.packcore.gui.titlescreen.fancy.FancyMainMenuScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

public class IntroductionScreenPageFinal extends BaseWizardPage {

    private final WizardDataManager dataManager;
    private LabelComponent statusLabel;
    private ButtonComponent applyButton;
    private FlowLayout progressContainer;
    private Map<String, LabelComponent> stepLabels = new LinkedHashMap<>();

    public IntroductionScreenPageFinal() {
        super(
                new WizardPageInfo(
                        Text.literal("Apply Configuration"),
                        5,
                        5
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        this.dataManager = WizardDataManager.getInstance();
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        contentContainer.gap(4);

        // Header
        contentContainer.child(createHeader());

        // Configuration summary
        contentContainer.child(createConfigurationSummary());

        // Progress section
        contentContainer.child(createProgressSection());

        // Status section
        contentContainer.child(createStatusSection());

        // Apply buttons
        contentContainer.child(createActionButtons());

        // Initialize UI state based on stored data
        initializeUIState();
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {
        // Show helpful information
        contentContainerRight.child(createHelpSection());
    }

    private void initializeUIState() {
        // Restore UI state from persistent data
        if (dataManager.isConfigurationApplying()) {
            // Show progress section if currently applying
            progressContainer.positioning(Positioning.layout());
            updateApplyButtonState(true, "Applying...");
            updateStatusLabel("⏳ Applying configuration...", Formatting.YELLOW);

            // Disable continue while applying
            updatePrimaryButtonState(false);
        } else if (dataManager.isConfigurationApplied()) {
            // Show appropriate state based on result
            String result = dataManager.getConfigurationResult();
            if ("success".equals(result)) {
                onConfigurationApplied();
            } else if ("failed".equals(result)) {
                onConfigurationFailed(null, new RuntimeException(dataManager.getConfigurationErrorMessage()));
            }

            // Enable continue only if applied successfully
            updatePrimaryButtonState(dataManager.isConfigurationApplied());
        } else {
            // Not applied -> keep continue disabled
            updatePrimaryButtonState(false);
        }
    }

    private FlowLayout createHeader() {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        LabelComponent title = (LabelComponent) Components.label(
                Text.literal("Ready to Apply Configuration!")
                        .setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        ).horizontalSizing(Sizing.fill(98)).margins(Insets.of(2));

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("Review your selections below and click 'Apply Configuration' to activate your chosen settings.")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).horizontalSizing(Sizing.fill(98)).margins(Insets.of(2));

        header.child(title).child(subtitle);
        return header;
    }

    private FlowLayout createConfigurationSummary() {
        FlowLayout summaryContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .surface(Surface.flat(0x20_FFD700).and(Surface.outline(ACCENT_GOLD)))
                .padding(Insets.of(6));

        // Configuration summary title
        summaryContainer.child(Components.label(
                Text.literal("Your Configuration Summary:")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).color(Color.ofRgb(ACCENT_GOLD))).horizontalSizing(Sizing.fill(98));

        // Optimization Profile (Performance Settings)
        String optimizationProfile = dataManager.getOptimizationProfile();
        summaryContainer.child(createSummaryItem("Performance Profile:",
                optimizationProfile.isEmpty() ? "Default" : optimizationProfile));

        // Resource packs with proper ordering display
        List<String> resourcePacks = dataManager.getResourcePacksOrdered();
        if (!resourcePacks.isEmpty()) {
            summaryContainer.child(createSummaryItem("Resource Packs (in load order):", ""));
            for (int i = 0; i < resourcePacks.size(); i++) {
                summaryContainer.child(Components.label(
                        Text.literal("  " + (i + 1) + ". " + resourcePacks.get(i))
                ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.left(16)));
            }
        } else {
            summaryContainer.child(createSummaryItem("Resource Packs:", "None selected"));
        }

        // Tab design
        String tabDesign = dataManager.getTabDesign();
        summaryContainer.child(createSummaryItem("Tab Design:",
                tabDesign.isEmpty() ? "Default" : tabDesign));

        return summaryContainer;
    }

    private FlowLayout createSummaryItem(String label, String value) {
        FlowLayout item = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .margins(Insets.vertical(2));

        item.child(Components.label(Text.literal(label))
                .color(Color.ofRgb(TEXT_WHITE)));

        LabelComponent valueLabel = (LabelComponent) Components.label(Text.literal(value))
                .color(Color.ofRgb(TEXT_SECONDARY))
                .horizontalSizing(Sizing.expand());

        item.child(valueLabel);

        return item;
    }

    private FlowLayout createProgressSection() {
        progressContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(3)
                .surface(Surface.flat(0x20_000000).and(Surface.outline(0x40_FFFFFF)))
                .padding(Insets.of(6));

        // Progress title
        progressContainer.child(Components.label(
                Text.literal("Configuration Progress:")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).color(Color.ofRgb(ACCENT_GOLD)));

        // Initialize step labels based on what configurations are selected
        initializeProgressSteps();

        // Initially hide progress section unless we're currently applying
        if (!dataManager.isConfigurationApplying()) {
            progressContainer.positioning(Positioning.absolute(0, -1000));
        }

        return progressContainer;
    }

    private void initializeProgressSteps() {
        stepLabels.clear();

        // Add steps based on selected configurations
        String optimizationProfile = dataManager.getOptimizationProfile();
        if (!optimizationProfile.isEmpty()) {
            LabelComponent stepLabel = createProgressStepLabel("Performance Profile", "pending");
            stepLabels.put("performance", stepLabel);
            progressContainer.child(stepLabel);
        }

        List<String> resourcePacks = dataManager.getResourcePacksOrdered();
        if (!resourcePacks.isEmpty()) {
            LabelComponent stepLabel = createProgressStepLabel("Resource Packs", "pending");
            stepLabels.put("resourcepacks", stepLabel);
            progressContainer.child(stepLabel);
        }

        String tabDesign = dataManager.getTabDesign();
        if (!tabDesign.isEmpty()) {
            LabelComponent stepLabel = createProgressStepLabel("Tab Design", "pending");
            stepLabels.put("tabdesign", stepLabel);
            progressContainer.child(stepLabel);
        }

        Set<String> additionalSettings = dataManager.getAdditionalSettings();
        if (!additionalSettings.isEmpty()) {
            LabelComponent stepLabel = createProgressStepLabel("Additional Settings", "pending");
            stepLabels.put("additional", stepLabel);
            progressContainer.child(stepLabel);
        }
    }

    private LabelComponent createProgressStepLabel(String stepName, String status) {
        String icon = switch (status) {
            case "success" -> "✅";
            case "error" -> "❌";
            case "running" -> "⏳";
            default -> "⏸";
        };

        Formatting color = switch (status) {
            case "success" -> Formatting.GREEN;
            case "error" -> Formatting.RED;
            case "running" -> Formatting.YELLOW;
            default -> Formatting.GRAY;
        };

        return (LabelComponent) Components.label(
                Text.literal(icon + " " + stepName)
                        .setStyle(Style.EMPTY.withColor(color))
        ).margins(Insets.left(8));
    }

    private void updateProgressStep(String stepKey, String status, String errorMessage) {
        MinecraftClient.getInstance().execute(() -> {
            LabelComponent stepLabel = stepLabels.get(stepKey);
            if (stepLabel != null) {
                String stepName = switch (stepKey) {
                    case "performance" -> "Performance Profile";
                    case "resourcepacks" -> "Resource Packs";
                    case "tabdesign" -> "Tab Design";
                    case "additional" -> "Additional Settings";
                    default -> "Unknown Step";
                };

                String icon = switch (status) {
                    case "success" -> "✅";
                    case "error" -> "❌";
                    case "running" -> "⏳";
                    default -> "⏸";
                };

                Formatting color = switch (status) {
                    case "success" -> Formatting.GREEN;
                    case "error" -> Formatting.RED;
                    case "running" -> Formatting.YELLOW;
                    default -> Formatting.GRAY;
                };

                String displayText = icon + " " + stepName;
                if (status.equals("error") && errorMessage != null && !errorMessage.isEmpty()) {
                    displayText += " - " + errorMessage;
                }

                stepLabel.text(Text.literal(displayText).setStyle(Style.EMPTY.withColor(color)));
            }
        });
    }

    private FlowLayout createStatusSection() {
        FlowLayout statusContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .margins(Insets.vertical(8));

        // Status label - initial text will be updated based on stored state
        statusLabel = (LabelComponent) Components.label(Text.literal("Click 'Apply Configuration' to begin...")
                        .setStyle(Style.EMPTY.withItalic(Boolean.TRUE)))
                .color(Color.ofRgb(TEXT_SECONDARY)).horizontalSizing(Sizing.fill(98)).margins(Insets.of(2));

        statusContainer.child(statusLabel);

        return statusContainer;
    }

    private FlowLayout createActionButtons() {
        FlowLayout buttonContainer = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.top(16))
                .positioning(Positioning.relative(50, 100));

        // Apply configuration button
        applyButton = (ButtonComponent) Components.button(
                        Text.literal("Apply Configuration"),
                        this::onApplyPressed
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 130, 66))
                .horizontalSizing(Sizing.fixed(130))
                .verticalSizing(Sizing.fixed(22));

        buttonContainer.child(applyButton);

        return buttonContainer;
    }

    private FlowLayout createHelpSection() {
        FlowLayout helpContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .surface(Surface.flat(0x20_000000).and(Surface.outline(0x40_FFFFFF)))
                .padding(Insets.of(8))
                .margins(Insets.of(5, 0, 0, 0));

        helpContainer.child(Components.label(
                Text.literal("What happens next?")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).color(Color.ofRgb(ACCENT_GOLD)));

        helpContainer.child(Components.label(
                Text.literal("1. Performance settings applied")
        ).color(Color.ofRgb(TEXT_WHITE)));

        helpContainer.child(Components.label(
                Text.literal("2. Resource packs activated")
        ).color(Color.ofRgb(TEXT_WHITE)));

        helpContainer.child(Components.label(
                Text.literal("3. Tab design configured")
        ).color(Color.ofRgb(TEXT_WHITE)));

        helpContainer.child(Components.label(
                Text.literal("4. Additional settings applied")
        ).color(Color.ofRgb(TEXT_WHITE)));

        helpContainer.child(Components.label(
                Text.literal("5. Settings saved automatically")
        ).color(Color.ofRgb(TEXT_WHITE)));

        helpContainer.child(Components.label(
                Text.literal("Note: Each step will show progress indicators. If something fails, you'll see exactly which step had issues!")
                        .setStyle(Style.EMPTY.withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).horizontalSizing(Sizing.fill(100)).margins(Insets.of(6, 2, 2 ,2)));

        return helpContainer;
    }

    private void onApplyPressed(ButtonComponent button) {
        if (dataManager.isConfigurationApplying() || dataManager.isConfigurationApplied()) {
            PackCore.LOGGER.debug("Apply pressed but already applying ({}) or has applied ({})",
                    dataManager.isConfigurationApplying(), dataManager.isConfigurationApplied());
            return;
        }

        PackCore.LOGGER.info("Starting configuration application process");

        // Update persistent state
        dataManager.setConfigurationApplying(true);
        dataManager.setConfigurationApplied(false);
        dataManager.setConfigurationResult("", "");

        // Show progress section
        progressContainer.positioning(Positioning.layout());

        // Update UI immediately on main thread
        updateApplyButtonState(true, "Applying...");
        updateStatusLabel("⏳ Applying configuration...", Formatting.YELLOW);

        // Apply configurations asynchronously with detailed progress
        ConfigurationApplicationService.applyAllConfigurationsWithProgress(this::updateProgressStep)
                .whenComplete((result, throwable) -> {
                    // Ensure UI updates happen on main thread
                    MinecraftClient.getInstance().execute(() -> {
                        // Update persistent state first
                        dataManager.setConfigurationApplying(false);

                        if (result.isOverallSuccess() && throwable == null) {
                            dataManager.setConfigurationApplied(true);
                            dataManager.setConfigurationResult("success", "");
                            onConfigurationApplied();
                        } else {
                            // Don't set configurationApplied to true on failure
                            dataManager.setConfigurationApplied(false);

                            // Build the error message here to store it
                            StringBuilder failureMessage = new StringBuilder("❌ Configuration failed: ");
                            if (result != null && !result.getFailedSteps().isEmpty()) {
                                failureMessage.append("\n");
                                for (Map.Entry<String, String> failure : result.getFailedSteps().entrySet()) {
                                    failureMessage.append("• ").append(failure.getKey()).append(": ").append(failure.getValue()).append("\n");
                                }
                                failureMessage.append("Check logs for detailed error information.");
                            } else if (throwable != null) {
                                failureMessage.append(throwable.getMessage() != null ? throwable.getMessage() : "Unknown error occurred");
                            } else {
                                failureMessage.append("Unknown error occurred");
                            }

                            dataManager.setConfigurationResult("failed", failureMessage.toString());
                            onConfigurationFailed(result, throwable);
                        }
                    });
                });
    }

    private void updateApplyButtonState(boolean isApplying, String message) {
        if (applyButton != null) {
            applyButton.setMessage(Text.literal(message));
            applyButton.active = !isApplying;
        }
    }

    private void updateStatusLabel(String message, Formatting color) {
        if (statusLabel != null) {
            statusLabel.text(Text.literal(message).setStyle(Style.EMPTY.withColor(color)));
        }
    }

    private void onConfigurationApplied() {
        PackCore.LOGGER.info("Wizard configuration applied successfully!");

        // Update UI to success state
        updateApplyButtonState(false, "✓ Applied Successfully");
        updateStatusLabel("✅ Configuration applied successfully! Your settings are now active.", Formatting.GREEN);

        // Enable continue now that configuration is applied
        updatePrimaryButtonState(true);

        // Mark wizard as completed
        PackCoreConfig.haveSetupWizardCompletedSuccessfully = true;

        // Create comprehensive configuration summary
        WizardDataManager.WizardConfiguration config = dataManager.getConfiguration();
        StringBuilder configSummary = new StringBuilder();

        if (!config.getOptimizationProfile().isEmpty()) {
            configSummary.append("Performance: ").append(config.getOptimizationProfile());
        }

        if (!config.getResourcePacksOrdered().isEmpty()) {
            if (configSummary.length() > 0) configSummary.append(", ");
            configSummary.append("Packs: ").append(String.join(", ", config.getResourcePacksOrdered()));
        }

        if (!config.getTabDesign().isEmpty()) {
            if (configSummary.length() > 0) configSummary.append(", ");
            configSummary.append("Tab: ").append(config.getTabDesign());
        }
    }

    private void onConfigurationFailed(ConfigurationApplicationService.ConfigurationResult result, Throwable throwable) {
        PackCore.LOGGER.error("Failed to apply wizard configuration", throwable);

        // Build detailed failure message
        StringBuilder failureMessage = new StringBuilder("❌ Configuration failed: ");

        if (result != null && !result.getFailedSteps().isEmpty()) {
            failureMessage.append("\n");
            for (Map.Entry<String, String> failure : result.getFailedSteps().entrySet()) {
                failureMessage.append("• ").append(failure.getKey()).append(": ").append(failure.getValue()).append("\n");
            }
            failureMessage.append("Check logs for detailed error information.");
        } else if (throwable != null) {
            failureMessage.append(throwable.getMessage() != null ? throwable.getMessage() : "Unknown error occurred");
        } else {
            failureMessage.append("Unknown error occurred");
        }

        // Update UI to error state - allow retry
        updateApplyButtonState(false, "Retry Configuration");
        updateStatusLabel(failureMessage.toString(), Formatting.RED);

        updatePrimaryButtonState(true);

        // Reset the application state to allow retry
        dataManager.setConfigurationApplied(false);
        dataManager.setConfigurationApplying(false);
        dataManager.setConfigurationResult("failed", failureMessage.toString());
    }

    @Override
    protected void onContinuePressed() {
        // Prevent continuing while configuration is being applied or has not been applied yet
        if (dataManager.isConfigurationApplying()) {
            updateStatusLabel("⏳ Configuration is still applying. Please wait...", Formatting.YELLOW);
            return;
        }

        if (!dataManager.isConfigurationApplied()) {
            updateStatusLabel("⚠️ Please apply configuration before continuing.", Formatting.RED);
            return;
        }

        // Proceed to main menu when configuration has finished successfully
        PackCoreConfig.haveShownWelcomeWizard = true;
        this.client.setScreen(new FancyMainMenuScreen());
    }

    @Override
    protected boolean isLastPage() {
        return true;
    }

    @Override
    protected boolean shouldShowStatusInfo() {
        return false;
    }

    @Override
    protected boolean shouldShowRightPanel() {
        return true;
    }

    @Override
    protected int getContentColumnWidthPercent() {
        return 60;
    }

    @Override
    protected int getContentColumnWidthRightPercent() {
        return 38;
    }
}