package com.github.kd_gaming1.packcore.scamshield.tracker;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.conversation.ConversationStage;
import com.github.kd_gaming1.packcore.scamshield.conversation.SequencePatternDetector;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tracks conversation history per user to detect multi-message scam patterns.
 *
 * Key features:
 * - Stores message history per sender
 * - Detects escalation patterns
 * - Identifies dangerous tactic sequences
 * - Manages conversation stages
 * - Auto-cleanup of old conversations
 */
public class UserSuspicionTracker {
    private static final UserSuspicionTracker INSTANCE = new UserSuspicionTracker();

    // Map of sender -> conversation history
    public final Map<String, EnhancedConversationHistory> conversations = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupExecutor;

    private UserSuspicionTracker() {
        // Background thread for cleaning up old conversations
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ScamShield-Cleanup");
            t.setDaemon(true);
            return t;
        });

        // Run cleanup every 5 minutes
        cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupOldConversations,
                5, 5, TimeUnit.MINUTES
        );
    }

    public static UserSuspicionTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Record a message and analyze multi-message patterns.
     *
     * This is where the magic happens:
     * - Stores message in history
     * - Updates conversation stage
     * - Detects escalation patterns
     * - Identifies dangerous sequences
     * - Returns bonus score from progression
     *
     * @param sender Username
     * @param message Message text
     * @param singleMessageScore Score from current message only
     * @param result Full detection result for current message
     * @return Bonus score from multi-message analysis
     */
    public int recordAndAnalyze(String sender, String message, int singleMessageScore,
                                DetectionResult result) {
        if (sender == null || sender.isEmpty()) {
            return 0;
        }

        String senderKey = sender.toLowerCase();

        // Get or create conversation history for this sender
        EnhancedConversationHistory history = conversations.computeIfAbsent(
                senderKey,
                k -> new EnhancedConversationHistory(sender)
        );

        // Extract which tactics were detected in this message
        Set<String> currentTactics = new HashSet<>(result.getTriggeredScamTypes());

        // Create record for this message
        MessageRecord record = new MessageRecord(
                message,
                singleMessageScore,
                System.currentTimeMillis(),
                result,
                currentTactics
        );

        // Add to history (also updates stage and tactic sequence)
        history.addMessage(record);

        // Run progression analysis and return bonus
        return history.analyzeProgression();
    }

    /**
     * Remove conversations older than timeout threshold.
     */
    private void cleanupOldConversations() {
        long cutoff = System.currentTimeMillis() -
                (PackCoreConfig.scamShieldConversationTimeoutMinutes * 60_000L);

        conversations.entrySet().removeIf(entry ->
                entry.getValue().getLastMessageTime() < cutoff
        );
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Conversation history for a single sender.
     * Tracks messages, tactics used, and current stage.
     */
    public static class EnhancedConversationHistory {
        private final String sender;
        private final List<MessageRecord> messages = Collections.synchronizedList(new ArrayList<>());
        private volatile long lastMessageTime = 0;
        private volatile ConversationStage currentStage = ConversationStage.INITIAL;

        // Ordered list of all tactics used across all messages
        private final List<String> tacticSequence = Collections.synchronizedList(new ArrayList<>());

        public EnhancedConversationHistory(String sender) {
            this.sender = sender;
        }

        /**
         * Add a message to history and update state.
         */
        public synchronized void addMessage(MessageRecord record) {
            messages.add(record);
            lastMessageTime = record.timestamp;

            // Build tactic sequence (skip consecutive duplicates)
            for (String tactic : record.detectedTactics) {
                if (tacticSequence.isEmpty() || !tacticSequence.getLast().equals(tactic)) {
                    tacticSequence.add(tactic);
                }
            }

            // Update conversation stage based on new tactics
            updateConversationStage(record);

            // Limit memory usage
            while (messages.size() > PackCoreConfig.scamShieldMaxMessagesPerUser) {
                messages.remove(0);
            }
            while (tacticSequence.size() > 20) {
                tacticSequence.remove(0);
            }
        }

        /**
         * Determine conversation stage based on tactics detected.
         * Stages progress: INITIAL → SETUP → TRANSITION → EXPLOITATION → PRESSURE
         */
        private void updateConversationStage(MessageRecord record) {
            Set<String> tactics = record.detectedTactics;

            // EXPLOITATION: Asking for credentials
            if (tactics.contains("credential_fishing") ||
                    tactics.contains("coop_command") ||
                    tactics.stream().anyMatch(t -> t.contains("credential"))) {
                currentStage = ConversationStage.EXPLOITATION;
            }
            // PRESSURE: Exploitation + urgency
            else if (currentStage == ConversationStage.EXPLOITATION &&
                    (tactics.contains("urgency") || tactics.contains("scarcity"))) {
                currentStage = ConversationStage.PRESSURE;
            }
            // TRANSITION: Moving off-platform
            else if (tactics.contains("discord_mention") ||
                    tactics.contains("visit_command") ||
                    tactics.contains("verification_request")) {
                if (currentStage.getLevel() < ConversationStage.TRANSITION.getLevel()) {
                    currentStage = ConversationStage.TRANSITION;
                }
            }
            // SETUP: Introducing the scam premise
            else if (tactics.contains("free_promise") ||
                    tactics.contains("quitting_claim") ||
                    tactics.contains("authority")) {
                if (currentStage.getLevel() < ConversationStage.SETUP.getLevel()) {
                    currentStage = ConversationStage.SETUP;
                }
            }
            // INITIAL: Normal conversation (default)
        }

        /**
         * Analyze progression patterns and calculate bonus score.
         *
         * Checks for:
         * - Score escalation (messages getting more suspicious)
         * - Context shifts (sudden topic changes)
         * - Tactic stacking (using many different tactics)
         * - Dangerous sequences (e.g., discord → verify → credentials)
         * - Temporal patterns (timing of messages)
         */
        public synchronized int analyzeProgression() {
            if (messages.size() < 2) {
                return 0; // Need at least 2 messages
            }

            int bonus = 0;

            // Check various progression patterns
            bonus += detectEscalation();
            bonus += detectContextShift();
            bonus += detectTacticStacking();
            bonus += detectSequencePatterns();
            bonus += applyStageMultiplier(bonus);

            // Cap total bonus to prevent runaway scores
            return Math.min(bonus, PackCoreConfig.scamShieldMaxProgressionBonus);
        }

        /**
         * Detect if scores are consistently increasing (escalation).
         */
        private int detectEscalation() {
            if (messages.size() < 3) return 0;

            int consecutiveIncreases = 0;
            for (int i = 1; i < messages.size(); i++) {
                if (messages.get(i).score > messages.get(i - 1).score) {
                    consecutiveIncreases++;
                    if (consecutiveIncreases >= 3) {
                        return 25; // Clear escalation pattern
                    }
                } else {
                    consecutiveIncreases = 0;
                }
            }

            return consecutiveIncreases >= 2 ? 10 : 0;
        }

        /**
         * Detect sudden jumps in suspicion (context shift).
         */
        private int detectContextShift() {
            if (messages.size() < 4) return 0;

            // Check for sudden score jump
            for (int i = 1; i < messages.size(); i++) {
                MessageRecord prev = messages.get(i - 1);
                MessageRecord curr = messages.get(i);

                if (curr.score - prev.score > 50) {
                    return 30; // Sudden suspicious shift
                }
            }

            // Check for first half vs second half average
            int midpoint = messages.size() / 2;
            double firstHalfAvg = messages.subList(0, midpoint).stream()
                    .mapToInt(m -> m.score)
                    .average()
                    .orElse(0);

            double secondHalfAvg = messages.subList(midpoint, messages.size()).stream()
                    .mapToInt(m -> m.score)
                    .average()
                    .orElse(0);

            // Second half significantly more suspicious?
            if (secondHalfAvg > firstHalfAvg * 2.5 && secondHalfAvg > 30) {
                return 25;
            }

            return 0;
        }

        /**
         * Detect if many different tactics are being used (desperation).
         */
        private int detectTacticStacking() {
            Set<String> allTriggeredTypes = new HashSet<>();

            for (MessageRecord record : messages) {
                if (record.detectedTactics != null) {
                    allTriggeredTypes.addAll(record.detectedTactics);
                }
            }

            // More tactics = more suspicious
            if (allTriggeredTypes.size() >= 4) {
                return 40;
            } else if (allTriggeredTypes.size() >= 3) {
                return 25;
            } else if (allTriggeredTypes.size() >= 2) {
                return 15;
            }

            return 0;
        }

        /**
         * Detect known dangerous sequences (e.g., PDF-documented scams).
         */
        private int detectSequencePatterns() {
            if (tacticSequence.size() < 3) {
                return 0;
            }

            // Use SequencePatternDetector to find matching patterns
            List<SequencePatternDetector.DetectedPattern> patterns =
                    SequencePatternDetector.analyzeSequence(tacticSequence);

            int totalBonus = 0;
            for (SequencePatternDetector.DetectedPattern pattern : patterns) {
                totalBonus += pattern.getBonus();

                PackCore.LOGGER.warn("[ScamShield] Sequence detected: '{}' from {} (+{})",
                        pattern.getName(), sender, pattern.getBonus());
            }

            return totalBonus;
        }

        /**
         * Apply multiplier based on conversation stage.
         * Later stages are more dangerous.
         */
        private int applyStageMultiplier(int currentBonus) {
            if (currentStage == ConversationStage.INITIAL) {
                return 0; // No stage bonus yet
            }

            double multiplier = currentStage.getDangerMultiplier();
            int stageBonus = (int) (currentBonus * (multiplier - 1.0));

            if (stageBonus > 0 && PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield] Stage multiplier ({}): +{} bonus",
                        currentStage.getDisplayName(), stageBonus);
            }

            return stageBonus;
        }

        // Getters
        public long getLastMessageTime() { return lastMessageTime; }
        public ConversationStage getCurrentStage() { return currentStage; }
        public List<String> getTacticSequence() { return new ArrayList<>(tacticSequence); }
    }

    /**
     * Single message record with metadata.
     */
    public static class MessageRecord {
        final String message;
        final int score;
        final long timestamp;
        final DetectionResult detectionResult;
        final Set<String> detectedTactics;

        public MessageRecord(String message, int score, long timestamp,
                             DetectionResult detectionResult, Set<String> detectedTactics) {
            this.message = message;
            this.score = score;
            this.timestamp = timestamp;
            this.detectionResult = detectionResult;
            this.detectedTactics = detectedTactics;
        }
    }
}