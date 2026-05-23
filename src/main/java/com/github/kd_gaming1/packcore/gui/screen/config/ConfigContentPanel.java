package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ConfigContentPanel extends AbstractComponent {

    public ConfigContentPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public void setPage(BaseConfigPage page) {
        clearComponents();
        addComponent(page);
        updateParentPosition(getParentX(), getParentY(), getWidth(), getHeight());
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        //?} else {
    /*@Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
    *///?}
        int x = getTotalX();
        int y = getTotalY();
        int w = getWidth();
        int h = getHeight();

        graphics.fill(x, y, x + w, y + h, GuiColors.PANEL_BACKGROUND);
        GuiHelper.drawBorder(graphics, x, y, w, h, GuiColors.PANEL_BORDER);
    }
}