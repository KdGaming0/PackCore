package com.github.kd_gaming1.packcore.scamshield.detector;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;

import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ScamPattern {
    private final String id;
    private final Pattern regex;
    private final int weight;
    private final ScamCategory category;
    private boolean enabled;
    private PatternStats stats = new PatternStats();

    // Shared executor for all patterns
    private static final ExecutorService REGEX_EXECUTOR = Executors.newFixedThreadPool(
            2,
            r -> {
                Thread t = new Thread(r, "ScamShield-Regex");
                t.setDaemon(true);
                return t;
            }
    );

    public ScamPattern(String id, Pattern regex, int weight, ScamCategory category) {
        this.id = id;
        this.regex = regex;
        this.weight = weight;
        this.category = category;
        this.enabled = true;
    }

    /**
     * Check if this pattern matches the given message with timeout protection
     */
    public boolean matches(String message) {
        if (!enabled) {
            return false;
        }

        long timeoutMs = PackCoreConfig.scamShieldRegexTimeoutMs;


        Future<Boolean> future = REGEX_EXECUTOR.submit(() -> {
            Matcher matcher = regex.matcher(message);
            return matcher.find();
        });

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);

            PackCore.LOGGER.warn("[ScamShield] Pattern '{}' timed out after {}ms - consider removing it",
                    id, timeoutMs);

            return false;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public String getId() {
        return id;
    }

    public Pattern getRegex() {
        return regex;
    }

    public int getWeight() {
        return weight;
    }

    public ScamCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public PatternStats getStats() {
        return stats;
    }

    /**
     * Shutdown the executor - call on mod shutdown
     */
    public static void shutdownExecutor() {
        REGEX_EXECUTOR.shutdown();
        try {
            if (!REGEX_EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                REGEX_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            REGEX_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return String.format("ScamPattern{id='%s', weight=%d, category=%s}",
                id, weight, category);
    }
}