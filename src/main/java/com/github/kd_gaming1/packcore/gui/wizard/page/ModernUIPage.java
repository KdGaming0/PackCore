package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.component.MarkdownComponent;
import com.github.kd_gaming1.packcore.gui.component.ModernUISkipWarningOverlay;
import com.github.kd_gaming1.packcore.gui.component.MultiSelectList;
import com.github.kd_gaming1.packcore.gui.component.OptionSelectList;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.integration.ModernUIConfigurator;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Wizard step -- lets the user configure Modern UI features. */
public class ModernUIPage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ModernUIPage");
    private static final Component PAGE_TITLE =
            Component.translatable("gui.packcore.wizard.page.modern_ui.title");

    public static final String FEATURES_KEY = "modernuiFeatures";
    public static final String FONT_MODE_KEY = "modernuiFontMode";

    private static final String INIT_KEY = "modernuiFeatures_ready";
    private static final long SKIP_THRESHOLD_MS = 8_000;

    private static final int PADDING = 16;
    private static final int COLUMN_GAP = 14;
    private static final int SECTION_GAP = 10;
    private static final int SCROLL_BAR_WIDTH = 8;
    private static final int COLOR_LABEL = 0xFFCCCCCC;

    private static final String FALLBACK_MARKDOWN = "*No Modern UI guide found.*";
    private static final Path MARKDOWN_PATH =
            PackCore.PACKCORE_DIR.resolve("markdown").resolve("modernui.md");

    // ── Skip detection ────────────────────────────────────────────────────────

    private long enterTime;
    private Set<String> initialSelections;
    private String initialFontMode;
    private boolean skipAcknowledged = false;

    // ── Scroll refs ───────────────────────────────────────────────────────────

    private ScrollContainerWidget leftScroll;
    private MultiSelectList<Feature> featureList;

    private ModernUISkipWarningOverlay skipOverlay;

    // ─────────────────────────────────────────────────────────────────────────

    public ModernUIPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override
    public Component getTitle() {
        return PAGE_TITLE;
    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public void onExit() {}

    @Override
    public void onEnter() {
        clearComponents();
        initDefaults();

        enterTime = System.currentTimeMillis();
        initialSelections = new HashSet<>(state.getMultiSelection(FEATURES_KEY));
        initialFontMode = state.getSelection(FONT_MODE_KEY);

        int availableWidth = getWidth() - PADDING * 2;
        int availableHeight = getHeight() - PADDING * 2;
        int columnWidth = (availableWidth - COLUMN_GAP) / 2;

        EmptyComponent leftColumn = new EmptyComponent(PADDING, PADDING, columnWidth, availableHeight);
        EmptyComponent rightColumn =
                new EmptyComponent(
                        PADDING + columnWidth + COLUMN_GAP, PADDING, columnWidth, availableHeight);

        buildLeftColumn(leftColumn, columnWidth, availableHeight);
        buildRightColumn(rightColumn, columnWidth, availableHeight);

        addComponent(leftColumn);
        addComponent(rightColumn);

        skipOverlay = new ModernUISkipWarningOverlay(getWidth(), getHeight());
        skipOverlay.setOnClose(() -> setScrollsActive(true));
        addComponent(skipOverlay);
    }

    @Override
    public boolean onContinueAttempted() {
        if (skipAcknowledged || !isLikelySkipped()) return true;
        setScrollsActive(false);
        skipOverlay.show(() -> {
            skipAcknowledged = true;
            navigator.nextPage();
        });
        return false;
    }

    private boolean isLikelySkipped() {
        boolean tooQuick = System.currentTimeMillis() - enterTime < SKIP_THRESHOLD_MS;
        boolean noChanges =
                state.getMultiSelection(FEATURES_KEY).equals(initialSelections)
                        && Objects.equals(state.getSelection(FONT_MODE_KEY), initialFontMode);
        return tooQuick && noChanges;
    }

    private void setScrollsActive(boolean active) {
        if (leftScroll != null) leftScroll.active = active;
        if (featureList != null) featureList.setScrollActive(active);
    }

    // ── Page building ─────────────────────────────────────────────────────────

    private void initDefaults() {
        if (state.getSelection(FONT_MODE_KEY) == null) {
            String current = ModernUIConfigurator.currentFontMode().id();
            state.setSelection(FONT_MODE_KEY, current);
            state.setSelection(FONT_MODE_KEY + "_original", current);
        }

        if (state.getSelection(INIT_KEY) != null) return;
        for (Feature f : Feature.all()) {
            if (f.isCurrentlyEnabled()) state.addMultiSelection(FEATURES_KEY, f.id());
        }
        state.setSelection(INIT_KEY, "true");
    }

    private void buildLeftColumn(EmptyComponent column, int columnWidth, int columnHeight) {
        MarkdownComponent markdown =
                new MarkdownComponent(
                        0,
                        0,
                        columnWidth - SCROLL_BAR_WIDTH - PADDING / 2,
                        GuiHelper.loadMarkdown(MARKDOWN_PATH, FALLBACK_MARKDOWN, LOGGER));

        leftScroll = new ScrollContainerWidget(columnWidth, columnHeight);
        leftScroll.addComponent(markdown);

        EmptyComponent wrapper = new EmptyComponent(0, 0, columnWidth, columnHeight);
        wrapper.addWidget(leftScroll);
        column.addComponent(wrapper);
    }

    private void buildRightColumn(EmptyComponent column, int columnWidth, int columnHeight) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int labelH = font.lineHeight + 4;

        // Split column: font mode (top, 55%) and feature toggles (bottom, 45%)
        int fontSectionH = (int) ((columnHeight - SECTION_GAP - labelH * 2) * 0.55);
        int featureSectionH = columnHeight - SECTION_GAP - labelH * 2 - fontSectionH;
        int featureSectionY = labelH + fontSectionH + SECTION_GAP + labelH;

        // ── Font mode label + single-select list ──
        column.addComponent(
                new TextComponent(
                        0,
                        0,
                        Component.translatable("gui.packcore.wizard.modern_ui.font_mode.label"),
                        COLOR_LABEL));

        OptionSelectList<FontOption> fontList =
                new OptionSelectList<>(
                        0,
                        labelH,
                        columnWidth,
                        fontSectionH,
                        FontOption.all(),
                        OptionSelectList.RowDescriptor.of(FontOption::id, FontOption::name, FontOption::description),
                        state.getSelection(FONT_MODE_KEY),
                        selected -> state.setSelection(FONT_MODE_KEY, selected.id()));
        column.addComponent(fontList);

        // ── Feature toggles label + multi-select list ──
        column.addComponent(
                new TextComponent(
                        0,
                        featureSectionY - labelH,
                        Component.translatable("gui.packcore.wizard.modern_ui.features.label"),
                        COLOR_LABEL));

        featureList =
                new MultiSelectList<>(
                        0,
                        featureSectionY,
                        columnWidth,
                        featureSectionH,
                        Feature.all(),
                        MultiSelectList.RowDescriptor.of(
                                Feature::id, Feature::displayName, Feature::description),
                        state.getMultiSelection(FEATURES_KEY),
                        f -> state.addMultiSelection(FEATURES_KEY, f.id()),
                        f -> state.removeMultiSelection(FEATURES_KEY, f.id()));
        column.addComponent(featureList);
    }

    // ── Font options (single-select) ──────────────────────────────────────────

    public record FontOption(String id, Component name, Component description) {
        public static List<FontOption> all() {
            return List.of(fromId("inter"), fromId("vanilla"));
        }

        private static FontOption fromId(String id) {
            return new FontOption(
                    id,
                    Component.translatable("gui.packcore.wizard.modern_ui.font." + id + ".name"),
                    Component.translatable("gui.packcore.wizard.modern_ui.font." + id + ".desc"));
        }
    }

    // ── Feature toggles (multi-select) ───────────────────────────────────────

    public enum Feature {
        FANCY_TOOLTIP("fancyTooltip") {
            @Override
            public boolean isCurrentlyEnabled() {
                return ModernUIConfigurator.isTooltipEnabled();
            }
        },
        DING_SOUND("dingSound") {
            @Override
            public boolean isCurrentlyEnabled() {
                return ModernUIConfigurator.isDingEnabled();
            }
        };

        private final String id;

        Feature(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public Component displayName() {
            return Component.translatable("gui.packcore.wizard.modern_ui." + id + ".name");
        }

        public Component description() {
            return Component.translatable("gui.packcore.wizard.modern_ui." + id + ".desc");
        }

        public boolean isCurrentlyEnabled() {
            return true;
        }

        public static List<Feature> all() {
            return List.of(values());
        }
    }
}