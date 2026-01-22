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

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Item Background selection page - allows choosing item background styles
 */
public class ItemBackgroundWizardPage extends BaseWizardPage {

    /**
     * Item background style options
     */
    private record BackgroundOption(
            String key,
            String displayName,
            String texturePath
    ) {}

    private static final List<BackgroundOption> OPTIONS = List.of(
            new BackgroundOption("No Background", "No Background", "item_bg_none.png"),
            new BackgroundOption("Circular", "Circular Background", "item_bg_circular.png"),
            new BackgroundOption("Square", "Square Background", "item_bg_square.png")
    );

    private final WizardDataStore dataStore;
    private String selectedBackground;
    private LabelComponent selectionLabel;
    private FlowLayout contentContainer;

    public ItemBackgroundWizardPage() {
        super(
                new WizardPageInfo(
                        Text.literal("Item Background"),
                        3,
                        6
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        this.dataStore = WizardDataStore.getInstance();
        this.selectedBackground = dataStore.getItemBackground();

        // Default to "None" if nothing selected
        if (selectedBackground.isEmpty()) {
            selectedBackground = "None";
        }
    }


    @Override
    protected void buildContent(FlowLayout contentContainer) {
        this.contentContainer = contentContainer;

        // Apply frame texture
        contentContainer.surface(TextureSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/wizard/frame.png"), 1920, 1080
        ));
        contentContainer.padding(Insets.of(22, 22, 18, 18));

        // Header
        contentContainer.child(
                WizardUIComponents.createHeader(
                        "Choose your preferred item background style for",
                        "Select how items should appear in your inventory and menus. " +
                                "You can choose no background, a circular design, or a full square background. " +
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

        if ("None".equals(selectedBackground)) {
            displayText = "👇 Click an image below to choose your item background style";
            color = ACCENT_SECONDARY;
        } else {
            displayText = "✓ Selected: " + selectedBackground;
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

        // Create each option
        for (BackgroundOption option : OPTIONS) {
            choicesRow.child(createBackgroundOption(option));
        }

        return choicesRow;
    }

    /**
     * Create a single background option
     */
    private FlowLayout createBackgroundOption(BackgroundOption option) {
        boolean isSelected = option.key.equals(selectedBackground);

        FlowLayout wrapper = (FlowLayout) Containers.verticalFlow(Sizing.fill(32), Sizing.expand())
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.of(8))
                .cursorStyle(CursorStyle.HAND);

        // Title
        wrapper.child(Components.label(
                        Text.literal(option.displayName).setStyle(Style.EMPTY.withBold(true))
                ).color(Color.ofRgb(TEXT_PRIMARY))
                .margins(Insets.of(4, 4, 2, 4)));

        // Image container
        Identifier textureId = Identifier.of(MOD_ID, "textures/gui/wizard/" + option.texturePath);

        Surface imageSurface = isSelected
                ? TextureSurfaces.scaledContain(textureId, 555, 666).and(Surface.outline(SUCCESS_BORDER))
                : TextureSurfaces.scaledContain(textureId, 555, 666);

        FlowLayout imageContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand())
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .surface(imageSurface)
                .margins(Insets.of(4))
                .cursorStyle(CursorStyle.HAND);

        // Click handler
        //? if <= 1.21.8 {
        imageContainer.mouseDown().subscribe((mouseX, mouseY, button) -> {
         //?} else
        //imageContainer.mouseDown().subscribe((click, doubled) -> {
            selectBackground(option.key);
            return true;
        });

        wrapper.child(imageContainer);
        return wrapper;
    }

    /**
     * Handle background selection
     */
    private void selectBackground(String background) {
        this.selectedBackground = background;
        dataStore.setItemBackground(background);

        // Update label
        if (this.selectionLabel != null) {
            this.selectionLabel.text(
                    Text.literal("✓ Selected: " + selectedBackground)
                            .setStyle(Style.EMPTY.withBold(true))
            ).color(Color.ofRgb(SUCCESS_BORDER));
        }

        // Rebuild to update borders (simpler than tracking all containers)
        rebuildContent();
    }

    /**
     * Rebuild the content to update selection states
     */
    private void rebuildContent() {
        if (this.contentContainer != null) {
            this.contentContainer.clearChildren();
            buildContent(this.contentContainer);
        }
    }

    @Override
    protected void onContinuePressed() {
        assert this.client != null;
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