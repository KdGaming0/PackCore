package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.text.ScrollingTextComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class ConfigStatusCard extends AbstractComponent {

    private static final int ACCENT_BAR_WIDTH = 3;
    private static final int PADDING_V = 8;
    private static final int PADDING_H = 10;
    private static final int ROW_GAP = 3;

    private final boolean isApplied;

    public ConfigStatusCard(int x, int y, int width, ConfigPackEntry appliedPack, boolean showMigrationHint) {
        super(x, y, width, 0);
        isApplied = appliedPack != null;
        build(appliedPack, showMigrationHint);
    }

    private void build(ConfigPackEntry appliedPack, boolean showMigrationHint) {
        int fontHeight = Minecraft.getInstance().font.lineHeight;
        int textX = ACCENT_BAR_WIDTH + PADDING_H;
        int textWidth = getWidth() - textX - PADDING_H;

        if (isApplied) {
            JsonObject config = appliedPack.config();
            String packName = config.has("name")
                    ? config.get("name").getAsString()
                    : appliedPack.zipPath().getFileName().toString();

            ScrollingTextComponent statusLine = new ScrollingTextComponent(
                    textX, PADDING_V, textWidth,
                    Component.translatable("gui.packcore.wizard.card.config.applied", packName),
                    GuiColors.TEXT_PRIMARY
            );
            statusLine.setDrawShadow(true);
            addComponent(statusLine);
            if (showMigrationHint) {
                addComponent(new TextComponent(
                        textX, PADDING_V + fontHeight + ROW_GAP,
                        Component.translatable("gui.packcore.wizard.card.config.applied.hint"),
                        GuiColors.TEXT_SECONDARY
                ));
                setHeight(PADDING_V + fontHeight + ROW_GAP + fontHeight + PADDING_V);
            } else {
                setHeight(PADDING_V + fontHeight + PADDING_V);
            }
        } else {
            ScrollingTextComponent errorLine = new ScrollingTextComponent(
                    textX, PADDING_V, textWidth,
                    Component.translatable("gui.packcore.wizard.card.config.error"),
                    GuiColors.TEXT_PRIMARY
            );
            errorLine.setDrawShadow(true);
            addComponent(errorLine);
            addComponent(new TextComponent(
                    textX, PADDING_V + fontHeight + ROW_GAP,
                    Component.translatable("gui.packcore.wizard.card.config.error.hint"),
                    GuiColors.TEXT_SECONDARY
            ));
            setHeight(PADDING_V + fontHeight + ROW_GAP + fontHeight + PADDING_V);
        }
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

        int accentColor = isApplied ? GuiColors.SUCCESS : GuiColors.WARNING;
        int borderColor = isApplied ? GuiColors.SUCCESS_BORDER : 0x55FFAA00;

        graphics.fill(x, y, x + w, y + h, GuiColors.PANEL_BACKGROUND);
        GuiHelper.drawBorder(graphics, x, y, w, h, borderColor);
        graphics.fill(x, y, x + ACCENT_BAR_WIDTH, y + h, accentColor);
    }
}