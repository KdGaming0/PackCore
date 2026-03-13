package com.github.kd_gaming1.packcore.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ScamScreenerConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ScamScreenerConfigurator");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final Path PRIMARY_RUNTIME_PATH = CONFIG_DIR.resolve("scamscreener").resolve("runtime.json");
    private static final Path LEGACY_RUNTIME_PATH = CONFIG_DIR.resolve("runtime.json");

    private static final String DEFAULT_MINIMUM_RISK_LEVEL = "MEDIUM";
    private static final boolean DEFAULT_PING_ON_RISK_WARNING = true;
    private static final boolean DEFAULT_PING_ON_BLACKLIST_WARNING = true;
    private static final List<String> DEFAULT_ALERT_LEVELS = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final List<String> DEFAULT_MUTE_PATTERNS = List.of();

    private ScamScreenerConfigurator() {}

    public record RuntimeSettings(
            String minimumRiskLevel,
            boolean pingOnRiskWarning,
            boolean pingOnBlacklistWarning
    ) {}

    public static RuntimeSettings loadSettings() {
        if (FabricLoader.getInstance().isModLoaded("scamscreener")) {
            try {
                if (ScamScreenerApiBridge.isAvailable()) {
                    return ScamScreenerApiBridge.loadSettings();
                }
            } catch (RuntimeException | LinkageError e) {
                LOGGER.warn("Failed to load ScamScreener settings from API, falling back to runtime.json: {}", e.getMessage());
            }
        }

        return loadSettingsFromJson();
    }

    public static List<String> availableAlertLevels() {
        if (FabricLoader.getInstance().isModLoaded("scamscreener")) {
            try {
                if (ScamScreenerApiBridge.isAvailable()) {
                    return ScamScreenerApiBridge.availableAlertLevels();
                }
            } catch (RuntimeException | LinkageError e) {
                LOGGER.warn("Failed to read ScamScreener alert levels from API, falling back to defaults: {}", e.getMessage());
            }
        }

        return DEFAULT_ALERT_LEVELS;
    }

    private static RuntimeSettings loadSettingsFromJson() {
        Path path = resolveExistingPath();
        if (path == null) {
            return defaultSettings();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return settingsFromJson(root);
        } catch (Exception e) {
            LOGGER.warn("Failed to read ScamScreener runtime config '{}': {}", path, e.getMessage());
            return defaultSettings();
        }
    }

    public static RuntimeSettings defaultSettings() {
        return new RuntimeSettings(
                DEFAULT_MINIMUM_RISK_LEVEL,
                DEFAULT_PING_ON_RISK_WARNING,
                DEFAULT_PING_ON_BLACKLIST_WARNING
        );
    }

    public static boolean apply(String minimumRiskLevel, boolean pingOnRiskWarning, boolean pingOnBlacklistWarning) {
        if (FabricLoader.getInstance().isModLoaded("scamscreener")) {
            try {
                if (ScamScreenerApiBridge.isAvailable()) {
                    return ScamScreenerApiBridge.apply(minimumRiskLevel, pingOnRiskWarning, pingOnBlacklistWarning);
                }
            } catch (RuntimeException | LinkageError e) {
                LOGGER.warn("Failed to apply ScamScreener settings through API, falling back to runtime.json: {}", e.getMessage());
            }
        }

        return applyViaJson(minimumRiskLevel, pingOnRiskWarning, pingOnBlacklistWarning);
    }

    private static boolean applyViaJson(String minimumRiskLevel, boolean pingOnRiskWarning, boolean pingOnBlacklistWarning) {
        Path targetPath = resolveWritePath();

        try {
            JsonObject root = loadOrCreateConfig(targetPath);
            applySettings(root, minimumRiskLevel, readMutePatterns(root), pingOnRiskWarning, pingOnBlacklistWarning);

            Files.createDirectories(targetPath.getParent());
            try (Writer writer = Files.newBufferedWriter(targetPath)) {
                GSON.toJson(root, writer);
            }

            LOGGER.info("Updated ScamScreener runtime config at '{}'", targetPath);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to write ScamScreener runtime config: {}", e.getMessage(), e);
            return false;
        }
    }

    private static Path resolveExistingPath() {
        if (Files.exists(PRIMARY_RUNTIME_PATH)) return PRIMARY_RUNTIME_PATH;
        if (Files.exists(LEGACY_RUNTIME_PATH)) return LEGACY_RUNTIME_PATH;
        return null;
    }

    private static Path resolveWritePath() {
        Path existing = resolveExistingPath();
        return existing != null ? existing : PRIMARY_RUNTIME_PATH;
    }

    private static JsonObject loadOrCreateConfig(Path targetPath) throws IOException {
        if (!Files.exists(targetPath)) {
            return createDefaultConfig();
        }

        try (Reader reader = Files.newBufferedReader(targetPath)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (Exception e) {
            LOGGER.warn("Existing ScamScreener config is invalid, recreating '{}': {}", targetPath, e.getMessage());
        }

        return createDefaultConfig();
    }

    private static RuntimeSettings settingsFromJson(JsonObject root) {
        JsonObject alerts = getOrCreateObject(root, "alerts");
        JsonObject output = getOrCreateObject(root, "output");

        String minimumRiskLevel = getString(alerts, "minimumRiskLevel", DEFAULT_MINIMUM_RISK_LEVEL);
        boolean pingOnRiskWarning = getBoolean(output, "pingOnRiskWarning", DEFAULT_PING_ON_RISK_WARNING);
        boolean pingOnBlacklistWarning = getBoolean(output, "pingOnBlacklistWarning", DEFAULT_PING_ON_BLACKLIST_WARNING);

        return new RuntimeSettings(minimumRiskLevel, pingOnRiskWarning, pingOnBlacklistWarning);
    }

    private static List<String> readMutePatterns(JsonObject root) {
        JsonObject safety = getOrCreateObject(root, "safety");
        if (!safety.has("mutePatterns") || !safety.get("mutePatterns").isJsonArray()) {
            return DEFAULT_MUTE_PATTERNS;
        }

        return safety.getAsJsonArray("mutePatterns").asList().stream()
                .filter(JsonElement::isJsonPrimitive)
                .filter(element -> element.getAsJsonPrimitive().isString())
                .map(JsonElement::getAsString)
                .toList();
    }

    private static void applySettings(JsonObject root, String minimumRiskLevel, List<String> mutePatterns,
                                      boolean pingOnRiskWarning, boolean pingOnBlacklistWarning) {
        JsonObject alerts = getOrCreateObject(root, "alerts");
        JsonObject safety = getOrCreateObject(root, "safety");
        JsonObject output = getOrCreateObject(root, "output");

        alerts.addProperty("minimumRiskLevel", minimumRiskLevel);

        JsonArray mutePatternsJson = new JsonArray();
        for (String pattern : mutePatterns) {
            mutePatternsJson.add(pattern);
        }

        safety.addProperty("muteFilterEnabled", !mutePatterns.isEmpty());
        safety.add("mutePatterns", mutePatternsJson);
        output.addProperty("pingOnRiskWarning", pingOnRiskWarning);
        output.addProperty("pingOnBlacklistWarning", pingOnBlacklistWarning);
    }

    private static JsonObject createDefaultConfig() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 3);
        root.addProperty("enabled", true);

        JsonObject pipeline = new JsonObject();
        pipeline.addProperty("reviewThreshold", 1);
        root.add("pipeline", pipeline);

        JsonObject alerts = new JsonObject();
        alerts.addProperty("minimumRiskLevel", DEFAULT_MINIMUM_RISK_LEVEL);
        alerts.addProperty("autoCaptureLevel", "MEDIUM");
        root.add("alerts", alerts);

        JsonObject output = new JsonObject();
        output.addProperty("showRiskWarningMessage", true);
        output.addProperty("pingOnRiskWarning", DEFAULT_PING_ON_RISK_WARNING);
        output.addProperty("showBlacklistWarningMessage", true);
        output.addProperty("pingOnBlacklistWarning", DEFAULT_PING_ON_BLACKLIST_WARNING);
        output.addProperty("showAutoLeaveMessage", true);
        output.addProperty("debugLogging", true);
        root.add("output", output);

        JsonObject review = new JsonObject();
        review.addProperty("captureEnabled", true);
        review.addProperty("maxEntries", 200);
        root.add("review", review);

        JsonObject safety = new JsonObject();
        safety.addProperty("autoLeaveOnBlacklist", false);
        safety.addProperty("muteFilterEnabled", false);
        JsonArray mutePatterns = new JsonArray();
        for (String pattern : DEFAULT_MUTE_PATTERNS) {
            mutePatterns.add(pattern);
        }
        safety.add("mutePatterns", mutePatterns);
        root.add("safety", safety);

        JsonObject profiler = new JsonObject();
        profiler.addProperty("hudEnabled", false);
        root.add("profiler", profiler);

        JsonObject debug = new JsonObject();
        debug.add("flags", new JsonObject());
        root.add("debug", debug);

        return root;
    }

    private static JsonObject getOrCreateObject(JsonObject parent, String key) {
        if (parent.has(key) && parent.get(key).isJsonObject()) {
            return parent.getAsJsonObject(key);
        }

        JsonObject child = new JsonObject();
        parent.add(key, child);
        return child;
    }

    private static String getString(JsonObject json, String key, String fallback) {
        if (json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isString()) {
            return json.get(key).getAsString();
        }
        return fallback;
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        if (json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isBoolean()) {
            return json.get(key).getAsBoolean();
        }
        return fallback;
    }
}
