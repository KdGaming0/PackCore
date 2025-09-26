package com.github.kd_gaming1.packcore.gui.help.guide;

import com.github.kd_gaming1.packcore.gui.util.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.help.guide.util.GuideInfo;
import com.github.kd_gaming1.packcore.gui.ui.UITheme;
import com.github.kd_gaming1.packcore.gui.help.guide.util.GuideUtil;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class BaseGuidePage extends BaseOwoScreen<FlowLayout> {

    private final Identifier backgroundTexture = Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png");
    private FlowLayout guideListContainer;

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(UiSurfaces.stretched(backgroundTexture, 1920, 1082))
                .padding(Insets.of(8));

        rootComponent.child(createHeader());
        rootComponent.child(createMainContent());

        // Load guides after UI is built
        loadGuides();
    }

    private FlowLayout createHeader() {
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(42))
                .padding(Insets.of(2))
                .verticalAlignment(VerticalAlignment.CENTER);

        header.child(Components.texture(
                Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png"),
                0, 0, 40, 40, 40, 40));

        header.child(Components.label(Text.literal("Guides & Help").styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))))
                .color(Color.ofRgb(UITheme.ACCENT_GOLD))
                .shadow(true)
                .margins(Insets.of(0, 0, 4, 4)));

        //Spacer
        header.child(Containers.horizontalFlow(Sizing.expand(), Sizing.expand()));

        header.child(Components.button(Text.literal("Close"),
                        btn -> MinecraftClient.getInstance().setScreen(null))
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/previous.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19)));

        return header;
    }

    private FlowLayout createMainContent() {
        FlowLayout mainContent = (FlowLayout) Containers.verticalFlow(Sizing.fill(98), Sizing.expand())
                .gap(6)
                .padding(Insets.of(8));

        mainContent.child(Components.label(TextOps.withColor("Welcome to the PackCore Guides & Help! Browse the list of guides, or join the Discord for help.", UITheme.TEXT_WHITE))
                .horizontalSizing(Sizing.fill(100))
                .margins(Insets.of(0, 0, 2, 0)));

        // Create the container for guide entries
        guideListContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(4);

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), guideListContainer)
                .scrollbar(ScrollContainer.Scrollbar.vanilla())
                .scrollbarThiccness(6);

        mainContent.child(scrollContainer);

        return mainContent;
    }

    private void loadGuides() {
        // Clear existing guides
        guideListContainer.clearChildren();

        List<GuideInfo> guides = GuideUtil.loadAvailableGuides();

        if (guides.isEmpty()) {
            // Show "no guides" message
            LabelComponent noGuidesLabel = Components.label(
                    TextOps.withColor("No guides found. Place .md files in the packcore/guides folder.", UITheme.TEXT_SECONDARY)
            );
            guideListContainer.child(noGuidesLabel);
        } else {
            // Add each guide as an entry
            for (GuideInfo guide : guides) {
                guideListContainer.child(createGuideEntry(guide));
            }
        }
    }

    private FlowLayout createGuideEntry(GuideInfo guide) {
        FlowLayout entry = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2)
                .padding(Insets.of(8))
                .surface(Surface.flat(0x30_000000).and(Surface.outline(0x20_FFFFFF)))
                .margins(Insets.of(0, 0, 2, 0));

        // Title
        LabelComponent titleLabel = Components.label(Text.literal(guide.getTitle()))
                .color(Color.ofRgb(UITheme.ACCENT_GOLD))
                .shadow(false);

        // Preview text
        LabelComponent previewLabel = (LabelComponent) Components.label(Text.literal(guide.getPreview()))
                .color(Color.ofRgb(UITheme.TEXT_SECONDARY))
                .sizing(Sizing.fill(100), Sizing.content());

        entry.child(titleLabel);
        entry.child(previewLabel);

        // Add hover effects and click handling
        setupGuideEntryInteraction(entry, guide);

        return entry;
    }

    private void setupGuideEntryInteraction(FlowLayout entry, GuideInfo guide) {
        // Mouse enter - highlight effect
        entry.mouseEnter().subscribe(() -> {
            entry.surface(Surface.flat(0x40_FFFFFF).and(Surface.outline(0x40_FFFFFF)));
        });

        // Mouse leave - remove highlight
        entry.mouseLeave().subscribe(() -> {
            entry.surface(Surface.flat(0x30_000000).and(Surface.outline(0x20_FFFFFF)));
        });

        // Click - open guide
        entry.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (button == 0) { // Left click
                openGuide(guide);
                return true;
            }
            return false;
        });

        // Add cursor pointer effect
        entry.cursorStyle(CursorStyle.HAND);
    }

    private void openGuide(GuideInfo guide) {
        // Load the guide content if not already loaded
        if (!guide.isContentLoaded()) {
            GuideUtil.loadGuideContent(guide);
        }

        // Open the guide viewer screen
        this.client.setScreen(new GuideViewerScreen(guide, this));
    }

    public void refresh() {
        loadGuides();
    }
}