package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Modal overlay shown on the Modern UI wizard page when the user appears to
 * have skipped it (spent very little time there and made no changes).
 *
 * <p>Blocks forward navigation until the user either goes back to read the page
 * or explicitly acknowledges that they understand and want to continue anyway.
 */
public class ModernUISkipWarningOverlay extends AbstractComponent {

    private static final int PANEL_WIDTH = 310;
    private static final int PANEL_HEIGHT = 150;
    private static final int PADDING = 16;
    private static final int BUTTON_WIDTH = 126;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 10;

    private boolean visible = false;
    private Runnable onContinue;
    private Runnable onClose;

    private final CustomButtonWidget goBackButton;
    private final CustomButtonWidget continueButton;

    // Cached panel origin so render and placeButtons stay in sync
    private int panelX;
    private int panelY;

    public ModernUISkipWarningOverlay(int width, int height) {
        super(0, 0, width, height);

        goBackButton = new CustomButtonWidget(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.overlay.modern_ui_skip.go_back"),
                GuiHelper.BLANK_BUTTON_SPRITES, btn -> hide());

        continueButton = new CustomButtonWidget(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.overlay.modern_ui_skip.continue_anyway"),
                GuiHelper.BLANK_BUTTON_SPRITES, btn -> {
            hide();
            if (onContinue != null) onContinue.run();
        });

        addWidgets(List.of(goBackButton, continueButton));
        setVisible(false);
    }

    /** Called when the overlay is dismissed for any reason (Go Back or Continue Anyway). */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /** Shows the overlay. {@code onContinue} is invoked if the user clicks "Continue Anyway". */
    public void show(Runnable onContinue) {
        this.onContinue = onContinue;
        computeLayout();
        setVisible(true);
    }

    public boolean isVisible() {
        return visible;
    }

    private void hide() {
        setVisible(false);
        if (onClose != null) onClose.run();
    }

    private void setVisible(boolean visible) {
        this.visible = visible;
        goBackButton.visible = goBackButton.active = visible;
        continueButton.visible = continueButton.active = visible;
    }

    private void computeLayout() {
        // Use absolute screen coordinates so the panel and buttons sit correctly
        // regardless of where the overlay component is positioned in the hierarchy.
        int ox = getTotalX();
        int oy = getTotalY();
        panelX = ox + (getWidth() - PANEL_WIDTH) / 2;
        panelY = oy + (getHeight() - PANEL_HEIGHT) / 2;

        int buttonsY = panelY + PANEL_HEIGHT - PADDING - BUTTON_HEIGHT;
        int totalBtnW = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int buttonsX = panelX + (PANEL_WIDTH - totalBtnW) / 2;

        goBackButton.uilib$updateParentPosition(buttonsX, buttonsY);
        continueButton.uilib$updateParentPosition(buttonsX + BUTTON_WIDTH + BUTTON_GAP, buttonsY);
    }

    @Override
    public void renderBase(GuiGraphics graphics, int mouseX, int mouseY,
                           float partialTick, int parentWidth, int parentHeight) {
        if (!visible) return;
        super.renderBase(graphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick, int parentWidth, int parentHeight) {
        if (!visible) return;

        // Dim the full page area using absolute screen coordinates
        int ox = getTotalX();
        int oy = getTotalY();
        graphics.fill(ox, oy, ox + getWidth(), oy + getHeight(), GuiColors.OVERLAY_DIM);

        // Panel background + border
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT,
                GuiColors.OVERLAY_BACKGROUND);
        GuiHelper.drawBorder(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT,
                GuiColors.OVERLAY_BORDER);

        var font = Minecraft.getInstance().font;
        int cx = panelX + PANEL_WIDTH / 2;
        int cy = panelY + PADDING;
        int innerWidth = PANEL_WIDTH - PADDING * 2;

        graphics.drawCenteredString(font,
                Component.translatable("gui.packcore.overlay.modern_ui_skip.title"),
                cx, cy, GuiColors.WARNING);
        cy += font.lineHeight + 10;

        // Word-wrap the body message within the panel
        for (var line : font.split(
                Component.translatable("gui.packcore.overlay.modern_ui_skip.message"), innerWidth)) {
            graphics.drawCenteredString(font, line, cx, cy, GuiColors.TEXT_SECONDARY);
            cy += font.lineHeight + 2;
        }
    }
}