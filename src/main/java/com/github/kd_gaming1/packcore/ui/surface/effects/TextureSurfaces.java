package com.github.kd_gaming1.packcore.ui.surface.effects;

import io.wispforest.owo.ui.core.Surface;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
//? if >= 1.21.8 {

/*import net.minecraft.client.gl.RenderPipelines;

*///?}

/**
 * Enhanced Surface implementations for texture rendering with different scaling modes.
 *
 * <p>Provides CSS-like background scaling options:
 * <ul>
 *   <li>{@link #scaledCover} - Scale to cover component while preserving aspect ratio (may crop)</li>
 *   <li>{@link #scaledContain} - Scale to fit within component while preserving aspect ratio (may letterbox)</li>
 *   <li>{@link #stretched} - Stretch texture to fill component exactly (may distort)</li>
 *   <li>{@link #tiled} - Tile texture to fill component (software-based, no GL_REPEAT dependency)</li>
 * </ul>
 */
public final class TextureSurfaces {
    private TextureSurfaces() {}

    /**
     * Scale texture to cover the entire component while preserving aspect ratio.
     * The texture may be cropped if aspect ratios don't match.
     * Equivalent to CSS {@code background-size: cover}.
     */
    public static Surface scaledCover(Identifier texture, int textureWidth, int textureHeight) {
        validateTextureDimensions(textureWidth, textureHeight);
        return (context, component) -> {
            if (component.width() <= 0 || component.height() <= 0) return;

            int cx = component.x();
            int cy = component.y();
            int cw = component.width();
            int ch = component.height();

            // Calculate scale to cover (larger scale factor)
            float scaleX = (float) cw / textureWidth;
            float scaleY = (float) ch / textureHeight;
            float scale = Math.max(scaleX, scaleY);

            int scaledWidth = Math.round(textureWidth * scale);
            int scaledHeight = Math.round(textureHeight * scale);

            // Center the scaled texture
            int drawX = cx + (cw - scaledWidth) / 2;
            int drawY = cy + (ch - scaledHeight) / 2;

            context.drawTexture(
                    //? if >=1.21.8 {
                    
                    /*RenderPipelines.GUI_TEXTURED,
                     
                    *///?} else {
                    RenderLayer::getGuiTextured,
                    //?}
                    texture,
                    drawX, drawY,
                    0f, 0f,
                    scaledWidth, scaledHeight,
                    textureWidth, textureHeight,
                    textureWidth, textureHeight,
                    -1
            );
        };
    }

    /**
     * Scale texture to fit within the component while preserving aspect ratio.
     * The component may have empty space if aspect ratios don't match.
     * Equivalent to CSS {@code background-size: contain}.
     */
    public static Surface scaledContain(Identifier texture, int textureWidth, int textureHeight) {
        validateTextureDimensions(textureWidth, textureHeight);
        return (context, component) -> {
            if (component.width() <= 0 || component.height() <= 0) return;

            int cx = component.x();
            int cy = component.y();
            int cw = component.width();
            int ch = component.height();

            // Calculate scale to contain (smaller scale factor)
            float scaleX = (float) cw / textureWidth;
            float scaleY = (float) ch / textureHeight;
            float scale = Math.min(scaleX, scaleY);

            int scaledWidth = Math.round(textureWidth * scale);
            int scaledHeight = Math.round(textureHeight * scale);

            // Center the scaled texture
            int drawX = cx + (cw - scaledWidth) / 2;
            int drawY = cy + (ch - scaledHeight) / 2;

            context.drawTexture(
                    //? if >=1.21.8 {
                    
                    /*RenderPipelines.GUI_TEXTURED,
                     
                    *///?} else {
                    RenderLayer::getGuiTextured,
                    //?}
                    texture,
                    drawX, drawY,
                    0f, 0f,
                    scaledWidth, scaledHeight,
                    textureWidth, textureHeight,
                    textureWidth, textureHeight,
                    -1
            );
        };
    }

    /**
     * Stretch texture to fill the component exactly, ignoring aspect ratio.
     * This may distort the texture if component and texture aspect ratios differ.
     * Equivalent to CSS {@code background-size: 100% 100%}.
     */
    public static Surface stretched(Identifier texture, int textureWidth, int textureHeight) {
        validateTextureDimensions(textureWidth, textureHeight);
        return (context, component) -> {
            if (component.width() <= 0 || component.height() <= 0) return;

            context.drawTexture(
                    //? if >=1.21.8 {
                    
                    /*RenderPipelines.GUI_TEXTURED,
                     
                    *///?} else {
                    RenderLayer::getGuiTextured,
                    //?}
                    texture,
                    component.x(), component.y(),
                    0f, 0f,
                    component.width(), component.height(),
                    textureWidth, textureHeight,
                    textureWidth, textureHeight,
                    -1
            );
        };
    }

    /**
     * Tile texture to fill the component by repeating it as needed.
     * This is implemented in software and doesn't depend on GL_REPEAT or atlas settings.
     * Partial tiles at edges are clipped appropriately.
     */
    public static Surface tiled(Identifier texture, int textureWidth, int textureHeight) {
        validateTextureDimensions(textureWidth, textureHeight);
        return (context, component) -> {
            if (component.width() <= 0 || component.height() <= 0) return;

            int startX = component.x();
            int startY = component.y();
            int endX = startX + component.width();
            int endY = startY + component.height();

            // Tile the texture across the component
            for (int y = startY; y < endY; y += textureHeight) {
                int tileHeight = Math.min(textureHeight, endY - y);

                for (int x = startX; x < endX; x += textureWidth) {
                    int tileWidth = Math.min(textureWidth, endX - x);

                    // Draw partial texture region for edge tiles
                    context.drawTexture(
                            //? if >=1.21.8 {
                            
                            /*RenderPipelines.GUI_TEXTURED,
                             
                            *///?} else {
                            RenderLayer::getGuiTextured,
                            //?}
                            texture,
                            x, y,
                            0f, 0f,
                            tileWidth, tileHeight,
                            textureWidth, textureHeight
                    );
                }
            }
        };
    }

    /**
     * Create a tiled surface with custom tile dimensions.
     * Useful when you want to tile at a different scale than the original texture.
     */
    public static Surface tiledCustom(Identifier texture, int textureWidth, int textureHeight, int tileWidth, int tileHeight) {
        validateTextureDimensions(textureWidth, textureHeight);
        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException("Tile dimensions must be positive: " + tileWidth + "x" + tileHeight);
        }

        return (context, component) -> {
            if (component.width() <= 0 || component.height() <= 0) return;

            int startX = component.x();
            int startY = component.y();
            int endX = startX + component.width();
            int endY = startY + component.height();

            for (int y = startY; y < endY; y += tileHeight) {
                int drawHeight = Math.min(tileHeight, endY - y);

                for (int x = startX; x < endX; x += tileWidth) {
                    int drawWidth = Math.min(tileWidth, endX - x);

                    context.drawTexture(
                            //? if >=1.21.8 {
                            
                            /*RenderPipelines.GUI_TEXTURED,
                             
                            *///?} else {
                            RenderLayer::getGuiTextured,
                            //?}
                            texture,
                            x, y,
                            0f, 0f,
                            drawWidth, drawHeight,
                            textureWidth, textureHeight,
                            tileWidth, tileHeight,
                            -1
                    );
                }
            }
        };
    }

    /**
     * Create a surface that combines multiple surfaces, drawing them in order.
     * Later surfaces are drawn on top of earlier ones.
     */
    public static Surface layered(Surface... surfaces) {
        if (surfaces.length == 0) {
            return Surface.BLANK;
        }

        return (context, component) -> {
            for (Surface surface : surfaces) {
                surface.draw(context, component);
            }
        };
    }

    private static void validateTextureDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive: " + width + "x" + height);
        }
    }
}