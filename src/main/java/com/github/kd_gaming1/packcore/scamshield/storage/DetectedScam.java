package com.github.kd_gaming1.packcore.scamshield.storage;

import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;

public class DetectedScam {
    private final String sender;
    private final String message;
    private final String category;
    private final int score;
    private final long timestamp;
    private final String[] triggeredScamTypes;

    public DetectedScam(String sender, String message, String category, int score,
                        long timestamp, String[] triggeredScamTypes) {
        this.sender = sender;
        this.message = message;
        this.category = category;
        this.score = score;
        this.timestamp = timestamp;
        this.triggeredScamTypes = triggeredScamTypes;
    }

    public static DetectedScam fromResult(DetectionResult result) {
        String[] scamTypeIds = result.getTriggeredScamTypes().toArray(new String[0]);

        return new DetectedScam(
                result.getSender(),
                result.getOriginalMessage(),
                result.getPrimaryCategory().name(),
                result.getTotalScore(),
                result.getTimestamp(),
                scamTypeIds
        );
    }

    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public String getCategory() { return category; }
    public int getScore() { return score; }
    public long getTimestamp() { return timestamp; }
    public String[] getTriggeredScamTypes() { return triggeredScamTypes; }
}