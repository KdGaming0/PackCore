package com.github.kd_gaming1.packcore.scamshield.detector;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.storage.PatternInitializer;
import com.github.kd_gaming1.packcore.scamshield.storage.ScamShieldDataManager;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main detection engine that analyzes chat messages for scam patterns.
 */
public class ScamDetector {
    private static final ScamDetector INSTANCE = new ScamDetector();

    private final List<ScamPattern> patterns = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService statsAutoSaver;

    private ScamDetector() {
        // Initialize pattern files (copy defaults if needed)
        PatternInitializer.initializePatterns();
        // Load patterns from file once at startup
        loadPatternsFromFile();
        // Load stats from file once at startup
        loadStats();
        // Start auto-saving stats to disk
        startAutoSave();
    }

    public static ScamDetector getInstance() {
        return INSTANCE;
    }

    /**
     * Load patterns from the patterns.json file.
     */
    private void loadPatternsFromFile() {
        List<ScamPattern> loadedPatterns = ScamShieldDataManager.getInstance().loadPatterns();

        patterns.clear();
        patterns.addAll(loadedPatterns);

        PackCore.LOGGER.info("[ScamShield] Loaded {} patterns from file", patterns.size());
    }

    /**
     * Analyze a chat message for scam patterns.
     */
    public DetectionResult analyze(String message, String sender) {
        if (!PackCoreConfig.enableScamShield) {
            return DetectionResult.SAFE;
        }

        String normalizedMessage = normalizeMessage(message);

        if (PackCoreConfig.enableScamShieldDebugging) {
            PackCore.LOGGER.debug("[ScamShield] Analyzing message from {}: '{}'",
                    sender != null ? sender : "unknown", normalizedMessage);
        }

        List<MatchedPattern> matches = new ArrayList<>();
        int totalScore = 0;

        // PERFORMANCE TRACKING: Measure how long detection takes
        long startTime = System.nanoTime();

        for (ScamPattern pattern : patterns) {
            if (pattern.matches(normalizedMessage)) {
                matches.add(new MatchedPattern(pattern));
                totalScore += pattern.getWeight();

                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.debug("[ScamShield]   ✓ Matched: {} (category: {}, weight: {}, running total: {})",
                            pattern.getId(),
                            pattern.getCategory().getDisplayName(),
                            pattern.getWeight(),
                            totalScore);
                }
            }
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        int threshold = PackCoreConfig.scamShieldTriggerThreshold;
        boolean triggered = totalScore >= threshold;

        if (PackCoreConfig.enableScamShieldDebugging && !matches.isEmpty()) {
            PackCore.LOGGER.debug("[ScamShield] Detection complete: score={} / threshold={} | triggered={} | took {}ms | matched {} patterns",
                    totalScore, threshold, triggered, durationMs, matches.size());
        }

        return new DetectionResult(triggered, totalScore, matches, message, sender);
    }

    /**
     * Normalize message for consistent pattern matching.
     */
    private String normalizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        StringBuilder normalized = new StringBuilder(message.length());
        char prevChar = '\0';

        for (char c : message.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                normalized.append(c);
                prevChar = c;
            } else if (Character.isWhitespace(c) && prevChar != ' ') {
                normalized.append(' ');
                prevChar = ' ';
            }
        }

        // Remove trailing space if present
        int len = normalized.length();
        if (len > 0 && normalized.charAt(len - 1) == ' ') {
            normalized.setLength(len - 1);
        }

        return normalized.toString();
    }

    /**
     * Auto-save statistics every 5 minutes.
     */
    private void startAutoSave() {
        statsAutoSaver = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ScamShield-StatsSaver");
            t.setDaemon(true);
            return t;
        });

        // Save every 5 minutes
        statsAutoSaver.scheduleAtFixedRate(
                this::saveStats,
                5,
                5,
                TimeUnit.MINUTES
        );

        PackCore.LOGGER.info("[ScamShield] Auto-save enabled (every 5 minutes)");
    }


    /**
     * Load statistics for all patterns from disk.
     */
    private void loadStats() {
        Map<String, PatternStats.SerializedStats> savedStats =
                ScamShieldDataManager.getInstance().loadStats();

        for (ScamPattern pattern : patterns) {
            PatternStats.SerializedStats saved = savedStats.get(pattern.getId());
            if (saved != null) {
                pattern.getStats().fromSerialized(saved);
            }
        }

        PackCore.LOGGER.info("[ScamShield] Loaded statistics for {} patterns", savedStats.size());
    }

    /**
     * Save current statistics to disk.
     */
    public void saveStats() {
        Map<String, PatternStats> stats = new HashMap<>();
        for (ScamPattern pattern : patterns) {
            stats.put(pattern.getId(), pattern.getStats());
        }

        ScamShieldDataManager.getInstance().saveStatsAsync(stats);
    }

    /**
     * Reload patterns manually (e.g., via command or restart).
     */
    public void reloadPatterns() {
        saveStats();
        loadPatternsFromFile();
        loadStats();
        PackCore.LOGGER.info("[ScamShield] Patterns manually reloaded");
    }

    /**
     * Called on shutdown (kept for consistency, no longer used for file watching).
     */
    public void shutdown() {
        if (statsAutoSaver != null) {
            statsAutoSaver.shutdown();
            try {
                if (!statsAutoSaver.awaitTermination(2, TimeUnit.SECONDS)) {
                    statsAutoSaver.shutdownNow();
                }
            } catch (InterruptedException e) {
                statsAutoSaver.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        saveStats();
        ScamPattern.shutdownExecutor();
        PackCore.LOGGER.info("[ScamShield] ScamDetector shutdown");
    }

    public List<ScamPattern> getPatterns() {
        return new ArrayList<>(patterns);
    }

    public Map<String, PatternStats> getPatternStats() {
        Map<String, PatternStats> stats = new HashMap<>();
        for (ScamPattern pattern : patterns) {
            stats.put(pattern.getId(), pattern.getStats());
        }
        return stats;
    }

    public void addPattern(ScamPattern pattern) {
        patterns.add(pattern);
        ScamShieldDataManager.getInstance().savePatternsAsync(patterns);
    }

    public void removePattern(String patternId) {
        patterns.removeIf(p -> p.getId().equals(patternId));
        ScamShieldDataManager.getInstance().savePatternsAsync(patterns);
    }

    public void clearPatterns() {
        patterns.clear();
    }
}
