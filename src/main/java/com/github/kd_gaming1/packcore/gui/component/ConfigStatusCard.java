package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.text.ScrollingTextComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ConfigStatusCard extends AbstractComponent {

    private static final int ACCENT_BAR_WIDTH = 3;
    private static final int PADDING_VERTICAL = 8;
    private static final int PADDING_HORIZONTAL = 10;
    private static final int ROW_GAP = 3;

    private static final int COLOR_SUCCESS_ACCENT = 0xFF4CAF50;
    private static final int COLOR_SUCCESS_BORDER = 0x554CAF50;
    private static final int COLOR_WARNING_ACCENT = 0xFFFFAA00;
    private static final int COLOR_WARNING_BORDER = 0x55FFAA00;

    private static final int COLOR_BACKGROUND = 0xCC0A1520;
    private static final int COLOR_TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SECONDARY = 0xFFAAAAAA;

    private final boolean isApplied;

    public ConfigStatusCard(int x, int y, int width, ConfigPackEntry appliedPack) {
        super(x, y, width, 0);
        this.isApplied = appliedPack != null;
        build(appliedPack);
    }

    private void build(ConfigPackEntry appliedPack) {
        int fontHeight = Minecraft.getInstance().font.lineHeight;
        int textStartX = ACCENT_BAR_WIDTH + PADDING_HORIZONTAL;
        int textWidth = getWidth() - textStartX - PADDING_HORIZONTAL;

        if (isApplied && appliedPack != null) {
            JsonObject config = appliedPack.config();
            String packName = config.has("name")
                    ? config.get("name").getAsString()
                    : appliedPack.zipPath().getFileName().toString();

            ScrollingTextComponent statusLine = new ScrollingTextComponent(
                    textStartX, PADDING_VERTICAL, textWidth,
                    Component.translatable("gui.packcore.wizard.card.config.applied", packName),
                    COLOR_TEXT_PRIMARY
            );
            statusLine.setDrawShadow(true);
            addComponent(statusLine);
            setHeight(PADDING_VERTICAL + fontHeight + PADDING_VERTICAL);
        } else {
            ScrollingTextComponent errorLine = new ScrollingTextComponent(
                    textStartX, PADDING_VERTICAL, textWidth,
                    Component.translatable("gui.packcore.wizard.card.config.error"),
                    COLOR_TEXT_PRIMARY
            );
            errorLine.setDrawShadow(true);
            addComponent(errorLine);

            addComponent(new TextComponent(
                    textStartX, PADDING_VERTICAL + fontHeight + ROW_GAP,
                    Component.translatable("gui.packcore.wizard.card.config.error.hint"),
                    COLOR_TEXT_SECONDARY
            ));
            setHeight(PADDING_VERTICAL + fontHeight + ROW_GAP + fontHeight + PADDING_VERTICAL);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        int cardX = getTotalX();
        int cardY = getTotalY();
        int cardWidth = getWidth();
        int cardHeight = getHeight();

        int accentColor = isApplied ? COLOR_SUCCESS_ACCENT : COLOR_WARNING_ACCENT;
        int borderColor = isApplied ? COLOR_SUCCESS_BORDER : COLOR_WARNING_BORDER;

        graphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, COLOR_BACKGROUND);
        graphics.fill(cardX, cardY, cardX + cardWidth, cardY + 1, borderColor);
        graphics.fill(cardX, cardY + cardHeight - 1, cardX + cardWidth, cardY + cardHeight, borderColor);
        graphics.fill(cardX, cardY, cardX + 1, cardY + cardHeight, borderColor);
        graphics.fill(cardX + cardWidth - 1, cardY, cardX + cardWidth, cardY + cardHeight, borderColor);
        graphics.fill(cardX, cardY, cardX + ACCENT_BAR_WIDTH, cardY + cardHeight, accentColor);
    }
}