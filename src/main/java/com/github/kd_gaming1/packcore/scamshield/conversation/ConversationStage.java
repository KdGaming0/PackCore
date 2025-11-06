package com.github.kd_gaming1.packcore.scamshield.conversation;

/**
 * Tracks the stage of a conversation to detect scam progression patterns.
 *
 * Scammers follow predictable stages:
 * 1. INITIAL: Building trust, normal conversation
 * 2. SETUP: Introducing the "opportunity" or reason to move off-platform
 * 3. TRANSITION: Moving to Discord, website, or other platform
 * 4. EXPLOITATION: Requesting credentials, codes, or sensitive actions
 * 5. PRESSURE: Creating urgency to prevent victim from thinking
 */
public enum ConversationStage {
    /**
     * Initial contact - normal conversation, building trust
     * Examples: "hey", "wanna party?", "need one more for dungeons"
     */
    INITIAL(0, "Initial Contact"),

    /**
     * Setup phase - introducing the scam premise
     * Examples: "join discord for vc", "free rank giveaway", "i'm quitting"
     */
    SETUP(1, "Setup Phase"),

    /**
     * Transition - moving to off-platform or specific action
     * Examples: "join my discord server", "visit my island", "click this link"
     */
    TRANSITION(2, "Platform Transition"),

    /**
     * Exploitation - requesting credentials or sensitive information
     * Examples: "verify your account", "send the code", "what's your email"
     */
    EXPLOITATION(3, "Credential Request"),

    /**
     * Pressure - creating urgency to close the scam
     * Examples: "quick before it expires", "do it now", "hurry"
     */
    PRESSURE(4, "Urgency Pressure");

    private final int level;
    private final String displayName;

    ConversationStage(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this stage is more advanced than another
     */
    public boolean isMoreAdvancedThan(ConversationStage other) {
        return this.level > other.level;
    }

    /**
     * Get the "danger level" multiplier for this stage.
     * Later stages in a conversation are more suspicious.
     */
    public double getDangerMultiplier() {
        switch (this) {
            case INITIAL: return 0.5;      // Normal conversation
            case SETUP: return 1.0;        // Starting to look suspicious
            case TRANSITION: return 1.5;   // Definitely suspicious
            case EXPLOITATION: return 2.5; // Very dangerous
            case PRESSURE: return 3.0;     // Critical - likely active scam
            default: return 1.0;
        }
    }
}