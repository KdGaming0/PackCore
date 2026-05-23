package com.github.kd_gaming1.packcore.gui.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * A branded toast notification showing a title and message line.
 * Use {@link ToastHelper} to display one — don't instantiate directly.
 */
class PackCoreToast implements Toast {

    private static final long DISPLAY_TIME_MS = 5_000L;

    private static final int COLOR_BACKGROUND = 0xF0080F1A;
    private static final int COLOR_BORDER = 0xFFFFAA00;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_MESSAGE = 0xFFAAAAAA;

    private static final int ICON_SIZE = 20;
    private static final int ICON_MARGIN = 6;
    private static final int TEXT_MARGIN = 6;

    private static final Identifier ICON = Identifier.fromNamespaceAndPath(MOD_ID, "assets/sbe_logo");

    private final Component title;
    private final Component message;
    private Visibility visibility = Visibility.SHOW;

    PackCoreToast(Component title, Component message) {
        this.title = title;
        this.message = message;
    }

    @Override
    public @NonNull Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleFor) {
        if (fullyVisibleFor >= (long) (DISPLAY_TIME_MS * manager.getNotificationDisplayTimeMultiplier())) {
            visibility = Visibility.HIDE;
        }
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleFor) {
        //?} else {
     /*@Override
    public void render(GuiGraphicsExtractor graphics, Font font, long fullyVisibleFor) {
    *///?}
        int w = width();
        int h = height();

        graphics.fill(0, 0, w, h, COLOR_BACKGROUND);
        GuiHelper.drawBorder(graphics, 0, 0, w, h, COLOR_BORDER);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON, ICON_MARGIN, (h - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE);

        int textX = ICON_MARGIN + ICON_SIZE + TEXT_MARGIN;
        int titleY = h / 2 - font.lineHeight - 1;
        int messageY = h / 2 + 1;

        //? if >=26.1 {
        graphics.text(font, title, textX, titleY, COLOR_TITLE, false);
        graphics.text(font, message, textX, messageY, COLOR_MESSAGE, false);
            //?} else {
     /*graphics.drawString(font, title, textX, titleY, COLOR_TITLE, false);
        graphics.drawString(font, message, textX, messageY, COLOR_MESSAGE, false);
    *///?}
    }

    @Override public int width()  { return 200; }
    @Override public int height() { return 40; }
}