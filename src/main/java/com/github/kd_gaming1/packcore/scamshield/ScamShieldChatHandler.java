package com.github.kd_gaming1.packcore.scamshield;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamDetector;

import com.github.kd_gaming1.packcore.scamshield.storage.DetectedScam;
import com.github.kd_gaming1.packcore.scamshield.storage.ScamShieldDataManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.concurrent.*;

/**
 * Handles incoming chat messages and coordinates scam detection
 */
public class ScamShieldChatHandler {
    private static final ScamShieldChatHandler INSTANCE = new ScamShieldChatHandler();

    private final ScamDetector detector;
    private final MinecraftClient client;
    private final ConcurrentLinkedQueue<DetectionResult> recentDetections;

    private final ExecutorService detectionExecutor;

    // Rate limiting to prevent spam
    private static final long COOLDOWN_MS = TimeUnit.SECONDS.toMillis(5);
    private long lastWarningTime = 0;

    private boolean enabled = true;

    private ScamShieldChatHandler() {
        this.detector = ScamDetector.getInstance();
        this.client = MinecraftClient.getInstance();
        this.recentDetections = new ConcurrentLinkedQueue<>();
        this.detectionExecutor = new ThreadPoolExecutor(
                1,
                2,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }

    public static ScamShieldChatHandler getInstance() {
        return INSTANCE;
    }

    /**
     * Process an incoming chat message
     * @param message The raw message text
     * @param sender The sender's name (can be null)
     */
    public void processChatMessage(String message, String sender) {
        if (!PackCoreConfig.enableScamShield || message == null || message.isEmpty()) {
            return;
        }

        if (ScamShieldWhitelist.getInstance().isWhitelisted(sender)) {
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.debug("[Scam Shield] Skipping analysis for whitelisted player: {}", sender);
            }
            return;
        }

        // Submit to thread pool
        detectionExecutor.execute(() -> {
            try {
                DetectionResult result = detector.analyze(message, sender);
                if (result.isTriggered()) {
                    handleDetection(result);
                }
            } catch (Exception e) {
                PackCore.LOGGER.error("[Scam Shield] Error during scam detection", e);
            }
        });
    }

    /**
     * Handle a triggered scam detection
     */
    private void handleDetection(DetectionResult result) {
        long currentTime = System.currentTimeMillis();

        long cooldownMs = TimeUnit.SECONDS.toMillis(PackCoreConfig.scamShieldNotificationCooldownSeconds);

        // Check cooldown to prevent spam
        if (currentTime - lastWarningTime < cooldownMs) {
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.debug("[Scam Shield] Scam detected but cooldown active ({} ms remaining)",
                        cooldownMs - (currentTime - lastWarningTime));
            }
            recentDetections.offer(result);
            return;
        }

        lastWarningTime = currentTime;
        recentDetections.offer(result);

        // Limit queue size
        int maxRecent = PackCoreConfig.scamShieldMaxRecentDetections;
        while (recentDetections.size() > maxRecent) {
            recentDetections.poll();
        }

        // Log detection
        PackCore.LOGGER.warn("[Scam Shield] Potential scam detected! Score: {}, Category: {}, Sender: {}",
                result.getTotalScore(),
                result.getPrimaryCategory().getDisplayName(),
                result.getSender());

        DetectedScam detectedScam = DetectedScam.fromResult(result);
        ScamShieldDataManager.getInstance().saveDetectionAsync(detectedScam);

        if (PackCoreConfig.scamShieldShowNotifications) {
            client.execute(() -> showWarningScreen(result));
        } else if (PackCoreConfig.enableScamShieldDebugging) {
            PackCore.LOGGER.debug("[Scam Shield] Notification suppressed by config");
        }
    }

    /**
     * Display the warning screen to the user
     */
    private void showWarningScreen(DetectionResult result) {
        if (client.currentScreen != null) {
            // Don't interrupt if user is already in a screen
            PackCore.LOGGER.info("[Scam Shield] Scam detected but user is in a screen, sending chat notification");
            sendChatNotification(result);
            return;
        }

        try {
            // TODO: Replace with your actual warning screen class
            // client.setScreen(new ScamWarningScreen(result));

            // Temporary: Send chat notification until screen is implemented
            sendChatNotification(result);
        } catch (Exception e) {
            PackCore.LOGGER.error("[Scam Shield] Failed to show warning screen", e);
            sendChatNotification(result);
        }
    }

    /**
     * Send a chat notification as fallback
     */
    private void sendChatNotification(DetectionResult result) {
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("§c§l[SCAM WARNING] §r§7Potential scam detected! Category: §e"
                            + result.getPrimaryCategory().getDisplayName()
                            + " §7(Score: " + result.getTotalScore() + ")"),
                    false
            );
        }
    }

    /**
     * Get recent detections for display in UI
     */
    public DetectionResult[] getRecentDetections() {
        return recentDetections.toArray(new DetectionResult[0]);
    }

    /**
     * Clear recent detections history
     */
    public void clearHistory() {
        recentDetections.clear();
    }

    /**
     * Shutdown the executor gracefully - call this on client shutdown
     */
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

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        PackCore.LOGGER.info("ScamShield {}", enabled ? "enabled" : "disabled");
    }

    public ScamDetector getDetector() {
        return detector;
    }
}