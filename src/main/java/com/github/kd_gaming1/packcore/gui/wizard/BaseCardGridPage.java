package com.github.kd_gaming1.packcore.gui.wizard;

import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.github.kd_gaming1.packcore.gui.component.OptionCardGrid;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Base class for wizard pages that show an explanation and an OptionCardGrid below it.
 */
public abstract class BaseCardGridPage<T> extends BaseWizardPage {

    private static final int OUTER_PADDING = 16;
    private static final int CARD_GAP = 10;
    private static final int EXPLANATION_GAP = 8;
    private static final int COLOR_EXPLANATION = 0xFFAAAAAA;

    protected BaseCardGridPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    protected abstract String stateKey();
    protected abstract int columns();
    protected abstract Component explanation();
    protected abstract List<T> options();
    protected abstract OptionCardGrid.CardDescriptor<T> descriptor();

    @Override public boolean validate() { return true; }
    @Override public void onExit() {}

    @Override
    public void onEnter() {
        this.clearComponents();

        int availableWidth = getWidth() - OUTER_PADDING * 2;

        MultiLineTextComponent explanationText = new MultiLineTextComponent(
                OUTER_PADDING, OUTER_PADDING, availableWidth, explanation(), COLOR_EXPLANATION
        );
        this.addComponent(explanationText);

        int gridTop    = OUTER_PADDING + explanationText.getHeight() + EXPLANATION_GAP;
        int gridHeight = getHeight() - gridTop - OUTER_PADDING;

        OptionCardGrid.CardDescriptor<T> desc = descriptor();

        OptionCardGrid<T> grid = new OptionCardGrid<>(
                OUTER_PADDING, gridTop,
                availableWidth, gridHeight,
                columns(), CARD_GAP,
                options(), desc,
                state.getSelection(stateKey()),
                selected -> state.setSelection(stateKey(), selected != null ? desc.id(selected) : null)
        );
        this.addComponent(grid);
    }
}
