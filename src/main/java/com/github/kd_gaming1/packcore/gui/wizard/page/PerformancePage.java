package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.component.MarkdownComponent;
import com.github.kd_gaming1.packcore.gui.component.OptionSelectList;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Step 2 — Performance profile selection.
 * Left column: scrollable Markdown guide. Right column: selectable profile list.
 */
public class PerformancePage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/PerformancePage");

    private static final Component PAGE_TITLE = Component.translatable("gui.packcore.wizard.page.performance.title");

    public static final String STATE_KEY = "performanceProfile";

    private static final int PADDING = 16;
    private static final int COLUMN_GAP = 14;
    private static final int SCROLL_BAR_WIDTH = 8;

    private static final String FALLBACK_MARKDOWN = "*No performance guide found.*";
    private static final Path MARKDOWN_PATH = PackCore.PACKCORE_DIR.resolve("markdown").resolve("performance.md");

    public PerformancePage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() { return PAGE_TITLE; }
    @Override public boolean validate() { return true; }
    @Override public void onExit() { }

    @Override
    public void onEnter() {
        this.clearComponents();

        int availableWidth = getWidth() - PADDING * 2;
        int availableHeight = getHeight() - PADDING * 2;
        int columnWidth = (availableWidth - COLUMN_GAP) / 2;

        EmptyComponent leftColumn = new EmptyComponent(PADDING, PADDING, columnWidth, availableHeight);
        EmptyComponent rightColumn = new EmptyComponent(PADDING + columnWidth + COLUMN_GAP, PADDING, columnWidth, availableHeight);

        buildLeftColumn(leftColumn, columnWidth, availableHeight);
        buildRightColumn(rightColumn, columnWidth, availableHeight);

        this.addComponent(leftColumn);
        this.addComponent(rightColumn);
    }

    private void buildLeftColumn(EmptyComponent column, int columnWidth, int columnHeight) {
        MarkdownComponent markdownComp = new MarkdownComponent(
                0, 0, columnWidth - SCROLL_BAR_WIDTH - (PADDING / 2), GuiHelper.loadMarkdown(MARKDOWN_PATH, FALLBACK_MARKDOWN, LOGGER)
        );
        column.addComponent(GuiHelper.scrollWrapped(0, 0, columnWidth, columnHeight,
                scroll -> scroll.addComponent(markdownComp)));
    }

    private void buildRightColumn(EmptyComponent column, int columnWidth, int columnHeight) {
        OptionSelectList<PerformanceProfile> list = new OptionSelectList<>(
                0, 0, columnWidth, columnHeight,
                PerformanceProfile.all(),
                OptionSelectList.RowDescriptor.of(
                        PerformanceProfile::id,
                        PerformanceProfile::name,
                        PerformanceProfile::description
                ),
                state.getSelection(STATE_KEY),
                selected -> state.setSelection(STATE_KEY, selected.id())
        );
        column.addComponent(list);
    }

    public record PerformanceProfile(String id, Component name, Component description) {

        public static List<PerformanceProfile> all() {
            return List.of(
                    fromId("max_fps"),
                    fromId("balanced"),
                    fromId("quality"),
                    fromId("quality_performance_shaders"),
                    fromId("quality_quality_shaders")
            );
        }

        private static PerformanceProfile fromId(String id) {
            return new PerformanceProfile(
                    id,
                    Component.translatable("gui.packcore.wizard.performance." + id + ".name"),
                    Component.translatable("gui.packcore.wizard.performance." + id + ".desc")
            );
        }
    }
}