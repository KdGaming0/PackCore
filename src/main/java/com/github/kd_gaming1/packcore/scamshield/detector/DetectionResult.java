package com.github.kd_gaming1.packcore.scamshield.detector;

import java.util.List;

/**
 * Result of a scam detection analysis
 */
public class DetectionResult {
    public static final DetectionResult SAFE = new DetectionResult(
            false, 0, List.of(), "", ""
    );

    private final boolean triggered;
    private final int totalScore;
    private final List<MatchedPattern> matchedPatterns;
    private final String originalMessage;
    private final String sender;
    private final long timestamp;

    public DetectionResult(boolean triggered, int totalScore, List<MatchedPattern> matchedPatterns,
                           String originalMessage, String sender) {
        this.triggered = triggered;
        this.totalScore = totalScore;
        this.matchedPatterns = matchedPatterns == null ? List.of() : List.copyOf(matchedPatterns);
        this.originalMessage = originalMessage == null ? "" : originalMessage;
        this.sender = sender == null ? "" : sender;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isTriggered() {
        return triggered;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public List<MatchedPattern> getMatchedPatterns() {
        return matchedPatterns;
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

    /**
     * Get primary scam category (highest weighted match)
     */
    public ScamCategory getPrimaryCategory() {
        return matchedPatterns.stream()
                .max((a, b) -> Integer.compare(a.getPattern().getWeight(), b.getPattern().getWeight()))
                .map(m -> m.getPattern().getCategory())
                .orElse(ScamCategory.CUSTOM);
    }

    @Override
    public String toString() {
        return String.format("DetectionResult{triggered=%s, score=%d, matches=%d, sender=%s}",
                triggered, totalScore, matchedPatterns.size(), sender);
    }
}
