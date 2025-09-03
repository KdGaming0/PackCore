package com.github.kd_gaming1.packcore.gui.help.introduction;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.help.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.help.WizardNavigator;
import com.github.kd_gaming1.packcore.util.ModpackInfo;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.event.MouseDown;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class IntroductionScreenPageThree extends BaseWizardPage {

    private final ModpackInfo modpackInfo;

    // UI state fields
    private String selectedDesign = "None";
    private LabelComponent selectionLabel; // placed in main content (above images)

    // image containers (outline will be put on these, so only the image gets bordered)
    private FlowLayout classicImageContainer;
    private FlowLayout modernImageContainer;

    public IntroductionScreenPageThree() {
        super(
                new BaseWizardPage.WizardPageInfo(
                        Text.literal("Tab design"),
                        3,
                        5 // Total wizard steps
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/test_temp.png")
        );

        this.modpackInfo = PackCore.getModpackInfo();
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {

        contentContainer.margins(Insets.bottom(42));
        // Welcome header
        contentContainer.child(createWelcomeHeader());

        // Selection label in the main box (just below header, above the choices)
        contentContainer.child(createSelectionLabel());

        // Images choices inside a scroll container (side-by-side by default, stacks/scrolls on small windows)
        contentContainer.child(createImagesChoiceSection());

        // Quick info section anchored below (unchanged; selection label is not inside this box)
        contentContainer.child(createQuickInfoSection().positioning(Positioning.relative(0, 100)));
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {

    }

    private FlowLayout createWelcomeHeader() {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6);

        // Create welcome text
        Text welcomeText = TextOps.concat(
                TextOps.withColor("Choose your preferred tab design to use in-game when playing with", TEXT_WHITE),
                Text.literal(modpackInfo.getName()).setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        );

        LabelComponent welcomeTitle = Components.label(welcomeText);

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("The pack has two mods that change the tab list: SkyHanni and Skyblocker. You can use both at the same time, so decide which one you like best—and select it.")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.of(2, 0, 2, 0)).sizing(Sizing.expand(), Sizing.content());

        header.child(welcomeTitle).child(subtitle);

        return header;
    }

    private LabelComponent createSelectionLabel() {
        this.selectionLabel = Components.label(
                TextOps.withColor("Selected TabList: " + this.selectedDesign, ACCENT_GOLD)
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        );

        // some spacing/margins so it's visually separated
        this.selectionLabel.margins(Insets.of(0, 0, 6, 0));
        return this.selectionLabel;
    }

    private ScrollContainer<FlowLayout> createImagesChoiceSection() {
        FlowLayout choicesRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8);

        final int textureWidth = 256;
        final int textureHeight = 203;

        // CLASSIC card
        FlowLayout classicWrapper = (FlowLayout) Containers.verticalFlow(Sizing.fill(45), Sizing.content())
                .verticalAlignment(VerticalAlignment.CENTER)
                .surface(Surface.flat(0x00_000000))
                .margins(Insets.of(4));

        classicWrapper.child(Components.label(
                TextOps.withColor("SkyHanni Compact Tab", TEXT_WHITE).setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).margins(Insets.of(4)));

        classicImageContainer = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .surface(Surface.flat(0x00_000000))
                .verticalAlignment(VerticalAlignment.CENTER);

        TextureComponent classicTexture = (TextureComponent) Components.texture(
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/classic_menu.png"),
                0, 0,
                textureWidth, textureHeight,
                textureWidth, textureHeight
        ).sizing(Sizing.fill(), Sizing.content());

        System.out.println(classicTexture.width());

        classicTexture.mouseDown().subscribe((MouseDown) (mouseX, mouseY, button) -> {
            selectDesign("SkyHanni");
            return true;
        });

        classicImageContainer.child(classicTexture);
        classicWrapper.child(classicImageContainer);

        ButtonComponent classicUseBtn = (ButtonComponent) Components.button(Text.literal("Use SkyHanni tab list"), btn -> selectDesign("SkyHanni"))
                .textShadow(false)
                .renderer(ButtonComponent.Renderer.flat(0xFF_FFC107, 0xFF_FFD54F, 0xFF_A0A0A0))
                .margins(Insets.of(4, 2, 2, 2))
                .sizing(Sizing.content(3), Sizing.fixed(20));

        classicWrapper.child(Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .child(classicUseBtn)
                .horizontalAlignment(HorizontalAlignment.CENTER)
        );

        // MODERN card
        FlowLayout modernWrapper = (FlowLayout) Containers.verticalFlow(Sizing.fill(45), Sizing.content())
                .verticalAlignment(VerticalAlignment.CENTER)
                .surface(Surface.flat(0x00_000000))
                .margins(Insets.of(4));

        modernWrapper.child(Components.label(
                TextOps.withColor("Skyblocker Fancy TabList", TEXT_WHITE).setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).margins(Insets.of(4)));

        modernImageContainer = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .surface(Surface.flat(0x00_000000))
                .verticalAlignment(VerticalAlignment.CENTER);

        TextureComponent modernTexture = (TextureComponent) Components.texture(
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/fancy_menu.png"),
                0, 0,
                textureWidth, textureHeight
        ).sizing(Sizing.fill(), Sizing.content());

        modernTexture.mouseDown().subscribe((MouseDown) (mouseX, mouseY, button) -> {
            selectDesign("Skyblocker");
            return true;
        });

        modernImageContainer.child(modernTexture);
        modernWrapper.child(modernImageContainer);

        ButtonComponent modernUseBtn = (ButtonComponent) Components.button(Text.literal("Use Skyblocker Fancy TabList"), btn -> selectDesign("Skyblocker"))
                .textShadow(false)
                .renderer(ButtonComponent.Renderer.flat(0xFF_8BC34A, 0xFFA5D6A7, 0xFF_A0A0A0))
                .margins(Insets.of(4, 2, 2, 2))
                .sizing(Sizing.content(3), Sizing.fixed(20));

        modernWrapper.child(Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .child(modernUseBtn)
                .horizontalAlignment(HorizontalAlignment.CENTER)
        );

        choicesRow.child(classicWrapper);
        choicesRow.child(modernWrapper);

        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                choicesRow
        );

        scroll.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scroll.scrollbarThiccness(6);
        scroll.surface(Surface.flat(0x00_000000));
        scroll.padding(Insets.of(6));
        scroll.margins(Insets.bottom(45));

        return scroll;
    }

    private FlowLayout createQuickInfoSection() {
        FlowLayout infoSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x20_FFD700).and(Surface.outline(ACCENT_GOLD)))
                .padding(Insets.of(6));

        LabelComponent infoTitle = Components.label(
                TextOps.withColor("Quick Links", ACCENT_GOLD)
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        );

        infoSection.child(infoTitle);

        // Discord link as a transparent button
        if (modpackInfo.getDiscord() != null && !modpackInfo.getDiscord().isEmpty()) {
            Text discordText = TextOps.concat(
                    TextOps.withColor("💬 Discord: ", TEXT_WHITE),
                    TextOps.withColor(modpackInfo.getDiscord(), TextOps.color(Formatting.AQUA))
            );

            ButtonComponent discordButton = (ButtonComponent) Components.button(discordText, btn -> {
                        Util.getOperatingSystem().open(modpackInfo.getDiscord());
                    })
                    .renderer(ButtonComponent.Renderer.flat(0x00000000, 0x00000000, 0x00000000)) // fully transparent background
                    .textShadow(false)
                    .sizing(Sizing.content(), Sizing.fixed(16));

            infoSection.child(discordButton);
        }

        // Issue tracker link as a transparent button
        if (modpackInfo.getIssueTracker() != null && !modpackInfo.getIssueTracker().isEmpty()) {
            Text issueText = TextOps.concat(
                    TextOps.withColor("🐛 Issues: ", TEXT_WHITE),
                    TextOps.withColor(modpackInfo.getIssueTracker(), TextOps.color(Formatting.AQUA))
            );

            ButtonComponent issueButton = (ButtonComponent) Components.button(issueText, btn -> {
                        Util.getOperatingSystem().open(modpackInfo.getIssueTracker());
                    })
                    .renderer(ButtonComponent.Renderer.flat(0x00000000, 0x00000000, 0x00000000)) // fully transparent background
                    .textShadow(false)
                    .sizing(Sizing.content(), Sizing.fixed(16));

            infoSection.child(issueButton);
        }

        return infoSection;
    }

    // Central selection handler updates label and the image-containers' surfaces to show outline on selected design (outline only on image container)
    private void selectDesign(String design) {
        this.selectedDesign = design;

        if (this.selectionLabel != null) {
            this.selectionLabel.text(TextOps.withColor("Selected menu: " + this.selectedDesign, ACCENT_GOLD)
                    .setStyle(Style.EMPTY.withBold(Boolean.TRUE)));
        }

        final int outlineColor = 0xFF_FFD700; // gold outline

        if ("SkyHanni".equals(design)) {
            if (classicImageContainer != null) classicImageContainer.surface(Surface.outline(outlineColor));
            if (modernImageContainer != null) modernImageContainer.surface(Surface.flat(0x00_000000));
        } else if ("Skyblocker".equals(design)) {
            if (modernImageContainer != null) modernImageContainer.surface(Surface.outline(outlineColor));
            if (classicImageContainer != null) classicImageContainer.surface(Surface.flat(0x00_000000));
        } else {
            if (classicImageContainer != null) classicImageContainer.surface(Surface.flat(0x00_000000));
            if (modernImageContainer != null) modernImageContainer.surface(Surface.flat(0x00_000000));
        }

        // TODO: persist selection or navigate next
    }

    @Override
    protected void onContinuePressed() {
        this.client.setScreen(WizardNavigator.createWizardPage(4));
    }

    @Override
    protected int getContentColumnWidthPercent() {
        return 100;
    }

    @Override
    protected boolean shouldShowStatusInfo() {
        return false;
    }
}
