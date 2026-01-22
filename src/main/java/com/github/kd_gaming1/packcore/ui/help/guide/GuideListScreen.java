package com.github.kd_gaming1.packcore.ui.help.guide;

import com.github.kd_gaming1.packcore.ui.screen.base.BasePackCoreScreen;
import com.github.kd_gaming1.packcore.ui.screen.components.ScreenUIComponents;
import com.github.kd_gaming1.packcore.ui.theme.UITheme;
import com.github.kd_gaming1.packcore.util.help.guide.GuideInfo;
import com.github.kd_gaming1.packcore.util.help.guide.GuideService;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.screen.Screen;
//? if >= 1.21.10 {
/*import net.minecraft.text.StyleSpriteSource;
*///?}
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Screen that displays a list of available guides for the user to browse and open.
 * Refactored to use BasePackCoreScreen and ScreenUIComponents for cleaner code.
 */
public class GuideListScreen extends BasePackCoreScreen {

    private FlowLayout guideListContainer;

    public GuideListScreen() {
        this(null);
    }

    public GuideListScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    protected Component createTitleLabel() {
        return Components.label(
                        Text.literal("Guides & Help")
                                //? if <= 1.21.8 {
                                .styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))
                                //?} else
                                //.styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte"))))
                ).color(Color.ofRgb(UITheme.ACCENT_SECONDARY))
                .shadow(true)
                .margins(Insets.of(0, 0, 4, 4));
    }

    @Override
    protected FlowLayout createMainContent() {
        FlowLayout mainContent = (FlowLayout) Containers.verticalFlow(Sizing.fill(98), Sizing.expand())
                .gap(6)
                .padding(Insets.of(8));

        // Description
        mainContent.child(Components.label(
                        TextOps.withColor(
                                "Welcome to the PackCore Guides & Help! Browse the list of guides, or join the Discord for help.",
                                UITheme.TEXT_PRIMARY
                        ))
                .horizontalSizing(Sizing.fill(100))
                .margins(Insets.of(0, 0, 2, 0)));

        // Guide list container
        guideListContainer = Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(4);

        // Wrap in scroll container
        mainContent.child(ScreenUIComponents.createScrollContainer(guideListContainer));

        // Load guides
        loadGuides();

        return mainContent;
    }

    /**
     * Load and display guides
     */
    private void loadGuides() {
        guideListContainer.clearChildren();

        List<GuideInfo> guides = GuideService.loadAvailableGuides();

        if (guides.isEmpty()) {
            guideListContainer.child(Components.label(
                    TextOps.withColor(
                            "No guides found. Place .md files in the packcore/guides folder.",
                            UITheme.TEXT_SECONDARY
                    )
            ));
        } else {
            for (GuideInfo guide : guides) {
                guideListContainer.child(createGuideEntry(guide));
            }
        }
    }

    /**
     * Create a single guide entry
     */
    private FlowLayout createGuideEntry(GuideInfo guide) {
        FlowLayout entry = ScreenUIComponents.createListEntry();

        // Title - strip markdown formatting
        String cleanTitle = ScreenUIComponents.stripMarkdown(guide.getTitle());
        LabelComponent titleLabel = Components.label(Text.literal(cleanTitle))
                .color(Color.ofRgb(UITheme.ACCENT_SECONDARY))
                .shadow(false);

        // Preview text
        String cleanPreview = ScreenUIComponents.stripMarkdown(guide.getPreview());
        LabelComponent previewLabel = (LabelComponent) Components.label(Text.literal(cleanPreview))
                .color(Color.ofRgb(UITheme.TEXT_SECONDARY))
                .sizing(Sizing.fill(100), Sizing.content());

        entry.child(titleLabel);
        entry.child(previewLabel);

        // Apply hover effects and click handling
        ScreenUIComponents.applyHoverEffects(entry, () -> openGuide(guide));

        return entry;
    }

    /**
     * Open a guide in the detail screen
     */
    private void openGuide(GuideInfo guide) {
        // Load guide content if not already loaded
        if (!guide.isContentLoaded()) {
            GuideService.loadGuideContent(guide);
        }

        // Open the guide viewer screen
        assert this.client != null;
        this.client.setScreen(new GuideDetailScreen(guide, this));
    }

    /**
     * Refresh the guide list
     */
    public void refresh() {
        loadGuides();
    }
}