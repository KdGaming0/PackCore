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

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class IntroductionScreenPageFinal extends BaseWizardPage {

    private final WizardDataManager dataManager;
    private LabelComponent statusLabel;
    private ButtonComponent applyButton;
    private FlowLayout progressContainer;
    private FlowLayout warningBanner;
    private Map<String, LabelComponent> stepLabels = new LinkedHashMap<>();

    public IntroductionScreenPageFinal() {
        super(
                new WizardPageInfo(
                        Text.literal("Apply Configuration"),
                        5,
                        5
                ),
                Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png")
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

        // Warning banner (initially hidden)
        warningBanner = createWarningBanner();
        contentContainer.child(warningBanner);

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
            updateApplyButtonState(true, "Applying Settings...");
            updateStatusLabel("⏳ Please wait while we apply your settings...", Formatting.YELLOW);

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
                Text.literal("✨ Ready to Apply Your Settings!")
                        .setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        ).horizontalSizing(Sizing.fill(98)).margins(Insets.of(2));

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("Review your choices below. When you're ready, click 'Apply Settings' to activate everything.")
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
                Text.literal("📋 What You've Selected:")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).color(Color.ofRgb(ACCENT_GOLD))).horizontalSizing(Sizing.fill(98));

        // Optimization Profile (Performance Settings)
        String optimizationProfile = dataManager.getOptimizationProfile();
        summaryContainer.child(createSummaryItem("🚀 Performance Level:",
                optimizationProfile.isEmpty() ? "Default (no changes)" : optimizationProfile));

        // Resource packs with proper ordering display
        List<String> resourcePacks = dataManager.getResourcePacksOrdered();
        if (!resourcePacks.isEmpty()) {
            summaryContainer.child(createSummaryItem("🎨 Resource Packs (loading order):", ""));
            for (int i = 0; i < resourcePacks.size(); i++) {
                summaryContainer.child(Components.label(
                        Text.literal("  " + (i + 1) + ". " + resourcePacks.get(i))
                ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.left(16)));
            }
        } else {
            summaryContainer.child(createSummaryItem("🎨 Resource Packs:", "None selected"));
        }

        // Tab design
        String tabDesign = dataManager.getTabDesign();
        summaryContainer.child(createSummaryItem("🖼️ Tab Menu Style:",
                tabDesign.isEmpty() ? "Default (no changes)" : tabDesign));

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

    private FlowLayout createWarningBanner() {
        FlowLayout banner = (FlowLayout) Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(4)
                .surface(Surface.flat(0x30_FF8C00).and(Surface.outline(0xFF_FFA500)))
                .padding(Insets.of(8))
                .margins(Insets.vertical(4));

        // Initially hidden - will be shown when warnings occur
        banner.positioning(Positioning.absolute(0, -1000));

        return banner;
    }

    private void showWarningBanner(String title, String message) {
        MinecraftClient.getInstance().execute(() -> {
            warningBanner.clearChildren();

            // Warning title
            warningBanner.child(Components.label(
                    Text.literal("⚠️ " + title)
                            .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
            ).color(Color.ofRgb(0xFFA500)));

            // Warning message
            warningBanner.child(Components.label(
                    Text.literal(message)
            ).color(Color.ofRgb(TEXT_WHITE)).horizontalSizing(Sizing.fill(95)));

            // Show the banner
            warningBanner.positioning(Positioning.layout());
        });
    }

    private void hideWarningBanner() {
        MinecraftClient.getInstance().execute(() -> {
            warningBanner.positioning(Positioning.absolute(0, -1000));
        });
    }

    private FlowLayout createProgressSection() {
        progressContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(3)
                .surface(Surface.flat(0x20_000000).and(Surface.outline(0x40_FFFFFF)))
                .padding(Insets.of(6));

        // Progress title
        progressContainer.child(Components.label(
                Text.literal("⚙️ Applying Your Settings:")
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
            LabelComponent stepLabel = createProgressStepLabel("Performance Settings", "pending");
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
            LabelComponent stepLabel = createProgressStepLabel("Tab Menu Style", "pending");
            stepLabels.put("tabdesign", stepLabel);
            progressContainer.child(stepLabel);
        }

        Set<String> additionalSettings = dataManager.getAdditionalSettings();
        if (!additionalSettings.isEmpty()) {
            LabelComponent stepLabel = createProgressStepLabel("Extra Settings", "pending");
            stepLabels.put("additional", stepLabel);
            progressContainer.child(stepLabel);
        }
    }

    private LabelComponent createProgressStepLabel(String stepName, String status) {
        String icon = switch (status) {
            case "success" -> "✅";
            case "error" -> "❌";
            case "running" -> "⏳";
            default -> "⏸️";
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
                    case "performance" -> "Performance Settings";
                    case "resourcepacks" -> "Resource Packs";
                    case "tabdesign" -> "Tab Menu Style";
                    case "additional" -> "Extra Settings";
                    default -> "Unknown Step";
                };

                String icon = switch (status) {
                    case "success" -> "✅";
                    case "error" -> "❌";
                    case "running" -> "⏳";
                    default -> "⏸️";
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
        statusLabel = (LabelComponent) Components.label(Text.literal("👉 Ready to begin! Click 'Apply Settings' when you're ready.")
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
                        Text.literal("Apply Settings"),
                        this::onApplyPressed
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 130, 66))
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
                Text.literal("ℹ️ What Happens Next?")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).color(Color.ofRgb(ACCENT_GOLD)));

        helpContainer.child(Components.label(
                Text.literal("1. Performance settings will be adjusted")
        ).color(Color.ofRgb(TEXT_WHITE)).horizontalSizing(Sizing.fill(95)));

        helpContainer.child(Components.label(
                Text.literal("2. Resource packs will be enabled in order")
        ).color(Color.ofRgb(TEXT_WHITE)).horizontalSizing(Sizing.fill(95)));

        helpContainer.child(Components.label(
                Text.literal("3. Tab menu style will be configured")
        ).color(Color.ofRgb(TEXT_WHITE)).horizontalSizing(Sizing.fill(95)));

        helpContainer.child(Components.label(
                Text.literal("💡 Tip: Each step shows a progress indicator. If something fails, you'll see exactly what went wrong!")
                        .setStyle(Style.EMPTY.withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).horizontalSizing(Sizing.fill(95)).margins(Insets.of(6, 2, 2 ,2)));

        return helpContainer;
    }

    private void onApplyPressed(ButtonComponent button) {
        if (dataManager.isConfigurationApplying() || dataManager.isConfigurationApplied()) {
            PackCore.LOGGER.debug("Apply pressed but already applying ({}) or has applied ({})",
                    dataManager.isConfigurationApplying(), dataManager.isConfigurationApplied());
            return;
        }

        PackCore.LOGGER.info("Starting configuration application process");

        // Hide any previous warnings
        hideWarningBanner();

        // Update persistent state
        dataManager.setConfigurationApplying(true);
        dataManager.setConfigurationApplied(false);
        dataManager.setConfigurationResult("", "");

        // Show progress section
        progressContainer.positioning(Positioning.layout());

        // Update UI immediately on main thread
        updateApplyButtonState(true, "Applying...");
        updateStatusLabel("⏳ Please wait while we apply your settings. This may take a moment...", Formatting.YELLOW);

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
                            StringBuilder failureMessage = new StringBuilder();
                            if (!result.getFailedSteps().isEmpty()) {
                                for (Map.Entry<String, String> failure : result.getFailedSteps().entrySet()) {
                                    failureMessage.append("❌ ").append(failure.getKey()).append(":\n   ")
                                            .append(failure.getValue()).append("\n\n");
                                }
                            } else if (throwable != null) {
                                String errorMsg = throwable.getMessage() != null ? throwable.getMessage() : "An unexpected error occurred";
                                failureMessage.append(errorMsg);
                            } else {
                                failureMessage.append("An unexpected error occurred.");
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
            statusLabel.text(Text.literal(message).setStyle(Style.EMPTY.withColor(color))).horizontalSizing(Sizing.fill(100));
        }
    }

    private void onConfigurationApplied() {
        PackCore.LOGGER.info("Wizard configuration applied successfully!");

        // Update UI to success state
        updateApplyButtonState(false, "✅ All Done!");
        updateStatusLabel("✅ Success! All your settings have been applied. Click 'Continue' to start playing!", Formatting.GREEN);

        // Enable continue now that configuration is applied
        updatePrimaryButtonState(true);

        // Mark wizard as completed
        PackCoreConfig.haveShownWelcomeWizard = true;
        PackCoreConfig.write(MOD_ID);

        // Create comprehensive configuration summary
        WizardDataManager.WizardConfiguration config = dataManager.getConfiguration();
        StringBuilder configSummary = new StringBuilder();

        if (!config.getOptimizationProfile().isEmpty()) {
            configSummary.append("Performance: ").append(config.getOptimizationProfile());
        }

        if (!config.getResourcePacksOrdered().isEmpty()) {
            if (!configSummary.isEmpty()) configSummary.append(", ");
            configSummary.append("Packs: ").append(String.join(", ", config.getResourcePacksOrdered()));
        }

        if (!config.getTabDesign().isEmpty()) {
            if (!configSummary.isEmpty()) configSummary.append(", ");
            configSummary.append("Tab: ").append(config.getTabDesign());
        }
    }

    private void onConfigurationFailed(ConfigurationApplicationService.ConfigurationResult result, Throwable throwable) {
        PackCore.LOGGER.error("Failed to apply wizard configuration", throwable);

        // Build detailed failure message
        StringBuilder failureMessage = new StringBuilder();
        boolean hasResourcePackFailure = false;
        List<String> selectedPacks = dataManager.getResourcePacksOrdered();
        boolean hasHypixelPlus = selectedPacks.stream()
                .anyMatch(pack -> pack.equalsIgnoreCase("HypixelPlus"));

        if (result != null && !result.getFailedSteps().isEmpty()) {
            for (Map.Entry<String, String> failure : result.getFailedSteps().entrySet()) {
                failureMessage.append("❌ ").append(failure.getKey()).append(":\n   ")
                        .append(failure.getValue()).append("\n\n");

                // Check if resource packs failed
                if (failure.getKey().contains("Resource Pack")) {
                    hasResourcePackFailure = true;
                }
            }
        } else if (throwable != null) {
            String errorMsg = throwable.getMessage() != null ? throwable.getMessage() : "An unexpected error occurred";
            failureMessage.append(errorMsg).append("\n\n");
        } else {
            failureMessage.append("An unexpected error occurred.\n\n");
        }

        // Show warning banner if resource packs failed and HypixelPlus was selected
        if (hasResourcePackFailure && hasHypixelPlus) {
            showWarningBanner(
                    "HypixelPlus Requires Special Setup",
                    "HypixelPlus needs the JVM argument -Xss4M to work properly. " +
                            "If the resource packs failed to apply, this is likely the cause. " +
                            "Please add -Xss4M to your launcher's JVM arguments and restart the game."
            );
        }

        // Update UI to error state - allow retry
        updateApplyButtonState(false, "🔄 Retry Settings");
        updateStatusLabel("⚠️ Some settings couldn't be applied. See details above. Click 'Retry Settings' or 'Skip' to continue.", Formatting.RED);

        // Allow skipping on failure
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
            updateStatusLabel("⏳ Please wait - we're still applying your settings...", Formatting.YELLOW);
            return;
        }

        if (!dataManager.isConfigurationApplied()) {
            updateStatusLabel("⚠️ Please apply your settings first, or click 'Skip' to configure them later. If some configurations failed, you can click 'Skip' — the ones that succeeded will be saved.", Formatting.GOLD);
            return;
        }

        this.client.setScreen(new FancyMainMenuScreen());
    }

    @Override
    protected void onSkipPressed() {
        // Prevent continuing while configuration is being applied
        if (dataManager.isConfigurationApplying()) {
            updateStatusLabel("⏳ Please wait - we're still applying your settings. You can skip once it finishes.", Formatting.YELLOW);
            return;
        }

        PackCoreConfig.haveShownWelcomeWizard = true;
        PackCoreConfig.write(MOD_ID);

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

    @Override
    protected boolean isSkippable() {
        return true;
    }
}