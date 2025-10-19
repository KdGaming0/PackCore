package com.github.kd_gaming1.packcore.scamshield.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistics about scam detections
 */
public class DetectionStats {
    private final int totalDetections;
    private final Map<String, Integer> categoryCounts;
    private final int uniqueSenders;
    private final long oldestTimestamp;
    private final long newestTimestamp;

    public DetectionStats(List<DetectedScam> detections) {
        this.totalDetections = detections.size();
        this.categoryCounts = new HashMap<>();

        Map<String, Boolean> uniqueSenderMap = new HashMap<>();
        long oldest = Long.MAX_VALUE;
        long newest = Long.MIN_VALUE;

        for (DetectedScam detection : detections) {
            // Count categories
            String category = detection.getCategory();
            categoryCounts.merge(category, 1, Integer::sum);

            // Track unique senders
            if (detection.getSender() != null) {
                uniqueSenderMap.put(detection.getSender(), true);
            }

            // Track timestamps
            if (detection.getTimestamp() < oldest) {
                oldest = detection.getTimestamp();
            }
            if (detection.getTimestamp() > newest) {
                newest = detection.getTimestamp();
            }
        }

        this.uniqueSenders = uniqueSenderMap.size();
        this.oldestTimestamp = oldest == Long.MAX_VALUE ? 0 : oldest;
        this.newestTimestamp = newest == Long.MIN_VALUE ? 0 : newest;
    }

    // Getters
    public int getTotalDetections() { return totalDetections; }
    public Map<String, Integer> getCategoryCounts() { return categoryCounts; }
    public int getUniqueSenders() { return uniqueSenders; }
    public long getOldestTimestamp() { return oldestTimestamp; }
    public long getNewestTimestamp() { return newestTimestamp; }
}