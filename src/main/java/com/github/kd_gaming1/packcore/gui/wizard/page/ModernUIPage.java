package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.component.MarkdownComponent;
import com.github.kd_gaming1.packcore.gui.component.MultiSelectList;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.integration.ModernUIConfigurator;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/** Wizard step -- lets the user toggle individual Modern UI features on or off. */
public class ModernUIPage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ModernUIPage");
    private static final Component PAGE_TITLE =
            Component.translatable("gui.packcore.wizard.page.modern_ui.title");

    public static final String FEATURES_KEY = "modernuiFeatures";
    /** Sentinel so we only seed defaults from config once per wizard session. */
    private static final String INIT_KEY = "modernuiFeatures_ready";

    private static final int PADDING = 16;
    private static final int COLUMN_GAP = 14;
    private static final int SCROLL_BAR_WIDTH = 8;

    private static final String FALLBACK_MARKDOWN = "*No Modern UI guide found.*";
    private static final Path MARKDOWN_PATH =
            PackCore.PACKCORE_DIR.resolve("markdown").resolve("modernui.md");

    public ModernUIPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() { return PAGE_TITLE; }
    @Override public boolean validate() { return true; }
    @Override public void onExit() {}

    @Override
    public void onEnter() {
        clearComponents();
        initDefaults();

        int availableWidth = getWidth() - PADDING * 2;
        int availableHeight = getHeight() - PADDING * 2;
        int columnWidth = (availableWidth - COLUMN_GAP) / 2;

        EmptyComponent leftColumn = new EmptyComponent(PADDING, PADDING, columnWidth, availableHeight);
        EmptyComponent rightColumn =
                new EmptyComponent(PADDING + columnWidth + COLUMN_GAP, PADDING, columnWidth, availableHeight);

        buildLeftColumn(leftColumn, columnWidth, availableHeight);
        buildRightColumn(rightColumn, columnWidth, availableHeight);

        addComponent(leftColumn);
        addComponent(rightColumn);
    }

    /**
     * Seeds selections from the live config the first time this page is entered per session.
     * Each feature reads its own current state so the wizard reflects what is actually set.
     */
    private void initDefaults() {
        if (state.getSelection(INIT_KEY) != null) return;
        for (Feature f : Feature.all()) {
            if (f.isCurrentlyEnabled()) {
                state.addMultiSelection(FEATURES_KEY, f.id());
            }
        }
        state.setSelection(INIT_KEY, "true");
    }

    private void buildLeftColumn(EmptyComponent column, int columnWidth, int columnHeight) {
        MarkdownComponent markdown =
                new MarkdownComponent(
                        0, 0,
                        columnWidth - SCROLL_BAR_WIDTH - PADDING / 2,
                        GuiHelper.loadMarkdown(MARKDOWN_PATH, FALLBACK_MARKDOWN, LOGGER));
        column.addComponent(
                GuiHelper.scrollWrapped(0, 0, columnWidth, columnHeight,
                        scroll -> scroll.addComponent(markdown)));
    }

    private void buildRightColumn(EmptyComponent column, int columnWidth, int columnHeight) {
        MultiSelectList<Feature> list =
                new MultiSelectList<>(
                        0, 0, columnWidth, columnHeight,
                        Feature.all(),
                        MultiSelectList.RowDescriptor.of(Feature::id, Feature::displayName, Feature::description),
                        state.getMultiSelection(FEATURES_KEY),
                        f -> state.addMultiSelection(FEATURES_KEY, f.id()),
                        f -> state.removeMultiSelection(FEATURES_KEY, f.id()));
        column.addComponent(list);
    }

    // ── Features ──────────────────────────────────────────────────────────────

    public enum Feature {
        CUSTOM_FONT("customFont") {
            @Override public boolean isCurrentlyEnabled() {
                return ModernUIConfigurator.isTextEngineEnabled();
            }
        },
        FANCY_TOOLTIP("fancyTooltip") {
            @Override public boolean isCurrentlyEnabled() {
                return ModernUIConfigurator.isTooltipEnabled();
            }
        },
        DING_SOUND("dingSound") {
            @Override public boolean isCurrentlyEnabled() {
                return ModernUIConfigurator.isDingEnabled();
            }
        };

        private final String id;

        Feature(String id) { this.id = id; }

        public String id() { return id; }

        public Component displayName() {
            return Component.translatable("gui.packcore.wizard.modern_ui." + id + ".name");
        }

        public Component description() {
            return Component.translatable("gui.packcore.wizard.modern_ui." + id + ".desc");
        }

        /** Reads current state from config files. Defaults to true on any error. */
        public boolean isCurrentlyEnabled() { return true; }

        public static List<Feature> all() { return List.of(values()); }
    }
}