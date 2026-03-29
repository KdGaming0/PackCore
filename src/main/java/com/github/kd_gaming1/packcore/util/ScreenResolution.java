package com.github.kd_gaming1.packcore.util;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects the primary screen resolution using GLFW.
 *
 * <p>AWT ({@code Toolkit.getDefaultToolkit()}) must NOT be used here — on macOS,
 * initializing AWT before GLFW claims the native AppKit event loop and causes a
 * deadlock during window creation on certain macOS versions (e.g. Sonoma 14.x).
 */
public class ScreenResolution {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ScreenResolution");

    public static final int FALLBACK_WIDTH = 1920;
    public static final int FALLBACK_HEIGHT = 1080;

    private ScreenResolution() {}

    public record ScreenSize(int width, int height) {}

    public static ScreenSize detect() {
        try {
            // glfwInit() is idempotent — safe even if Minecraft already called it.
            // We must call it ourselves because during PreLaunch, GLFW may not be
            // initialized yet. Minecraft's later glfwInit() call will simply return true.
            if (!GLFW.glfwInit()) {
                LOGGER.warn("GLFW init failed, using fallback resolution");
                return fallback();
            }

            long monitor = GLFW.glfwGetPrimaryMonitor();
            if (monitor == MemoryUtil.NULL) {
                LOGGER.warn("No primary monitor found, using fallback resolution");
                return fallback();
            }

            GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
            if (mode == null || mode.width() <= 0 || mode.height() <= 0) {
                LOGGER.warn("GLFW returned invalid video mode, using fallback resolution");
                return fallback();
            }

            LOGGER.info("Detected screen resolution: {}x{}", mode.width(), mode.height());
            return new ScreenSize(mode.width(), mode.height());
        } catch (Exception e) {
            LOGGER.warn("Failed to detect screen resolution: {}", e.getMessage());
            return fallback();
        }
    }

    private static ScreenSize fallback() {
        LOGGER.info("Using fallback resolution: {}x{}", FALLBACK_WIDTH, FALLBACK_HEIGHT);
        return new ScreenSize(FALLBACK_WIDTH, FALLBACK_HEIGHT);
    }
}