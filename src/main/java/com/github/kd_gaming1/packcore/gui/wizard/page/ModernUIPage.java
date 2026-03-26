package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.component.MarkdownComponent;
import com.github.kd_gaming1.packcore.gui.component.ModernUISkipWarningOverlay;
import com.github.kd_gaming1.packcore.gui.component.MultiSelectList;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.integration.ModernUIConfigurator;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Wizard step -- lets the user toggle individual Modern UI features on or off. */
public class ModernUIPage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ModernUIPage");
    private static final Component PAGE_TITLE =
            Component.translatable("gui.packcore.wizard.page.modern_ui.title");

    public static final String FEATURES_KEY = "modernuiFeatures";
    /** Sentinel so we only seed defaults from config once per wizard session. */
    private static final String INIT_KEY = "modernuiFeatures_ready";

    /** Time in milliseconds below which the page is considered skipped if no changes were made. */
    private static final long SKIP_THRESHOLD_MS = 8_000;

    private static final int PADDING = 16;
    private static final int COLUMN_GAP = 14;
    private static final int SCROLL_BAR_WIDTH = 8;

    private static final String FALLBACK_MARKDOWN = "*No Modern UI guide found.*";
    private static final Path MARKDOWN_PATH =
            PackCore.PACKCORE_DIR.resolve("markdown").resolve("modernui.md");

    // ── Skip detection ────────────────────────────────────────────────────────

    private long enterTime;
    private Set<String> initialSelections;
    /** True once the user has acknowledged the skip warning. Survives back/forward navigation. */
    private boolean skipAcknowledged = false;

    // ── Scroll refs (needed to disable them while the overlay is open) ────────

    private ScrollContainerWidget leftScroll;
    private MultiSelectList<Feature> rightList;

    private ModernUISkipWarningOverlay skipOverlay;

    // ─────────────────────────────────────────────────────────────────────────

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

        enterTime = System.currentTimeMillis();
        initialSelections = new HashSet<>(state.getMultiSelection(FEATURES_KEY));

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

        // Overlay added last so it renders on top of the columns.
        skipOverlay = new ModernUISkipWarningOverlay(getWidth(), getHeight());
        skipOverlay.setOnClose(() -> setScrollsActive(true));
        addComponent(skipOverlay);
    }

    /**
     * Intercepts the Continue button. If the user appears to have skipped the page
     * (spent less than {@value #SKIP_THRESHOLD_MS}ms and made no toggle changes),
     * the skip-warning overlay is shown and navigation is blocked until acknowledged.
     */
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

    /** Returns true if the user spent very little time and changed nothing. */
    private boolean isLikelySkipped() {
        boolean tooQuick = System.currentTimeMillis() - enterTime < SKIP_THRESHOLD_MS;
        boolean noChanges = state.getMultiSelection(FEATURES_KEY).equals(initialSelections);
        return tooQuick && noChanges;
    }

    /** Enables or disables the scroll widgets behind the overlay to prevent click-through. */
    private void setScrollsActive(boolean active) {
        if (leftScroll != null) {
            leftScroll.active = active;
        }
        if (rightList != null) {
            rightList.setScrollActive(active);
        }
    }

    // ── Page building ─────────────────────────────────────────────────────────

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

        // Build scroll inline (not via GuiHelper.scrollWrapped) so we can hold the ref.
        leftScroll = new ScrollContainerWidget(columnWidth, columnHeight);
        leftScroll.addComponent(markdown);

        EmptyComponent scrollWrapper = new EmptyComponent(0, 0, columnWidth, columnHeight);
        scrollWrapper.addWidget(leftScroll);
        column.addComponent(scrollWrapper);
    }

    private void buildRightColumn(EmptyComponent column, int columnWidth, int columnHeight) {
        rightList = new MultiSelectList<>(
                0, 0, columnWidth, columnHeight,
                Feature.all(),
                MultiSelectList.RowDescriptor.of(Feature::id, Feature::displayName, Feature::description),
                state.getMultiSelection(FEATURES_KEY),
                f -> state.addMultiSelection(FEATURES_KEY, f.id()),
                f -> state.removeMultiSelection(FEATURES_KEY, f.id()));
        column.addComponent(rightList);
    }

    // ── Features ──────────────────────────────────────────────────────────────

    public enum Feature {
        TEXT_ENGINE("textEngine") {
            @Override public boolean isCurrentlyEnabled() {
                return ModernUIConfigurator.isTextEngineEnabled();
            }
        },
        CUSTOM_FONT("customFont") {
            @Override public boolean isCurrentlyEnabled() {
                return ModernUIConfigurator.isCustomFontEnabled();
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