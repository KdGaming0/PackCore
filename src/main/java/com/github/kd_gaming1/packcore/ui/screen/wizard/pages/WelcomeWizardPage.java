package com.github.kd_gaming1.packcore.ui.screen.wizard.pages;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.github.kd_gaming1.packcore.ui.screen.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.ui.screen.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.ui.screen.components.WizardUIComponents;
import com.github.kd_gaming1.packcore.util.markdown.MarkdownService;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Welcome page - first page of the wizard
 */
public class WelcomeWizardPage extends BaseWizardPage {

    private static final String FALLBACK_CONTENT = """
            # Welcome
            
            Welcome to PackCore! This is the default welcome content.
            
            Find and edit this content in `rundir/packcore/wizard_markdown_content/welcome.md`
            """;

    private final String markdownContent;
    private final boolean configApplied;

    private boolean detailsExpanded = false;
    private FlowLayout detailsSection;
    private ButtonComponent toggleButton;

    public WelcomeWizardPage() {
        super(
                new WizardPageInfo(Text.literal("Welcome"), 0, 6),
                Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        MarkdownService markdownService = new MarkdownService();
        this.markdownContent = markdownService.getOrDefault("welcome.md", FALLBACK_CONTENT);
        this.configApplied = PackCoreConfig.defaultConfigSuccessfullyApplied;
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        contentContainer.gap(8);

        // Header
        contentContainer.child(
                WizardUIComponents.createHeader(
                        "Welcome to",
                        "Let's get you set up for the best experience! Take 30 seconds and read through the welcome please."
                )
        );

        // Status card
        contentContainer.child(createStatusCard());

        // Expandable details
        contentContainer.child(createExpandableDetails());

        // Bottom tip
        contentContainer.child(createBottomTip());
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {
        contentContainerRight.gap(8);
        contentContainerRight.child(createWizardOverview());
    }

    /**
     * Create status card showing config application status
     */
    private FlowLayout createStatusCard() {
        if (configApplied) {
            return createSuccessCard();
        } else {
            return createFailureCard();
        }
    }

    /**
     * Create success status card
     */
    private FlowLayout createSuccessCard() {
        FlowLayout card = WizardUIComponents.createInfoCard(
                "✓ Default Configs Successfully Applied",
                null,
                0x20_10B981,
                SUCCESS_BORDER
        );

        // Add resolution details
        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        String configName = ConfigFileRepository.getCurrentConfig().getDisplayName();

        LabelComponent details = (LabelComponent) Components.label(
                        Text.literal("We have detected your screen resolution to be " + width + "x" + height +
                                " and have enabled configs: " + configName)
                ).color(Color.ofRgb(TEXT_SECONDARY))
                .horizontalSizing(Sizing.fill(100));

        card.child(details);
        return card;
    }

    /**
     * Create failure status card with reset option
     */
    private FlowLayout createFailureCard() {
        FlowLayout card = WizardUIComponents.createInfoCard(
                "⚠ Automatic Config Application Failed",
                "Automatic config setup didn't work. You can reset and try again, " +
                        "or continue and configure manually from the main menu later.",
                0x20_EF4444,
                ERROR_BORDER
        );

        ButtonComponent resetButton = (ButtonComponent) Components.button(
                        Text.literal("Reset & Try Again"),
                        btn -> showResetConfirmation()
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20));

        card.child(resetButton);
        return card;
    }

    /**
     * Create expandable details section
     */
    private FlowLayout createExpandableDetails() {
        FlowLayout container = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(8);

        toggleButton = (ButtonComponent) Components.button(
                        Text.literal(getToggleButtonText()),
                        btn -> toggleDetails()
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/menu/blank_button.png"), 0, 0, 200, 66))
                .horizontalSizing(Sizing.fixed(200))
                .verticalSizing(Sizing.fixed(22));

        container.child(toggleButton);

        // Details section (initially hidden)
        detailsSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand(64))
                .gap(8)
                .surface(Surface.flat(0x20_000000).and(Surface.outline(0x30_FFFFFF)))
                .padding(Insets.of(8));

        detailsSection.child(WizardUIComponents.createMarkdownScroll(markdownContent)
                .sizing(Sizing.fill(100), Sizing.fill()));

        if (!detailsExpanded) {
            detailsSection.positioning(Positioning.absolute(0, -10000));
        }

        container.child(detailsSection);
        return container;
    }

    /**
     * Toggle details visibility
     */
    private void toggleDetails() {
        detailsExpanded = !detailsExpanded;
        toggleButton.setMessage(Text.literal(getToggleButtonText()));

        if (detailsExpanded) {
            detailsSection.positioning(Positioning.layout());
        } else {
            detailsSection.positioning(Positioning.absolute(0, -10000));
        }
    }

    private String getToggleButtonText() {
        return detailsExpanded ? "▼ Hide Details" : "▶ Show More Details";
    }

    /**
     * Create bottom tip panel
     */
    private FlowLayout createBottomTip() {
        FlowLayout tip = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .surface(Surface.flat(0x20_FFB84D))
                .padding(Insets.of(12));

        tip.child(Components.label(Text.literal("💡")).color(Color.ofRgb(ACCENT_SECONDARY)));

        LabelComponent tipText = (LabelComponent) Components.label(
                        Text.literal("Tip: You can reconfigure everything later using ")
                                .append(Text.literal("/packcore").setStyle(Style.EMPTY.withBold(true)))
                                .append(Text.literal(" in-game"))
                ).color(Color.ofRgb(TEXT_PRIMARY))
                .horizontalSizing(Sizing.expand());

        tip.child(tipText);
        return tip;
    }

    /**
     * Create wizard overview panel for right side
     */
    private FlowLayout createWizardOverview() {
        FlowLayout overview = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .margins(Insets.of(4));

        overview.child(Components.label(
                Text.literal("📋 Setup Steps").setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(TEXT_PRIMARY)));

        // Create step list
        FlowLayout stepsContainer = Containers.verticalFlow(Sizing.fill(98), Sizing.content()).gap(6);

        String[][] steps = {
                {"1", "Performance", "Optimize FPS & quality"},
                {"2", "Tab Design", "Choose Tab style"},
                {"3", "Background", "Pick item style"},
                {"4", "Resources", "Select texture packs"},
                {"5", "Info", "Useful tips"},
                {"6", "Apply", "Activate settings"}
        };

        for (String[] step : steps) {
            stepsContainer.child(createStepIndicator(step[0], step[1], step[2]));
        }

        // Wrap the steps container in an scroll container
        ScrollContainer<FlowLayout> stepsScroll = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                stepsContainer
        );

        stepsScroll.scrollbar(ScrollContainer.Scrollbar.vanilla());
        stepsScroll.scrollbarThiccness(6);
        stepsScroll.surface(Surface.flat(0x40_000000).and(Surface.outline(0x30_FFFFFF)));
        stepsScroll.padding(Insets.of(8));

        overview.child(stepsScroll);
        return overview;
    }

    /**
     * Create a step indicator for the overview
     */
    private FlowLayout createStepIndicator(String number, String title, String desc) {
        FlowLayout indicator = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .padding(Insets.of(8))
                .surface(Surface.flat(0x20_4A90E2).and(Surface.outline(0x30_FFFFFF)))
                .verticalAlignment(VerticalAlignment.CENTER);

        // Badge
        FlowLayout badge = (FlowLayout) Containers.horizontalFlow(Sizing.fixed(28), Sizing.fixed(28))
                .surface(Surface.flat(ACCENT_PRIMARY).and(Surface.outline(0xFF_FFFFFF)))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        badge.child(Components.label(
                Text.literal(number).setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(TEXT_PRIMARY)).shadow(true));

        indicator.child(badge);

        // Text content
        FlowLayout textContent = Containers.verticalFlow(Sizing.expand(), Sizing.content()).gap(2);

        textContent.child(Components.label(
                Text.literal(title).setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(TEXT_PRIMARY)));

        textContent.child(Components.label(Text.literal(desc))
                .color(Color.ofRgb(TEXT_SECONDARY)));

        indicator.child(textContent);
        return indicator;
    }

    /**
     * Show reset confirmation dialog
     */
    private void showResetConfirmation() {
        FlowLayout dialog = (FlowLayout) Containers.verticalFlow(Sizing.fixed(350), Sizing.content())
                .gap(12)
                .surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(WARNING_BORDER)))
                .padding(Insets.of(20))
                .positioning(Positioning.relative(50, 50));

        dialog.child(Components.label(
                Text.literal("⚠ Reset Setup?").setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(WARNING_BORDER)));

        LabelComponent msg = (LabelComponent) Components.label(
                        Text.literal("This will reset PackCore and close the game. " +
                                "The wizard will restart when you reopen Minecraft.")
                ).color(Color.ofRgb(TEXT_PRIMARY))
                .horizontalSizing(Sizing.fill(100));

        dialog.child(msg);

        // Buttons
        FlowLayout buttons = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.top(8));

        buttons.child(Components.button(
                        Text.literal("Cancel"),
                        btn -> getRootComponent().removeChild(getRootComponent().children().getLast())
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20)));

        buttons.child(Components.button(
                        Text.literal("Reset & Exit"),
                        btn -> performReset()
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20)));

        dialog.child(buttons);

        // Show as overlay
        OverlayContainer<FlowLayout> overlay = Containers.overlay(dialog);
        overlay.closeOnClick(true);
        overlay.surface(Surface.flat(0x80_000000));
        //? if <=1.21.8 {
        overlay.zIndex(10);
         //?}

        getRootComponent().child(overlay);
    }

    /**
     * Perform reset action
     */
    private void performReset() {
        PackCoreConfig.defaultConfigSuccessfullyApplied = false;
        PackCoreConfig.write(MOD_ID);
        MinecraftClient.getInstance().scheduleStop();
    }

    @Override
    protected void onContinuePressed() {
        assert this.client != null;
        this.client.setScreen(WizardNavigator.createWizardPage(1));
    }

    @Override
    protected int getContentColumnWidthPercent() {
        return 58;
    }

    @Override
    protected boolean shouldShowRightPanel() {
        return true;
    }

    @Override
    protected boolean shouldShowStatusInfo() {
        return false;
    }
}