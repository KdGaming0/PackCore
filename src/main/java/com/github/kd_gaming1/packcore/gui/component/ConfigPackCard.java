package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.text.TruncatedTextComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public class ConfigPackCard extends AbstractComponent {

    private static final int ACCENT_BAR_WIDTH = 3;
    private static final int PADDING_VERTICAL = 8;
    private static final int PADDING_HORIZONTAL = 10;
    private static final int ROW_GAP = 3;
    private static final int BADGE_PADDING = 4;

    private static final int COLOR_ACCENT = 0xFF2196F3;
    private static final int COLOR_BORDER = 0x552196F3;
    private static final int COLOR_BACKGROUND = 0xCC0A1520;
    private static final int COLOR_HOVER = 0xCC0D2035;

    private static final int COLOR_ACTIVE_ACCENT = 0xFF4CAF50;
    private static final int COLOR_ACTIVE_BORDER = 0x554CAF50;
    private static final int COLOR_ACTIVE_BACKGROUND = 0xCC081408;

    private static final int COLOR_BADGE_BACKGROUND = 0x554CAF50;
    private static final int COLOR_BADGE_TEXT = 0xFF4CAF50;

    private static final int COLOR_TEXT_MAIN = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SUB = 0xFFAAAAAA;

    private final ConfigPackEntry entry;
    private final boolean isActive;
    private final String badgeText;
    private final Consumer<ConfigPackEntry> onSelect;

    public ConfigPackCard(int x, int y, int width, ConfigPackEntry entry, boolean isActive, String badgeText, Consumer<ConfigPackEntry> onSelect) {
        super(x, y, width, 0);
        this.entry = entry;
        this.isActive = isActive;
        this.badgeText = badgeText;
        this.onSelect = onSelect;
        this.build();
    }

    private void build() {
        JsonObject config = entry.config();
        String name = config.has("name") ? config.get("name").getAsString() : entry.zipPath().getFileName().toString();
        String meta = (config.has("author") ? config.get("author").getAsString() : "Unknown")
                + "  ·  v" + (config.has("version") ? config.get("version").getAsString() : "?.?.?");

        int fontHeight = Minecraft.getInstance().font.lineHeight;
        int textX = ACCENT_BAR_WIDTH + PADDING_HORIZONTAL;
        int textWidth = getWidth() - textX - PADDING_HORIZONTAL;

        addComponent(new TruncatedTextComponent(textX, PADDING_VERTICAL, textWidth, Component.literal(name), COLOR_TEXT_MAIN));

        int metaRowY = PADDING_VERTICAL + fontHeight + ROW_GAP;
        addComponent(new TextComponent(textX, metaRowY, Component.literal(meta), COLOR_TEXT_SUB));

        int resolutionRowY = metaRowY + fontHeight + ROW_GAP;
        addComponent(new TextComponent(textX, resolutionRowY, Component.literal(formatResolution(config)), COLOR_TEXT_SUB));

        setHeight(resolutionRowY + fontHeight + PADDING_VERTICAL);

        if (!isActive) {
            addWidget(new ButtonWidget(0, 0, getWidth(), getHeight(), Component.empty(), button -> onSelect.accept(entry)) {
                @Override
                protected void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}
            });
        }
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
    //?} else {
     /*@Override
     public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
    *///?}
        boolean isHovered = !isActive
                && mouseX >= getTotalX() && mouseX <= getTotalX() + getWidth()
                && mouseY >= getTotalY() && mouseY <= getTotalY() + getHeight();

        int cardX = getTotalX();
        int cardY = getTotalY();
        int cardWidth = getWidth();
        int cardHeight = getHeight();

        int accentColor = isActive ? COLOR_ACTIVE_ACCENT : COLOR_ACCENT;
        int borderColor = isActive ? COLOR_ACTIVE_BORDER : COLOR_BORDER;
        int backgroundColor = isActive ? COLOR_ACTIVE_BACKGROUND : (isHovered ? COLOR_HOVER : COLOR_BACKGROUND);

        graphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, backgroundColor);
        drawOutline(graphics, cardX, cardY, cardWidth, cardHeight, borderColor);
        graphics.fill(cardX, cardY, cardX + ACCENT_BAR_WIDTH, cardY + cardHeight, accentColor);

        if (isActive && badgeText != null && !badgeText.isBlank()) {
            var font = Minecraft.getInstance().font;

            int badgeWidth = font.width(badgeText) + (BADGE_PADDING * 2);
            int badgeHeight = font.lineHeight + BADGE_PADDING;
            int badgeX = cardX + cardWidth - badgeWidth - PADDING_HORIZONTAL;
            int badgeY = cardY + (cardHeight - badgeHeight) / 2;

            graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, COLOR_BADGE_BACKGROUND);
            //? if >=26.1 {
            graphics.text(font, badgeText, badgeX + BADGE_PADDING, badgeY + (BADGE_PADDING / 2), COLOR_BADGE_TEXT, false);
            //?} else {
            /*graphics.drawString(font, badgeText, badgeX + BADGE_PADDING, badgeY + (BADGE_PADDING / 2), COLOR_BADGE_TEXT, false);
            *///?}
        }
    }

    private void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        GuiHelper.drawBorder(graphics, x, y, width, height, color);
    }

    private String formatResolution(JsonObject config) {
        if (!config.has("targetWidth") || !config.has("targetHeight")) return "Unknown resolution";
        String resolution = config.get("targetWidth").getAsInt() + "×" + config.get("targetHeight").getAsInt();
        return config.has("guiScale") ? resolution + "  ·  GUI Scale: " + config.get("guiScale").getAsString() : resolution;
    }
}