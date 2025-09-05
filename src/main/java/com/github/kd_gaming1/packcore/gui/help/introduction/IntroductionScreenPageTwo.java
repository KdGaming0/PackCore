package com.github.kd_gaming1.packcore.gui.help.introduction;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.help.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.help.WizardNavigator;
import com.github.kd_gaming1.packcore.util.MarkdownFileUtil;
import com.github.kd_gaming1.packcore.util.ModpackInfo;
import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.lavendermd.feature.*;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class IntroductionScreenPageTwo extends BaseWizardPage{
    private final ModpackInfo modpackInfo;

    // UI state fields
    private String selectedDesign = "None";
    private LabelComponent selectionLabel; // placed in main content (above images)

    // image containers (outline will be put on these, so only the image gets bordered)

    public IntroductionScreenPageTwo() {
        super(
                new BaseWizardPage.WizardPageInfo(
                        Text.literal("Tab design"),
                        2,
                        5 // Total wizard steps
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/test_temp.png")
        );

        this.modpackInfo = PackCore.getModpackInfo();
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {

        contentContainer.margins(Insets.bottom(28));
        // Welcome header
        contentContainer.child(createWelcomeHeader());

        // Selection label in the main box (just below header, above the choices)
        contentContainer.child(createSelectionLabel());

        // Images choices inside a scroll container (side-by-side by default, stacks/scrolls on small windows)
        contentContainer.child(createImagesChoiceSection());
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {

    }

    private FlowLayout createWelcomeHeader() {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6);

        // Create welcome text
        Text welcomeText = TextOps.concat(
                TextOps.withColor("Choose your preferred tab design to use in-game when playing with ", TEXT_WHITE),
                Text.literal(modpackInfo.getName()).setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        );

        LabelComponent welcomeTitle = Components.label(welcomeText);

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("The pack has two mods that change the tab list: SkyHanni and Skyblocker. You can not use both at the same time, so decide which one you like best—and select it. (Tip: Scroll down or click the image)")
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

        return this.selectionLabel;
    }

    private ScrollContainer<FlowLayout> createImagesChoiceSection() {
        FlowLayout choicesRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(2);

        // CLASSIC card
        FlowLayout classicWrapper = (FlowLayout) Containers.verticalFlow(Sizing.fill(45), Sizing.content())
                .verticalAlignment(VerticalAlignment.CENTER)
                .margins(Insets.of(4));

        classicWrapper.child(Components.label(
                TextOps.withColor("SkyHanni Compact Tab", TEXT_WHITE).setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).margins(Insets.of(4)));

        FlowLayout classicImageContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .surface(UiSurfaces.scaledContain(Identifier.of(MOD_ID, "textures/gui/wizard/skyhanni_tab.png"), 2560, 1441));

        classicImageContainer.mouseDown().subscribe((MouseDown) (mouseX, mouseY, button) -> {
            selectDesign("SkyHanni");
            return true;
        });

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
                .margins(Insets.of(4));

        modernWrapper.child(Components.label(
                TextOps.withColor("Skyblocker Fancy TabList", TEXT_WHITE).setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).margins(Insets.of(4)));

        FlowLayout modernImageContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .surface(UiSurfaces.scaledContain(Identifier.of(MOD_ID, "textures/gui/wizard/skyblocker_tab.png"), 2560, 1441));


        modernImageContainer.mouseDown().subscribe((MouseDown) (mouseX, mouseY, button) -> {
            selectDesign("Skyblocker");
            return true;
        });

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

        return scroll;
    }

    // Central selection handler updates label and the image-containers' surfaces to show outline on selected design (outline only on image container)
    private void selectDesign(String design) {
        this.selectedDesign = design;

        if (this.selectionLabel != null) {
            this.selectionLabel.text(TextOps.withColor("Selected menu: " + this.selectedDesign, ACCENT_GOLD)
                    .setStyle(Style.EMPTY.withBold(Boolean.TRUE)));
        }

        // TODO: persist selection or navigate next
    }

    @Override
    protected void onContinuePressed() {
        this.client.setScreen(WizardNavigator.createWizardPage(3));
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