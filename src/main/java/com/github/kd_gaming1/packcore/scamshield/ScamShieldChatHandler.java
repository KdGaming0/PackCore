package com.github.kd_gaming1.packcore.scamshield;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.detector.ConfidenceLevel;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamDetector;
import com.github.kd_gaming1.packcore.scamshield.storage.DetectedScam;
import com.github.kd_gaming1.packcore.scamshield.storage.ScamShieldDataManager;
import com.github.kd_gaming1.packcore.ui.screen.scamshield.ScamWarningScreen;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.*;

/**
 * Main chat message handler - coordinates scam detection flow.
 * Updated to integrate with ScamWarningScreen UI.
 */
public class ScamShieldChatHandler {
    private static final ScamShieldChatHandler INSTANCE = new ScamShieldChatHandler();

    private final ScamDetector detector;
    private final MinecraftClient client;
    private final ConcurrentLinkedQueue<DetectionResult> recentDetections;
    private final ExecutorService detectionExecutor;

    // Cooldown tracking per confidence level
    private long lastLowConfidenceWarning = 0;
    private long lastMediumConfidenceWarning = 0;
    private long lastHighConfidenceWarning = 0;

    private ScamShieldChatHandler() {
        this.detector = ScamDetector.getInstance();
        this.client = MinecraftClient.getInstance();
        this.recentDetections = new ConcurrentLinkedQueue<>();

        this.detectionExecutor = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                r -> {
                    Thread t = new Thread(r, "ScamShield-Detector");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public static ScamShieldChatHandler getInstance() {
        return INSTANCE;
    }

    /**
     * Main entry point for processing chat messages.
     */
    public void processChatMessage(String message, String sender) {
        if (!PackCoreConfig.enableScamShield || message == null || message.isEmpty()) {
            return;
        }

        // Skip whitelisted players
        if (ScamShieldWhitelist.getInstance().isWhitelisted(sender)) {
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield] Skipping whitelisted player: {}", sender);
            }
            return;
        }

        // Submit to background thread
        detectionExecutor.execute(() -> {
            try {
                DetectionResult result = detector.analyze(message, sender);

                if (result.isTriggered()) {
                    handleDetection(result);
                }
            } catch (Exception e) {
                PackCore.LOGGER.error("[ScamShield] Detection failed", e);
            }
        });
    }

    /**
     * Handle a triggered scam detection with tiered warnings.
     */
    private void handleDetection(DetectionResult result) {
        long now = System.currentTimeMillis();
        ConfidenceLevel level = result.getConfidenceLevel();

        // Check confidence-specific cooldown
        if (!shouldShowWarning(level, now)) {
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield] {} confidence cooldown active", level);
            }
            recentDetections.offer(result);
            return;
        }

        // Update cooldown timestamp
        updateCooldown(level, now);
        recentDetections.offer(result);

        // Trim history
        while (recentDetections.size() > PackCoreConfig.scamShieldMaxRecentDetections) {
            recentDetections.poll();
        }

        // Log with appropriate severity
        logDetection(result);

        // Save to persistent history
        DetectedScam detectedScam = DetectedScam.fromResult(result);
        ScamShieldDataManager.getInstance().saveDetectionAsync(detectedScam);

        // Show warning based on confidence level
        if (PackCoreConfig.scamShieldShowNotifications) {
            client.execute(() -> showWarning(result));
        }
    }

    /**
     * Check if warning should be shown based on confidence-specific cooldown.
     * Cooldown strategy:
     * - LOW: 30 seconds (don't spam for uncertain detections)
     * - MEDIUM: 20 seconds (more urgent)
     * - HIGH: 10 seconds (always show critical warnings)
     */
    private boolean shouldShowWarning(ConfidenceLevel level, long now) {
        long cooldownMs;
        long lastWarning = switch (level) {
            case LOW -> {
                cooldownMs = 30_000; // 30 seconds
                yield lastLowConfidenceWarning;
            }
            case MEDIUM -> {
                cooldownMs = 20_000; // 20 seconds
                yield lastMediumConfidenceWarning;
            }
            case HIGH -> {
                cooldownMs = 10_000; // 10 seconds
                yield lastHighConfidenceWarning;
            }
            default -> {
                cooldownMs = 30_000;
                yield lastLowConfidenceWarning;
            }
        };

        return (now - lastWarning) >= cooldownMs;
    }

    /**
     * Update the appropriate cooldown timestamp.
     */
    private void updateCooldown(ConfidenceLevel level, long timestamp) {
        switch (level) {
            case LOW:
                lastLowConfidenceWarning = timestamp;
                break;
            case MEDIUM:
                lastMediumConfidenceWarning = timestamp;
                break;
            case HIGH:
                lastHighConfidenceWarning = timestamp;
                break;
        }
    }

    /**
     * Log detection with appropriate severity level.
     */
    private void logDetection(DetectionResult result) {
        ConfidenceLevel level = result.getConfidenceLevel();
        String logMessage = String.format(
                "[ScamShield] %s confidence detection | Score: %d | Category: %s | Sender: %s",
                level.getDisplayName(),
                result.getTotalScore(),
                result.getPrimaryCategory().getDisplayName(),
                result.getSender()
        );

        switch (level) {
            case LOW:
                PackCore.LOGGER.info(logMessage);
                break;
            case MEDIUM:
                PackCore.LOGGER.warn(logMessage);
                break;
            case HIGH:
                PackCore.LOGGER.error(logMessage);
                break;
        }
    }

    /**
     * Display appropriate warning based on confidence level.
     */
    private void showWarning(DetectionResult result) {
        ConfidenceLevel level = result.getConfidenceLevel();

        // Send chat message (always)
        sendChatWarning(result);

        // Force open screen for HIGH confidence
        if (level.shouldForceScreen()) {
            openWarningScreen(result);
        }
    }

    /**
     * Send formatted chat warning message.
     */
    private void sendChatWarning(DetectionResult result) {
        if (client.player != null) {
            client.player.sendMessage(
                    ScamWarningMessageBuilder.buildWarningMessage(result),
                    false
            );
        }
    }

    /**
     * Force open warning screen (HIGH confidence only).
     * This blocks user interaction until they acknowledge the warning.
     */
    private void openWarningScreen(DetectionResult result) {
        // Don't interrupt if already in a screen (could be previous warning)
        if (client.currentScreen != null) {
            PackCore.LOGGER.info("[ScamShield] User already in screen, skipping forced screen open");
            return;
        }

        try {
            // Convert detection result to warning format
            ScamWarningScreen.ScamWarning warning = ScamShieldScreenIntegration.convertToWarning(result);

            // Create the warning screen with callback
            ScamWarningScreen warningScreen = new ScamWarningScreen(warning, () -> {
                // Callback when user dismisses the warning
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield] Warning screen dismissed for: {}", result.getSender());
                }
            });

            // Open the screen
            client.setScreen(warningScreen);

            PackCore.LOGGER.warn("[ScamShield] Opened critical warning screen for: {}", result.getSender());

        } catch (Exception e) {
            PackCore.LOGGER.error("[ScamShield] Failed to open warning screen", e);

            // Fallback: Send urgent chat message
            if (client.player != null) {
                client.player.sendMessage(
                        net.minecraft.text.Text.literal(
                                "§c§l[!] CRITICAL SCAM ALERT: " +
                                        "Do NOT interact with " + result.getSender() + "! " +
                                        "Type /scamshield education to learn more.§r"
                        ),
                        false
                );
            }
        }
    }

    public DetectionResult[] getRecentDetections() {
        return recentDetections.toArray(new DetectionResult[0]);
    }

    public void clearHistory() {
        recentDetections.clear();
    }

    public void shutdown() {
        detectionExecutor.shutdown();
        try {
            if (!detectionExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                detectionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            detectionExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public ScamDetector getDetector() {
        return detector;
    }
}