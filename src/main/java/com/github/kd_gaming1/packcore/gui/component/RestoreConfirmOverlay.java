package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.configpack.BackupEntry;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import com.daqem.uilib.gui.widget.CustomButtonWidget;

public class RestoreConfirmOverlay extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/RestoreConfirmOverlay");

    private static final float PANEL_W_PCT = 0.80f;
    private static final float PANEL_H_PCT = 0.75f;
    private static final float TREE_W_PCT  = 0.45f;

    private static final int PADDING      = 14;
    private static final int DIVIDER_GAP  = 10;
    private static final int BUTTON_WIDTH = 130;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP   = 8;

    private boolean visible = false;
    private BackupEntry backup;
    private Runnable onClose;

    private FileTreeNode treeRoot;
    private FileTreeComponent fileTree;

    private final CustomButtonWidget cancelButton;
    private final CustomButtonWidget confirmButton;

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

    public void setOnClose(Runnable onClose) { this.onClose = onClose; }

    public void show(BackupEntry backup) {
        this.backup = backup;
        this.treeRoot = null;
        this.fileTree = null;

        try {
            treeRoot = FileTreeBuilder.fromZip(backup.zipPath());
            treeRoot.setSelectedRecursive(true);
        } catch (IOException e) {
            LOGGER.error("Failed to read backup zip for file tree: {}", e.getMessage());
        }

        setVisible(true);
        rebuildTree();
    }

    /** Called after show() and after any resize so the tree sits in the right place. */
    private void rebuildTree() {
        clearComponents();
        if (treeRoot == null) return;

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelW = getPanelW();
        int panelH = getPanelH();
        int treeW  = getTreeW(panelW);
        int treeH  = panelH - PADDING * 3;

        fileTree = new FileTreeComponent(
                panelX,
                panelY - 14,
                treeW,
                treeH,
                treeRoot);
        addComponent(fileTree);

        updateParentPosition(getParentX(), getParentY(), getWidth(), getHeight());
        placeButtons(panelX, panelY, panelW, panelH, treeW);
    }

    private void placeButtons(int panelX, int panelY, int panelW, int panelH, int treeW) {
        int rightX     = panelX + PADDING + treeW + DIVIDER_GAP;
        int rightW     = panelW - PADDING - treeW - DIVIDER_GAP - PADDING;
        int buttonsY   = panelY + panelH - PADDING - BUTTON_HEIGHT;
        int buttonsX   = rightX + (rightW - (BUTTON_WIDTH * 2 + BUTTON_GAP)) / 2;
        cancelButton.uilib$updateParentPosition(buttonsX, buttonsY);
        confirmButton.uilib$updateParentPosition(buttonsX + BUTTON_WIDTH + BUTTON_GAP, buttonsY);
    }

    // ── Dimension helpers (all derived from current screen size) ─────────────

    private int getPanelW() { return (int)(getWidth()  * PANEL_W_PCT); }
    private int getPanelH() { return (int)(getHeight() * PANEL_H_PCT); }
    private int getPanelX() { return (getWidth()  - getPanelW()) / 2; }
    private int getPanelY() { return (getHeight() - getPanelH()) / 2; }
    private int getTreeW(int panelW) { return (int)(panelW * TREE_W_PCT); }

    // ── Visibility ────────────────────────────────────────────────────────────

    public void setVisible(boolean visible) {
        this.visible = visible;
        cancelButton.visible = confirmButton.visible = visible;
        cancelButton.active  = confirmButton.active  = visible;
        if (!visible && onClose != null) onClose.run();
    }

    public boolean isVisible() { return visible; }

    // ── Confirm ───────────────────────────────────────────────────────────────

    private void confirmRestore() {
        if (backup == null) return;
        PackCoreConfig.pendingRestoreBackup = backup.zipPath().getFileName().toString();
        if (treeRoot != null) {
            List<String> selected = treeRoot.collectSelectedPaths();
            List<String> all = collectAllFiles(treeRoot);
            PackCoreConfig.pendingRestoreBackupFiles =
                    selected.size() < all.size() ? String.join("|", selected) : "";
        } else {
            PackCoreConfig.pendingRestoreBackupFiles = "";
        }
        MidnightConfig.write(MOD_ID);
        Minecraft.getInstance().stop();
    }

    private List<String> collectAllFiles(FileTreeNode node) {
        List<String> result = new ArrayList<>();
        collectFilesRecursive(node, result);
        return result;
    }

    private void collectFilesRecursive(FileTreeNode node, List<String> result) {
        if (!node.isDirectory()) result.add(node.path());
        for (FileTreeNode child : node.children()) collectFilesRecursive(child, result);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void renderBase(GuiGraphics graphics, int mouseX, int mouseY,
                           float partialTick, int parentWidth, int parentHeight) {
        if (visible) super.renderBase(graphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick, int parentWidth, int parentHeight) {
        if (!visible || backup == null) return;

        var font = Minecraft.getInstance().font;
        int lh = font.lineHeight;

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelW = getPanelW();
        int panelH = getPanelH();
        int treeW  = getTreeW(panelW);

        // Dim + panel
        graphics.fill(getTotalX(), getTotalY(),
                getTotalX() + getWidth(), getTotalY() + getHeight(), GuiColors.OVERLAY_DIM);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, GuiColors.OVERLAY_BACKGROUND);
        GuiHelper.drawBorder(graphics, panelX, panelY, panelW, panelH, GuiColors.OVERLAY_BORDER);

        // Tree column header
        graphics.drawString(font,
                Component.translatable("gui.packcore.overlay.restore.select_files").getString(),
                panelX + PADDING, panelY + PADDING, GuiColors.TEXT_SECONDARY, false);

        // Divider
        int divX = panelX + PADDING + treeW + DIVIDER_GAP / 2;
        graphics.fill(divX, panelY + PADDING, divX + 1, panelY + panelH - PADDING, GuiColors.BORDER_IDLE);

        // Right column
        int rightX = panelX + PADDING + treeW + DIVIDER_GAP;
        int rightW = panelW - PADDING - treeW - DIVIDER_GAP - PADDING;
        int cy = panelY + PADDING;

        graphics.drawCenteredString(font,
                Component.translatable("gui.packcore.overlay.restore.title").getString(),
                rightX + rightW / 2, cy, GuiColors.TEXT_PRIMARY);
        cy += lh + 6;

        graphics.drawCenteredString(font,
                font.plainSubstrByWidth(backup.zipPath().getFileName().toString(), rightW),
                rightX + rightW / 2, cy, GuiColors.TEXT_PRIMARY);
        cy += lh + 4;

        graphics.drawCenteredString(font, backup.displayName(),
                rightX + rightW / 2, cy, GuiColors.TEXT_SECONDARY);
        cy += lh + 12;

        int warnH = lh * 2 + 12;
        graphics.fill(rightX, cy, rightX + rightW, cy + warnH, GuiColors.WARNING_BACKGROUND);
        graphics.drawString(font,
                Component.translatable("gui.packcore.overlay.restore.warning1"),
                rightX + 8, cy + 5, GuiColors.WARNING, false);
        graphics.drawString(font,
                Component.translatable("gui.packcore.overlay.restore.warning2"),
                rightX + 8, cy + 5 + lh + 2, GuiColors.WARNING, false);
        cy += warnH + 8;

        graphics.drawCenteredString(font,
                Component.translatable("gui.packcore.overlay.restore.note"),
                rightX + rightW / 2, cy, GuiColors.TEXT_NOTE);
    }
}