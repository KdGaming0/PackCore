package com.github.kd_gaming1.packcore.scamshield.storage;

import com.github.kd_gaming1.packcore.scamshield.detector.ScamPattern;

import java.util.List;

/**
 * Container for exporting all ScamShield data
 */
public class ExportData {
    private final List<ScamPattern> patterns;
    private final List<DetectedScam> detections;
    private final long exportTimestamp;
    private final String version;

    public ExportData(List<ScamPattern> patterns, List<DetectedScam> detections) {
        this.patterns = patterns;
        this.detections = detections;
        this.exportTimestamp = System.currentTimeMillis();
        this.version = "1.0.0";
    }

    // Getters for Gson
    public List<ScamPattern> getPatterns() { return patterns; }
    public List<DetectedScam> getDetections() { return detections; }
    public long getExportTimestamp() { return exportTimestamp; }
    public String getVersion() { return version; }
}