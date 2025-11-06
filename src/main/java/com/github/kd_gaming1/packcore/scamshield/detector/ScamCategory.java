package com.github.kd_gaming1.packcore.scamshield.detector;

public enum ScamCategory {
    DISCORD_VERIFY("Discord Verification Scam", "discord_verify_scam"),
    SUSPICIOUS_LINK("Suspicious Link", "suspicious_link_scam"),
    FAKE_THREAT("Fake Account Threat", "fake_threat_scam"),
    FAKE_REWARD("Too Good to be True", "free_rank_bait_scam"),
    IMPERSONATION("Staff Impersonation", "impersonation_scam"),
    ACCOUNT_THEFT("Account Theft Attempt", "island_theft_scam"),
    URGENCY("Urgency Tactic", "urgency_scam"),
    PHISHING("Phishing Attempt", "phishing_language"),
    TRADE_MANIPULATION("Price Manipulation", "trade_manipulation_scam"),
    CRAFTING_SCAM("Crafting/Reforging Scams", "crafting_scam"),
    BORROWING_SCAM("Borrowing/Loaning Scams", "borrowing_scam"),
    RANK_SELLING("Rank Selling", "rank_selling_scam"),
    CUSTOM("Custom Pattern", "custom");
    private final String displayName;
    private final String scamTypeId;

    ScamCategory(String displayName, String scamTypeId) {
        this.displayName = displayName;
        this.scamTypeId = scamTypeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getScamTypeId() {
        return scamTypeId;
    }

    /**
     * Get category from scam type ID, or CUSTOM if not found
     */
    public static ScamCategory fromScamTypeId(String scamTypeId) {
        if (scamTypeId == null) {
            return CUSTOM;
        }

        for (ScamCategory category : values()) {
            if (category.scamTypeId.equals(scamTypeId)) {
                return category;
            }
        }

        return CUSTOM;
    }
}