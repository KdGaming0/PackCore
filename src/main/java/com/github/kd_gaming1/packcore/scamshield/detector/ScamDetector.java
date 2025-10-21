package com.github.kd_gaming1.packcore.scamshield.detector;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.context.ConversationContext;
import com.github.kd_gaming1.packcore.scamshield.conversation.ConversationStage;
import com.github.kd_gaming1.packcore.scamshield.detector.cache.MessageAnalysisCache;
import com.github.kd_gaming1.packcore.scamshield.detector.types.*;
import com.github.kd_gaming1.packcore.scamshield.tracker.UserSuspicionTracker;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScamDetector {
    private static final ScamDetector INSTANCE = new ScamDetector();

    private final List<ScamType> scamTypes = new CopyOnWriteArrayList<>();
    private final ConversationContext context = ConversationContext.getInstance();
    private final UserSuspicionTracker suspicionTracker = UserSuspicionTracker.getInstance();

    private final MessageAnalysisCache cache;

    private ScamDetector() {
        initializeScamTypes();
        this.cache = new MessageAnalysisCache(
                PackCoreConfig.scamShieldCacheSize,
                PackCoreConfig.scamShieldCacheTTLSeconds * 1000L
        );
    }

    public static ScamDetector getInstance() {
        return INSTANCE;
    }

    private void initializeScamTypes() {
        scamTypes.add(new JsonBasedScamType("scamtype-discord-verify.json"));
        scamTypes.add(new JsonBasedScamType("scamtype-free-rank.json"));
        scamTypes.add(new JsonBasedScamType("scamtype-island-theft.json"));
        scamTypes.add(new JsonBasedScamType("scamtype-trade-manipulation.json"));
        scamTypes.add(new PhishingLanguageScam());
        scamTypes.add(new CommandInstructionScam());

        PackCore.LOGGER.info("[ScamShield] Initialized {} scam type analyzers", scamTypes.size());
    }

    public DetectionResult analyze(String message, String sender) {
        if (!PackCoreConfig.enableScamShield) {
            return DetectionResult.SAFE;
        }

        String normalizedMessage = normalizeMessage(message);

        // Check cache first
        DetectionResult cached = cache.get(normalizedMessage);
        if (cached != null) {
            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.debug("[ScamShield] Cache hit for message from {}", sender);
            }
            return cached;
        }

        if (PackCoreConfig.enableScamShieldDebugging) {
            PackCore.LOGGER.debug("[ScamShield] Analyzing message from {}: '{}'",
                    sender != null ? sender : "unknown", normalizedMessage);
        }

        long startTime = System.nanoTime();
        int threshold = PackCoreConfig.scamShieldTriggerThreshold;
        DetectionResult.Builder resultBuilder = new DetectionResult.Builder(message, sender, threshold);

        // Get current conversation stage BEFORE analyzing
        ConversationStage currentStage = ConversationStage.INITIAL;
        if (sender != null && !sender.isEmpty()) {
            String senderKey = sender.toLowerCase();
            UserSuspicionTracker.EnhancedConversationHistory history =
                    suspicionTracker.conversations.get(senderKey);

            if (history != null) {
                currentStage = history.getCurrentStage();
            }
        }

        // Set the stage in the result builder
        resultBuilder.setConversationStage(currentStage);

        // Analyze all scam types
        for (ScamType scamType : scamTypes) {
            if (scamType.isEnabled()) {
                scamType.analyze(normalizedMessage, message, sender, context, resultBuilder);
            }
        }

        int currentScore = resultBuilder.getCurrentTotalScore();

        // Record and analyze progression (this will update the stage for NEXT message)
        int progressionBonus = suspicionTracker.recordAndAnalyze(
                sender, message, currentScore, resultBuilder.build()
        );

        if (progressionBonus > 0) {
            resultBuilder.addProgressionBonus(progressionBonus);
        }

        DetectionResult result = resultBuilder.build();
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        if (PackCoreConfig.enableScamShieldDebugging) {
            PackCore.LOGGER.debug("[ScamShield] Detection complete: total={} (type={}, progression={}) | stage={} | threshold={} | triggered={} | took {}ms",
                    result.getTotalScore(), result.getScamTypeScore(),
                    result.getProgressionScore(), currentStage.getDisplayName(),
                    threshold, result.isTriggered(), durationMs);
        }

        cache.put(normalizedMessage, result);

        return result;
    }

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
        }

        int len = result.length();
        if (len > 0 && result.charAt(len - 1) == ' ') {
            result.setLength(len - 1);
        }

        return result.toString();
    }

    public void reloadScamTypes() {
        // Clear cache on reload
        cache.clear();

        for (ScamType scamType : scamTypes) {
            scamType.reload();
        }
        PackCore.LOGGER.info("[ScamShield] ScamTypes reloaded");
    }

    public void shutdown() {
        cache.clear();
        PackCore.LOGGER.info("[ScamShield] ScamDetector shutdown");
    }

    public List<ScamType> getScamTypes() {
        return new ArrayList<>(scamTypes);
    }

    public ConversationContext getContext() {
        return context;
    }

    public UserSuspicionTracker getSuspicionTracker() {
        return suspicionTracker;
    }
}