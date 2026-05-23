package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.util.ScreenResolution;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class ConfigSwitchOverlay extends AbstractComponent {

    private static final int PANEL_WIDTH = 310;
    private static final int PADDING = 14;

    private static final int BUTTON_WIDTH = 130;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 10;

    private static final int RESOLUTION_TOLERANCE = 100;

    private static final int COLOR_DIM = 0xBB000000;
    private static final int COLOR_BACKGROUND = 0xF0080F1A;
    private static final int COLOR_BORDER = 0x882196F3;
    private static final int COLOR_ACCENT = 0xFF2196F3;

    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SUBTEXT = 0xFFAAAAAA;

    private static final int COLOR_WARNING_BACKGROUND = 0x33FFAA00;
    private static final int COLOR_WARNING = 0xFFFFAA00;

    private boolean visible = false;

    private ConfigPackEntry currentPack;
    private ConfigPackEntry newPack;
    private ScreenResolution.ScreenSize screenSize;

    private final ButtonWidget cancelButton;
    private final ButtonWidget applyButton;

    private Runnable onClose;

    // Cached layout values computed when the overlay is shown
    private int panelX;
    private int panelY;
    private int panelHeight;

    public ConfigSwitchOverlay(int width, int height) {
        super(0, 0, width, height);

        cancelButton = new CustomButtonWidget(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Cancel"), GuiHelper.BLANK_BUTTON_SPRITES, button -> setVisible(false));

        applyButton = new CustomButtonWidget(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Yes, apply on restart"), GuiHelper.BLANK_BUTTON_SPRITES, button -> applyConfig());

        addWidgets(List.of(cancelButton, applyButton));
        setVisible(false);
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void show(ConfigPackEntry current, ConfigPackEntry next) {
        this.currentPack = current;
        this.newPack = next;
        setVisible(true);
        layoutPanel();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;

        cancelButton.visible = applyButton.visible = visible;
        cancelButton.active = applyButton.active = visible;

        if (!visible && onClose != null) {
            onClose.run();
        }
    }

    private void layoutPanel() {
        if (screenSize == null) screenSize = ScreenResolution.detect();

        boolean hasMismatch = hasResolutionMismatch();

        var font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight;
        int warningHeight = hasMismatch ? (lineHeight * 2 + 18) : 0;

        panelHeight = (PADDING * 2) + lineHeight + 10 + (lineHeight * 3 + 10) + warningHeight + BUTTON_HEIGHT + lineHeight + 25;

        panelX = getTotalX() + (getWidth() - PANEL_WIDTH) / 2;
        panelY = getTotalY() + (getHeight() - panelHeight) / 2;

        int buttonsX = panelX + (PANEL_WIDTH - (BUTTON_WIDTH * 2 + BUTTON_GAP)) / 2;
        int buttonsY = panelY + PADDING + lineHeight + 10 + (lineHeight * 3 + 20) + (hasMismatch ? (lineHeight * 2 + 18) : 0);

        cancelButton.uilib$updateParentPosition(buttonsX, buttonsY);
        applyButton.uilib$updateParentPosition(buttonsX + BUTTON_WIDTH + BUTTON_GAP, buttonsY);
    }

    private void applyConfig() {
        if (newPack == null) return;

        PackCoreConfig.pendingConfigPack = newPack.zipPath().getFileName().toString();
        MidnightConfig.write(MOD_ID);
        Minecraft.getInstance().stop();
    }

    private boolean hasResolutionMismatch() {
        if (newPack == null || !newPack.config().has("targetWidth")) return false;

        if (screenSize == null) screenSize = ScreenResolution.detect();

        var config = newPack.config();
        int targetWidth = config.get("targetWidth").getAsInt();
        int targetHeight = config.get("targetHeight").getAsInt();

        return Math.abs(targetWidth - screenSize.width()) > RESOLUTION_TOLERANCE
                || Math.abs(targetHeight - screenSize.height()) > RESOLUTION_TOLERANCE;
    }

    //? if >=26.1 {
    @Override
    public void extractRenderStateBase(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        //?} else {
    /*@Override
    public void renderBase(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
     *///?}
        if (visible) {
            super.extractRenderStateBase(graphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
        }
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        //?} else {
    /*@Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
     *///?}
        if (!visible || newPack == null) return;

        var font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight;

        boolean hasMismatch = hasResolutionMismatch();
        int warningHeight = hasMismatch ? (lineHeight * 2 + 18) : 0;

        graphics.fill(getTotalX(), getTotalY(), getTotalX() + getWidth(), getTotalY() + getHeight(), COLOR_DIM);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, COLOR_BACKGROUND);
        drawBorder(graphics, panelX, panelY, panelHeight);

        int currentY = panelY + PADDING;

        graphics.centeredText(font, "Switch Active Config?", panelX + PANEL_WIDTH / 2, currentY, COLOR_TEXT);
        currentY += lineHeight + 10;

        int columnWidth = (PANEL_WIDTH - PADDING * 2 - 20) / 2;

        drawPackInfo(graphics, font, currentPack, panelX + PADDING, currentY, columnWidth, "Current");
        graphics.centeredText(font, "→", panelX + PANEL_WIDTH / 2, currentY + lineHeight, COLOR_ACCENT);
        drawPackInfo(graphics, font, newPack, panelX + PANEL_WIDTH - PADDING - columnWidth, currentY, columnWidth, "New");

        currentY += lineHeight * 3 + 20;

        if (hasMismatch) {
            var config = newPack.config();
            String packResolution = config.get("targetWidth").getAsInt() + "×" + config.get("targetHeight").getAsInt();
            String screenResolution = screenSize.width() + "×" + screenSize.height();

            graphics.fill(panelX + PADDING, currentY, panelX + PANEL_WIDTH - PADDING, currentY + warningHeight - 6, COLOR_WARNING_BACKGROUND);
            //? if >=26.1 {
            graphics.text(font, "⚠ Resolution Mismatch", panelX + PADDING + 8, currentY + 5, COLOR_WARNING, false);
            graphics.text(font, "Pack: " + packResolution + " | Screen: " + screenResolution,panelX + PADDING + 8, currentY + 5 + lineHeight + 2, COLOR_WARNING, false);
                //?} else {
              /*graphics.drawString(font, "⚠ Resolution Mismatch", panelX + PADDING + 8, currentY + 5, COLOR_WARNING, false);
            graphics.drawString(font, "Pack: " + packResolution + " | Screen: " + screenResolution, panelX + PADDING + 8, currentY + 5 + lineHeight + 2, COLOR_WARNING, false);
            *///?}

            currentY += warningHeight + 4;
        }

        graphics.centeredText(font,
                "The game will close to apply the config.",
                panelX + PANEL_WIDTH / 2,
                currentY + BUTTON_HEIGHT + 8,
                0xFF666666);
    }

    private void drawPackInfo(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font, ConfigPackEntry pack, int x, int y, int width, String label) {
        int lineHeight = font.lineHeight;

        //? if >=26.1 {
        graphics.text(font, label, x, y, 0xFF555555, false);
        //?} else {
        /*graphics.drawString(font, label, x, y, 0xFF555555, false);
         *///?}

        if (pack == null) {
            //? if >=26.1 {
            graphics.text(font, "None", x, y + lineHeight + 2, COLOR_SUBTEXT, false);
            //?} else {
            /*graphics.drawString(font, "None", x, y + lineHeight + 2, COLOR_SUBTEXT, false);
             *///?}
            return;
        }

        var config = pack.config();
        String name = config.has("name") ? config.get("name").getAsString() : pack.zipPath().getFileName().toString();
        String author = config.has("author") ? config.get("author").getAsString() : "Unknown";
        String version = config.has("version") ? config.get("version").getAsString() : "?";

        //? if >=26.1 {
        graphics.text(font, font.plainSubstrByWidth(name, width), x, y + lineHeight + 2, COLOR_TEXT, false);
        graphics.text(font, font.plainSubstrByWidth(author + " v" + version, width), x, y + lineHeight * 2 + 3, COLOR_SUBTEXT, false);
        //?} else {
        /*graphics.drawString(font, font.plainSubstrByWidth(name, width), x, y + lineHeight + 2, COLOR_TEXT, false);
        graphics.drawString(font, font.plainSubstrByWidth(author + " v" + version, width), x, y + lineHeight * 2 + 3, COLOR_SUBTEXT, false);
         *///?}
    }

    private void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int height) {
        GuiHelper.drawBorder(graphics, x, y, PANEL_WIDTH, height, COLOR_BORDER);
    }
}