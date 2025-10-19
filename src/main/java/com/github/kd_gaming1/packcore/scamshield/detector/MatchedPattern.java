package com.github.kd_gaming1.packcore.scamshield.detector;

/**
 * Wrapper for a pattern that was matched during detection
 */
public class MatchedPattern {
    private final ScamPattern pattern;

    public MatchedPattern(ScamPattern pattern) {
        this.pattern = pattern;
    }

    public ScamPattern getPattern() {
        return pattern;
    }
}
