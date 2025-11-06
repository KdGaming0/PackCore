package com.github.kd_gaming1.packcore.scamshield.conversation;

import com.github.kd_gaming1.packcore.PackCore;

import java.util.*;

/**
 * Detects dangerous sequences of tactics across multiple messages.
 *
 * Key insight: Individual tactics can be innocent, but sequences reveal intent.
 * - "trust me" alone = innocent
 * - "trust me" → "join discord" → "verify account" → "send code" = clear scam
 */
public class SequencePatternDetector {

    private static final List<SequencePattern> DANGEROUS_PATTERNS = new ArrayList<>();

    static {
        // Discord verification scam sequence
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Discord Verification Scam",
                List.of("trust_building", "discord_mention", "verification_request", "credential_fishing"),
                150,  // High bonus - this is a dead giveaway
                ConversationStage.EXPLOITATION
        ));

        // Quick credential phishing (no trust building)
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Direct Credential Phishing",
                List.of("discord_mention", "verification_request", "credential_fishing"),
                120,
                ConversationStage.EXPLOITATION
        ));

        // Island/Co-op theft sequence
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Island Theft Progression",
                List.of("trust_building", "quitting_claim", "coop_command"),
                140,
                ConversationStage.EXPLOITATION
        ));

        // Authority-based phishing
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Staff Impersonation Scam",
                List.of("authority", "urgency", "credential_fishing"),
                160,  // Very high - claiming to be staff then asking for credentials
                ConversationStage.EXPLOITATION
        ));

        // Trust → Urgency → Credentials (classic pressure tactic)
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Trust Exploitation Pattern",
                List.of("trust_building", "urgency", "credential_fishing"),
                130,
                ConversationStage.PRESSURE
        ));

        // Free reward → Visit/Action → Credentials
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Reward Bait Scam",
                List.of("free_promise", "visit_command", "credential_fishing"),
                125,
                ConversationStage.EXPLOITATION
        ));

        // Setup → Off-platform → Verification (moving to Discord then asking to verify)
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Off-Platform Verification",
                List.of("discord_mention", "technical_excuse", "verification_request"),
                115,
                ConversationStage.TRANSITION
        ));

        // Wealth inquiry → Trust building → Credential fishing (sizing up then scamming)
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Target Assessment Scam",
                List.of("wealth_inquiry", "trust_building", "credential_fishing"),
                135,
                ConversationStage.EXPLOITATION
        ));

        // Quitting → Co-op → Island theft
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Quitting Island Theft",
                List.of("quitting_claim", "coop_command", "visit_command"),
                145,
                ConversationStage.EXPLOITATION
        ));

        // Flex → Trade → Payment (flex scam)
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Flex Payment Scam",
                List.of("trade_manipulation", "command_instruction", "reciprocity"),
                140,
                ConversationStage.EXPLOITATION
        ));

        // Free rank → Visit → Discord → Coins
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Free Rank to Coin Theft",
                List.of("free_promise", "command_instruction", "discord_transition", "wealth_inquiry"),
                155,
                ConversationStage.EXPLOITATION
        ));

        // Tournament → Must Win → Discord verification
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Tournament Verification Scam",
                List.of("tournament_bait", "urgency", "discord_transition", "verification_request"),
                150,
                ConversationStage.EXPLOITATION
        ));

        // Fake accident → Help → BIN/Auction manipulation
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Fake Accident Auction Scam",
                List.of("fake_accident", "urgency", "specific_valuables"),
                130,
                ConversationStage.EXPLOITATION
        ));

        // Crafting help → Materials → Collateral trick
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Crafting Material Theft",
                List.of("crafting_help", "trust_building", "specific_valuables"),
                125,
                ConversationStage.EXPLOITATION
        ));

        // Party invite → VC excuse → Discord → Verify
        // This is the EXACT sequence from the bedwars scam in the PDF
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Party to VC Verification",
                List.of("discord_transition", "technical_excuse", "verification_request", "credential_fishing"),
                165, // Very high - this is documented in PDF as active scam
                ConversationStage.EXPLOITATION
        ));

        // Command instruction early in conversation
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Immediate Command Request",
                List.of("command_instruction", "free_promise"),
                110,
                ConversationStage.TRANSITION
        ));

        // Multiple wealth inquiries followed by offers
        DANGEROUS_PATTERNS.add(new SequencePattern(
                "Wealth Assessment to Scam",
                List.of("wealth_inquiry", "wealth_inquiry", "trade_manipulation"),
                140,
                ConversationStage.EXPLOITATION
        ));
    }

    /**
     * Analyze a sequence of tactics and detect dangerous patterns.
     *
     * @param tacticSequence Ordered list of tactics detected (chronological)
     * @return List of detected patterns with their bonuses
     */
    public static List<DetectedPattern> analyzeSequence(List<String> tacticSequence) {
        List<DetectedPattern> detected = new ArrayList<>();

        for (SequencePattern pattern : DANGEROUS_PATTERNS) {
            MatchResult match = pattern.matches(tacticSequence);
            if (match.isMatch()) {
                detected.add(new DetectedPattern(
                        pattern.name,
                        pattern.bonus,
                        pattern.stage,
                        match.matchedIndices
                ));

                PackCore.LOGGER.warn("[ScamShield] Detected dangerous sequence: {} (bonus: +{})",
                        pattern.name, pattern.bonus);
            }
        }

        return detected;
    }

    /**
     * Defines a dangerous sequence of tactics
     */
    private static class SequencePattern {
        final String name;
        final List<String> requiredSequence;
        final int bonus;
        final ConversationStage stage;

        SequencePattern(String name, List<String> requiredSequence, int bonus, ConversationStage stage) {
            this.name = name;
            this.requiredSequence = requiredSequence;
            this.bonus = bonus;
            this.stage = stage;
        }

        /**
         * Check if this pattern matches the given tactic sequence.
         * The tactics don't need to be consecutive, but they must appear in order.
         *
         * Example: Pattern [A, B, C] matches sequence [X, A, Y, B, Z, C]
         */
        MatchResult matches(List<String> tacticSequence) {
            List<Integer> matchedIndices = new ArrayList<>();
            int patternIndex = 0;

            for (int i = 0; i < tacticSequence.size() && patternIndex < requiredSequence.size(); i++) {
                String requiredTactic = requiredSequence.get(patternIndex);
                String actualTactic = tacticSequence.get(i);

                // Check if this tactic matches (exact or contains)
                if (actualTactic.equals(requiredTactic) ||
                        actualTactic.contains(requiredTactic) ||
                        requiredTactic.contains(actualTactic)) {
                    matchedIndices.add(i);
                    patternIndex++;
                }
            }

            boolean isMatch = patternIndex == requiredSequence.size();
            return new MatchResult(isMatch, matchedIndices);
        }
    }

    /**
     * Result of a pattern match
     */
    private static class MatchResult {
        final boolean isMatch;
        final List<Integer> matchedIndices;

        MatchResult(boolean isMatch, List<Integer> matchedIndices) {
            this.isMatch = isMatch;
            this.matchedIndices = matchedIndices;
        }

        boolean isMatch() {
            return isMatch;
        }
    }

    /**
     * A detected dangerous pattern
     */
    public static class DetectedPattern {
        private final String name;
        private final int bonus;
        private final ConversationStage stage;
        private final List<Integer> matchedIndices;

        public DetectedPattern(String name, int bonus, ConversationStage stage, List<Integer> matchedIndices) {
            this.name = name;
            this.bonus = bonus;
            this.stage = stage;
            this.matchedIndices = matchedIndices;
        }

        public String getName() { return name; }
        public int getBonus() { return bonus; }
        public ConversationStage getStage() { return stage; }
        public List<Integer> getMatchedIndices() { return matchedIndices; }
    }
}