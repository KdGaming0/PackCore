package com.github.kd_gaming1.packcore.gui.util;

import com.daqem.uilib.gui.component.sprite.SpriteComponent;
import net.minecraft.resources.Identifier;

/**
 * Utility for creating and positioning sprites with responsive scaling.
 */
@SuppressWarnings("unused")
public class SpriteHelper {

    /** Holds the calculated position and size of a sprite. */
    public record SpriteDimensions(int x, int y, int width, int height) { }

    /** Scale to a percentage of screen width, maintaining aspect ratio. */
    public static SpriteDimensions scale(
            int screenWidth, int screenHeight,
            int originalWidth, int originalHeight,
            double scalePercent,
            int x, int y) {

        int scaledWidth = (int) (screenWidth * scalePercent);
        int scaledHeight = (originalHeight * scaledWidth) / originalWidth;
        return new SpriteDimensions(x, y, scaledWidth, scaledHeight);
    }

    /** Scale and center horizontally on the screen. */
    public static SpriteDimensions scaleAndCenter(
            int screenWidth, int screenHeight,
            int originalWidth, int originalHeight,
            double scalePercent,
            int y) {

        int scaledWidth = (int) (screenWidth * scalePercent);
        int scaledHeight = (originalHeight * scaledWidth) / originalWidth;
        int centeredX = (screenWidth - scaledWidth) / 2;
        return new SpriteDimensions(centeredX, y, scaledWidth, scaledHeight);
    }

    /** Scale with a maximum width cap. */
    public static SpriteDimensions scaleWithMax(
            int screenWidth, int screenHeight,
            int originalWidth, int originalHeight,
            int maxWidth,
            double scalePercent,
            int x, int y) {

        int scaledWidth = Math.min(maxWidth, (int) (screenWidth * scalePercent));
        int scaledHeight = (originalHeight * scaledWidth) / originalWidth;
        return new SpriteDimensions(x, y, scaledWidth, scaledHeight);
    }

    /** Scale with a maximum width cap and center horizontally. */
    public static SpriteDimensions scaleWithMaxAndCenter(
            int screenWidth, int screenHeight,
            int originalWidth, int originalHeight,
            int maxWidth,
            double scalePercent,
            int y) {

        int scaledWidth = Math.min(maxWidth, (int) (screenWidth * scalePercent));
        int scaledHeight = (originalHeight * scaledWidth) / originalWidth;
        int centeredX = (screenWidth - scaledWidth) / 2;
        return new SpriteDimensions(centeredX, y, scaledWidth, scaledHeight);
    }

    public static SpriteComponent createSprite(SpriteDimensions dims, Identifier spriteLocation) {
        return new SpriteComponent(dims.x(), dims.y(), dims.width(), dims.height(), spriteLocation);
    }

    public static SpriteComponent createScaledSprite(
            int screenWidth, int screenHeight,
            int originalWidth, int originalHeight,
            double scalePercent,
            int x, int y,
            Identifier spriteLocation) {

        return createSprite(scale(screenWidth, screenHeight, originalWidth, originalHeight, scalePercent, x, y), spriteLocation);
    }

    public static SpriteComponent createScaledCenteredSprite(
            int screenWidth, int screenHeight,
            int originalWidth, int originalHeight,
            double scalePercent,
            int y,
            Identifier spriteLocation) {

        return createSprite(scaleAndCenter(screenWidth, screenHeight, originalWidth, originalHeight, scalePercent, y), spriteLocation);
    }

    public static int centerX(int screenWidth, int componentWidth) {
        return (screenWidth - componentWidth) / 2;
    }

    public static int centerY(int screenHeight, int componentHeight) {
        return (screenHeight - componentHeight) / 2;
    }
}