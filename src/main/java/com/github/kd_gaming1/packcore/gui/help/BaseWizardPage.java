package com.github.kd_gaming1.packcore.gui.help;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.util.UiSurfaces;
import com.github.kd_gaming1.packcore.util.ConfigFileUtils;
import com.github.kd_gaming1.packcore.util.modpack.ModpackInfo;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Base class for wizard pages with consistent styling and layout.
 * Designed with island background in mind - content favors left side.
 */
public abstract class BaseWizardPage extends BaseOwoScreen<StackLayout> {

    // Theme constants
    protected static final int OVERLAY_DARK = 0x80_000000;
    protected static final int PANEL_BACKGROUND = 0xC0_1A1A1A;
    protected static final int ACCENT_GOLD = 0xFF_FFD700;
    protected static final int TEXT_WHITE = 0xFFFFFF;
    protected static final int TEXT_SECONDARY = 0xB9BBBE;

    // Status panel colors
    protected static final int STATUS_SUCCESS_BG = 0xC0_2D5016;
    protected static final int STATUS_SUCCESS_BORDER = 0xFF_52C41A;
    protected static final int STATUS_WARNING_BG = 0xC0_5C3317;
    protected static final int STATUS_WARNING_BORDER = 0xFF_FAAD14;

    // Layout constants
    protected static final int HEADER_HEIGHT = 35;
    protected static final int CONTENT_PADDING = getScaledPadding();
    protected static final int PROGRESS_BAR_WIDTH = 125;

    protected ButtonComponent primaryButton;
    private boolean primaryButtonInitialActive = true;

    private boolean wizardFailed;
    private StackLayout confirmOverlay;

    private final Identifier backgroundTexture;
    private final WizardPageInfo pageInfo;

    private static final ModpackInfo info = PackCore.getModpackInfo();

    protected BaseWizardPage(@NotNull WizardPageInfo pageInfo, @Nullable Identifier backgroundTexture) {
        super(pageInfo.title());
        this.pageInfo = pageInfo;
        this.backgroundTexture = backgroundTexture != null ? backgroundTexture :
                Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png");
    }

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::stack);
    }

    @Override
    protected final void build(StackLayout rootComponent) {
        // Set background
        rootComponent.surface(UiSurfaces.stretched(backgroundTexture, 1920, 1082));

        // Create main layout structure
        FlowLayout mainLayout = createMainLayout();

        // Add header
        mainLayout.child(createHeader());

        // Add status panel if needed
        if (shouldShowStatusInfo()) {
            mainLayout.child(createStatusPanel());
        }

        // Add main content area (left-aligned for island background)
        FlowLayout contentContainer = createContentContainer();
        buildContent(contentContainer);
        mainLayout.child(contentContainer);

        // Add right-aligned content area if needed
        if (shouldShowRightPanel()) {
            FlowLayout contentContainerRight = createContentContainerRight();
            buildContentRight(contentContainerRight);
            mainLayout.child(contentContainerRight);
        }

        // Add footer with navigation
        mainLayout.child(createFooter());

        // Add the main layout to root with overlay for better text visibility
        rootComponent.child(
                Containers.stack(Sizing.fill(100), Sizing.fill(100))
                        .child(Components.box(Sizing.fill(100), Sizing.fill(100)).color(Color.ofArgb(OVERLAY_DARK)))
                        .child(mainLayout)
        );

        // Store reference for overlay management
        this.confirmOverlay = rootComponent;
    }

    private FlowLayout createMainLayout() {
        return (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
                .padding(Insets.of(CONTENT_PADDING));
    }

    private FlowLayout createHeader() {
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(getContentColumnWidthPercent()), Sizing.fixed(HEADER_HEIGHT))
                .padding(Insets.of(CONTENT_PADDING - 6))
                .verticalAlignment(VerticalAlignment.CENTER);

        // Title
        LabelComponent titleLabel = (LabelComponent) Components.label(pageInfo.title.copy().styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))))
                .color(Color.ofRgb(ACCENT_GOLD))
                .shadow(true)
                .margins(Insets.of(0, 0, 4, 4));

        header.child(titleLabel);

        // Progress indicator
        FlowLayout progressIndicator = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.fill(100))
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        progressIndicator.child(Components.label(
                TextOps.withColor("Step " + pageInfo.currentStep() + " of " + pageInfo.totalSteps(), ACCENT_GOLD)
        ));

        // Progress bar
        progressIndicator.child(createProgressBar());

        header.child(progressIndicator);

        return header;
    }

    private Component createProgressBar() {
        FlowLayout progressContainer = (FlowLayout) Containers.horizontalFlow(Sizing.fixed(PROGRESS_BAR_WIDTH), Sizing.fixed(4))
                .surface(Surface.flat(0x40_FFFFFF));

        int progressWidth = (int) (PROGRESS_BAR_WIDTH * ((double) pageInfo.currentStep() / pageInfo.totalSteps()));

        Component progressFill = Components.box(Sizing.fixed(progressWidth), Sizing.fill(100))
                .color(Color.ofRgb(ACCENT_GOLD));

        return Containers.stack(Sizing.fixed(PROGRESS_BAR_WIDTH), Sizing.fixed(4))
                .child(progressContainer)
                .child(progressFill);
    }

    private FlowLayout createStatusPanel() {
        StatusInfo status = getStatusInfo();

        FlowLayout statusPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(38), Sizing.content())
                .gap(8)
                .surface(UiSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/wizard/small_info_box.png"), 722, 338))
                .padding(Insets.of(10))
                .verticalAlignment(VerticalAlignment.CENTER)
                .positioning(Positioning.relative(100, 0));

        // Main message section
        FlowLayout messageSection = Containers.verticalFlow(Sizing.expand(), Sizing.content())
                .gap(4);

        // Status header with icon
        FlowLayout headerRow = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(6)
                .verticalAlignment(VerticalAlignment.CENTER);

        headerRow.child(Components.label(
                Text.literal(status.icon + " " + status.title)
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_WHITE)));

        messageSection.child(headerRow);

        // Detailed message
        LabelComponent detailLabel = Components.label(
                Text.literal(status.message).setStyle(Style.EMPTY)
        ).color(Color.ofRgb(TEXT_SECONDARY));
        detailLabel.horizontalSizing(Sizing.fill(100));
        messageSection.child(detailLabel);

        // Additional info if needed
        if (status.additionalInfo != null) {
            LabelComponent infoLabel = (LabelComponent) Components.label(
                    Text.literal(status.additionalInfo)
                            .setStyle(Style.EMPTY.withItalic(Boolean.TRUE))
            ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.of(2, 2, 2, 2));
            infoLabel.horizontalSizing(Sizing.fill(100));
            messageSection.child(infoLabel.margins(Insets.top(2)));
        }

        statusPanel.child(messageSection);

        // Reset button
        ButtonComponent resetButton = (ButtonComponent) Components.button(Text.literal("Reset Setup"),
                        button -> showResetConfirmation()
                ).renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20));

        resetButton.tooltip(Text.literal("Reset the setup wizard and close the game"));
        statusPanel.child(resetButton);

        return statusPanel;
    }

    private void showResetConfirmation() {
        // Create overlay with the dialog as the required child
        OverlayContainer<FlowLayout> overlay = Containers.overlay(createConfirmDialog());

        // Configure behavior and appearance
        overlay.closeOnClick(true);
        overlay.surface(Surface.flat(0x80_000000));
        overlay.zIndex(10);

        // Add to root overlay stack
        confirmOverlay.child(overlay);
    }

    private FlowLayout createConfirmDialog() {
        FlowLayout dialog = (FlowLayout) Containers.verticalFlow(Sizing.fixed(350), Sizing.content())
                .gap(15)
                .surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(STATUS_WARNING_BORDER)))
                .padding(Insets.of(20))
                .positioning(Positioning.relative(50, 50));

        // Title
        dialog.child(Components.label(
                Text.literal("⚠ Reset Setup Wizard?")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).color(Color.ofRgb(STATUS_WARNING_BORDER)).margins(Insets.of(2, 2, 2, 2)));

        // Message
        LabelComponent message = (LabelComponent) Components.label(
                Text.literal("This will reset PackCore and close the game. When you reopen it, PackCore will try to apply the configs automatically again.")
        ).color(Color.ofRgb(TEXT_WHITE)).margins(Insets.of(2, 2, 2, 2));
        message.horizontalSizing(Sizing.fill(100));
        dialog.child(message);

        // Warning
        LabelComponent warning = (LabelComponent) Components.label(
                Text.literal("Note: Any manual changes you've made may be lost.")
                        .setStyle(Style.EMPTY.withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(STATUS_WARNING_BORDER)).margins(Insets.of(2, 2, 2, 2));
        warning.horizontalSizing(Sizing.fill(100));
        dialog.child(warning);

        // Buttons
        FlowLayout buttons = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(10)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        ButtonComponent cancelButton = (ButtonComponent) Components.button(
                        Text.literal("Cancel"),
                        button -> {
                            // Remove the overlay
                            confirmOverlay.removeChild(confirmOverlay.children().getLast());
                        }
                ).renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20));

        ButtonComponent confirmButton = (ButtonComponent) Components.button(
                        Text.literal("Reset & Exit"),
                        button -> onResetPressed()
                ).renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20));

        buttons.child(cancelButton);
        buttons.child(confirmButton);
        dialog.child(buttons);

        return dialog;
    }

    private StatusInfo getStatusInfo() {
        if (PackCoreConfig.defaultConfigSuccessfullyApplied) {
            wizardFailed = false;
            return new StatusInfo(
                    "✓",
                    "Configuration Successful",
                    "PackCore has automatically applied the config below based on your screen resolution. If it's incorrect, continue the welcome wizard and open the Configuration Manager in the main menu to change it.",
                    "Applied configuration: " + ConfigFileUtils.getCurrentConfig().getDisplayName(),
                    STATUS_SUCCESS_BG,
                    STATUS_SUCCESS_BORDER,
                    false
            );
        } else {
            wizardFailed = true;
            return new StatusInfo(
                    "⚠",
                    "Setup Incomplete",
                    "Automatic config application failed. Close the game with the button below and restart to try again, or continue the welcome wizard and apply the config manually from the Configuration Manager in the main menu.",
                    "Click 'Reset Setup' to close the game and try the automatic configuration again.",
                    STATUS_WARNING_BG,
                    STATUS_WARNING_BORDER,
                    true
            );
        }
    }

    private FlowLayout createContentContainer() {
        return (FlowLayout) Containers.verticalFlow(Sizing.fill(getContentColumnWidthPercent()), Sizing.expand())
                .gap(12)
                .surface(UiSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/wizard/info_box.png"), 1142, 934))
                .padding(Insets.of(CONTENT_PADDING + 10, CONTENT_PADDING + 10, CONTENT_PADDING + 4, CONTENT_PADDING + 4))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.of(0, 28, 0, 0));
    }

    private FlowLayout createContentContainerRight() {
        // Alternative content container on the right side
        return (FlowLayout) Containers.verticalFlow(Sizing.fill(getContentColumnWidthRightPercent()), Sizing.fill(100))
                .gap(12)
                .surface(UiSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/wizard/box.png"), 607, 755))
                .padding(Insets.of(CONTENT_PADDING + 10, CONTENT_PADDING + 10, CONTENT_PADDING + 4, CONTENT_PADDING + 4))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .positioning(Positioning.relative(100, 0))
                .margins(Insets.of(0, 28, 0, 0));
    }

    private FlowLayout createFooter() {
        FlowLayout footer = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .padding(Insets.of(CONTENT_PADDING - 8, CONTENT_PADDING - 8, CONTENT_PADDING, CONTENT_PADDING))
                .verticalAlignment(VerticalAlignment.CENTER)
                .positioning(Positioning.relative(0, 100));

        // Right side - navigation buttons
        FlowLayout buttonContainer = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(12)
                .positioning(Positioning.relative(100, 50));

        if (hasPreviousPage()) {
            ButtonComponent backButton = (ButtonComponent) Components.button(
                            Text.literal("Back"),
                            button -> onBackPressed()
                    ).renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/previous.png"), 0, 0, 100, 60))
                    .horizontalSizing(Sizing.fixed(100))
                    .verticalSizing(Sizing.fixed(20));
            buttonContainer.child(backButton);
        }

        // Skip button for optional steps
        if (isSkippable()) {
            ButtonComponent skipButton = (ButtonComponent) Components.button(
                            Text.literal("Skip"),
                            button -> onSkipPressed()
                    ).renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/continue.png"), 0, 0, 100, 60))
                    .horizontalSizing(Sizing.fixed(100))
                    .verticalSizing(Sizing.fixed(20));
            buttonContainer.child(skipButton);
        }

        ButtonComponent primaryButton = createPrimaryButton();
        buttonContainer.child(primaryButton);

        // Left side - link buttons
        FlowLayout linkButtonContainer = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(8)
                .margins(Insets.of(0, 0, 4, 0));

        ButtonComponent discord = (ButtonComponent) Components.button(
                        Text.empty(),
                        button -> Util.getOperatingSystem().open(info.getDiscord())
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/discord_icon.png"), 0, 0, 22, 22))
                .horizontalSizing(Sizing.fixed(22))
                .verticalSizing(Sizing.fixed(22));

        ButtonComponent modrinth = (ButtonComponent) Components.button(
                        Text.empty(),
                        button -> Util.getOperatingSystem().open(info.getWebsite())
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/modrinth_icon.png"), 0, 0, 22, 22))
                .horizontalSizing(Sizing.fixed(22))
                .verticalSizing(Sizing.fixed(22));

        ButtonComponent github = (ButtonComponent) Components.button(
                        Text.empty(),
                        button -> Util.getOperatingSystem().open(info.getIssueTracker())
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/github_icon.png"), 0, 0, 22, 22))
                .horizontalSizing(Sizing.fixed(22))
                .verticalSizing(Sizing.fixed(22));

        linkButtonContainer.child(discord);
        linkButtonContainer.child(modrinth);
        linkButtonContainer.child(github);
        footer.child(linkButtonContainer);
        footer.child(buttonContainer);

        return footer;
    }

    protected void updatePrimaryButtonState(boolean enabled) {
        if (this.primaryButton != null) {
            this.primaryButton.active = enabled;
        } else {
            this.primaryButtonInitialActive = enabled;
        }
    }

    private ButtonComponent createPrimaryButton() {
        String buttonText = isLastPage() ? "Finish" : "Continue";
        this.primaryButton = (ButtonComponent) Components.button(
                        Text.literal(buttonText),
                        button -> onContinuePressed()
                ).renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/continue.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20));

        // Apply any initial state that may have been set earlier
        this.primaryButton.active = this.primaryButtonInitialActive;
        return this.primaryButton;
    }

    private static int getScaledPadding() {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        return screenWidth < 900 ? 8 : (screenWidth > 1400 ? 20 : 15);
    }

    // Abstract methods for subclasses to implement

    /**
     * Build the main content for this wizard page.
     */
    protected abstract void buildContent(FlowLayout contentContainer);

    protected abstract void buildContentRight(FlowLayout contentContainerRight);

    /**
     * Called when the continue/next button is pressed.
     */
    protected abstract void onContinuePressed();

    // Optional override methods

    protected void onBackPressed() {
        if (hasPreviousPage()) {
            MinecraftClient.getInstance().setScreen(
                    WizardNavigator.createWizardPage(pageInfo.currentStep() - 1)
            );
        }
    }

    protected void onSkipPressed() {
        onContinuePressed();
    }

    protected void onResetPressed() {
        // Reset values and close the game
        PackCoreConfig.defaultConfigSuccessfullyApplied = false;
        PackCoreConfig.write(MOD_ID);
        MinecraftClient.getInstance().scheduleStop();
    }

    protected boolean hasPreviousPage() {
        return pageInfo.currentStep() >= 1;
    }

    protected boolean isLastPage() {
        return pageInfo.currentStep() >= pageInfo.totalSteps();
    }

    protected boolean isSkippable() {
        return false;
    }

    protected boolean shouldShowStatusInfo() {
        return true;
    }

    protected boolean shouldShowRightPanel() {
        return false;
    }

    protected int getContentColumnWidthPercent() {
        return 60;
    }

    protected int getContentColumnWidthRightPercent() {
        return 40;
    }

    // Helper classes

    private record StatusInfo(String icon, String title, String message, String additionalInfo, int backgroundColor,
                              int borderColor, boolean showResetButton) {
    }

    public record WizardPageInfo(
            Text title,
            int currentStep,
            int totalSteps
    ) {}
}