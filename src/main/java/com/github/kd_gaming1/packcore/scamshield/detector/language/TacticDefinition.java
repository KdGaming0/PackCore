package com.github.kd_gaming1.packcore.scamshield.detector.language;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a psychological manipulation tactic.
 *
 * A tactic is a category of language patterns that serve the same purpose.
 * For example, "urgency" is a tactic that includes patterns like:
 * - Time pressure words: "quick", "hurry", "now"
 * - Scarcity words: "limited", "expires", "last chance"
 *
 * Tactics are scored based on:
 * 1. Base score - just detecting the tactic at all
 * 2. Combination bonus - using multiple patterns from the same tactic
 */
public class TacticDefinition {
    // Human-readable description of this tactic
    private String description;

    // Base score awarded for detecting this tactic
    private int baseScore;

    // Bonus score when multiple patterns of this tactic are detected
    // (e.g., saying "quick" AND "now" AND "limited time" is way more suspicious)
    private int combinationBonus;

    // List of language patterns that belong to this tactic
    private List<LanguagePattern> patterns;

    // Getters
    public String getDescription() { return description; }
    public int getBaseScore() { return baseScore; }
    public int getCombinationBonus() { return combinationBonus; }
    public List<LanguagePattern> getPatterns() { return patterns; }

    /**
     * Analyze a message for this tactic and calculate score.
     *
     * Scoring logic:
     * - If no patterns match: 0 points
     * - If 1 pattern matches: base_score * pattern_weight
     * - If 2+ patterns match: base_score + (combination_bonus * extra_patterns)
     *
     * This rewards detecting multiple similar tactics, which is more suspicious.
     *
     * @param message The normalized message to analyze
     * @return Score contribution from this tactic
     */
    public int analyze(String message) {
        int matchingPatterns = 0;
        double totalWeight = 0.0;
        List<String> matchedPhrases = new ArrayList<>();

        for (LanguagePattern pattern : patterns) {
            int matches = pattern.countMatches(message);
            if (matches > 0) {
                matchingPatterns++;
                totalWeight += pattern.getWeight();
                // Track what actually matched for debugging
                matchedPhrases.addAll(pattern.getPhrases());
            }
        }

        if (matchingPatterns == 0) {
            return 0;
        }

        double lengthPenalty = calculateLengthPenalty(message.length(), matchingPatterns);

        if (matchingPatterns == 1) {
            int score = (int) (baseScore * totalWeight * lengthPenalty);
            return Math.max(0, score);
        }

        // Multiple matches
        int score = baseScore;
        score += (matchingPatterns - 1) * combinationBonus;

        double avgWeight = totalWeight / matchingPatterns;
        score = (int) (score * avgWeight * lengthPenalty);

        return Math.max(0, score);
    }

    /**
     * Adjust scores based on message length.
     */
    private double calculateLengthPenalty(int messageLength, int tacticCount) {
        if (messageLength < 20) {
            // Very short message - if it has multiple tactics, boost the score
            return tacticCount >= 2 ? 1.5 : 1.0;
        } else if (messageLength < 50) {
            // Medium message - normal scoring
            return 1.0;
        } else if (messageLength < 100) {
            // Longer message - reduce score slightly
            return 0.9;
        } else {
            // Very long message - probably someone explaining something
            // Reduce score significantly if only 1-2 tactics
            return tacticCount <= 2 ? 0.6 : 0.8;
        }
    }
}