package com.github.kd_gaming1.packcore.gui.wizard;

import com.daqem.uilib.gui.component.AbstractComponent;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The main content area of the Welcome Wizard.
 */
public class WizardContentPanel extends AbstractComponent {

    private static final int COLOR_BACKGROUND = 0xCC0A1520;
    private static final int COLOR_BORDER = 0xFFD4A017;

    private final WizardNavigator navigator;

    /** Tracks the last rendered page index to detect changes without polling. */
    private int lastRenderedPageIndex = -1;

    public WizardContentPanel(int x, int y, int width, int height, WizardNavigator navigator) {
        super(x, y, width, height);
        this.navigator = navigator;
        swapToCurrentPage();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        int currentIndex = navigator.getCurrentIndex();
        if (currentIndex != lastRenderedPageIndex) {
            lastRenderedPageIndex = currentIndex;
            swapToCurrentPage();
            this.updateParentPosition(getParentX(), getParentY(), parentWidth, parentHeight);
        }

        int x = getTotalX();
        int y = getTotalY();
        int w = getWidth();
        int h = getHeight();

        graphics.fill(x, y, x + w, y + h, COLOR_BACKGROUND);
        graphics.fill(x, y, x + w, y + 1, COLOR_BORDER);
        graphics.fill(x, y + h - 1, x + w, y + h, COLOR_BORDER);
        graphics.fill(x, y, x + 1, y + h, COLOR_BORDER);
        graphics.fill(x + w - 1, y, x + w, y + h, COLOR_BORDER);
    }

    /** Removes the previous page and installs the current one from the navigator. */
    private void swapToCurrentPage() {
        this.clearComponents();
        this.addComponent(navigator.getCurrentPage());
    }
}