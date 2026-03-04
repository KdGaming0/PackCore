package com.github.kd_gaming1.packcore.gui.util;

import com.daqem.uilib.gui.component.sprite.SpriteComponent;
import net.minecraft.resources.Identifier;

/**
 * Utility class for creating and positioning sprites with responsive scaling
 */
public class SpriteHelper {

    /**
     * Container for calculated sprite dimensions and position
     */
    public record SpriteDimensions(int x, int y, int width, int height) { }

    /**
     * Calculate scaled dimensions maintaining aspect ratio
     *
     * @param screenWidth Current screen width
     * @param screenHeight Current screen height
     * @param originalWidth Original image width
     * @param originalHeight Original image height
     * @param scalePercent Scale as percentage of screen width (0.0 to 1.0, e.g., 0.8 = 80%)
     * @param xPos X position (or use centerX() for centering)
     * @param yPos Y position
     * @return SpriteDimensions with position and scaled size
     */
    public static SpriteDimensions scale(
            int screenWidth,
            int screenHeight,
            int originalWidth,
            int originalHeight,
            double scalePercent,
            int xPos,
            int yPos) {

        int scaledWidth = (int)(screenWidth * scalePercent);
        int scaledHeight = (originalHeight * scaledWidth) / originalWidth;

        return new SpriteDimensions(xPos, yPos, scaledWidth, scaledHeight);
    }

    /**
     * Calculate scaled dimensions and center horizontally
     */
    public static SpriteDimensions scaleAndCenter(
            int screenWidth,
            int screenHeight,
            int originalWidth,
            int originalHeight,
            double scalePercent,
            int yPos) {

        int scaledWidth = (int)(screenWidth * scalePercent);
        int scaledHeight = (originalHeight * scaledWidth) / originalWidth;
        int centeredX = (screenWidth - scaledWidth) / 2;

        return new SpriteDimensions(centeredX, yPos, scaledWidth, scaledHeight);
    }

    /**
     * Calculate scaled dimensions with maximum width constraint
     */
    public static SpriteDimensions scaleWithMax(
            int screenWidth,
            int screenHeight,
            int originalWidth,
            int originalHeight,
            int maxWidth,
            double scalePercent,
            int xPos,
            int yPos) {

        int scaledWidth = Math.min(maxWidth, (int)(screenWidth * scalePercent));
        int scaledHeight = (originalHeight * scaledWidth) / originalWidth;

        return new SpriteDimensions(xPos, yPos, scaledWidth, scaledHeight);
    }

    /**
     * Calculate scaled dimensions with max width and center horizontally
     */
    public static SpriteDimensions scaleWithMaxAndCenter(
            int screenWidth,
            int screenHeight,
            int originalWidth,
            int originalHeight,
            int maxWidth,
            double scalePercent,
            int yPos) {

        int scaledWidth = Math.min(maxWidth, (int)(screenWidth * scalePercent));
        int scaledHeight = (originalHeight * scaledWidth) / originalWidth;
        int centeredX = (screenWidth - scaledWidth) / 2;

        return new SpriteDimensions(centeredX, yPos, scaledWidth, scaledHeight);
    }

    /**
     * Create a sprite component with calculated dimensions
     */
    public static SpriteComponent createSprite(SpriteDimensions dims, Identifier spriteLocation) {
        return new SpriteComponent(dims.x, dims.y, dims.width, dims.height, spriteLocation);
    }

    /**
     * Create a scaled sprite in one call
     */
    public static SpriteComponent createScaledSprite(
            int screenWidth,
            int screenHeight,
            int originalWidth,
            int originalHeight,
            double scalePercent,
            int xPos,
            int yPos,
            Identifier spriteLocation) {

        SpriteDimensions dims = scale(screenWidth, screenHeight, originalWidth, originalHeight, scalePercent, xPos, yPos);
        return createSprite(dims, spriteLocation);
    }

    /**
     * Create a scaled and centered sprite in one call
     */
    public static SpriteComponent createScaledCenteredSprite(
            int screenWidth,
            int screenHeight,
            int originalWidth,
            int originalHeight,
            double scalePercent,
            int yPos,
            Identifier spriteLocation) {

        SpriteDimensions dims = scaleAndCenter(screenWidth, screenHeight, originalWidth, originalHeight, scalePercent, yPos);
        return createSprite(dims, spriteLocation);
    }

    /**
     * Calculate centered X position
     */
    public static int centerX(int screenWidth, int componentWidth) {
        return (screenWidth - componentWidth) / 2;
    }

    /**
     * Calculate centered Y position
     */
    public static int centerY(int screenHeight, int componentHeight) {
        return (screenHeight - componentHeight) / 2;
    }
}