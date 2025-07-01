package com.kd_gaming1.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;

/**
 * Utility class for macOS-specific window management and system integration.
 * Provides methods to ensure proper window focus, system notifications, and dock integration.
 */
public class MacOSWindowUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MacOSWindowUtils.class);
    
    // macOS-specific timeout for window operations
    private static final int MACOS_WINDOW_TIMEOUT_MS = 2000;
    
    /**
     * Configure a window for optimal macOS behavior
     */
    public static void configureMacOSWindow(Window window) {
        if (!PlatformUtils.isMacOS()) {
            return;
        }
        
        try {
            LOGGER.info("Configuring window for macOS compatibility");
            
            // Set always on top for macOS to ensure visibility
            if (window instanceof JFrame) {
                JFrame frame = (JFrame) window;
                frame.setAlwaysOnTop(true);
                LOGGER.debug("Set window to always on top for macOS");
            }
            
            // Try to bring window to front using macOS-specific methods
            bringWindowToFront(window);
            
            // Request focus and ensure window is visible
            window.requestFocus();
            window.toFront();
            
            LOGGER.info("macOS window configuration completed");
            
        } catch (Exception e) {
            LOGGER.warn("Failed to configure macOS-specific window properties: {}", e.getMessage());
        }
    }
    
    /**
     * Bring window to front using macOS-specific techniques
     */
    private static void bringWindowToFront(Window window) {
        try {
            // Try to use macOS-specific Application class if available
            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            Method getApplicationMethod = applicationClass.getMethod("getApplication");
            Object application = getApplicationMethod.invoke(null);
            
            Method requestForegroundMethod = applicationClass.getMethod("requestForeground", boolean.class);
            requestForegroundMethod.invoke(application, true);
            
            LOGGER.debug("Successfully used macOS Application.requestForeground()");
            
        } catch (Exception e) {
            LOGGER.debug("macOS Application class not available, using standard methods: {}", e.getMessage());
            
            // Fallback to standard Java methods
            window.setVisible(true);
            window.toFront();
            window.requestFocus();
        }
    }
    
    /**
     * Add keyboard shortcuts for common actions (macOS uses Cmd instead of Ctrl)
     */
    public static void addMacOSKeyboardShortcuts(JFrame frame, Runnable onExtract, Runnable onSkip) {
        if (!PlatformUtils.isMacOS()) {
            return;
        }
        
        JRootPane rootPane = frame.getRootPane();
        
        // Add Cmd+Enter for extract
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK), "extract");
        rootPane.getActionMap().put("extract", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LOGGER.debug("Extract action triggered via Cmd+Enter shortcut");
                onExtract.run();
            }
        });
        
        // Add Cmd+S for skip
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.META_DOWN_MASK), "skip");
        rootPane.getActionMap().put("skip", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LOGGER.debug("Skip action triggered via Cmd+S shortcut");
                onSkip.run();
            }
        });
        
        // Add Escape key for skip
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        rootPane.getActionMap().put("escape", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LOGGER.debug("Skip action triggered via Escape key");
                onSkip.run();
            }
        });
        
        LOGGER.info("Added macOS keyboard shortcuts (Cmd+Enter: Extract, Cmd+S/Escape: Skip)");
    }
    
    /**
     * Show system notification on macOS if supported
     */
    public static void showSystemNotification(String title, String message) {
        if (!PlatformUtils.isMacOS()) {
            return;
        }
        
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                
                // Create a simple tray icon (1x1 transparent image)
                Image image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                TrayIcon trayIcon = new TrayIcon(image);
                trayIcon.setImageAutoSize(true);
                
                tray.add(trayIcon);
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                
                // Remove tray icon after showing notification
                Timer timer = new Timer(3000, e -> tray.remove(trayIcon));
                timer.setRepeats(false);
                timer.start();
                
                LOGGER.info("Displayed macOS system notification: {} - {}", title, message);
            } else {
                LOGGER.debug("System tray not supported on this macOS version");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to show macOS system notification: {}", e.getMessage());
        }
    }
    
    /**
     * Request dock badge or attention (macOS specific)
     */
    public static void requestDockAttention() {
        if (!PlatformUtils.isMacOS()) {
            return;
        }
        
        try {
            // Try to use macOS-specific Application class for dock badge
            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            Method getApplicationMethod = applicationClass.getMethod("getApplication");
            Object application = getApplicationMethod.invoke(null);
            
            Method requestUserAttentionMethod = applicationClass.getMethod("requestUserAttention", boolean.class);
            requestUserAttentionMethod.invoke(application, true);
            
            LOGGER.info("Requested dock attention on macOS");
            
        } catch (Exception e) {
            LOGGER.debug("Failed to request dock attention: {}", e.getMessage());
        }
    }
    
    /**
     * Create a timeout mechanism for macOS dialog visibility
     */
    public static Timer createVisibilityTimeout(Window window, Runnable onTimeout) {
        if (!PlatformUtils.isMacOS()) {
            return null;
        }
        
        Timer timer = new Timer(MACOS_WINDOW_TIMEOUT_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!window.isVisible() || !window.isFocused()) {
                    LOGGER.warn("Window may be hidden on macOS after {}ms, triggering fallback", MACOS_WINDOW_TIMEOUT_MS);
                    onTimeout.run();
                }
            }
        });
        
        timer.setRepeats(false);
        return timer;
    }
}