package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.component.AbstractComponent;
import net.minecraft.client.gui.GuiGraphics;

public class ConfigContentPanel extends AbstractComponent {

    private static final int COLOR_BACKGROUND = 0xCC0A1520;
    private static final int COLOR_BORDER = 0xFFD4A017;
    public ConfigContentPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public void setPage(BaseConfigPage page) {
        this.clearComponents();
        this.addComponent(page);
        this.updateParentPosition(getParentX(), getParentY(), getWidth(), getHeight());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
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
}