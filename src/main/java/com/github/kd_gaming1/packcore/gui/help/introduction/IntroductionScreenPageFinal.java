package com.github.kd_gaming1.packcore.gui.help.introduction;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.help.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.help.WizardDataManager;
import com.github.kd_gaming1.packcore.util.ResourcePackUtil;
import io.wispforest.owo.ops.TextOps;
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
    private boolean isApplying = false;
    private boolean hasApplied = false;
    private LabelComponent statusLabel;
    private ButtonComponent applyButton;
    private ButtonComponent finishButton;

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
        // Header
        contentContainer.child(createHeader());

        // Configuration summary
        contentContainer.child(createConfigurationSummary());

        // Status section
        contentContainer.child(createStatusSection());

        // Apply buttons
        contentContainer.child(createActionButtons());
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {
        // Show helpful information
        contentContainerRight.child(createHelpSection());
    }

    private FlowLayout createHeader() {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6);

        LabelComponent title = Components.label(
                Text.literal("Ready to Apply Configuration!")
                        .setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        );

        LabelComponent subtitle = Components.label(
                Text.literal("Review your selections below and click 'Apply Configuration' to activate your chosen settings.")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY));

        header.child(title).child(subtitle);
        return header;
    }

    private FlowLayout createConfigurationSummary() {
        FlowLayout summaryContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .surface(Surface.flat(0x20_FFD700).and(Surface.outline(ACCENT_GOLD)))
                .padding(Insets.of(12))
                .margins(Insets.vertical(8));

        // Configuration summary title
        summaryContainer.child(Components.label(
                Text.literal("Your Configuration Summary:")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).color(Color.ofRgb(ACCENT_GOLD)));

        // NEW: Resource packs (use proper ordered list)
        List<String> resourcePacks = dataManager.getResourcePacksOrdered();
        summaryContainer.child(createSummaryItem("📦 Resource Packs:",
                resourcePacks.isEmpty() ? "None selected" : String.join(", ", resourcePacks)));

        // Tab design
        String tabDesign = dataManager.getTabDesign();
        summaryContainer.child(createSummaryItem("📋 Tab Design:",
                tabDesign.isEmpty() ? "None selected" : tabDesign));

        // Misc settings
        var miscSettings = dataManager.getMiscSettings();
        summaryContainer.child(createSummaryItem("⚙️ Miscellaneous Settings:",
                miscSettings.isEmpty() ? "None selected" : String.join(", ", miscSettings)));

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

    private FlowLayout createStatusSection() {
        FlowLayout statusContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .margins(Insets.vertical(8));

        // Status label
        statusLabel = Components.label(Text.literal("Click 'Apply Configuration' to begin...")
                        .setStyle(Style.EMPTY.withItalic(Boolean.TRUE)))
                .color(Color.ofRgb(TEXT_SECONDARY));

        statusContainer.child(statusLabel);

        return statusContainer;
    }

    private FlowLayout createActionButtons() {
        FlowLayout buttonContainer = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(12)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.top(16));

        // Apply configuration button
        applyButton = (ButtonComponent) Components.button(
                        Text.literal("Apply Configuration"),
                        this::onApplyPressed
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/continue.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(130))
                .verticalSizing(Sizing.fixed(20));

        // Finish button (initially disabled)
        finishButton = (ButtonComponent) Components.button(
                        Text.literal("Finish & Exit"),
                        this::onFinishPressed
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/continue.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(130))
                .verticalSizing(Sizing.fixed(20));

        finishButton.active = false; // Start disabled

        buttonContainer.child(applyButton);
        buttonContainer.child(finishButton);

        return buttonContainer;
    }

    private FlowLayout createHelpSection() {
        FlowLayout helpContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .surface(Surface.flat(0x20_000000).and(Surface.outline(0x40_FFFFFF)))
                .padding(Insets.of(12));

        helpContainer.child(Components.label(
                Text.literal("What happens next?")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).color(Color.ofRgb(ACCENT_GOLD)));

        helpContainer.child(Components.label(
                Text.literal("1. Resource packs will be activated")
        ).color(Color.ofRgb(TEXT_WHITE)));

        helpContainer.child(Components.label(
                Text.literal("2. Game resources will reload")
        ).color(Color.ofRgb(TEXT_WHITE)));

        helpContainer.child(Components.label(
                Text.literal("3. Settings will be saved")
        ).color(Color.ofRgb(TEXT_WHITE)));

        helpContainer.child(Components.label(
                Text.literal("4. You can exit the wizard")
        ).color(Color.ofRgb(TEXT_WHITE)));

        helpContainer.child(Components.label(
                Text.literal("Note: The game may freeze briefly during resource loading. This is normal!")
                        .setStyle(Style.EMPTY.withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.top(8)));

        return helpContainer;
    }

    private void onApplyPressed(ButtonComponent button) {
        if (isApplying || hasApplied) return;

        isApplying = true;

        // Update UI
        applyButton.setMessage(Text.literal("Applying..."));
        applyButton.active = false;
        statusLabel.text(Text.literal("⏳ Applying configuration...").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));

        // NEW: Get selected resource packs using proper ordered list
        List<String> selectedPacksList = dataManager.getResourcePacksOrdered();

        PackCore.LOGGER.info("Starting configuration application with packs (in order): {}", selectedPacksList);

        // Apply resource packs with preserved order
        ResourcePackUtil.applyResourcePacksOrdered(selectedPacksList)
                .whenComplete((success, throwable) -> {
                    MinecraftClient.getInstance().execute(() -> {
                        isApplying = false;
                        hasApplied = true;

                        if (success && throwable == null) {
                            onConfigurationApplied();
                        } else {
                            onConfigurationFailed(throwable);
                        }
                    });
                });
    }

    private void onConfigurationApplied() {
        PackCore.LOGGER.info("Wizard configuration applied successfully!");

        // Update UI to success state
        applyButton.setMessage(Text.literal("✓ Applied"));
        statusLabel.text(Text.literal("✅ Configuration applied successfully! Your resource packs are now active.")
                .setStyle(Style.EMPTY.withColor(Formatting.GREEN)));

        // Enable finish button
        finishButton.active = true;

        // Mark wizard as completed
        PackCoreConfig.haveSetupWizardCompletedSuccessfully = true;
        PackCoreConfig.appliedConfigName = "Wizard Configuration";
    }

    private void onConfigurationFailed(Throwable throwable) {
        PackCore.LOGGER.error("Failed to apply wizard configuration", throwable);

        // Update UI to error state
        applyButton.setMessage(Text.literal("Apply Configuration"));
        applyButton.active = true;
        statusLabel.text(Text.literal("❌ Configuration failed. Please try again or check the logs.")
                .setStyle(Style.EMPTY.withColor(Formatting.RED)));

        hasApplied = false;
    }

    private void onFinishPressed(ButtonComponent button) {
        if (!hasApplied) return;

        PackCore.LOGGER.info("Wizard completed successfully");

        // Reset wizard data
        dataManager.reset();

        // Close wizard and return to main menu
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    protected void onContinuePressed() {
        // Override the default continue behavior
        if (!hasApplied) {
            // If not applied yet, do nothing (button should be disabled anyway)
            return;
        } else {
            // If applied, finish
            onFinishPressed(null);
        }
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
        return 40;
    }
}