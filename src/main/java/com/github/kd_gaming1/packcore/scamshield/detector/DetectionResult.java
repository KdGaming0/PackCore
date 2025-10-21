package com.github.kd_gaming1.packcore.scamshield.detector;

import com.github.kd_gaming1.packcore.scamshield.conversation.ConversationStage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetectionResult {
    public static final DetectionResult SAFE = new DetectionResult(
            false, 0, 0, Map.of(), List.of(), "", ""
    );

    private final boolean triggered;
    private final int scamTypeScore;
    private final int progressionScore;
    private final Map<String, Integer> scamTypeContributions;
    private final List<String> triggeredScamTypes;
    private final String originalMessage;
    private final String sender;
    private final long timestamp;

    private DetectionResult(boolean triggered, int scamTypeScore, int progressionScore,
                            Map<String, Integer> scamTypeContributions,
                            List<String> triggeredScamTypes,
                            String originalMessage, String sender) {
        this.triggered = triggered;
        this.scamTypeScore = scamTypeScore;
        this.progressionScore = progressionScore;
        this.scamTypeContributions = Map.copyOf(scamTypeContributions);
        this.triggeredScamTypes = List.copyOf(triggeredScamTypes);
        this.originalMessage = originalMessage == null ? "" : originalMessage;
        this.sender = sender == null ? "" : sender;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isTriggered() {
        return triggered;
    }

    public int getTotalScore() {
        return scamTypeScore + progressionScore;
    }

    public int getScamTypeScore() {
        return scamTypeScore;
    }

    public int getProgressionScore() {
        return progressionScore;
    }

    public Map<String, Integer> getScamTypeContributions() {
        return scamTypeContributions;
    }

    public List<String> getTriggeredScamTypes() {
        return triggeredScamTypes;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public String getSender() {
        return sender;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ScamCategory getPrimaryCategory() {
        return triggeredScamTypes.isEmpty()
                ? ScamCategory.CUSTOM
                : ScamCategory.fromScamTypeId(triggeredScamTypes.getFirst());
    }

    @Override
    public String toString() {
        return String.format("DetectionResult{triggered=%s, total=%d (type=%d, progression=%d), types=%s, sender=%s}",
                triggered, getTotalScore(), scamTypeScore, progressionScore,
                triggeredScamTypes, sender);
    }

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

        public DetectionResult build() {
            // Calculate stage-adjusted threshold
            int adjustedThreshold = threshold;

            if (stage == ConversationStage.EXPLOITATION) {
                adjustedThreshold = (int) (threshold * 0.7); // 30% more sensitive
            } else if (stage == ConversationStage.PRESSURE) {
                adjustedThreshold = (int) (threshold * 0.6); // 40% more sensitive
            } else if (stage == ConversationStage.TRANSITION) {
                adjustedThreshold = (int) (threshold * 0.85); // 15% more sensitive
            }
            // INITIAL and SETUP stages use normal threshold

            int totalScore = scamTypeScore + progressionScore;
            boolean triggered = totalScore >= adjustedThreshold;

            return new DetectionResult(
                    triggered,
                    scamTypeScore,
                    progressionScore,
                    scamTypeContributions,
                    triggeredScamTypes,
                    originalMessage,
                    sender
            );
        }

        public int getCurrentTotalScore() {
            return scamTypeScore + progressionScore;
        }

        public String getOriginalMessage() {
            return originalMessage;
        }

        public String getSender() {
            return sender;
        }
    }
}