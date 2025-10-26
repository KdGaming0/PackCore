package com.github.kd_gaming1.packcore.scamshield.detector;

/**
 * Confidence level for scam detections.
 * Determines warning severity and user experience.
 *
 * Levels:
 * - LOW: Suspicious but could be legitimate (suggest whitelist)
 * - MEDIUM: Likely a scam (strong warning + education)
 * - HIGH: Almost certainly a scam (critical warning + block interaction)
 */
public enum ConfidenceLevel {
    /**
     * LOW confidence (100-149 points)
     *
     * Characteristics:
     * - Single suspicious element
     * - Could be poorly worded legitimate message
     * - Friend might be quitting for real
     *
     * Response:
     * - Gentle chat warning
     * - Suggest whitelist command
     * - Allow user to proceed
     */
    LOW(100, 149, "Low", "§e", false),

    /**
     * MEDIUM confidence (150-249 points)
     *
     * Characteristics:
     * - Multiple suspicious elements
     * - Classic scam patterns detected
     * - Escalating conversation
     *
     * Response:
     * - Strong chat warning
     * - Provide education link
     * - Encourage caution
     */
    MEDIUM(150, 249, "Medium", "§6", false),

    /**
     * HIGH confidence (250+ points)
     *
     * Characteristics:
     * - Credential fishing detected
     * - Multiple dangerous patterns
     * - Documented scam sequence
     *
     * Response:
     * - Critical warning
     * - Force open warning screen
     * - Block until user acknowledges
     */
    HIGH(250, Integer.MAX_VALUE, "High", "§c", true);

    private final int minScore;
    private final int maxScore;
    private final String displayName;
    private final String colorCode;
    private final boolean forceScreen;

    ConfidenceLevel(int minScore, int maxScore, String displayName, String colorCode, boolean forceScreen) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.forceScreen = forceScreen;
    }

    /**
     * Determine confidence level from detection score.
     */
    public static ConfidenceLevel fromScore(int score) {
        for (ConfidenceLevel level : values()) {
            if (score >= level.minScore && score <= level.maxScore) {
                return level;
            }
        }
        return LOW; // Fallback
    }

    public int getMinScore() { return minScore; }
    public int getMaxScore() { return maxScore; }
    public String getDisplayName() { return displayName; }
    public String getColorCode() { return colorCode; }
    public boolean shouldForceScreen() { return forceScreen; }

    /**
     * Check if this level should suggest whitelisting.
     * Only LOW confidence suggests whitelist (might be friend).
     */
    public boolean shouldSuggestWhitelist() {
        return this == LOW;
    }
}