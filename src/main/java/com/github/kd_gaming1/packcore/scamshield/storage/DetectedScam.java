package com.github.kd_gaming1.packcore.scamshield.storage;

import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;

public record DetectedScam(String sender, String message, String category, int score, long timestamp,
                           String[] triggeredScamTypes) {

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
}