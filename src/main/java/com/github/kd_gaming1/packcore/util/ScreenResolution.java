package com.github.kd_gaming1.packcore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Detects the primary screen resolution using AWT.
 */
public class ScreenResolution {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ScreenResolution");

    public static final int FALLBACK_WIDTH  = 1920;
    public static final int FALLBACK_HEIGHT = 1080;

    private ScreenResolution() {}

    public record ScreenSize(int width, int height) {}

    public static ScreenSize detect() {
        try {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

            if (screen.width <= 0 || screen.height <= 0) {
                LOGGER.warn("AWT returned invalid screen size ({}x{}), using fallback", screen.width, screen.height);
                return fallback();
            }

            LOGGER.info("Detected screen resolution: {}x{}", screen.width, screen.height);
            return new ScreenSize(screen.width, screen.height);

        } catch (HeadlessException e) {
            // Should not happen on a client — headless environment detected
            LOGGER.warn("Headless environment detected, using fallback resolution");
            return fallback();
        }
    }

    private static ScreenSize fallback() {
        LOGGER.info("Using fallback resolution: {}x{}", FALLBACK_WIDTH, FALLBACK_HEIGHT);
        return new ScreenSize(FALLBACK_WIDTH, FALLBACK_HEIGHT);
    }
}