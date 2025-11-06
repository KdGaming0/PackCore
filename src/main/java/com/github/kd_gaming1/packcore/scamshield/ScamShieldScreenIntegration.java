package com.github.kd_gaming1.packcore.scamshield;

import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamCategory;
import com.github.kd_gaming1.packcore.ui.screen.scamshield.ScamWarningScreen;

/**
 * Utility class to bridge between ScamShield detection system and UI screens.
 * Maps DetectionResult data to screen-compatible format.
 */
public class ScamShieldScreenIntegration {

    /**
     * Convert DetectionResult to ScamWarning for the warning screen
     */
    public static ScamWarningScreen.ScamWarning convertToWarning(DetectionResult result) {
        return new ScamWarningScreen.ScamWarning(
                result.getSender(),
                mapCategory(result.getPrimaryCategory()),
                result.getOriginalMessage(),
                mapConfidence(result.getConfidenceLevel())
        );
    }

    /**
     * Map ScamCategory to ScamWarningScreen.ScamType
     */
    private static ScamWarningScreen.ScamType mapCategory(ScamCategory category) {
        return switch (category) {
            case DISCORD_VERIFY, PHISHING -> ScamWarningScreen.ScamType.PHISHING_LINK;
            case FAKE_REWARD -> ScamWarningScreen.ScamType.FALSE_REWARDS;
            case ACCOUNT_THEFT -> ScamWarningScreen.ScamType.COOP_SCAM;
            case TRADE_MANIPULATION -> ScamWarningScreen.ScamType.PRICE_MANIPULATION;
            case CRAFTING_SCAM -> ScamWarningScreen.ScamType.CRAFTING;
            case BORROWING_SCAM -> ScamWarningScreen.ScamType.BORROWING;
            case RANK_SELLING -> ScamWarningScreen.ScamType.RANK_SELLING;
            default -> ScamWarningScreen.ScamType.GENERAL;
        };
    }

    /**
     * Map ConfidenceLevel to percentage (0-100)
     */
    private static int mapConfidence(com.github.kd_gaming1.packcore.scamshield.detector.ConfidenceLevel level) {
        return switch (level) {
            case LOW -> 60;      // 50-69% range
            case MEDIUM -> 80;   // 70-89% range
            case HIGH -> 95;     // 90-100% range
        };
    }

    /**
     * Get a human-readable summary of the detection for the warning screen.
     * Extracts key information from the detection result.
     */
    public static String getDetectionSummary(DetectionResult result) {
        return "Category: " + result.getPrimaryCategory().getDisplayName() +

                // Add score information
                " | Score: " + result.getTotalScore() +

                // Add confidence
                " | Confidence: " + result.getConfidenceLevel().getDisplayName();
    }

    /**
     * Check if the warning screen should be shown based on config and confidence
     */
    public static boolean shouldShowWarningScreen(DetectionResult result) {
        // Only show screen for HIGH confidence detections
        // (Medium and Low use chat warnings only)
        return result.getConfidenceLevel() ==
                com.github.kd_gaming1.packcore.scamshield.detector.ConfidenceLevel.HIGH;
    }

    /**
     * Get appropriate education content based on scam category
     * Returns the markdown content or null to use default
     */
    public static String getEducationContentForCategory(ScamCategory category) {
        // You can customize this to return category-specific content
        // For now, return null to use the default full guide
        return null;
    }

    /**
     * Get a highlighted section name for the education screen
     * This can be used to jump to a specific section in the markdown
     */
    public static String getEducationSectionForCategory(ScamCategory category) {
        return switch (category) {
            case DISCORD_VERIFY, PHISHING -> "Phishing Links";
            case FAKE_REWARD -> "False Rewards";
            case ACCOUNT_THEFT -> "Co-op Island Stealing";
            case TRADE_MANIPULATION -> "Price Manipulation";
            case CRAFTING_SCAM -> "Crafting/Reforging Scams";
            case BORROWING_SCAM -> "Borrowing/Loaning Scams";
            case RANK_SELLING -> "Rank Selling";
            default -> null;
        };
    }
}