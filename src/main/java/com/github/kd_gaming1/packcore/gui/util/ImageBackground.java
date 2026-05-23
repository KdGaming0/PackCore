package com.github.kd_gaming1.packcore.gui.util;

import com.daqem.uilib.gui.background.AbstractBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Custom background_old that renders a texture using one of four layout modes.
 */
public class ImageBackground extends AbstractBackground {

    public enum BackgroundMode {
        /** Stretch the image to fill the entire screen. */
        STRETCH,
        /** Tile the image across the screen. */
        TILE,
        /** Center image at its natural size. */
        CENTER,
        /** Scale to fit the screen while maintaining an aspect ratio. */
        FIT
    }

    private final Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    private final BackgroundMode mode;

    // Cached layout — invalidated when screen size changes
    private int cachedScreenWidth = -1;
    private int cachedScreenHeight = -1;
    private int cachedX, cachedY, cachedW, cachedH;

    public ImageBackground(Identifier texture, int textureWidth, int textureHeight, BackgroundMode mode) {
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.mode = mode;
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        //?} else {
     /*@Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    *///?}
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        switch (mode) {
            case STRETCH -> renderStretched(graphics, screenWidth, screenHeight);
            case TILE -> renderTiled(graphics, screenWidth, screenHeight);
            case CENTER -> renderCentered(graphics, screenWidth, screenHeight);
            case FIT -> renderFit(graphics, screenWidth, screenHeight);
        }
    }

    private boolean cacheIsStale(int screenWidth, int screenHeight) {
        return screenWidth != cachedScreenWidth || screenHeight != cachedScreenHeight;
    }

    private void updateCache(int screenWidth, int screenHeight, int x, int y, int w, int h) {
        cachedScreenWidth = screenWidth;
        cachedScreenHeight = screenHeight;
        cachedX = x;
        cachedY = y;
        cachedW = w;
        cachedH = h;
    }

    private void renderStretched(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                0, 0,
                0.0F, 0.0F,
                screenWidth, screenHeight,
                textureWidth, textureHeight,
                textureWidth, textureHeight
        );
    }

    private void renderTiled(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        for (int tx = 0; tx * textureWidth < screenWidth; tx++) {
            for (int ty = 0; ty * textureHeight < screenHeight; ty++) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        tx * textureWidth, ty * textureHeight,
                        0.0F, 0.0F,
                        textureWidth, textureHeight,
                        textureWidth, textureHeight,
                        textureWidth, textureHeight
                );
            }
        }
    }

    private void renderCentered(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        if (cacheIsStale(screenWidth, screenHeight)) {
            int x = (screenWidth - textureWidth) / 2;
            int y = (screenHeight - textureHeight) / 2;
            updateCache(screenWidth, screenHeight, x, y, textureWidth, textureHeight);
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                cachedX, cachedY,
                0.0F, 0.0F,
                textureWidth, textureHeight,
                textureWidth, textureHeight,
                textureWidth, textureHeight
        );
    }

    private void renderFit(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        if (cacheIsStale(screenWidth, screenHeight)) {
            float scale = Math.min(
                    (float) screenWidth / textureWidth,
                    (float) screenHeight / textureHeight
            );
            int w = (int) (textureWidth * scale);
            int h = (int) (textureHeight * scale);
            int x = (screenWidth - w) / 2;
            int y = (screenHeight - h) / 2;
            updateCache(screenWidth, screenHeight, x, y, w, h);
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                cachedX, cachedY,
                0.0F, 0.0F,
                cachedW, cachedH,
                textureWidth, textureHeight,
                textureWidth, textureHeight
        );
    }
}