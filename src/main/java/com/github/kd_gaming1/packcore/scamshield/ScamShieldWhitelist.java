package com.github.kd_gaming1.packcore.scamshield;

import com.github.kd_gaming1.packcore.PackCore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a whitelist of trusted players whose messages bypass scam detection.
 */
public class ScamShieldWhitelist {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ScamShieldWhitelist INSTANCE = new ScamShieldWhitelist();

    private final Set<String> whitelistedPlayers = ConcurrentHashMap.newKeySet();
    private final Path whitelistFile;

    private ScamShieldWhitelist() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        this.whitelistFile = gameDir.resolve("packcore/scamshield/whitelist.json");
        load();
    }

    public static ScamShieldWhitelist getInstance() {
        return INSTANCE;
    }

    /**
     * Check if a player is whitelisted.
     */
    public boolean isWhitelisted(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        return whitelistedPlayers.contains(playerName.toLowerCase());
    }

    /**
     * Add a player to the whitelist.
     */
    public boolean add(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }

        String normalized = playerName.toLowerCase();
        boolean added = whitelistedPlayers.add(normalized);

        if (added) {
            save();
            PackCore.LOGGER.info("[ScamShield] Added '{}' to whitelist", playerName);
        }

        return added;
    }

    /**
     * Remove a player from the whitelist.
     */
    public boolean remove(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }

        String normalized = playerName.toLowerCase();
        boolean removed = whitelistedPlayers.remove(normalized);

        if (removed) {
            save();
            PackCore.LOGGER.info("[ScamShield] Removed '{}' from whitelist", playerName);
        }

        return removed;
    }

    /**
     * Get all whitelisted players.
     */
    public Set<String> getWhitelistedPlayers() {
        return new HashSet<>(whitelistedPlayers);
    }

    /**
     * Clear the entire whitelist.
     */
    public void clear() {
        whitelistedPlayers.clear();
        save();
        PackCore.LOGGER.info("[ScamShield] Cleared whitelist");
    }

    private void load() {
        try {
            if (!Files.exists(whitelistFile)) {
                PackCore.LOGGER.info("[ScamShield] No whitelist file found, starting empty");
                return;
            }

            String json = Files.readString(whitelistFile, StandardCharsets.UTF_8);
            String[] players = GSON.fromJson(json, String[].class);

            if (players != null) {
                for (String player : players) {
                    whitelistedPlayers.add(player.toLowerCase());
                }
            }

            PackCore.LOGGER.info("[ScamShield] Loaded {} whitelisted players", whitelistedPlayers.size());
        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to load whitelist", e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(whitelistFile.getParent());

            String json = GSON.toJson(whitelistedPlayers.toArray(new String[0]));

            // Atomic write
            Path tempFile = whitelistFile.resolveSibling(whitelistFile.getFileName() + ".tmp");
            Files.writeString(tempFile, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tempFile, whitelistFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to save whitelist", e);
        }
    }
}