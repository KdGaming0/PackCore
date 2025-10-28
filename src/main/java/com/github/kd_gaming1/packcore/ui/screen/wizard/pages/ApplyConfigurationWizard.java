package com.github.kd_gaming1.packcore.ui.screen.wizard.pages;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.config.apply.WizardOptionApplyService;
import com.github.kd_gaming1.packcore.ui.screen.title.SBEStyledTitleScreen;
import com.github.kd_gaming1.packcore.ui.screen.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.ui.screen.wizard.components.WizardUIComponents;
import com .github.kd_gaming1.packcore.util.wizard.WizardDataStore;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;
import static com.github.kd_gaming1.packcore.ui.screen.wizard.components.WizardUIComponents.ProgressStatus;

/**
 * Apply Configuration page - final page that applies all selected settings
 */
public class ApplyConfigurationWizard extends BaseWizardPage {

    private final WizardDataStore dataStore;
    private LabelComponent statusLabel;
    private ButtonComponent applyButton;
    private FlowLayout progressContainer;
    private FlowLayout warningBanner;
    private final Map<String, LabelComponent> stepLabels = new LinkedHashMap<>();

    public ApplyConfigurationWizard() {
        super(
                new WizardPageInfo(
                        Text.literal("Apply Configuration"),
                        6,
                        6
                ),
                Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        this.dataStore = WizardDataStore.getInstance();
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

        // Initialize UI state
        initializeUIState();
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {
        contentContainerRight.child(createHelpSection());
    }

    /**
     * Create the header section
     */
    private FlowLayout createHeader() {
        return Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .child(Components.label(
                        Text.literal("✨ Ready to Apply Your Settings!")
                                .setStyle(Style.EMPTY.withColor(ACCENT_SECONDARY).withBold(true))
                ).horizontalSizing(Sizing.fill(98)).margins(Insets.of(2)))
                .child(Components.label(
                                Text.literal("Review your choices below. When you're ready, click 'Apply Settings' to activate everything.")
                                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true))
                        ).color(Color.ofRgb(TEXT_SECONDARY))
                        .horizontalSizing(Sizing.fill(98))
                        .margins(Insets.of(2)));
    }

    /**
     * Create configuration summary
     */
    private FlowLayout createConfigurationSummary() {
        FlowLayout summary = WizardUIComponents.createInfoCard(
                "📋 What You've Selected:",
                null,
                0x20_FFD700,
                ACCENT_SECONDARY
        );

        // Optimization Profile
        String optimization = dataStore.getOptimizationProfile();
        summary.child(WizardUIComponents.createSummaryItem(
                "🚀 Performance Level:",
                optimization.isEmpty() ? "Default (no changes)" : optimization
        ));

        // Tab design
        String tabDesign = dataStore.getTabDesign();
        summary.child(WizardUIComponents.createSummaryItem(
                "🖼 Tab Menu Style:",
                tabDesign.isEmpty() ? "Default (no changes)" : tabDesign
        ));

        // Item background
        String itemBackground = dataStore.getItemBackground();
        summary.child(WizardUIComponents.createSummaryItem(
                "🎨 Item Background Style:",
                itemBackground.isEmpty() ? "Default (no changes)" : itemBackground
        ));

        // Resource packs
        List<String> resourcePacks = dataStore.getResourcePacksOrdered();
        if (!resourcePacks.isEmpty()) {
            summary.child(WizardUIComponents.createSummaryItem("📦 Resource Packs (loading order):", ""));
            for (int i = 0; i < resourcePacks.size(); i++) {
                summary.child(Components.label(
                        Text.literal("  " + (i + 1) + ". " + resourcePacks.get(i))
                ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.left(16)));
            }
        } else {
            summary.child(WizardUIComponents.createSummaryItem("📦 Resource Packs:", "None selected"));
        }

        return summary;
    }

    /**
     * Create warning banner (initially hidden)
     */
    private FlowLayout createWarningBanner() {
        FlowLayout banner = (FlowLayout) Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(4)
                .surface(Surface.flat(0x30_FF8C00).and(Surface.outline(0xFF_FFA500)))
                .padding(Insets.of(8))
                .margins(Insets.vertical(4));

        // Initially hidden
        banner.positioning(Positioning.absolute(0, -1000));
        return banner;
    }

    /**
     * Show warning banner with message
     */
    private void showWarningBanner() {
        MinecraftClient.getInstance().execute(() -> {
            warningBanner.clearChildren();
            warningBanner.child(Components.label(
                    Text.literal("⚠ " + "HypixelPlus Requires Special Setup").setStyle(Style.EMPTY.withBold(true))
            ).color(Color.ofRgb(0xFFA500)));

            warningBanner.child(Components.label(Text.literal("HypixelPlus needs the JVM argument -Xss4M to work properly. Please add -Xss4M to your launcher's JVM arguments and restart the game."))
                    .color(Color.ofRgb(TEXT_PRIMARY))
                    .horizontalSizing(Sizing.fill(95)));

            warningBanner.positioning(Positioning.layout());
        });
    }

    /**
     * Hide warning banner
     */
    private void hideWarningBanner() {
        MinecraftClient.getInstance().execute(() ->
                warningBanner.positioning(Positioning.absolute(0, -1000)));
    }

    /**
     * Create progress section
     */
    private FlowLayout createProgressSection() {
        progressContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(3)
                .surface(Surface.flat(0x20_000000).and(Surface.outline(0x40_FFFFFF)))
                .padding(Insets.of(6));

        progressContainer.child(Components.label(
                Text.literal("⚙ Applying Your Settings:")
                        .setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(ACCENT_SECONDARY)));

        initializeProgressSteps();

        // Hide initially unless applying
        if (!dataStore.isConfigurationApplying()) {
            progressContainer.positioning(Positioning.absolute(0, -1000));
        }

        return progressContainer;
    }

    /**
     * Initialize progress steps based on configuration
     */
    private void initializeProgressSteps() {
        stepLabels.clear();

        if (!dataStore.getOptimizationProfile().isEmpty()) {
            addProgressStep("performance", "Performance Settings");
        }

        if (!dataStore.getResourcePacksOrdered().isEmpty()) {
            addProgressStep("resourcepacks", "Resource Packs");
        }

        if (!dataStore.getTabDesign().isEmpty()) {
            addProgressStep("tabdesign", "Tab Menu Style");
        }

        if (!dataStore.getItemBackground().isEmpty()) {
            addProgressStep("itembackground", "Item Background Style");
        }

        if (!dataStore.getAdditionalSettings().isEmpty()) {
            addProgressStep("additional", "Extra Settings");
        }
    }

    /**
     * Add a progress step
     */
    private void addProgressStep(String key, String name) {
        LabelComponent stepLabel = WizardUIComponents.createProgressStepLabel(name, ProgressStatus.PENDING);
        stepLabels.put(key, stepLabel);
        progressContainer.child(stepLabel);
    }

    /**
     * Update progress step status
     */
    private void updateProgressStep(String stepKey, String status, String errorMessage) {
        MinecraftClient.getInstance().execute(() -> {
            LabelComponent stepLabel = stepLabels.get(stepKey);
            if (stepLabel != null) {
                String stepName = getStepName(stepKey);

                ProgressStatus progressStatus = switch (status) {
                    case "success" -> ProgressStatus.SUCCESS;
                    case "error" -> ProgressStatus.ERROR;
                    case "running" -> ProgressStatus.RUNNING;
                    default -> ProgressStatus.PENDING;
                };

                String displayText = getStatusIcon(progressStatus) + " " + stepName;
                if (progressStatus == ProgressStatus.ERROR && errorMessage != null) {
                    displayText += " - " + errorMessage;
                }

                Formatting color = getStatusColor(progressStatus);
                stepLabel.text(Text.literal(displayText).setStyle(Style.EMPTY.withColor(color)));
            }
        });
    }

    /**
     * Get step name from key
     */
    private String getStepName(String key) {
        return switch (key) {
            case "performance" -> "Performance Settings";
            case "resourcepacks" -> "Resource Packs";
            case "tabdesign" -> "Tab Menu Style";
            case "itembackground" -> "Item Background Style";
            case "additional" -> "Extra Settings";
            default -> "Unknown Step";
        };
    }

    /**
     * Get status icon
     */
    private String getStatusIcon(ProgressStatus status) {
        return switch (status) {
            case SUCCESS -> "✅";
            case ERROR -> "❌";
            case RUNNING -> "⏳";
            case PENDING -> "⏸";
        };
    }

    /**
     * Get status color
     */
    private Formatting getStatusColor(ProgressStatus status) {
        return switch (status) {
            case SUCCESS -> Formatting.GREEN;
            case ERROR -> Formatting.RED;
            case RUNNING -> Formatting.YELLOW;
            case PENDING -> Formatting.GRAY;
        };
    }

    /**
     * Create status section
     */
    private FlowLayout createStatusSection() {
        FlowLayout statusContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .margins(Insets.vertical(8));

        statusLabel = (LabelComponent) Components.label(
                        Text.literal("👉 Ready to begin! Click 'Apply Settings' when you're ready.")
                                .setStyle(Style.EMPTY.withItalic(true))
                ).color(Color.ofRgb(TEXT_SECONDARY))
                .horizontalSizing(Sizing.fill(98))
                .margins(Insets.of(2));

        statusContainer.child(statusLabel);
        return statusContainer;
    }

    /**
     * Create action buttons
     */
    private FlowLayout createActionButtons() {
        FlowLayout buttonContainer = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.top(16))
                .positioning(Positioning.relative(50, 100));

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

    /**
     * Create help section for right panel
     */
    private FlowLayout createHelpSection() {
        FlowLayout help = WizardUIComponents.createInfoCard(
                "ℹ What Happens Next?",
                null,
                0x20_000000,
                ACCENT_SECONDARY
        );

        String[] steps = {
                "1. Performance settings will be adjusted",
                "2. Tab menu style will be configured",
                "3. Item background style will be applied",
                "4. Resource packs will be enabled in order"
        };

        for (String step : steps) {
            help.child(Components.label(Text.literal(step))
                    .color(Color.ofRgb(TEXT_PRIMARY))
                    .horizontalSizing(Sizing.fill(95)));
        }

        help.child(Components.label(
                        Text.literal("💡 Tip: Each step shows a progress indicator. If something fails, you'll see exactly what went wrong!")
                                .setStyle(Style.EMPTY.withItalic(true))
                ).color(Color.ofRgb(TEXT_SECONDARY))
                .horizontalSizing(Sizing.fill(95))
                .margins(Insets.of(6, 2, 2, 2)));

        return help;
    }

    /**
     * Initialize UI state based on stored data
     */
    private void initializeUIState() {
        if (dataStore.isConfigurationApplying()) {
            progressContainer.positioning(Positioning.layout());
            updateApplyButtonState(true, "Applying Settings...");
            updateStatusLabel("⏳ Please wait while we apply your settings...", Formatting.YELLOW);
            updatePrimaryButtonState(false);
        } else if (dataStore.isConfigurationApplied()) {
            String result = dataStore.getConfigurationResult();
            if ("success".equals(result)) {
                onConfigurationApplied();
            } else if ("failed".equals(result)) {
                onConfigurationFailed(null, new RuntimeException(dataStore.getConfigurationErrorMessage()));
            }
            updatePrimaryButtonState(dataStore.isConfigurationApplied());
        } else {
            updatePrimaryButtonState(false);
        }
    }

    /**
     * Handle apply button press
     */
    private void onApplyPressed(ButtonComponent button) {
        if (dataStore.isConfigurationApplying() || dataStore.isConfigurationApplied()) {
            return;
        }

        PackCore.LOGGER.info("Starting configuration application process");

        hideWarningBanner();

        // Update state
        dataStore.setConfigurationApplying(true);
        dataStore.setConfigurationApplied(false);
        dataStore.setConfigurationResult("", "");

        // Show progress
        progressContainer.positioning(Positioning.layout());
        updateApplyButtonState(true, "Applying...");
        updateStatusLabel("⏳ Please wait while we apply your settings. This may take a moment...", Formatting.YELLOW);

        // Apply configurations
        WizardOptionApplyService.applyAllConfigurationsWithProgress(this::updateProgressStep)
                .whenComplete((result, throwable) ->
                    MinecraftClient.getInstance().execute(() -> {
                        dataStore.setConfigurationApplying(false);

                        if (result.overallSuccess() && throwable == null) {
                            dataStore.setConfigurationApplied(true);
                            dataStore.setConfigurationResult("success", "");
                            onConfigurationApplied();
                        } else {
                            dataStore.setConfigurationApplied(false);

                            StringBuilder failureMessage = new StringBuilder();
                            if (!result.failedSteps().isEmpty()) {
                                for (Map.Entry<String, String> failure : result.failedSteps().entrySet()) {
                                    failureMessage.append("❌ ").append(failure.getKey())
                                            .append(":\n   ").append(failure.getValue()).append("\n\n");
                                }
                            } else if (throwable != null) {
                                failureMessage.append(throwable.getMessage() != null ?
                                        throwable.getMessage() : "An unexpected error occurred");
                            }

                            dataStore.setConfigurationResult("failed", failureMessage.toString());
                            onConfigurationFailed(result, throwable);
                        }
                    }));
    }

    /**
     * Update apply button state
     */
    private void updateApplyButtonState(boolean isApplying, String message) {
        if (applyButton != null) {
            applyButton.setMessage(Text.literal(message));
            applyButton.active = !isApplying;
        }
    }

    /**
     * Update status label
     */
    private void updateStatusLabel(String message, Formatting color) {
        if (statusLabel != null) {
            statusLabel.text(Text.literal(message).setStyle(Style.EMPTY.withColor(color)))
                    .horizontalSizing(Sizing.fill(100));
        }
    }

    /**
     * Handle successful configuration
     */
    private void onConfigurationApplied() {
        PackCore.LOGGER.info("Wizard configuration applied successfully!");

        updateApplyButtonState(true, "✅ All Done!");
        updateStatusLabel("✅ Success! All your settings have been applied. Click 'Continue' to start playing!",
                Formatting.GREEN);

        updatePrimaryButtonState(true);

        PackCoreConfig.haveShownWelcomeWizard = true;
        PackCoreConfig.write(MOD_ID);
    }

    /**
     * Handle configuration failure
     */
    private void onConfigurationFailed(WizardOptionApplyService.ConfigurationResult result, Throwable throwable) {
        PackCore.LOGGER.error("Failed to apply wizard configuration", throwable);

        // Check for specific failures
        if (result != null && !result.failedSteps().isEmpty()) {
            boolean hasResourcePackFailure = result.failedSteps().keySet().stream()
                    .anyMatch(key -> key.contains("Resource Pack"));

            if (hasResourcePackFailure && dataStore.getResourcePacksOrdered().stream()
                    .anyMatch(pack -> pack.equalsIgnoreCase("HypixelPlus"))) {
                showWarningBanner(
                );
            }
        }

        updateApplyButtonState(false, "🔄 Retry Settings");
        updateStatusLabel("⚠ Some settings couldn't be applied. See details above. " +
                "Click 'Retry Settings' or 'Finish' to continue.", Formatting.RED);

        updatePrimaryButtonState(true);

        dataStore.setConfigurationApplied(false);
        dataStore.setConfigurationApplying(false);
    }

    /**
     * Show skip confirmation dialog
     */
    private void showSkipConfirmation() {
        FlowLayout dialog = WizardUIComponents.createInfoCard(
                "⚠ Skip Configuration?",
                "You haven't applied your configuration yet. If you skip now, none of your selected settings will be saved.",
                PANEL_BACKGROUND,
                WARNING_BORDER
        );

        dialog.positioning(Positioning.relative(50, 50));

        // Add buttons
        FlowLayout buttons = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(10)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.top(8));

        buttons.child(Components.button(
                        Text.literal("Go Back"),
                        btn -> getRootComponent().removeChild(getRootComponent().children().getLast())
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20)));

        buttons.child(Components.button(
                        Text.literal("Skip Anyway"),
                        btn -> proceedWithSkip()
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 120, 60))
                .horizontalSizing(Sizing.fixed(120))
                .verticalSizing(Sizing.fixed(20)));

        dialog.child(buttons);

        OverlayContainer<FlowLayout> overlay = Containers.overlay(dialog);
        overlay.closeOnClick(true);
        overlay.surface(Surface.flat(0x80_000000));
        overlay.zIndex(10);

        getRootComponent().child(overlay);
    }

    /**
     * Proceed with skipping configuration
     */
    private void proceedWithSkip() {
        PackCoreConfig.haveShownWelcomeWizard = true;
        PackCoreConfig.write(MOD_ID);
        assert this.client != null;
        this.client.setScreen(new SBEStyledTitleScreen());
    }

    @Override
    protected void onContinuePressed() {
        if (dataStore.isConfigurationApplying()) {
            updateStatusLabel("⏳ Please wait - we're still applying your settings...", Formatting.YELLOW);
            return;
        }

        if (!dataStore.isConfigurationApplied() && !"failed".equals(dataStore.getConfigurationResult())) {
            updateStatusLabel("⚠ Please apply your settings first, or click 'Skip' to configure manually later.",
                    Formatting.GOLD);
            return;
        }

        assert this.client != null;
        this.client.setScreen(new SBEStyledTitleScreen());
    }

    @Override
    protected void onSkipPressed() {
        if (dataStore.isConfigurationApplying()) {
            updateStatusLabel("⏳ Please wait - we're still applying your settings. You can skip once it finishes.",
                    Formatting.YELLOW);
            return;
        }

        if (!dataStore.isConfigurationApplied() && !"failed".equals(dataStore.getConfigurationResult())) {
            showSkipConfirmation();
            return;
        }

        proceedWithSkip();
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
        return super.getContentColumnWidthPercent();
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