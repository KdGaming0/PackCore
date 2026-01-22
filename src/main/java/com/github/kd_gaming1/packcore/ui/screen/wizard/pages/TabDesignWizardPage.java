package com.github.kd_gaming1.packcore.ui.screen.wizard.pages;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.ui.screen.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.util.wizard.WizardDataStore;
import com.github.kd_gaming1.packcore.ui.screen.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.ui.screen.components.WizardUIComponents;
import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Tab Design selection page - allows choosing between SkyHanni and Skyblocker tab styles
 */
public class TabDesignWizardPage extends BaseWizardPage {

    /**
     * Tab design options configuration
     */
    private enum TabDesignOption {
        SKYHANNI("SkyHanni", "SkyHanni Compact Tab", "skyhanni_tab.png"),
        SKYBLOCKER("Skyblocker", "Skyblocker Fancy TabList", "skyblocker_tab.png");

        private final String key;
        private final String displayName;
        private final String texturePath;

        TabDesignOption(String key, String displayName, String texturePath) {
            this.key = key;
            this.displayName = displayName;
            this.texturePath = texturePath;
        }
    }

    private final WizardDataStore dataStore;
    private String selectedDesign;
    private LabelComponent selectionLabel;
    private FlowLayout classicImageContainer;
    private FlowLayout modernImageContainer;

    public TabDesignWizardPage() {
        super(
                new WizardPageInfo(
                        Text.literal("Tab design"),
                        2,
                        6
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        this.dataStore = WizardDataStore.getInstance();
        this.selectedDesign = dataStore.getTabDesign();

        // Default to "None" if nothing selected
        if (selectedDesign.isEmpty()) {
            selectedDesign = "None";
        }
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        // Apply frame texture
        contentContainer.surface(TextureSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/wizard/frame.png"), 1920, 1080
        ));
        contentContainer.padding(Insets.of(22, 22, 18, 18));

        // Header
        contentContainer.child(
                WizardUIComponents.createHeader(
                        "Choose your preferred tab design to use in-game when playing with",
                        "The pack has two mods that change the tab list: SkyHanni and Skyblocker. " +
                                "You can not use both at the same time, so decide which one you like best—and select it. " +
                                "(Tip: Click the image)"
                ).margins(Insets.horizontal(34))
        );

        // Selection label
        contentContainer.child(createSelectionLabel());

        // Image choices
        contentContainer.child(createImageChoices());
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {
        // No right panel for this page
    }

    /**
     * Create the selection status label
     */
    private LabelComponent createSelectionLabel() {
        String displayText;
        int color;

        if ("None".equals(selectedDesign)) {
            displayText = "👇 Click an image below to choose your tab design";
            color = ACCENT_SECONDARY;
        } else {
            displayText = "✓ Selected: " + selectedDesign;
            color = SUCCESS_BORDER;
        }

        this.selectionLabel = WizardUIComponents.createStatusLabel(displayText, null, color);
        this.selectionLabel.margins(Insets.left(16));

        return this.selectionLabel;
    }

    /**
     * Create the image choice section
     */
    private FlowLayout createImageChoices() {
        FlowLayout choicesRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.expand())
                .gap(12)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        // SkyHanni option
        choicesRow.child(createTabDesignOption(TabDesignOption.SKYHANNI));

        // Skyblocker option
        choicesRow.child(createTabDesignOption(TabDesignOption.SKYBLOCKER));

        return choicesRow;
    }

    /**
     * Create a single tab design option
     */
    private FlowLayout createTabDesignOption(TabDesignOption option) {
        FlowLayout wrapper = (FlowLayout) Containers.verticalFlow(Sizing.fill(48), Sizing.expand())
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.of(0,10,8,8))
                .cursorStyle(CursorStyle.HAND);

        // Title
        wrapper.child(Components.label(
                        Text.literal(option.displayName).setStyle(Style.EMPTY.withBold(true))
                ).color(Color.ofRgb(TEXT_PRIMARY))
                .margins(Insets.of(4, 4, 2, 4)));

        // Image container
        boolean isSelected = option.key.equals(selectedDesign);
        Identifier textureId = Identifier.of(MOD_ID, "textures/gui/wizard/" + option.texturePath);

        FlowLayout imageContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand())
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .surface(TextureSurfaces.scaledContain(textureId, 2560, 1441))
                .margins(Insets.of(4))
                .cursorStyle(CursorStyle.HAND);

        // Apply selection border if selected
        if (isSelected) {
            imageContainer.surface(
                    Surface.outline(SUCCESS_BORDER).and(
                            TextureSurfaces.scaledContain(textureId, 2560, 1441)
                    )
            );
        }

        // Store references for updating
        if (option == TabDesignOption.SKYHANNI) {
            this.classicImageContainer = imageContainer;
        } else {
            this.modernImageContainer = imageContainer;
        }

        // Click handler
        //? if <= 1.21.8 {
        /*imageContainer.mouseDown().subscribe((mouseX, mouseY, button) -> {
         *///?} else
        imageContainer.mouseDown().subscribe((click, doubled) -> {
            selectDesign(option.key);
            return true;
        });

        wrapper.child(imageContainer);
        return wrapper;
    }

    /**
     * Handle design selection
     */
    private void selectDesign(String design) {
        this.selectedDesign = design;
        dataStore.setTabDesign(design);

        // Update label
        if (this.selectionLabel != null) {
            this.selectionLabel.text(
                    Text.literal("✓ Selected: " + selectedDesign)
                            .setStyle(Style.EMPTY.withBold(true))
            ).color(Color.ofRgb(SUCCESS_BORDER));
        }

        // Update borders
        updateBorders();
    }

    /**
     * Update image borders based on selection
     */
    private void updateBorders() {
        if (classicImageContainer != null && modernImageContainer != null) {
            // Remove all borders first
            classicImageContainer.surface(
                    TextureSurfaces.scaledContain(
                            Identifier.of(MOD_ID, "textures/gui/wizard/skyhanni_tab.png"),
                            2560, 1441
                    )
            );

            modernImageContainer.surface(
                    TextureSurfaces.scaledContain(
                            Identifier.of(MOD_ID, "textures/gui/wizard/skyblocker_tab.png"),
                            2560, 1441
                    )
            );

            // Add border to selected
            if ("SkyHanni".equals(selectedDesign)) {
                classicImageContainer.surface(
                        TextureSurfaces.scaledContain(
                                        Identifier.of(MOD_ID, "textures/gui/wizard/skyhanni_tab.png"),
                                        2560, 1441
                                ).and(Surface.outline(SUCCESS_BORDER)
                        )
                );
            } else if ("Skyblocker".equals(selectedDesign)) {
                modernImageContainer.surface(
                        TextureSurfaces.scaledContain(
                                        Identifier.of(MOD_ID, "textures/gui/wizard/skyblocker_tab.png"),
                                        2560, 1441
                                ).and(Surface.outline(SUCCESS_BORDER)
                        )
                );
            }
        }
    }

    @Override
    protected void onContinuePressed() {
        assert this.client != null;
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