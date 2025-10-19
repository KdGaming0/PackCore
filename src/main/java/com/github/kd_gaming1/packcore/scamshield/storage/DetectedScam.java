package com.github.kd_gaming1.packcore.scamshield.storage;

import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;

/**
 * Represents a detected scam message stored in history
 */
public class DetectedScam {
    private final String sender;
    private final String message;
    private final String category;
    private final int score;
    private final long timestamp;
    private final String[] matchedPatternIds;

    public DetectedScam(String sender, String message, String category, int score,
                        long timestamp, String[] matchedPatternIds) {
        this.sender = sender;
        this.message = message;
        this.category = category;
        this.score = score;
        this.timestamp = timestamp;
        this.matchedPatternIds = matchedPatternIds;
    }

    /**
     * Create from DetectionResult
     */
    public static DetectedScam fromResult(DetectionResult result) {
        String[] patternIds = result.getMatchedPatterns().stream()
                .map(mp -> mp.getPattern().getId())
                .toArray(String[]::new);

        return new DetectedScam(
                result.getSender(),
                result.getOriginalMessage(),
                result.getPrimaryCategory().name(),
                result.getTotalScore(),
                result.getTimestamp(),
                patternIds
        );
    }

    // Getters
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public String getCategory() { return category; }
    public int getScore() { return score; }
    public long getTimestamp() { return timestamp; }
    public String[] getMatchedPatternIds() { return matchedPatternIds; }
}