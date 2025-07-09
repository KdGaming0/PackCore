package com.kd_gaming1.copysystem;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.FabricLoader;
import com.kd_gaming1.config.PackCoreConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Pre-launch entrypoint that handles configuration extraction before Minecraft starts.
 * This ensures all necessary configs are in place before the game initializes.
 * Enhanced with platform-specific behavior for better user experience.
 */
public class PreLaunchConfigExtractor implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore-PreLaunch");

    @Override
    public void onPreLaunch() {
        LOGGER.info("Starting PackCore pre-launch configuration extraction...");

        try {
            // Initialize MidnightLib config early - this works in pre-launch!
            MidnightConfig.init("packcore", PackCoreConfig.class);

            File minecraftRoot = FabricLoader.getInstance().getGameDir().toFile();
            ConfigExtractionService extractionService = new ConfigExtractionService(minecraftRoot);

            // Check if we need to show the dialog using MidnightLib
            if (!PackCoreConfig.promptSetDefaultConfig) {
                LOGGER.info("Config dialog disabled, skipping user prompt");
                return;
            }

            String osName = System.getProperty("os.name").toLowerCase();
            LOGGER.info("Detected operating system: {}", osName);

            ConfigSelectionResult result = extractionService.selectAndExtractConfig();

            if (result.shouldShowDialog()) {
                LOGGER.info("Multiple configs found");

                if (osName.contains("mac")) {
                    // macOS: Auto-apply 1080p config
                    handleMacOSAutoConfig(extractionService);
                } else {
                    // Other platforms: Show dialog with notification
                    showConfigSelectionDialog(extractionService);
                }
            } else if (result.hasAutoExtractConfig()) {
                LOGGER.info("Auto-extracting single config: {}", result.getConfigName());
                boolean success = extractionService.extractConfig(result.getConfigName(), result.getConfigType());

                if (success) {
                    // Disable the prompt for next time using MidnightLib
                    LOGGER.info("Auto-extraction successful, disabling config prompt for next launch");
                    PackCoreConfig.promptSetDefaultConfig = false;
                    PackCoreConfig.lastConfigApplied = result.getConfigName(); // Store last applied config

                    // Mark that we're no longer on first startup
                    if (PackCoreConfig.isFirstStartup) {
                        PackCoreConfig.isFirstStartup = false;
                        LOGGER.info("Marked first startup as complete");
                    }

                    MidnightConfig.write("packcore"); // Save using MidnightLib

                    try {
                        // Short delay to ensure file write completes
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                LOGGER.info("No configs found, using default settings");
            }

        } catch (Exception e) {
            LOGGER.error("Error during pre-launch config extraction", e);
            // Continue startup even if config extraction fails
        }

        LOGGER.info("Pre-launch configuration extraction completed");
    }

    private void handleMacOSAutoConfig(ConfigExtractionService extractionService) {
        LOGGER.info("Handling macOS auto-configuration");

        try {
            List<ConfigInfo> officialConfigs = extractionService.getOfficialConfigs();

            // Since the file will always contain "1080p", just look for that
            ConfigInfo targetConfig = findConfigContaining1080p(officialConfigs);

            if (targetConfig != null) {
                LOGGER.info("Auto-applying 1080p configuration on macOS: {}", targetConfig.getName());

                boolean success = extractionService.extractConfig(targetConfig.getName(), ConfigType.OFFICIAL);

                if (success) {
                    LOGGER.info("Successfully auto-applied 1080p configuration on macOS");

                    // Disable the prompt for next time
                    PackCoreConfig.promptSetDefaultConfig = false;
                    PackCoreConfig.lastConfigApplied = targetConfig.getName();

                    // Mark that we're no longer on first startup
                    if (PackCoreConfig.isFirstStartup) {
                        PackCoreConfig.isFirstStartup = false;
                        LOGGER.info("Marked first startup as complete");
                    }

                    MidnightConfig.write("packcore");

                    // Show a system notification on macOS
                    showMacOSNotification(targetConfig.getDisplayName());

                } else {
                    LOGGER.error("Failed to auto-apply 1080p configuration on macOS: {}", targetConfig.getName());
                }
            } else {
                LOGGER.warn("1080p configuration not found, falling back to first available config");

                // Fallback to first official config
                if (!officialConfigs.isEmpty()) {
                    ConfigInfo firstConfig = officialConfigs.get(0);
                    LOGGER.info("Auto-applying first available config on macOS: {}", firstConfig.getName());

                    boolean success = extractionService.extractConfig(firstConfig.getName(), ConfigType.OFFICIAL);
                    if (success) {
                        PackCoreConfig.promptSetDefaultConfig = false;
                        PackCoreConfig.lastConfigApplied = firstConfig.getName();

                        // Mark that we're no longer on first startup
                        if (PackCoreConfig.isFirstStartup) {
                            PackCoreConfig.isFirstStartup = false;
                            LOGGER.info("Marked first startup as complete");
                        }

                        MidnightConfig.write("packcore");
                        showMacOSNotification(firstConfig.getDisplayName());
                    } else {
                        LOGGER.error("Failed to auto-apply fallback config on macOS: {}", firstConfig.getName());
                    }
                } else {
                    LOGGER.warn("No official configurations found on macOS");
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error during macOS auto-configuration", e);
        }
    }

    private ConfigInfo findConfigContaining1080p(List<ConfigInfo> configs) {
        // Look for any config containing "1080p" (case-insensitive)
        for (ConfigInfo config : configs) {
            if (config.getName().toLowerCase().contains("1080p")) {
                LOGGER.debug("Found 1080p config: {}", config.getName());
                return config;
            }
        }
        return null;
    }

    private void showMacOSNotification(String configName) {
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();

                // Create tray icon
                TrayIcon trayIcon = new TrayIcon(createPackCoreIcon().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip("PackCore Configuration Applied");

                try {
                    tray.add(trayIcon);
                } catch (AWTException ex) {
                    LOGGER.debug("Could not add tray icon: {}", ex.getMessage());
                }

                trayIcon.displayMessage(
                        "PackCore Configuration Applied",
                        "Applied configuration: " + configName + "\nMinecraft will continue loading.",
                        TrayIcon.MessageType.INFO
                );

                LOGGER.info("macOS system notification displayed");

                // Remove tray icon after a delay
                Timer removeTimer = new Timer(5000, e -> {
                    try {
                        tray.remove(trayIcon);
                    } catch (Exception ex) {
                        // Ignore
                    }
                });
                removeTimer.setRepeats(false);
                removeTimer.start();

            } else {
                LOGGER.debug("System tray not supported on macOS");
            }
        } catch (Exception e) {
            LOGGER.debug("Could not show macOS notification: {}", e.getMessage());
        }
    }

    private void showConfigSelectionDialog(ConfigExtractionService extractionService) {
        try {
            // Show system notification first
            showSystemNotification();

            // Use CompletableFuture to handle the dialog asynchronously but block the main thread
            CompletableFuture<Boolean> dialogResult = CompletableFuture.supplyAsync(() -> {
                ConfigSelectionDialog dialog = new ConfigSelectionDialog(extractionService);
                return dialog.showAndWait();
            });

            // Block until dialog completes or times out
            Boolean success = dialogResult.get(PackCoreConfig.dialogTimeoutMinutes, TimeUnit.MINUTES);

            if (success) {
                LOGGER.info("Config selection completed successfully");
            } else {
                LOGGER.warn("Config selection was cancelled or failed");
            }

        } catch (Exception e) {
            LOGGER.error("Error showing config selection dialog", e);
        }
    }

    private void showSystemNotification() {
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();

                // Create tray icon
                TrayIcon trayIcon = new TrayIcon(createPackCoreIcon().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip("PackCore Configuration Required");

                // Add click listener to bring dialog to front
                trayIcon.addActionListener(e -> {
                    // Try to find and focus any PackCore windows
                    SwingUtilities.invokeLater(() -> {
                        Window[] windows = Window.getWindows();
                        for (Window window : windows) {
                            if (window instanceof JFrame) {
                                JFrame frame = (JFrame) window;
                                if (frame.getTitle().contains("PackCore")) {
                                    frame.setExtendedState(JFrame.NORMAL);
                                    frame.toFront();
                                    frame.requestFocus();
                                    break;
                                }
                            }
                        }
                    });
                });

                try {
                    tray.add(trayIcon);
                } catch (AWTException ex) {
                    LOGGER.debug("Could not add tray icon: {}", ex.getMessage());
                }

                trayIcon.displayMessage(
                        "PackCore Configuration Required",
                        "Minecraft is waiting for your configuration choice. Check for the PackCore dialog window.",
                        TrayIcon.MessageType.WARNING
                );

                LOGGER.info("System notification displayed");

                // Remove tray icon after a delay
                Timer removeTimer = new Timer(10000, e -> {
                    try {
                        tray.remove(trayIcon);
                    } catch (Exception ex) {
                        // Ignore
                    }
                });
                removeTimer.setRepeats(false);
                removeTimer.start();

            } else {
                LOGGER.debug("System tray not supported");
            }
        } catch (Exception e) {
            LOGGER.debug("Could not show system notification: {}", e.getMessage());
        }
    }

    private Image createPackCoreIcon() {
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Create a distinctive PackCore icon
        g2.setColor(new Color(0, 123, 255));
        g2.fillOval(2, 2, 28, 28);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        String text = "PC";
        int x = (32 - fm.stringWidth(text)) / 2;
        int y = (32 + fm.getAscent()) / 2;
        g2.drawString(text, x, y);
        g2.dispose();

        return icon;
    }
}