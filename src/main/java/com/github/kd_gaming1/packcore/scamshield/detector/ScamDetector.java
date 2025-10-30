package com.github.kd_gaming1.packcore.scamshield.detector;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.context.ConversationContext;
import com.github.kd_gaming1.packcore.scamshield.conversation.ConversationStage;
import com.github.kd_gaming1.packcore.scamshield.detector.cache.MessageAnalysisCache;
import com.github.kd_gaming1.packcore.scamshield.detector.types.*;
import com.github.kd_gaming1.packcore.scamshield.tracker.UserSuspicionTracker;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Core detection engine - coordinates all scam type analyzers.
 *
 * NOW WITH AUTO-DISCOVERY: Automatically finds and loads all scamtype-*.json files!
 *
 * Pipeline:
 * 1. Normalize message
 * 2. Check cache
 * 3. Run all ScamType analyzers
 * 4. Track conversation history
 * 5. Apply stage-based threshold
 * 6. Return result
 */
public class ScamDetector {
    private static final ScamDetector INSTANCE = new ScamDetector();

    private final List<ScamType> scamTypes = new CopyOnWriteArrayList<>();
    private final ConversationContext context = ConversationContext.getInstance();
    private final UserSuspicionTracker suspicionTracker = UserSuspicionTracker.getInstance();
    private final MessageAnalysisCache cache;
    private final Path scamShieldDir;
    private final Map<String, MessageRepetition> recentMessages = new ConcurrentHashMap<>();

    private ScamDetector() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        this.scamShieldDir = gameDir.resolve("packcore/scamshield");

        this.cache = new MessageAnalysisCache(
                PackCoreConfig.scamShieldCacheSize,
                PackCoreConfig.scamShieldCacheTTLSeconds * 1000L
        );

        initializeScamTypes();
    }

    private static class MessageRepetition {
        String normalizedMessage;
        int count;
        long firstSeen;
        boolean markedAsLegit;

        MessageRepetition(String msg) {
            this.normalizedMessage = msg;
            this.count = 1;
            this.firstSeen = System.currentTimeMillis();
            this.markedAsLegit = false;
        }
    }

    public static ScamDetector getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize all scam detection modules.
     *
     * This method now:
     * 1. Loads hard-coded specialized detectors
     * 2. AUTO-DISCOVERS all scamtype-*.json files in the scamshield directory
     * 3. Loads each discovered JSON file as a JsonBasedScamType
     */
    private void initializeScamTypes() {
        scamTypes.clear();

        // Hard-coded specialized detectors that aren't JSON-based
        scamTypes.add(new PhishingLanguageScam());
        scamTypes.add(new CommandInstructionScam());

        // AUTO-DISCOVER JSON-based scam types
        loadJsonScamTypes();

        PackCore.LOGGER.info("[ScamShield] Loaded {} total scam detectors ({} JSON-based)",
                scamTypes.size(), scamTypes.size() - 2);
    }

    /**
     * Automatically discover and load all scamtype-*.json files.
     *
     * Scans the scamshield directory for files matching pattern: scamtype-*.json
     * Each file is loaded as a JsonBasedScamType detector.
     */
    private void loadJsonScamTypes() {
        try {
            // Ensure directory exists
            if (!Files.exists(scamShieldDir)) {
                Files.createDirectories(scamShieldDir);
                PackCore.LOGGER.info("[ScamShield] Created scamshield directory: {}", scamShieldDir);
            }

            // Find all scamtype-*.json files
            List<String> discoveredFiles = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                    scamShieldDir,
                    "scamtype-*.json")) {

                for (Path file : stream) {
                    String fileName = file.getFileName().toString();
                    discoveredFiles.add(fileName);
                }
            }

            // Sort for consistent loading order
            Collections.sort(discoveredFiles);

            // Load each discovered file
            if (discoveredFiles.isEmpty()) {
                PackCore.LOGGER.warn("[ScamShield] No scamtype-*.json files found in {}", scamShieldDir);
                PackCore.LOGGER.warn("[ScamShield] Creating default scam type files...");
                createDefaultScamTypes();

                // Try again after creating defaults
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                        scamShieldDir,
                        "scamtype-*.json")) {

                    for (Path file : stream) {
                        String fileName = file.getFileName().toString();
                        discoveredFiles.add(fileName);
                    }
                }
                Collections.sort(discoveredFiles);
            }

            // Load all discovered scam types
            for (String fileName : discoveredFiles) {
                try {
                    JsonBasedScamType scamType = new JsonBasedScamType(fileName);
                    scamTypes.add(scamType);

                    PackCore.LOGGER.info("[ScamShield] ✓ Loaded: {} ({})",
                            scamType.getDisplayName(), fileName);

                } catch (Exception e) {
                    PackCore.LOGGER.error("[ScamShield] ✗ Failed to load: {}", fileName, e);
                }
            }

            if (discoveredFiles.isEmpty()) {
                PackCore.LOGGER.error("[ScamShield] WARNING: No scam type files loaded! Detection will be limited.");
            }

        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to scan for scam type files", e);
        }
    }

    /**
     * Create default scam type files if none exist.
     * This ensures the mod works out of the box.
     */
    private void createDefaultScamTypes() {
        String[] defaultFiles = {
                "scamtype-discord-verify.json",
                "scamtype-free-rank.json",
                "scamtype-island-theft.json",
                "scamtype-trade-manipulation.json"
        };

        for (String fileName : defaultFiles) {
            try {
                // JsonBasedScamType constructor handles copying from resources
                new JsonBasedScamType(fileName);
                PackCore.LOGGER.info("[ScamShield] Created default file: {}", fileName);
            } catch (Exception e) {
                PackCore.LOGGER.error("[ScamShield] Failed to create default file: {}", fileName, e);
            }
        }
    }

    /**
     * Main analysis method - checks if a message is a scam.
     *
     * @param message Raw message text
     * @param sender Username of sender
     * @return DetectionResult with score and triggered flag
     */
    public DetectionResult analyze(String message, String sender) {
        // Bail out if disabled
        if (!PackCoreConfig.enableScamShield) {
            return DetectionResult.SAFE;
        }

        String normalizedMessage = normalizeMessage(message);

        String senderKey = sender != null ? sender.toLowerCase() : "";
        MessageRepetition rep = recentMessages.computeIfAbsent(senderKey, k -> new MessageRepetition(normalizedMessage));

        if (rep.normalizedMessage.equals(normalizedMessage)) {
            rep.count++;

            // If same message repeated 3+ times AND it's a trade ad, mark as legitimate
            if (rep.count >= 3 && LegitimateTradeContext.isLegitimateTradeAd(normalizedMessage)) {
                rep.markedAsLegit = true;
            }

            // If marked as legit due to repetition, return SAFE
            if (rep.markedAsLegit) {
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield] Repetition-whitelisted: {} (repeated {}x)", sender, rep.count);
                }
                return DetectionResult.SAFE;
            }
        } else {
            // Different message, reset
            recentMessages.put(senderKey, new MessageRepetition(normalizedMessage));
        }

        // Check cache first - avoid re-analyzing identical messages
        DetectionResult cached = cache.get(normalizedMessage);
        if (cached != null) {
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield] Cache hit for: {}", sender);
            }
            return cached;
        }

        if (PackCoreConfig.enableScamShieldDebugging) {
            PackCore.LOGGER.info("[ScamShield] Analyzing: [{}] '{}'", sender, normalizedMessage);
        }

        long startTime = System.nanoTime();

        // Get current conversation stage BEFORE analyzing this message
        ConversationStage currentStage = getCurrentStage(sender);

        // Build result with initial threshold
        int threshold = PackCoreConfig.scamShieldTriggerThreshold;
        DetectionResult.Builder resultBuilder = new DetectionResult.Builder(message, sender, threshold);
        resultBuilder.setConversationStage(currentStage);

        // CHECK FOR LEGITIMATE TRADE CONTEXT
        double legitimacyMultiplier = LegitimateTradeContext.getScoreMultiplier(normalizedMessage);
        if (legitimacyMultiplier < 1.0 && PackCoreConfig.enableScamShieldDebugging) {
            PackCore.LOGGER.info("[ScamShield]   Legitimate trade context detected, applying {}x multiplier",
                    legitimacyMultiplier);
        }

        // Run all scam type analyzers
        for (ScamType scamType : scamTypes) {
            if (scamType.isEnabled()) {
                scamType.analyze(normalizedMessage, message, sender, context, resultBuilder);
            }
        }

        if (legitimacyMultiplier < 1.0) {
            resultBuilder.applyMultiplier(legitimacyMultiplier);
        }

        int currentScore = resultBuilder.getCurrentTotalScore();

        // Analyze conversation progression (multi-message patterns)
        // This updates the stage for the NEXT message
        int progressionBonus = suspicionTracker.recordAndAnalyze(
                sender, message, currentScore, resultBuilder.build()
        );

        if (progressionBonus > 0) {
            resultBuilder.addProgressionBonus(progressionBonus);
        }

        // Build final result (applies stage-adjusted threshold)
        DetectionResult result = resultBuilder.build();

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        if (PackCoreConfig.enableScamShieldDebugging) {
            PackCore.LOGGER.info(
                    "[ScamShield] Analysis complete: total={} (type={}, prog={}) | stage={} | threshold={} | triggered={} | {}ms",
                    result.getTotalScore(), result.getScamTypeScore(), result.getProgressionScore(),
                    currentStage.getDisplayName(), threshold, result.isTriggered(), durationMs
            );
        }

        // Cache the result
        cache.put(normalizedMessage, result);

        long now = System.currentTimeMillis();
        recentMessages.entrySet().removeIf(e -> (now - e.getValue().firstSeen) > 300_000);

        return result;
    }

    /**
     * Get the current conversation stage for a sender.
     * Used to adjust detection threshold.
     */
    private ConversationStage getCurrentStage(String sender) {
        if (sender == null || sender.isEmpty()) {
            return ConversationStage.INITIAL;
        }

        String senderKey = sender.toLowerCase();
        UserSuspicionTracker.EnhancedConversationHistory history =
                suspicionTracker.conversations.get(senderKey);

        return history != null ? history.getCurrentStage() : ConversationStage.INITIAL;
    }

    /**
     * Normalize message for consistent matching.
     * - Lowercase
     * - Remove extra whitespace
     * - Keep alphanumeric and spaces only
     */
    private String normalizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String lower = message.toLowerCase();
        StringBuilder result = new StringBuilder(lower.length());
        boolean lastWasSpace = true;

        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);

            if (Character.isLetterOrDigit(c)) {
                result.append(c);
                lastWasSpace = false;
            } else if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    result.append(' ');
                    lastWasSpace = true;
                }
            }
            // Drop all other characters
        }

        // Trim trailing space
        int len = result.length();
        if (len > 0 && result.charAt(len - 1) == ' ') {
            result.setLength(len - 1);
        }

        return result.toString();
    }

    /**
     * Reload all scam types.
     *
     * This will:
     * 1. Clear the cache
     * 2. Re-scan the directory for scamtype-*.json files
     * 3. Load any new files that were added
     * 4. Reload existing files (picks up edits)
     *
     * @return Number of scam types loaded
     */
    public int reloadScamTypes() {
        PackCore.LOGGER.info("[ScamShield] Reloading all scam types...");

        cache.clear();

        int previousCount = scamTypes.size();

        // Re-initialize from scratch
        initializeScamTypes();

        int newCount = scamTypes.size();

        PackCore.LOGGER.info("[ScamShield] Reload complete! {} scam detectors active (previously: {})",
                newCount, previousCount);

        if (newCount > previousCount) {
            PackCore.LOGGER.info("[ScamShield] {} NEW scam types discovered!",
                    newCount - previousCount);
        } else if (newCount < previousCount) {
            PackCore.LOGGER.info("[ScamShield] {} scam types removed",
                    previousCount - newCount);
        }

        return newCount;
    }

    public void shutdown() {
        cache.clear();
        PackCore.LOGGER.info("[ScamShield] Shutdown complete");
    }

    // Getters
    public List<ScamType> getScamTypes() { return new ArrayList<>(scamTypes); }
    public ConversationContext getContext() { return context; }
    public UserSuspicionTracker getSuspicionTracker() { return suspicionTracker; }
    public Path getScamShieldDirectory() { return scamShieldDir; }
}