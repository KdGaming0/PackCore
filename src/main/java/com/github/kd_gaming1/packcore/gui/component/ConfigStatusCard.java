package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.daqem.uilib.gui.component.text.ScrollingTextComponent;
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

    public ConfigStatusCard(int x, int y, int width, ConfigPackEntry appliedPack, boolean preserved) {
        super(x, y, width, 0);
        isApplied = appliedPack != null;
        build(appliedPack, preserved);
    }

    private void build(ConfigPackEntry appliedPack, boolean preserved) {
        int fontHeight = Minecraft.getInstance().font.lineHeight;
        int textX = ACCENT_BAR_WIDTH + PADDING_H;
        int textWidth = getWidth() - textX - PADDING_H;

        // Track our Y coordinate dynamically
        int currentY = PADDING_V;

        if (isApplied) {
            JsonObject config = appliedPack.config();
            String packName = config.has("name")
                    ? config.get("name").getAsString()
                    : appliedPack.zipPath().getFileName().toString();

            // ScrollingTextComponent enforces maxWidth natively, so it won't overflow
            ScrollingTextComponent statusLine = new ScrollingTextComponent(
                    textX, currentY, textWidth,
                    Component.translatable("gui.packcore.wizard.card.config.applied", packName),
                    GuiColors.TEXT_PRIMARY
            );
            statusLine.setDrawShadow(true);
            addComponent(statusLine);

            // Move Y down by the height of a single line plus our gap
            currentY += fontHeight + ROW_GAP;

            String hintKey = preserved
                    ? "gui.packcore.wizard.card.config.applied.preserved.hint"
                    : "gui.packcore.wizard.card.config.applied.full.hint";

            // Replaced TextComponent with MultiLineTextComponent
            MultiLineTextComponent hintLine = new MultiLineTextComponent(
                    textX, currentY, textWidth,
                    Component.translatable(hintKey),
                    GuiColors.TEXT_SECONDARY
            );
            addComponent(hintLine);

            // Add the dynamic height of the wrapped text to our Y tracker
            currentY += hintLine.getHeight();

        } else {
            ScrollingTextComponent errorLine = new ScrollingTextComponent(
                    textX, currentY, textWidth,
                    Component.translatable("gui.packcore.wizard.card.config.error"),
                    GuiColors.TEXT_PRIMARY
            );
            errorLine.setDrawShadow(true);
            addComponent(errorLine);

            currentY += fontHeight + ROW_GAP;

            MultiLineTextComponent hintLine = new MultiLineTextComponent(
                    textX, currentY, textWidth,
                    Component.translatable("gui.packcore.wizard.card.config.error.hint"),
                    GuiColors.TEXT_SECONDARY
            );
            addComponent(hintLine);

            currentY += hintLine.getHeight();
        }

        // Set the final height of the card using our dynamically tracked Y coordinate
        setHeight(currentY + PADDING_V);
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