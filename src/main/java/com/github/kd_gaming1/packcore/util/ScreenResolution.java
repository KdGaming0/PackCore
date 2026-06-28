package com.github.kd_gaming1.packcore.util;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects the primary screen resolution using GLFW during pre-launch.
 *
 * <p>AWT ({@code Toolkit.getDefaultToolkit()}) must NOT be used here — on macOS,
 * initializing AWT before GLFW claims the native AppKit event loop and causes a
 * deadlock during window creation on certain macOS versions (e.g. Sonoma 14.x).
 *
 * <p>This runs at the {@code preLaunch} entrypoint, before Minecraft initializes
 * GLFW. We must therefore {@code glfwTerminate()} again afterwards: GLFW init
 * hints (e.g. {@code GLFW_PLATFORM}, which Minecraft's {@code GLX._initGlfw}
 * sets to {@code GLFW_PLATFORM_X11} to force XWayland over native Wayland) only
 * apply at the next {@code glfwInit()}, and a second {@code glfwInit()} on an
 * already-initialized library is a silent no-op. Leaving GLFW initialized here
 * would make Minecraft's hints be ignored and run the game on native Wayland,
 * which breaks cursor/content-scale mapping under fractional display scaling.
 */
public class ScreenResolution {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ScreenResolution");

    public static final int FALLBACK_WIDTH = 1920;
    public static final int FALLBACK_HEIGHT = 1080;

    private ScreenResolution() {}

    public record ScreenSize(int width, int height) {}

    public static ScreenSize detect() {
        try {
            if (!GLFW.glfwInit()) {
                LOGGER.warn("GLFW init failed, using fallback resolution");
                return fallback();
            }

            // We initialized GLFW; always terminate so Minecraft re-initializes it
            // with its own platform init hints (see class javadoc).
            try {
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

                // Copy the native struct fields into locals before terminate frees them.
                int width = mode.width();
                int height = mode.height();
                LOGGER.info("Detected screen resolution: {}x{}", width, height);
                return new ScreenSize(width, height);
            } finally {
                GLFW.glfwTerminate();
            }
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