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
 *
 * <p>There are two distinct calling contexts with opposite GLFW-lifecycle needs,
 * so they have separate entry points:
 *
 * <ul>
 *   <li>{@link #detectAtPreLaunch()} runs at the {@code preLaunch} entrypoint,
 *       before Minecraft initializes GLFW. It owns the GLFW lifecycle and must
 *       {@code glfwTerminate()} afterwards (see that method's javadoc).</li>
 *   <li>{@link #detectFromRunningGame()} runs while the game is live and
 *       Minecraft owns GLFW (its window and OpenGL context). It only queries and
 *       must NEVER init or terminate GLFW — terminating would destroy the live
 *       window/context and crash the graphics driver.</li>
 * </ul>
 */
public class ScreenResolution {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ScreenResolution");

    public static final int FALLBACK_WIDTH = 1920;
    public static final int FALLBACK_HEIGHT = 1080;

    private ScreenResolution() {}

    public record ScreenSize(int width, int height) {}

    /**
     * Detects the resolution during pre-launch, before Minecraft initializes GLFW.
     */
    public static ScreenSize detectAtPreLaunch() {
        try {
            if (!GLFW.glfwInit()) {
                LOGGER.warn("GLFW init failed, using fallback resolution");
                return fallback();
            }

            try {
                return queryPrimaryMonitor();
            } finally {
                GLFW.glfwTerminate();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to detect screen resolution: {}", e.getMessage());
            return fallback();
        }
    }

    /**
     * Detects the resolution while the game is running and Minecraft owns GLFW.
     */
    public static ScreenSize detectFromRunningGame() {
        try {
            return queryPrimaryMonitor();
        } catch (Exception e) {
            LOGGER.warn("Failed to detect screen resolution: {}", e.getMessage());
            return fallback();
        }
    }

    private static ScreenSize queryPrimaryMonitor() {
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

        int width = mode.width();
        int height = mode.height();
        LOGGER.info("Detected screen resolution: {}x{}", width, height);
        return new ScreenSize(width, height);
    }

    private static ScreenSize fallback() {
        LOGGER.info("Using fallback resolution: {}x{}", FALLBACK_WIDTH, FALLBACK_HEIGHT);
        return new ScreenSize(FALLBACK_WIDTH, FALLBACK_HEIGHT);
    }
}
