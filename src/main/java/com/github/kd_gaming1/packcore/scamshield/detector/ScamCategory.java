package com.github.kd_gaming1.packcore.scamshield.detector;

/**
 * Categories of scam types for organization and filtering
 */
public enum ScamCategory {
    DISCORD_VERIFY("Discord Verification Scam"),
    SUSPICIOUS_LINK("Suspicious Link"),
    FAKE_THREAT("Fake Account Threat"),
    FAKE_REWARD("Too Good to be True"),
    IMPERSONATION("Staff Impersonation"),
    ACCOUNT_THEFT("Account Theft Attempt"),
    URGENCY("Urgency Tactic"),
    PHISHING("Phishing Attempt"),
    CUSTOM("Custom Pattern");

    private final String displayName;

    ScamCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}