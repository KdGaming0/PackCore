package com.github.kd_gaming1.packcore.scamshield.context;

import com.github.kd_gaming1.packcore.PackCore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks conversation context including player activity, location, and party members.
 */
//TODO add a real system for party detection and location tracking
public class ConversationContext {
    private static final ConversationContext INSTANCE = new ConversationContext();

    private volatile PlayerActivity currentActivity = PlayerActivity.UNKNOWN;
    private volatile String currentLocation = "";
    private final Set<String> recentPartyMembers = ConcurrentHashMap.newKeySet();
    private volatile long activityStartTime = 0;
    private volatile long lastActivityUpdate = 0;

    // Context sensitivity rules
    private final Map<String, Map<PlayerActivity, Double>> sensitivityRules = new HashMap<>();

    private ConversationContext() {
        initializeDefaultRules();
    }

    public static ConversationContext getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize default sensitivity rules for different contexts.
     * These define how much more/less sensitive each scam type should be in different activities.
     */
    private void initializeDefaultRules() {
        // Discord Verification Scam sensitivity
        Map<PlayerActivity, Double> discordVerifyRules = new HashMap<>();
        discordVerifyRules.put(PlayerActivity.DUNGEON, 1.5);
        discordVerifyRules.put(PlayerActivity.IN_PARTY, 1.3);
        discordVerifyRules.put(PlayerActivity.UNKNOWN, 1.0);
        discordVerifyRules.put(PlayerActivity.LOBBY, 0.8);
        sensitivityRules.put("discord_verify_scam", discordVerifyRules);

        // Free Rank Bait sensitivity
        Map<PlayerActivity, Double> freeRankRules = new HashMap<>();
        freeRankRules.put(PlayerActivity.LOBBY, 1.2);             // More common in lobbies
        freeRankRules.put(PlayerActivity.UNKNOWN, 1.0);
        sensitivityRules.put("free_rank_bait_scam", freeRankRules);

        // Phishing Sequence sensitivity (always high when in social contexts)
        Map<PlayerActivity, Double> phishingRules = new HashMap<>();
        phishingRules.put(PlayerActivity.DUNGEON, 1.4);
        phishingRules.put(PlayerActivity.IN_PARTY, 1.4);
        phishingRules.put(PlayerActivity.TRADING, 1.3);
        phishingRules.put(PlayerActivity.UNKNOWN, 1.0);
        sensitivityRules.put("phishing_sequence_scam", phishingRules);
    }

    /**
     * Update the player's current activity.
     * Call this from your game state tracker when activity changes.
     *
     * @param activity The new activity
     */
    public void updateActivity(PlayerActivity activity) {
        if (activity == null) {
            activity = PlayerActivity.UNKNOWN;
        }

        if (this.currentActivity != activity) {
            PackCore.LOGGER.debug("[ScamShield] Activity changed: {} → {}",
                    this.currentActivity, activity);
            this.currentActivity = activity;
            this.activityStartTime = System.currentTimeMillis();
        }

        this.lastActivityUpdate = System.currentTimeMillis();
    }

    /**
     * Update the player's current location
     *
     * @param location The location name
     */
    public void updateLocation(String location) {
        if (location != null && !location.equals(this.currentLocation)) {
            PackCore.LOGGER.debug("[ScamShield] Location changed: {} → {}",
                    this.currentLocation, location);
            this.currentLocation = location;
        }
    }

    /**
     * Add a player to the recent party members set.
     * This helps track who you're playing with.
     *
     * @param playerName The player's username
     */
    public void addPartyMember(String playerName) {
        if (playerName != null && !playerName.isEmpty()) {
            recentPartyMembers.add(playerName.toLowerCase());
            PackCore.LOGGER.debug("[ScamShield] Added party member: {}", playerName);
        }
    }

    /**
     * Remove a player from party members (when they leave)
     *
     * @param playerName The player's username
     */
    public void removePartyMember(String playerName) {
        if (playerName != null) {
            recentPartyMembers.remove(playerName.toLowerCase());
            PackCore.LOGGER.debug("[ScamShield] Removed party member: {}", playerName);
        }
    }

    /**
     * Clear all party members (when party disbands)
     */
    public void clearPartyMembers() {
        if (!recentPartyMembers.isEmpty()) {
            PackCore.LOGGER.debug("[ScamShield] Cleared party members");
            recentPartyMembers.clear();
        }
    }

    /**
     * Check if a player is in your recent party
     *
     * @param playerName The player's username
     * @return true if they're in your party
     */
    public boolean isInParty(String playerName) {
        if (playerName == null) {
            return false;
        }
        return recentPartyMembers.contains(playerName.toLowerCase());
    }

    /**
     * Get sensitivity multiplier for a specific scam type based on current context.
     * This is how context affects detection!
     *
     * @param scamTypeId The scam type ID (e.g., "discord_verify_scam")
     * @return Sensitivity multiplier (1.0 = normal, >1.0 = more sensitive, <1.0 = less sensitive)
     */
    public double getSensitivityMultiplier(String scamTypeId) {
        Map<PlayerActivity, Double> rules = sensitivityRules.get(scamTypeId);
        if (rules == null) {
            return 1.0; // No specific rules, use default
        }

        return rules.getOrDefault(currentActivity, 1.0);
    }

    /**
     * Get how long the player has been in the current activity (in milliseconds)
     *
     * @return Duration in milliseconds
     */
    public long getActivityDuration() {
        if (activityStartTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - activityStartTime;
    }

    /**
     * Reset all context (call when leaving Hypixel or major state change)
     */
    public void reset() {
        PackCore.LOGGER.debug("[ScamShield] Context reset");
        currentActivity = PlayerActivity.UNKNOWN;
        currentLocation = "";
        recentPartyMembers.clear();
        activityStartTime = 0;
        lastActivityUpdate = 0;
    }

    // Getters
    public PlayerActivity getCurrentActivity() {
        return currentActivity;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public Set<String> getRecentPartyMembers() {
        return new HashSet<>(recentPartyMembers);
    }

    /**
     * Add or update a sensitivity rule for a scam type.
     *
     * @param scamTypeId The scam type ID
     * @param activity The activity
     * @param multiplier The sensitivity multiplier
     */
    public void setSensitivityRule(String scamTypeId, PlayerActivity activity, double multiplier) {
        sensitivityRules
                .computeIfAbsent(scamTypeId, k -> new HashMap<>())
                .put(activity, multiplier);

        PackCore.LOGGER.debug("[ScamShield] Updated sensitivity rule: {} in {} = {}x",
                scamTypeId, activity, multiplier);
    }
}