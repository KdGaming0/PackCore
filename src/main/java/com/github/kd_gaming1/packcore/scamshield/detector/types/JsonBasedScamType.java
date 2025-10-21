package com.github.kd_gaming1.packcore.scamshield.detector.types;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.context.ConversationContext;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.detector.pattern.AhoCorasickMatcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * A ScamType that loads its detection logic from a JSON configuration file.
 *
 * This makes ScamTypes data-driven - you can add new patterns and adjust scores
 * without changing any code. Just edit the JSON file and reload.
 *
 * JSON Structure:
 * {
 *   "id": "unique_id",
 *   "name": "Display Name",
 *   "description": "What this detects",
 *   "base_score": 30,
 *   "pattern_groups": {
 *     "group_name": {
 *       "score": 20,
 *       "phrases": ["word1", "word2", ...]
 *     }
 *   },
 *   "combination_rules": [
 *     {
 *       "requires": ["group1", "group2"],
 *       "bonus": 50
 *     }
 *   ]
 * }
 */
public class JsonBasedScamType implements ScamType {
    private static final Gson GSON = new GsonBuilder().create();

    private final String configFileName;
    private boolean enabled = true;

    // Loaded from JSON
    private String id;
    private String displayName;
    private String description;
    private int baseScore;
    private Map<String, PatternGroup> patternGroups = new HashMap<>();
    private List<CombinationRule> combinationRules = new ArrayList<>();

    public JsonBasedScamType(String configFileName) {
        this.configFileName = configFileName;
        loadFromFile();
    }

    private void loadFromFile() {
        try {
            Path configFile = getConfigPath();
            ensureFileExists(configFile);
            String json = Files.readString(configFile, StandardCharsets.UTF_8);
            parseConfig(json);

            // Validate
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("ScamType config missing required 'id' field");
            }
            if (displayName == null || displayName.isEmpty()) {
                throw new IllegalStateException("ScamType config missing required 'name' field");
            }

            PackCore.LOGGER.info("[ScamShield] Loaded JSON-based ScamType: {} ({} pattern groups)",
                    displayName, patternGroups.size());

        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to read ScamType config file: {}", configFileName, e);
            initializeAsSafeDefaults();
        } catch (com.google.gson.JsonSyntaxException e) {
            PackCore.LOGGER.error("[ScamShield] Invalid JSON in ScamType config: {}", configFileName, e);
            initializeAsSafeDefaults();
        } catch (Exception e) {
            PackCore.LOGGER.error("[ScamShield] Unexpected error loading ScamType: {}", configFileName, e);
            initializeAsSafeDefaults();
        }
    }

    private void initializeAsSafeDefaults() {
        // Set safe defaults
        this.id = "disabled_" + configFileName;
        this.displayName = "Disabled: " + configFileName;
        this.description = "Failed to load configuration";
        this.baseScore = 0;
        this.enabled = false;
        this.patternGroups = new HashMap<>();
        this.combinationRules = new ArrayList<>();

        PackCore.LOGGER.warn("[ScamShield] ScamType '{}' has been disabled due to configuration errors", configFileName);
    }

    private void parseConfig(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);

        this.id = root.get("id").getAsString();
        this.displayName = root.get("name").getAsString();
        this.description = root.has("description") ? root.get("description").getAsString() : "";
        this.baseScore = root.has("base_score") ? root.get("base_score").getAsInt() : 0;

        if (root.has("pattern_groups")) {
            JsonObject groups = root.getAsJsonObject("pattern_groups");
            for (String groupName : groups.keySet()) {
                PatternGroup group = GSON.fromJson(groups.get(groupName), PatternGroup.class);
                patternGroups.put(groupName, group);
            }
        }

        if (root.has("combination_rules")) {
            CombinationRule[] rules = GSON.fromJson(
                    root.get("combination_rules"),
                    CombinationRule[].class
            );
            combinationRules = new ArrayList<>(Arrays.asList(rules));
        }
    }

    private void ensureFileExists(Path configFile) throws IOException {
        if (Files.exists(configFile)) {
            return;
        }

        Files.createDirectories(configFile.getParent());
        String resourcePath = "/assets/packcore/scamshield/" + configFileName;

        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.copy(in, configFile);
            PackCore.LOGGER.info("[ScamShield] Created config file from defaults: {}", configFileName);
        }
    }

    private Path getConfigPath() {
        return net.fabricmc.loader.api.FabricLoader.getInstance()
                .getGameDir()
                .resolve("packcore/scamshield/" + configFileName);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void analyze(String message, String rawMessage, String sender,
                        ConversationContext context, DetectionResult.Builder result) {
        if (!enabled || patternGroups.isEmpty()) {
            return;
        }

        Set<String> matchedGroups = new HashSet<>();
        Map<String, Integer> groupMatchCounts = new HashMap<>();
        int totalScore = 0;

        // Check each pattern group
        for (Map.Entry<String, PatternGroup> entry : patternGroups.entrySet()) {
            String groupName = entry.getKey();
            PatternGroup group = entry.getValue();

            int matchCount = group.countMatches(message);

            if (matchCount > 0) {
                matchedGroups.add(groupName);
                groupMatchCounts.put(groupName, matchCount);

                // Score scales with number of matches (multiple phrases = more suspicious)
                // But with diminishing returns to prevent one group dominating
                int groupScore = group.score;
                if (matchCount > 1) {
                    // +50% for 2nd match, +25% for 3rd, etc. Caps at 2x base score
                    double multiplier = 1.0 + (1.0 - Math.pow(0.5, matchCount - 1));
                    multiplier = Math.min(multiplier, 2.0);
                    groupScore = (int) (groupScore * multiplier);
                }
                totalScore += groupScore;

                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.debug("[ScamShield]   Pattern group '{}' matched {} times: +{} points",
                            groupName, matchCount, groupScore);
                }
            }
        }

        if (matchedGroups.isEmpty()) {
            return;
        }

        // Check combination rules
        for (CombinationRule rule : combinationRules) {
            if (matchedGroups.containsAll(rule.requires)) {
                totalScore += rule.bonus;

                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.debug("[ScamShield]   Combination rule triggered: {} = +{} bonus",
                            rule.requires, rule.bonus);
                }
            }
        }

        // Apply context sensitivity
        if (totalScore > 0) {
            double contextMultiplier = context.getSensitivityMultiplier(getId());
            totalScore = (int) (totalScore * contextMultiplier);

            result.addScamTypeContribution(getId(), totalScore);

            if (PackCoreConfig.enableScamShieldDebugging) {
                PackCore.LOGGER.debug("[ScamShield] {} detected: +{} points (groups: {}, context: {}x)",
                        displayName, totalScore, matchedGroups, contextMultiplier);
            }
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
        PackCore.LOGGER.info("[ScamShield] Reloading {}", displayName);
        loadFromFile();
    }

    /**
     * A group of related phrases with a score.
     */
    private static class PatternGroup {
        private String description;
        private int score;
        private List<String> phrases;
        private transient AhoCorasickMatcher matcher;

        public boolean matches(String message) {
            ensureMatcherBuilt();
            return !matcher.findMatches(message).isEmpty();
        }

        public int countMatches(String message) {
            ensureMatcherBuilt();
            return matcher.findMatches(message).size();
        }

        private void ensureMatcherBuilt() {
            if (matcher == null) {
                matcher = new AhoCorasickMatcher();
                if (phrases != null) {
                    for (String phrase : phrases) {
                        matcher.addPattern(phrase);
                    }
                }
            }
        }
    }

    /**
     * A rule that gives bonus score when multiple pattern groups match together.
     * Example: Discord + verification = bonus points
     */
    private static class CombinationRule {
        private String description;
        private List<String> requires;
        private int bonus;
    }
}