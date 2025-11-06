package com.github.kd_gaming1.packcore.scamshield.context;

/**
 * Represents different activities a player can be doing on Hypixel.
 * Used by ConversationContext to adjust scam detection sensitivity.
 */
public enum PlayerActivity {
    UNKNOWN("Unknown"),
    LOBBY("Lobby"),
    DUNGEON("Dungeon"),
    IN_PARTY("In Party"),
    TRADING("Trading"),
    MINIGAME("Minigame"),
    SKYBLOCK_HUB("SkyBlock Hub"),
    PRIVATE_ISLAND("Private Island");

    private final String displayName;

    PlayerActivity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}