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
 * Tracks conversation sequences and stages, not just scores.
 */
public class UserSuspicionTracker {
    private static final UserSuspicionTracker INSTANCE = new UserSuspicionTracker();

    public final Map<String, EnhancedConversationHistory> conversations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    private UserSuspicionTracker() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ScamShield-EnhancedCleanup");
            t.setDaemon(true);
            return t;
        });

        cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupOldConversations,
                5, 5, TimeUnit.MINUTES
        );
    }

    public static UserSuspicionTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Record a message and analyze conversation patterns.
     * Returns the bonus score from multi-message patterns.
     */
    public int recordAndAnalyze(String sender, String message, int singleMessageScore,
                                DetectionResult result) {
        if (sender == null || sender.isEmpty()) {
            return 0;
        }

        String senderKey = sender.toLowerCase();
        EnhancedConversationHistory history = conversations.computeIfAbsent(
                senderKey,
                k -> new EnhancedConversationHistory(sender)
        );

        // Extract tactics from this message
        Set<String> currentTactics = new HashSet<>(result.getTriggeredScamTypes());

        // Record the message with its tactics
        MessageRecord record = new MessageRecord(
                message,
                singleMessageScore,
                System.currentTimeMillis(),
                result,
                currentTactics
        );

        history.addMessage(record);

        // Analyze and return total bonus
        return history.analyzeProgression();
    }

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
     * Enhanced conversation history that tracks sequences and stages
     */
    public static class EnhancedConversationHistory {
        private final String sender;
        private final List<MessageRecord> messages = Collections.synchronizedList(new ArrayList<>());
        private volatile long lastMessageTime = 0;
        private volatile ConversationStage currentStage = ConversationStage.INITIAL;

        // Track all tactics used across the conversation (in order)
        private final List<String> tacticSequence = Collections.synchronizedList(new ArrayList<>());

        public EnhancedConversationHistory(String sender) {
            this.sender = sender;
        }

        public synchronized void addMessage(MessageRecord record) {
            messages.add(record);
            lastMessageTime = record.timestamp;

            // Add new tactics to the sequence
            for (String tactic : record.detectedTactics) {
                if (!tacticSequence.isEmpty() && tacticSequence.get(tacticSequence.size() - 1).equals(tactic)) {
                    // Don't add consecutive duplicates
                    continue;
                }
                tacticSequence.add(tactic);
            }

            // Update conversation stage based on tactics
            updateConversationStage(record);

            // Limit size
            while (messages.size() > PackCoreConfig.scamShieldMaxMessagesPerUser) {
                messages.remove(0);
            }

            // Also limit tactic sequence size
            while (tacticSequence.size() > 20) {
                tacticSequence.remove(0);
            }
        }

        /**
         * Determine what stage the conversation is at based on tactics used
         */
        private void updateConversationStage(MessageRecord record) {
            Set<String> tactics = record.detectedTactics;

            // Check for exploitation stage indicators
            if (tactics.contains("credential_fishing") ||
                    tactics.contains("coop_command") ||
                    tactics.stream().anyMatch(t -> t.contains("credential"))) {
                currentStage = ConversationStage.EXPLOITATION;
            }
            // Check for pressure stage
            else if (currentStage == ConversationStage.EXPLOITATION &&
                    (tactics.contains("urgency") || tactics.contains("scarcity"))) {
                currentStage = ConversationStage.PRESSURE;
            }
            // Check for transition stage
            else if (tactics.contains("discord_mention") ||
                    tactics.contains("visit_command") ||
                    tactics.contains("verification_request")) {
                if (currentStage.getLevel() < ConversationStage.TRANSITION.getLevel()) {
                    currentStage = ConversationStage.TRANSITION;
                }
            }
            // Check for setup stage
            else if (tactics.contains("free_promise") ||
                    tactics.contains("quitting_claim") ||
                    tactics.contains("authority")) {
                if (currentStage.getLevel() < ConversationStage.SETUP.getLevel()) {
                    currentStage = ConversationStage.SETUP;
                }
            }
        }

        /**
         * Comprehensive analysis including:
         * 1. Original progression patterns (escalation, context shift, tactic stacking)
         * 2. NEW: Sequence pattern detection
         * 3. NEW: Conversation stage awareness
         * 4. NEW: Temporal pattern analysis
         */
        public synchronized int analyzeProgression() {
            if (messages.size() < 2) {
                return 0;
            }

            int bonus = 0;

            // Original patterns (keep these - they still work)
            bonus += detectEscalation();
            bonus += detectContextShift();
            bonus += detectTacticStacking();

            // NEW: Detect dangerous sequences
            bonus += detectSequencePatterns();

            // NEW: Stage-based scoring
            bonus += applyStageMultiplier(bonus);

            // NEW: Temporal analysis
            bonus += detectTemporalPatterns();

            // Cap total bonus
            return Math.min(bonus, PackCoreConfig.scamShieldMaxProgressionBonus);
        }

        /**
         * NEW: Detect dangerous tactic sequences across messages
         */
        private int detectSequencePatterns() {
            if (tacticSequence.size() < 3) {
                return 0;
            }

            List<SequencePatternDetector.DetectedPattern> patterns =
                    SequencePatternDetector.analyzeSequence(tacticSequence);

            int totalBonus = 0;
            for (SequencePatternDetector.DetectedPattern pattern : patterns) {
                totalBonus += pattern.getBonus();

                PackCore.LOGGER.warn("[ScamShield] Detected sequence '{}' from {}: +{} bonus",
                        pattern.getName(), sender, pattern.getBonus());
            }

            return totalBonus;
        }

        /**
         * NEW: Apply multiplier based on conversation stage
         * Later stages are more dangerous
         */
        private int applyStageMultiplier(int currentBonus) {
            if (currentStage == ConversationStage.INITIAL) {
                return 0; // No stage bonus for initial contact
            }

            double multiplier = currentStage.getDangerMultiplier();
            int stageBonus = (int) (currentBonus * (multiplier - 1.0));

            if (stageBonus > 0 && PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.debug("[ScamShield] Stage multiplier ({}): +{} bonus",
                        currentStage.getDisplayName(), stageBonus);
            }

            return stageBonus;
        }

        /**
         * NEW: Detect suspicious temporal patterns
         * - Very fast messages (< 5 seconds apart) suggesting copy-paste
         * - Deliberately paced messages (10-30 seconds) to avoid detection
         * - Sudden acceleration after slow start (building trust then rushing)
         */
        private int detectTemporalPatterns() {
            if (messages.size() < 3) {
                return 0;
            }

            int bonus = 0;

            // Calculate time gaps between messages
            List<Long> gaps = new ArrayList<>();
            for (int i = 1; i < messages.size(); i++) {
                long gap = messages.get(i).timestamp - messages.get(i - 1).timestamp;
                gaps.add(gap);
            }

            // Pattern 1: Very fast messages (copy-paste scripts)
            long fastMessageCount = gaps.stream().filter(gap -> gap < 5000).count();
            if (fastMessageCount >= 3 && messages.size() >= 4) {
                bonus += 20;
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.debug("[ScamShield] Fast message pattern detected: +20 bonus");
                }
            }

            // Pattern 2: Deliberate pacing (10-30 second gaps consistently)
            long pacedMessageCount = gaps.stream()
                    .filter(gap -> gap >= 10000 && gap <= 30000)
                    .count();
            if (pacedMessageCount >= 3) {
                bonus += 15;
                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.debug("[ScamShield] Deliberate pacing detected: +15 bonus");
                }
            }

            // Pattern 3: Sudden acceleration
            // First half slow (building trust), second half fast (closing scam)
            if (messages.size() >= 6) {
                int midpoint = messages.size() / 2;
                double firstHalfAvgGap = gaps.subList(0, midpoint).stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0);
                double secondHalfAvgGap = gaps.subList(midpoint, gaps.size()).stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0);

                // If second half is 3x faster than first half
                if (firstHalfAvgGap > 15000 && secondHalfAvgGap < firstHalfAvgGap / 3) {
                    bonus += 25;
                    if (PackCoreConfig.enableScamShieldDebugging) {
                        PackCore.LOGGER.debug("[ScamShield] Acceleration pattern detected: +25 bonus");
                    }
                }
            }

            return bonus;
        }

        // Keep original detection methods
        private int detectEscalation() {
            if (messages.size() < 3) return 0;

            int consecutiveIncreases = 0;
            for (int i = 1; i < messages.size(); i++) {
                if (messages.get(i).score > messages.get(i - 1).score) {
                    consecutiveIncreases++;
                    if (consecutiveIncreases >= 3) {
                        return 25;
                    }
                } else {
                    consecutiveIncreases = 0;
                }
            }

            return consecutiveIncreases >= 2 ? 10 : 0;
        }

        private int detectContextShift() {
            if (messages.size() < 4) return 0;

            for (int i = 1; i < messages.size(); i++) {
                MessageRecord prev = messages.get(i - 1);
                MessageRecord curr = messages.get(i);

                if (curr.score - prev.score > 50) {
                    return 30;
                }
            }

            int midpoint = messages.size() / 2;
            double firstHalfAvg = messages.subList(0, midpoint).stream()
                    .mapToInt(m -> m.score)
                    .average()
                    .orElse(0);

            double secondHalfAvg = messages.subList(midpoint, messages.size()).stream()
                    .mapToInt(m -> m.score)
                    .average()
                    .orElse(0);

            if (secondHalfAvg > firstHalfAvg * 2.5 && secondHalfAvg > 30) {
                return 25;
            }

            return 0;
        }

        private int detectTacticStacking() {
            Set<String> allTriggeredTypes = new HashSet<>();

            for (MessageRecord record : messages) {
                if (record.detectedTactics != null) {
                    allTriggeredTypes.addAll(record.detectedTactics);
                }
            }

            if (allTriggeredTypes.size() >= 4) {
                return 40;
            } else if (allTriggeredTypes.size() >= 3) {
                return 25;
            } else if (allTriggeredTypes.size() >= 2) {
                return 15;
            }

            return 0;
        }

        public long getLastMessageTime() {
            return lastMessageTime;
        }

        public ConversationStage getCurrentStage() {
            return currentStage;
        }

        public List<String> getTacticSequence() {
            return new ArrayList<>(tacticSequence);
        }
    }

    /**
     * Enhanced message record that includes detected tactics
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