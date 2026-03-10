package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.github.kd_gaming1.packcore.gui.component.OptionCardGrid;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Step 3 — Tab Design chooser.
 */
public class TabDesignPage extends BaseWizardPage {

    private static final Component PAGE_TITLE = Component.translatable("gui.packcore.wizard.page.tab_design.title");
    private static final Component EXPLANATION = Component.translatable("gui.packcore.wizard.page.tab_design.explanation");

    public static final String STATE_KEY = "tabDesign";

    private static final int OUTER_PADDING = 16;
    private static final int GRID_COLUMNS = 2;
    private static final int CARD_GAP = 10;
    private static final int EXPLANATION_GAP = 8;
    private static final int COLOR_EXPLANATION = 0xFFAAAAAA;

    public TabDesignPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() { return PAGE_TITLE; }
    @Override public boolean validate() { return true; }
    @Override public void onExit() { }

    @Override
    public void onEnter() {
        this.clearComponents();

        int availableWidth = getWidth() - OUTER_PADDING * 2;

        MultiLineTextComponent explanation = new MultiLineTextComponent(
                OUTER_PADDING, OUTER_PADDING, availableWidth, EXPLANATION, COLOR_EXPLANATION
        );
        this.addComponent(explanation);

        int gridTop = OUTER_PADDING + explanation.getHeight() + EXPLANATION_GAP;
        int gridHeight = getHeight() - gridTop - OUTER_PADDING;

        OptionCardGrid<TabDesignOption> grid = new OptionCardGrid<>(
                OUTER_PADDING, gridTop,
                availableWidth, gridHeight,
                GRID_COLUMNS, CARD_GAP,
                TabDesignOption.all(),
                OptionCardGrid.CardDescriptor.of(
                        TabDesignOption::id,
                        TabDesignOption::name,
                        TabDesignOption::description,
                        TabDesignOption::previewTexture,
                        TabDesignOption::previewTextureWidth,
                        TabDesignOption::previewTextureHeight
                ),
                state.getSelection(STATE_KEY),
                selected -> state.setSelection(STATE_KEY, selected != null ? selected.id() : null)
        );

        this.addComponent(grid);
    }

    public record TabDesignOption(
            String id,
            Component name,
            Component description,
            Identifier previewTexture,
            int previewTextureWidth,
            int previewTextureHeight
    ) {
        public static List<TabDesignOption> all() {
            return List.of(fromId("compact"), fromId("fancy"));
        }

        private static TabDesignOption fromId(String id) {
            return new TabDesignOption(
                    id,
                    Component.translatable("gui.packcore.wizard.tab_design." + id + ".name"),
                    Component.translatable("gui.packcore.wizard.tab_design." + id + ".desc"),
                    Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/wizard/tab_preview/" + id + ".png"),
                    320, 180
            );
        }
    }
}