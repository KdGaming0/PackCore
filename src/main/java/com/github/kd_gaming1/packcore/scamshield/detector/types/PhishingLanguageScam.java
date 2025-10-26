package com.github.kd_gaming1.packcore.scamshield.detector.types;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.context.ConversationContext;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.detector.language.LanguageDatabase;
import com.github.kd_gaming1.packcore.scamshield.detector.language.TacticDefinition;

import java.util.*;

/**
 * Detects phishing attempts by analyzing psychological manipulation tactics.
 *
 * Unlike keyword-based detection, this analyzer looks for HOW scammers communicate:
 * - Do they create urgency? ("quick", "now", "hurry")
 * - Do they claim authority? ("I'm staff", "official")
 * - Do they use scarcity? ("only you", "limited time")
 * - Do they build false trust? ("trust me", "I'll help")
 *
 * The key insight: Scammers follow predictable patterns because these tactics WORK.
 * By detecting the tactics themselves, we catch scams even with new wording.
 *
 * How it works:
 * 1. Load tactics from phishing-language.json
 * 2. For each message, check which tactics are present
 * 3. Score based on tactic severity and combinations
 * 4. Detect tactic clustering (multiple tactics = very suspicious)
 */
public class PhishingLanguageScam implements ScamType {
    private boolean enabled = true;
    private final LanguageDatabase languageDB;

    // Track which tactics were detected for multi-message analysis
    private final Map<String, Set<String>> userDetectedTactics = new HashMap<>();

    public PhishingLanguageScam() {
        // Load the language database
        this.languageDB = new LanguageDatabase();
        PackCore.LOGGER.info("[ScamShield] PhishingLanguageScam initialized with {} tactics",
                languageDB.getTactics().size());
    }

    @Override
    public String getId() {
        return "phishing_language";
    }

    @Override
    public String getDisplayName() {
        return "Phishing Language & Tactics";
    }

    @Override
    public void analyze(String message, String rawMessage, String sender,
                        ConversationContext context, DetectionResult.Builder result) {
        if (!enabled) {
            return;
        }

        // Get all tactics from database
        Map<String, TacticDefinition> tactics = languageDB.getTactics();
        if (tactics.isEmpty()) {
            // Database not loaded or empty
            return;
        }

        int totalScore = 0;
        Set<String> detectedTactics = new HashSet<>();
        Map<String, Integer> tacticScores = new HashMap<>();

        // Analyze message for each tactic
        for (Map.Entry<String, TacticDefinition> entry : tactics.entrySet()) {
            String tacticId = entry.getKey();
            TacticDefinition tactic = entry.getValue();

            // Get score from this tactic
            int tacticScore = tactic.analyze(message);

            if (tacticScore > 0) {
                detectedTactics.add(tacticId);
                tacticScores.put(tacticId, tacticScore);
                totalScore += tacticScore;

                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.info("[ScamShield]   Tactic '{}' detected: +{} points",
                            tacticId, tacticScore);
                }
            }
        }

        // CRITICAL: Detect tactic clustering
        // When multiple different tactics appear together, it's HIGHLY suspicious
        // Example: urgency + authority + credential_fishing = almost certainly a scam
        if (detectedTactics.size() >= 2) {
            int clusteringBonus = calculateClusteringBonus(detectedTactics, tacticScores);
            totalScore += clusteringBonus;

            if (PackCoreConfig.enableScamShieldDebugging && clusteringBonus > 0) {
                PackCore.LOGGER.info("[ScamShield]   Tactic clustering detected! {} tactics = +{} bonus",
                        detectedTactics.size(), clusteringBonus);
            }
        }

        // Track detected tactics for this user (for multi-message analysis)
        if (sender != null && !detectedTactics.isEmpty()) {
            userDetectedTactics
                    .computeIfAbsent(sender.toLowerCase(), k -> new HashSet<>())
                    .addAll(detectedTactics);
        }

        // Apply context sensitivity
        // Some contexts make language patterns more suspicious
        if (totalScore > 0) {
            double contextMultiplier = context.getSensitivityMultiplier(getId());
            totalScore = (int) (totalScore * contextMultiplier);

            result.addScamTypeContribution(getId(), totalScore);

            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.info("[ScamShield] {} detected: +{} points (tactics: {}, context: {}x)",
                        getDisplayName(), totalScore, detectedTactics, contextMultiplier);
            }
        }
    }

    /**
     * Calculate bonus score for tactic clustering.
     *
     * The more different tactics used together, the more suspicious:
     * - 2 tactics: +10 points (could be coincidence)
     * - 3 tactics: +25 points (very suspicious)
     * - 4+ tactics: +50 points (almost certainly a scam)
     *
     * Additionally, certain tactic combinations are EXTREMELY suspicious:
     * - authority + credential_fishing = impersonation scam
     * - urgency + scarcity + authority = classic high-pressure scam
     *
     * @param detectedTactics Set of tactic IDs that were detected
     * @param tacticScores Individual scores for each tactic
     * @return Clustering bonus score
     */
    private int calculateClusteringBonus(Set<String> detectedTactics, Map<String, Integer> tacticScores) {
        int size = detectedTactics.size();
        int bonus = 0;

        // Base clustering bonus by count
        if (size == 2) {
            bonus = 10;
        } else if (size == 3) {
            bonus = 25;
        } else if (size >= 4) {
            bonus = 50;
        }

        // Check for dangerous combinations
        // These are red flag combinations that indicate sophisticated scams
        if (detectedTactics.contains("authority") && detectedTactics.contains("credential_fishing")) {
            // Staff impersonation asking for credentials = MAJOR red flag
            bonus += 40;
        }

        if (detectedTactics.contains("urgency") && detectedTactics.contains("scarcity")) {
            // Time pressure + exclusivity = classic manipulation
            bonus += 20;
        }

        if (detectedTactics.contains("trust_building") && detectedTactics.contains("reciprocity")) {
            // Building rapport then asking for favor = grooming behavior
            bonus += 25;
        }

        if (detectedTactics.contains("urgency") &&
                detectedTactics.contains("authority") &&
                detectedTactics.contains("credential_fishing")) {
            // The "holy trinity" of scam tactics - urgent staff member asking for codes
            bonus += 60;
        }

        return bonus;
    }

    /**
     * Get tactics detected across all messages from a specific user.
     * Used for multi-message pattern detection.
     *
     * @param sender Username to check
     * @return Set of tactic IDs detected across all messages from this user
     */
    public Set<String> getUserTactics(String sender) {
        if (sender == null) {
            return Set.of();
        }
        return userDetectedTactics.getOrDefault(sender.toLowerCase(), Set.of());
    }

    /**
     * Clear tracked tactics for a user.
     * Call this when conversation ends or user disconnects.
     *
     * @param sender Username to clear
     */
    public void clearUserTactics(String sender) {
        if (sender != null) {
            userDetectedTactics.remove(sender.toLowerCase());
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void reload() {
        // Reload the language database from disk
        languageDB.reload();
        PackCore.LOGGER.info("[ScamShield] {} reloaded language database", getDisplayName());
    }

    /**
     * Get the language database for inspection/debugging.
     */
    public LanguageDatabase getLanguageDatabase() {
        return languageDB;
    }
}