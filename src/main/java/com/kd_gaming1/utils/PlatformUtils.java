package com.kd_gaming1.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for platform-specific operations and detection.
 * Provides cross-platform compatibility methods for window management and OS-specific features.
 */
public class PlatformUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformUtils.class);
    
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_MACOS = OS_NAME.contains("mac") || OS_NAME.contains("darwin");
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_LINUX = OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");
    
    /**
     * Check if the current platform is macOS
     */
    public static boolean isMacOS() {
        return IS_MACOS;
    }
    
    /**
     * Check if the current platform is Windows
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }
    
    /**
     * Check if the current platform is Linux
     */
    public static boolean isLinux() {
        return IS_LINUX;
    }
    
    /**
     * Get the current operating system name
     */
    public static String getOSName() {
        return OS_NAME;
    }
    
    /**
     * Log platform information for debugging
     */
    public static void logPlatformInfo() {
        LOGGER.info("Platform detected - OS: {}, macOS: {}, Windows: {}, Linux: {}", 
                   OS_NAME, IS_MACOS, IS_WINDOWS, IS_LINUX);
        LOGGER.debug("Java Version: {}", System.getProperty("java.version"));
        LOGGER.debug("Java Vendor: {}", System.getProperty("java.vendor"));
    }
}