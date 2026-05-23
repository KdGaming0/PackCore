package com.github.kd_gaming1.packcore.gui.screen;

import com.github.kd_gaming1.packcore.update.UpdateStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ChangelogScreen extends Screen {

    private static final int PADDING = 16;
    private static final int BOX_PADDING = 12;
    private static final int CLOSE_BUTTON_HEIGHT = 20;
    private static final int CLOSE_BUTTON_WIDTH = 120;
    private static final int LINE_SPACING = 2;

    private static final int COLOR_BACKGROUND = 0xDD101010;
    private static final int COLOR_BORDER = 0x88FFD700;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_HEADING = 0xFFFFDD44;
    private static final int COLOR_CONTENT = 0xFFCCCCCC;

    private final Screen parent;
    private final String rawChangelog;

    private final List<RenderedLine> lines = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int totalContentHeight = 0;

    public ChangelogScreen(Screen parent, UpdateStatus status) {
        super(Component.translatable(
                "gui.packcore.overlay.changelog.title",
                status.latestVersion() != null ? status.latestVersion() : ""
        ));
        this.parent = parent;
        this.rawChangelog = status.changelog() != null ? status.changelog() : "";
    }

    @Override
    protected void init() {
        lines.clear();
        scrollOffset = 0;
        totalContentHeight = 0;

        int contentWidth = this.width - PADDING * 2 - BOX_PADDING * 2;

        for (String raw : rawChangelog.split("\n")) {
            parseAndAddLine(raw.stripTrailing(), contentWidth);
        }

        totalContentHeight = lines.stream().mapToInt(RenderedLine::height).sum();
        maxScroll = Math.max(0, totalContentHeight - getViewHeight());

        int closeX = this.width / 2 - CLOSE_BUTTON_WIDTH / 2;
        int closeY = this.height - PADDING * 2 - CLOSE_BUTTON_HEIGHT;
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> onClose()
        ).bounds(closeX, closeY, CLOSE_BUTTON_WIDTH, CLOSE_BUTTON_HEIGHT).build());
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        //?} else {
     /*@Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    *///?}
        int boxX = PADDING;
        int boxY = PADDING;
        int boxWidth = this.width - PADDING * 2;
        int boxHeight = this.height - PADDING * 2;

        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, COLOR_BACKGROUND);
        graphics.outline(boxX, boxY, boxWidth, boxHeight, COLOR_BORDER);

        int titleY = boxY + BOX_PADDING;
        graphics.centeredText(this.font, this.getTitle(), this.width / 2, titleY, COLOR_TITLE);

        int contentX = boxX + BOX_PADDING;
        int scrollbarTop = titleY + this.font.lineHeight + BOX_PADDING;
        int scrollbarBottom = boxY + boxHeight - PADDING - CLOSE_BUTTON_HEIGHT - BOX_PADDING;

        graphics.enableScissor(contentX, scrollbarTop, contentX + boxWidth - BOX_PADDING * 2, scrollbarBottom);

        int drawY = scrollbarTop - scrollOffset;
        for (RenderedLine line : lines) {
            if (drawY + line.height >= scrollbarTop && drawY <= scrollbarBottom) {
                //? if >=26.1 {
                graphics.text(this.font, line.text, contentX, drawY, line.color, false);
                //?} else {
                /*graphics.drawString(this.font, line.text, contentX, drawY, line.color, false);
                 *///?}
            }
            drawY += line.height;
        }

        graphics.disableScissor();

        if (maxScroll > 0 && totalContentHeight > 0) {
            int scrollbarX = boxX + boxWidth - BOX_PADDING / 2 - 2;
            int scrollbarHeight = scrollbarBottom - scrollbarTop;

            graphics.fill(scrollbarX, scrollbarTop, scrollbarX + 3, scrollbarBottom, 0x44FFFFFF);

            int viewHeight = getViewHeight();
            float thumbRatio = (float) viewHeight / totalContentHeight;
            int thumbHeight = Math.max(20, (int) (scrollbarHeight * thumbRatio));
            int thumbY = scrollbarTop + (int) ((scrollbarHeight - thumbHeight) * ((float) scrollOffset / maxScroll));

            graphics.fill(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbHeight, 0xCCFFD700);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * 12));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void parseAndAddLine(String raw, int maxWidth) {
        int lineHeight = this.font.lineHeight + LINE_SPACING;

        if (raw.isBlank()) {
            lines.add(new RenderedLine("", COLOR_CONTENT, lineHeight / 2));
            return;
        }

        String text;
        int color;

        if (raw.startsWith("### ")) {
            text = raw.substring(4);
            color = COLOR_HEADING;
        } else if (raw.startsWith("## ")) {
            text = raw.substring(3);
            color = COLOR_HEADING;
        } else if (raw.startsWith("# ")) {
            text = raw.substring(2);
            color = COLOR_HEADING;
        } else if (raw.startsWith("- ") || raw.startsWith("* ")) {
            text = "• " + raw.substring(2);
            color = COLOR_CONTENT;
        } else {
            text = raw;
            color = COLOR_CONTENT;
        }

        text = text
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("`(.+?)`", "$1");

        if (this.font.width(text) > maxWidth) {
            for (FormattedText segment : this.font.getSplitter().splitLines(
                    FormattedText.of(text),
                    maxWidth,
                    Style.EMPTY
            )) {
                lines.add(new RenderedLine(segment.getString(), color, lineHeight));
            }
        } else {
            lines.add(new RenderedLine(text, color, lineHeight));
        }
    }

    private int getViewHeight() {
        int boxHeight = this.height - PADDING * 2;
        int titleSpace = BOX_PADDING + this.font.lineHeight + BOX_PADDING;
        int buttonSpace = PADDING + CLOSE_BUTTON_HEIGHT + BOX_PADDING;
        return boxHeight - titleSpace - buttonSpace;
    }

    private record RenderedLine(String text, int color, int height) {}
}