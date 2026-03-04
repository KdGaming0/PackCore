package com.github.kd_gaming1.packcore.gui.util;

import com.daqem.uilib.gui.background.AbstractBackground;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Custom background that renders a texture/image
 */
public class ImageBackground extends AbstractBackground {

    private final Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    private final BackgroundMode mode;

    // Cached computed layout — invalidated when screen size changes
    private int cachedScreenWidth = -1;
    private int cachedScreenHeight = -1;
    private int cachedX, cachedY, cachedW, cachedH;

    public enum BackgroundMode {
        /** Stretch image to fill entire screen */
        STRETCH,
        /** Tile image across screen */
        TILE,
        /** Center image (no scaling) */
        CENTER,
        /** Scale to fit screen while maintaining aspect ratio */
        FIT
    }

    public ImageBackground(Identifier texture, int textureWidth, int textureHeight, BackgroundMode mode) {
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.mode = mode;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        switch (mode) {
            case STRETCH -> renderStretched(guiGraphics, screenWidth, screenHeight);
            case TILE    -> renderTiled(guiGraphics, screenWidth, screenHeight);
            case CENTER  -> renderCentered(guiGraphics, screenWidth, screenHeight);
            case FIT     -> renderFit(guiGraphics, screenWidth, screenHeight);
        }
    }

    /**
     * Invalidates the layout cache if the screen has been resized.
     * Returns true if the cache was already valid.
     */
    private boolean needsRecalculation(int screenWidth, int screenHeight) {
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

    private void renderStretched(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                0, 0,
                0.0F, 0.0F,
                screenWidth, screenHeight,
                textureWidth, textureHeight,
                textureWidth, textureHeight
        );
    }

    private void renderTiled(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        for (int tx = 0; tx * textureWidth < screenWidth; tx++) {
            for (int ty = 0; ty * textureHeight < screenHeight; ty++) {
                guiGraphics.blit(
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

    private void renderCentered(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (needsRecalculation(screenWidth, screenHeight)) {
            int x = (screenWidth - textureWidth) / 2;
            int y = (screenHeight - textureHeight) / 2;
            updateCache(screenWidth, screenHeight, x, y, textureWidth, textureHeight);
        }

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                cachedX, cachedY,
                0.0F, 0.0F,
                textureWidth, textureHeight,
                textureWidth, textureHeight,
                textureWidth, textureHeight
        );
    }

    private void renderFit(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (needsRecalculation(screenWidth, screenHeight)) {
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

        guiGraphics.blit(
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