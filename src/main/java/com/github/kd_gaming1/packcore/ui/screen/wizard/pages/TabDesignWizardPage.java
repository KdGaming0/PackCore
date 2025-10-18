package com.github.kd_gaming1.packcore.ui.screen.wizard.pages;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import com.github.kd_gaming1.packcore.ui.screen.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.util.wizard.WizardDataStore;
import com.github.kd_gaming1.packcore.ui.screen.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.modpack.ModpackInfo;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class TabDesignWizardPage extends BaseWizardPage {
    private final ModpackInfo modpackInfo;

    // UI state fields
    private String selectedDesign = "None";
    private LabelComponent selectionLabel; // placed in main content (above images)

    private FlowLayout classicImageContainer;
    private FlowLayout modernImageContainer;

    // image containers (outline will be put on these, so only the image gets bordered)

    public TabDesignWizardPage() {
        super(
                new BaseWizardPage.WizardPageInfo(
                        Text.literal("Tab design"),
                        2,
                        5 // Total wizard steps
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        this.modpackInfo = PackCore.getModpackInfo();

        // Restore saved state
        String savedDesign = WizardDataStore.getInstance().getTabDesign();
        if (!savedDesign.isEmpty()) {
            this.selectedDesign = savedDesign;
        }
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        contentContainer.surface(TextureSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/wizard/frame.png"), 1920, 1080));
        contentContainer.padding(Insets.of(24, 20, 18, 18));

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
        FlowLayout header = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .margins(Insets.of(0, 0, 36, 36));

        // Create welcome text
        Text welcomeText = TextOps.concat(
                TextOps.withColor("Choose your preferred tab design to use in-game when playing with ", TEXT_WHITE),
                Text.literal(modpackInfo.getName()).setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        );

        LabelComponent welcomeTitle = Components.label(welcomeText);

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("The pack has two mods that change the tab list: SkyHanni and Skyblocker. You can not use both at the same time, so decide which one you like best—and select it. (Tip: Click the image)")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.of(2, 0, 0, 0)).sizing(Sizing.expand(), Sizing.content());

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

    private FlowLayout createImagesChoiceSection() {
        // Remove ScrollContainer and use direct FlowLayout with expand sizing for responsive height
        FlowLayout choicesRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.expand())
                .gap(12) // Increased gap for better visual separation
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        // CLASSIC card - improved sizing and centering
        FlowLayout classicWrapper = (FlowLayout) Containers.verticalFlow(Sizing.fill(48), Sizing.expand())
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.of(8))
                .cursorStyle(CursorStyle.HAND); // Add hand cursor for clickability

        // Add title text closer to image
        classicWrapper.child(Components.label(
                TextOps.withColor("SkyHanni Compact Tab", TEXT_WHITE).setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).margins(Insets.of(4, 4, 2, 4))); // Reduced bottom margin

        // Improved image container with responsive height and selection border
        FlowLayout classicImageContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand())
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .surface(TextureSurfaces.scaledContain(Identifier.of(MOD_ID, "textures/gui/wizard/skyhanni_tab.png"), 2560, 1441))
                .margins(Insets.of(4))
                .cursorStyle(CursorStyle.HAND); // Hand cursor on image

        // Store reference for selection border updates
        this.classicImageContainer = classicImageContainer;

        classicImageContainer.mouseDown().subscribe((mouseX, mouseY, button) -> {
            selectDesign("SkyHanni");
            return true;
        });

        classicWrapper.child(classicImageContainer);

        // MODERN card - same improvements
        FlowLayout modernWrapper = (FlowLayout) Containers.verticalFlow(Sizing.fill(48), Sizing.expand())
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.of(8))
                .cursorStyle(CursorStyle.HAND);

        modernWrapper.child(Components.label(
                TextOps.withColor("Skyblocker Fancy TabList", TEXT_WHITE).setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).margins(Insets.of(4, 4, 2, 4))); // Reduced bottom margin

        FlowLayout modernImageContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand())
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .surface(TextureSurfaces.scaledContain(Identifier.of(MOD_ID, "textures/gui/wizard/skyblocker_tab.png"), 2560, 1441))
                .margins(Insets.of(4))
                .cursorStyle(CursorStyle.HAND);

        // Store reference for selection border updates
        this.modernImageContainer = modernImageContainer;

        modernImageContainer.mouseDown().subscribe((mouseX, mouseY, button) -> {
            selectDesign("Skyblocker");
            return true;
        });

        modernWrapper.child(modernImageContainer);

        choicesRow.child(classicWrapper);
        choicesRow.child(modernWrapper);

        return choicesRow; // Return FlowLayout directly instead of ScrollContainer
    }

    private void selectDesign(String design) {
        this.selectedDesign = design;

        // Store in data manager
        WizardDataStore.getInstance().setTabDesign(design);

        if (this.selectionLabel != null) {
            this.selectionLabel.text(TextOps.withColor("Selected TabList: " + this.selectedDesign, ACCENT_GOLD)
                    .setStyle(Style.EMPTY.withBold(Boolean.TRUE)));
        }

        // Update border colors based on selection
        if (this.classicImageContainer != null && this.modernImageContainer != null) {
            if ("SkyHanni".equals(design)) {
                // Add green border to selected image
                this.classicImageContainer.surface(
                        Surface.outline(Color.GREEN.argb()).and(
                                TextureSurfaces.scaledContain(Identifier.of(MOD_ID, "textures/gui/wizard/skyhanni_tab.png"), 2560, 1441)
                        )
                );
                // Remove border from unselected image
                this.modernImageContainer.surface(
                        TextureSurfaces.scaledContain(Identifier.of(MOD_ID, "textures/gui/wizard/skyblocker_tab.png"), 2560, 1441)
                );
            } else if ("Skyblocker".equals(design)) {
                // Add green border to selected image
                this.modernImageContainer.surface(
                        Surface.outline(Color.GREEN.argb()).and(
                                TextureSurfaces.scaledContain(Identifier.of(MOD_ID, "textures/gui/wizard/skyblocker_tab.png"), 2560, 1441)
                        )
                );
                // Remove border from unselected image
                this.classicImageContainer.surface(
                        TextureSurfaces.scaledContain(Identifier.of(MOD_ID, "textures/gui/wizard/skyhanni_tab.png"), 2560, 1441)
                );
            }
        }
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