package com.github.kd_gaming1.packcore.scamshield.detector;

import com.github.kd_gaming1.packcore.scamshield.conversation.ConversationStage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable result of scam detection analysis.
 *
 * Contains:
 * - Total score (type + progression)
 * - Triggered flag (score >= threshold)
 * - Breakdown of contributions
 * - Original message metadata
 */
public class DetectionResult {
    public static final DetectionResult SAFE = new DetectionResult(
            false, 0, 0, Map.of(), List.of(), "", "", ConfidenceLevel.LOW
    );

    private final boolean triggered;
    private final int scamTypeScore;
    private final int progressionScore;
    private final Map<String, Integer> scamTypeContributions;
    private final List<String> triggeredScamTypes;
    private final String originalMessage;
    private final String sender;
    private final long timestamp;
    private final ConfidenceLevel confidenceLevel;

    private DetectionResult(boolean triggered, int scamTypeScore, int progressionScore,
                            Map<String, Integer> scamTypeContributions,
                            List<String> triggeredScamTypes,
                            String originalMessage, String sender, ConfidenceLevel confidenceLevel) {
        this.triggered = triggered;
        this.scamTypeScore = scamTypeScore;
        this.progressionScore = progressionScore;
        this.scamTypeContributions = Map.copyOf(scamTypeContributions);
        this.triggeredScamTypes = List.copyOf(triggeredScamTypes);
        this.originalMessage = originalMessage == null ? "" : originalMessage;
        this.sender = sender == null ? "" : sender;
        this.timestamp = System.currentTimeMillis();
        this.confidenceLevel = confidenceLevel;
    }

    // Getters
    public boolean isTriggered() { return triggered; }
    public int getTotalScore() { return scamTypeScore + progressionScore; }
    public int getScamTypeScore() { return scamTypeScore; }
    public int getProgressionScore() { return progressionScore; }
    public Map<String, Integer> getScamTypeContributions() { return scamTypeContributions; }
    public List<String> getTriggeredScamTypes() { return triggeredScamTypes; }
    public String getOriginalMessage() { return originalMessage; }
    public String getSender() { return sender; }
    public long getTimestamp() { return timestamp; }
    public ConfidenceLevel getConfidenceLevel() { return confidenceLevel; }

    public ScamCategory getPrimaryCategory() {
        return triggeredScamTypes.isEmpty()
                ? ScamCategory.CUSTOM
                : ScamCategory.fromScamTypeId(triggeredScamTypes.getFirst());
    }

    @Override
    public String toString() {
        return String.format("DetectionResult{triggered=%s, total=%d (type=%d, prog=%d), confidence=%s, types=%s, sender=%s}",
                triggered, getTotalScore(), scamTypeScore, progressionScore,
                confidenceLevel, triggeredScamTypes, sender);
    }

    /**
     * Builder for constructing DetectionResult.
     * Accumulates scores from multiple analyzers.
     */
    public static class Builder {
        private int scamTypeScore = 0;
        private int progressionScore = 0;
        private final Map<String, Integer> scamTypeContributions = new HashMap<>();
        private final List<String> triggeredScamTypes = new ArrayList<>();
        private final String originalMessage;
        private final String sender;
        private final int threshold;
        private ConversationStage stage = ConversationStage.INITIAL;

        public Builder(String originalMessage, String sender, int threshold) {
            this.originalMessage = originalMessage;
            this.sender = sender;
            this.threshold = threshold;
        }

        public void addScamTypeContribution(String scamTypeId, int score) {
            scamTypeScore += score;
            scamTypeContributions.merge(scamTypeId, score, Integer::sum);
            if (score > 0 && !triggeredScamTypes.contains(scamTypeId)) {
                triggeredScamTypes.add(scamTypeId);
            }
        }

        public void addProgressionBonus(int score) {
            progressionScore += score;
        }

        public void setConversationStage(ConversationStage stage) {
            this.stage = stage;
        }

        /**
         * Build final result with stage-adjusted threshold and confidence level.
         */
        public DetectionResult build() {
            // Adjust threshold based on conversation stage
            int adjustedThreshold = threshold;

            switch (stage) {
                case EXPLOITATION:
                    adjustedThreshold = (int) (threshold * 0.7);
                    break;
                case PRESSURE:
                    adjustedThreshold = (int) (threshold * 0.6);
                    break;
                case TRANSITION:
                    adjustedThreshold = (int) (threshold * 0.85);
                    break;
            }

            int totalScore = scamTypeScore + progressionScore;
            boolean triggered = totalScore >= adjustedThreshold;

            // Determine confidence level based on total score
            ConfidenceLevel confidence = ConfidenceLevel.fromScore(totalScore);

            return new DetectionResult(
                    triggered,
                    scamTypeScore,
                    progressionScore,
                    scamTypeContributions,
                    triggeredScamTypes,
                    originalMessage,
                    sender,
                    confidence
            );
        }

        /**
         * Apply a multiplier to all scam type contributions.
         * Used for legitimate trade context detection to reduce false positives.
         *
         * @param multiplier Value between 0.0 and 1.0 to scale scores
         */
        public void applyMultiplier(double multiplier) {
            if (multiplier >= 1.0) {
                return;
            }

            // Adjust the total scam type score
            scamTypeScore = (int)(scamTypeScore * multiplier);

            // Adjust individual contributions
            for (Map.Entry<String, Integer> entry : scamTypeContributions.entrySet()) {
                int originalScore = entry.getValue();
                int adjustedScore = (int)(originalScore * multiplier);
                scamTypeContributions.put(entry.getKey(), adjustedScore);
            }
        }

        public int getCurrentTotalScore() {
            return scamTypeScore + progressionScore;
        }

        public String getOriginalMessage() { return originalMessage; }
        public String getSender() { return sender; }
    }
}