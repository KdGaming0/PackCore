package com.github.kd_gaming1.packcore.ui.screen.wizard;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.github.kd_gaming1.packcore.modpack.ModpackInfo;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

public abstract class BaseWizardPage extends BaseOwoScreen<StackLayout> {

    protected static final int HEADER_HEIGHT = 35;
    protected static final int CONTENT_PADDING = getScaledPadding();
    protected static final int PROGRESS_BAR_WIDTH = 125;

    protected ButtonComponent primaryButton;
    private boolean primaryButtonInitialActive = true;

    private StackLayout rootComponent;

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
        this.rootComponent = rootComponent;

        rootComponent.surface(TextureSurfaces.stretched(backgroundTexture, 1920, 1082));

        FlowLayout mainLayout = createMainLayout();

        mainLayout.child(createHeader());

        if (shouldShowStatusInfo()) {
            mainLayout.child(createStatusPanel());
        }

        FlowLayout contentContainer = createContentContainer();
        buildContent(contentContainer);
        mainLayout.child(contentContainer);

        if (shouldShowRightPanel()) {
            FlowLayout contentContainerRight = createContentContainerRight();
            buildContentRight(contentContainerRight);
            mainLayout.child(contentContainerRight);
        }

        mainLayout.child(createFooter());

        rootComponent.child(
                Containers.stack(Sizing.fill(100), Sizing.fill(100))
                        .child(Components.box(Sizing.fill(100), Sizing.fill(100)).color(Color.ofArgb(0x80_000000)))
                        .child(mainLayout)
        );
    }

    protected StackLayout getRootComponent() {
        return rootComponent;
    }

    private FlowLayout createMainLayout() {
        return (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
                .padding(Insets.of(CONTENT_PADDING));
    }

    private FlowLayout createHeader() {
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(getContentColumnWidthPercent()), Sizing.fixed(HEADER_HEIGHT))
                .padding(Insets.of(CONTENT_PADDING - 6))
                .verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent titleLabel = (LabelComponent) Components.label(pageInfo.title.copy()
                .styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte")))))
                .color(Color.ofRgb(ACCENT_SECONDARY))
                .shadow(true)
                .margins(Insets.of(0, 0, 4, 4));

        header.child(titleLabel);

        FlowLayout progressIndicator = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.fill(100))
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        progressIndicator.child(Components.label(Text.literal("| Step " + pageInfo.currentStep() + " of " + pageInfo.totalSteps())
                .styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte")))))
                .color(Color.ofRgb(ACCENT_SECONDARY))
                .shadow(true)
        );

        progressIndicator.child(createProgressBar());

        header.child(progressIndicator);

        return header;
    }

    private Component createProgressBar() {
        int barHeight = 6;

        FlowLayout progressContainer = (FlowLayout) Containers.horizontalFlow(
                        Sizing.fixed(PROGRESS_BAR_WIDTH),
                        Sizing.fixed(barHeight)
                )
                .surface(Surface.flat(0x60_000000));

        int progressWidth = (int) (PROGRESS_BAR_WIDTH * ((double) pageInfo.currentStep() / pageInfo.totalSteps()));

        Component progressFill = Components.box(Sizing.fixed(progressWidth), Sizing.fill(100))
                .color(Color.ofRgb(ACCENT_PRIMARY));

        Component progressHighlight = Components.box(Sizing.fixed(2), Sizing.fill(100))
                .color(Color.ofRgb(ACCENT_SECONDARY))
                .positioning(Positioning.absolute(progressWidth - 2, 0));

        return Containers.stack(Sizing.fixed(PROGRESS_BAR_WIDTH), Sizing.fixed(barHeight))
                .child(progressContainer)
                .child(progressFill)
                .child(progressHighlight);
    }

    private FlowLayout createStatusPanel() {
        FlowLayout statusPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(38), Sizing.content())
                .gap(8)
                .surface(TextureSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/wizard/small_info_box.png"), 722, 338))
                .padding(Insets.of(10))
                .verticalAlignment(VerticalAlignment.CENTER)
                .positioning(Positioning.relative(100, 0));

        FlowLayout messageSection = Containers.verticalFlow(Sizing.expand(), Sizing.content())
                .gap(4);

        FlowLayout headerRow = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(6)
                .verticalAlignment(VerticalAlignment.CENTER);

        headerRow.child(Components.label(
                Text.literal("✓ Configuration Successful")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_PRIMARY)));

        messageSection.child(headerRow);

        LabelComponent detailLabel = Components.label(
                Text.literal("PackCore has automatically applied the config below based on your screen resolution.").setStyle(Style.EMPTY)
        ).color(Color.ofRgb(TEXT_SECONDARY));
        detailLabel.horizontalSizing(Sizing.fill(100));
        messageSection.child(detailLabel);

        LabelComponent infoLabel = (LabelComponent) Components.label(
                        Text.literal("Applied configuration: " + ConfigFileRepository.getCurrentConfig().getDisplayName())
                                .setStyle(Style.EMPTY.withItalic(Boolean.TRUE))
                ).color(Color.ofRgb(TEXT_SECONDARY))
                .margins(Insets.of(2, 2, 2, 2));
        infoLabel.horizontalSizing(Sizing.fill(100));
        messageSection.child(infoLabel.margins(Insets.top(2)));

        statusPanel.child(messageSection);

        return statusPanel;
    }

    private FlowLayout createContentContainer() {
        return (FlowLayout) Containers.verticalFlow(Sizing.fill(getContentColumnWidthPercent()), Sizing.expand())
                .gap(12)
                .surface(TextureSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/wizard/info_box.png"), 1142, 934))
                .padding(Insets.of(CONTENT_PADDING + 10, CONTENT_PADDING + 10, CONTENT_PADDING + 4, CONTENT_PADDING + 4))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.of(0, 28, 0, 0));
    }

    private FlowLayout createContentContainerRight() {
        return (FlowLayout) Containers.verticalFlow(Sizing.fill(getContentColumnWidthRightPercent()), Sizing.fill(100))
                .gap(12)
                .surface(TextureSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/wizard/box.png"), 607, 755))
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

        this.primaryButton.active = this.primaryButtonInitialActive;
        return this.primaryButton;
    }

    private static int getScaledPadding() {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        return screenWidth < 900 ? 8 : (screenWidth > 1400 ? 20 : 15);
    }

    protected FlowLayout createSelectionCard(boolean selected) {
        int borderColor = selected ? SUCCESS_BORDER : 0x40_FFFFFF;

        return (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .surface((Surface.outline(borderColor)))
                .padding(Insets.of(12))
                .cursorStyle(CursorStyle.HAND);
    }

    protected abstract void buildContent(FlowLayout contentContainer);

    protected abstract void buildContentRight(FlowLayout contentContainerRight);

    protected abstract void onContinuePressed();

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

    public record WizardPageInfo(
            Text title,
            int currentStep,
            int totalSteps
    ) {}
}