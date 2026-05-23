package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public class TabNavBar extends AbstractComponent {

    private static final int COLOR_ACTIVE_INDICATOR = 0xFF2196F3;
    private static final int COLOR_ACTIVE_BG = 0x551A3A5C;
    private static final int COLOR_HOVER_BG = 0x441A3A5C;
    private static final int COLOR_TEXT_ACTIVE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_INACTIVE = 0xFFCCCCCC;
    private static final int COLOR_SEPARATOR = 0x44FFFFFF;
    private static final int INDICATOR_HEIGHT = 2;

    private ConfigTab activeTab;

    public TabNavBar(int x, int y, int width, int height, ConfigTab activeTab, Consumer<ConfigTab> onTabClick) {
        super(x, y, width, height);
        this.activeTab = activeTab;
        buildTabWidgets(width, height, onTabClick);
    }

    private void buildTabWidgets(int width, int height, Consumer<ConfigTab> onTabClick) {
        ConfigTab[] tabs = ConfigTab.ordered();
        int tabWidth = width / tabs.length;

        for (ConfigTab tab : tabs) {
            ButtonWidget btn = new ButtonWidget(tab.index() * tabWidth, 0, tabWidth, height, tab.label(), b -> onTabClick.accept(tab)) {
                //? if >=26.1 {
                @Override
                public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}
                //?} else {
                /*@Override
                protected void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}
                *///?}
            };
            this.addWidget(btn);
        }
    }

    public void setActiveTab(ConfigTab tab) {
        this.activeTab = tab;
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

        var font = Minecraft.getInstance().font;
        ConfigTab[] tabs = ConfigTab.ordered();
        int tabWidth = w / tabs.length;

        graphics.fill(x, y, x + w, y + 1, COLOR_SEPARATOR);

        for (ConfigTab tab : tabs) {
            int tabX = x + tab.index() * tabWidth;
            boolean active = tab == activeTab;
            boolean hovered = mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= y && mouseY < y + h;

            if (active) {
                graphics.fill(tabX, y + 1, tabX + tabWidth, y + h, COLOR_ACTIVE_BG);
                graphics.fill(tabX, y + h - INDICATOR_HEIGHT, tabX + tabWidth, y + h, COLOR_ACTIVE_INDICATOR);
            } else if (hovered) {
                graphics.fill(tabX, y + 1, tabX + tabWidth, y + h, COLOR_HOVER_BG);
            }

            String label = tab.label().getString();
            int labelX = tabX + (tabWidth - font.width(label)) / 2;
            int labelY = y + (h - font.lineHeight) / 2;
            //? if >=26.1 {
            graphics.text(font, label, labelX, labelY, active ? COLOR_TEXT_ACTIVE : COLOR_TEXT_INACTIVE, false);
            //?} else {
            /*graphics.drawString(font, label, labelX, labelY, active ? COLOR_TEXT_ACTIVE : COLOR_TEXT_INACTIVE, false);
             *///?}
        }
    }
}