package com.github.kd_gaming1.packcore.gui.wizard;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import net.minecraft.client.gui.GuiGraphics;

/** Main content area of the Welcome Wizard. Swaps pages on navigation. */
public class WizardContentPanel extends AbstractComponent {

    private final WizardNavigator navigator;
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
            updateParentPosition(getParentX(), getParentY(), parentWidth, parentHeight);
        }

        int x = getTotalX();
        int y = getTotalY();
        int w = getWidth();
        int h = getHeight();

        graphics.fill(x, y, x + w, y + h, GuiColors.PANEL_BACKGROUND);
        GuiHelper.drawBorder(graphics, x, y, w, h, GuiColors.PANEL_BORDER);
    }

    private void swapToCurrentPage() {
        clearComponents();
        addComponent(navigator.getCurrentPage());
    }
}