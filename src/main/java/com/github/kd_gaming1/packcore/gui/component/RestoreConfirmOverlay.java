package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.configpack.BackupEntry;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RestoreConfirmOverlay extends AbstractComponent {

    private static final int PANEL_WIDTH = 310;
    private static final int PADDING = 14;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 10;

    private static final int COLOR_DIM = 0xBB000000;
    private static final int COLOR_BACKGROUND = 0xF0080F1A;
    private static final int COLOR_BORDER = 0x882196F3;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SUBTEXT = 0xFFAAAAAA;
    private static final int COLOR_WARNING = 0xFFFFAA00;
    private static final int COLOR_WARNING_BG = 0x33FFAA00;
    private static final int COLOR_NOTE = 0xFF666666;

    private boolean visible = false;
    private BackupEntry backup;
    private Runnable onClose;

    private final CustomButtonWidget cancelButton;
    private final CustomButtonWidget confirmButton;

    private int panelX;
    private int panelY;
    private int panelHeight;

    public RestoreConfirmOverlay(int width, int height) {
        super(0, 0, width, height);

        cancelButton = new CustomButtonWidget(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.overlay.restore.button.cancel"),
                GuiHelper.BLANK_BUTTON_SPRITES, btn -> setVisible(false));

        confirmButton = new CustomButtonWidget(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.overlay.restore.button.confirm"),
                GuiHelper.BLANK_BUTTON_SPRITES, btn -> confirmRestore());

        addWidgets(List.of(cancelButton, confirmButton));
        setVisible(false);
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void show(BackupEntry backup) {
        this.backup = backup;
        setVisible(true);
        layoutPanel();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        cancelButton.visible = confirmButton.visible = visible;
        cancelButton.active = confirmButton.active = visible;

        if (!visible && onClose != null) {
            onClose.run();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    private void layoutPanel() {
        var font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight;
        int warningHeight = lineHeight * 2 + 12;

        // Sum every content row so panelHeight and buttonsY share one source of truth
        int contentHeight = lineHeight + 10   // title
                + lineHeight + 4             // filename
                + lineHeight + 14            // timestamp
                + warningHeight + 8          // warning box
                + lineHeight + 12            // closing note
                + BUTTON_HEIGHT;

        panelHeight = PADDING * 2 + contentHeight;

        panelX = getTotalX() + (getWidth() - PANEL_WIDTH) / 2;
        panelY = getTotalY() + (getHeight() - panelHeight) / 2;

        int buttonsX = panelX + (PANEL_WIDTH - (BUTTON_WIDTH * 2 + BUTTON_GAP)) / 2;
        int buttonsY = panelY + PADDING + contentHeight - BUTTON_HEIGHT;

        cancelButton.uilib$updateParentPosition(buttonsX, buttonsY);
        confirmButton.uilib$updateParentPosition(buttonsX + BUTTON_WIDTH + BUTTON_GAP, buttonsY);
    }

    private void confirmRestore() {
        if (backup == null) return;
        PackCoreConfig.pendingRestoreBackup = backup.zipPath().getFileName().toString();
        MidnightConfig.write("packcore");
        Minecraft.getInstance().stop();
    }

    @Override
    public void renderBase(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        if (visible) {
            super.renderBase(graphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        if (!visible || backup == null) return;

        var font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight;
        int warningHeight = lineHeight * 2 + 12;

        graphics.fill(getTotalX(), getTotalY(), getTotalX() + getWidth(), getTotalY() + getHeight(), COLOR_DIM);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, COLOR_BACKGROUND);
        drawBorder(graphics);

        int currentY = panelY + PADDING;

        graphics.drawCenteredString(font,
                Component.translatable("gui.packcore.overlay.restore.title"),
                panelX + PANEL_WIDTH / 2, currentY, COLOR_TEXT);
        currentY += lineHeight + 10;

        String truncatedName = font.plainSubstrByWidth(
                backup.zipPath().getFileName().toString(), PANEL_WIDTH - PADDING * 2);
        graphics.drawCenteredString(font, truncatedName, panelX + PANEL_WIDTH / 2, currentY, COLOR_TEXT);
        currentY += lineHeight + 4;

        graphics.drawCenteredString(font, backup.displayName(), panelX + PANEL_WIDTH / 2, currentY, COLOR_SUBTEXT);
        currentY += lineHeight + 14;

        graphics.fill(panelX + PADDING, currentY,
                panelX + PANEL_WIDTH - PADDING, currentY + warningHeight, COLOR_WARNING_BG);
        graphics.drawString(font,
                Component.translatable("gui.packcore.overlay.restore.warning1"),
                panelX + PADDING + 8, currentY + 5, COLOR_WARNING, false);
        graphics.drawString(font,
                Component.translatable("gui.packcore.overlay.restore.warning2"),
                panelX + PADDING + 8, currentY + 5 + lineHeight + 2, COLOR_WARNING, false);
        currentY += warningHeight + 8;

        graphics.drawCenteredString(font,
                Component.translatable("gui.packcore.overlay.restore.note"),
                panelX + PANEL_WIDTH / 2, currentY, COLOR_NOTE);
    }

    private void drawBorder(GuiGraphics graphics) {
        GuiHelper.drawBorder(graphics, panelX, panelY, PANEL_WIDTH, panelHeight, COLOR_BORDER);
    }
}
