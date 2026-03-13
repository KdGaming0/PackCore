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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ScamScreenerConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ScamScreenerConfigurator");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final Path PRIMARY_RUNTIME_PATH = CONFIG_DIR.resolve("scamscreener").resolve("runtime.json");
    private static final Path LEGACY_RUNTIME_PATH = CONFIG_DIR.resolve("runtime.json");

    private static final String DEFAULT_MINIMUM_RISK_LEVEL = "MEDIUM";
    private static final boolean DEFAULT_PING_ON_RISK_WARNING = true;
    private static final boolean DEFAULT_PING_ON_BLACKLIST_WARNING = true;
    private static final List<String> DEFAULT_MUTE_PATTERNS = List.of();

    private ScamScreenerConfigurator() {}

    public record RuntimeSettings(
            String minimumRiskLevel,
            Set<String> mutePatterns,
            boolean pingOnRiskWarning,
            boolean pingOnBlacklistWarning
    ) {}

    public static RuntimeSettings loadSettings() {
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
                new LinkedHashSet<>(DEFAULT_MUTE_PATTERNS),
                DEFAULT_PING_ON_RISK_WARNING,
                DEFAULT_PING_ON_BLACKLIST_WARNING
        );
    }

    public static boolean apply(String minimumRiskLevel, Set<String> mutePatterns,
                                boolean pingOnRiskWarning, boolean pingOnBlacklistWarning) {
        Path targetPath = resolveWritePath();

        try {
            JsonObject root = loadOrCreateConfig(targetPath);
            applySettings(root, minimumRiskLevel, mutePatterns, pingOnRiskWarning, pingOnBlacklistWarning);

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
        JsonObject safety = getOrCreateObject(root, "safety");
        JsonObject output = getOrCreateObject(root, "output");

        String minimumRiskLevel = getString(alerts, "minimumRiskLevel", DEFAULT_MINIMUM_RISK_LEVEL);
        LinkedHashSet<String> mutePatterns = new LinkedHashSet<>();

        if (safety.has("mutePatterns") && safety.get("mutePatterns").isJsonArray()) {
            for (JsonElement element : safety.getAsJsonArray("mutePatterns")) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    mutePatterns.add(element.getAsString());
                }
            }
        }

        boolean pingOnRiskWarning = getBoolean(output, "pingOnRiskWarning", DEFAULT_PING_ON_RISK_WARNING);
        boolean pingOnBlacklistWarning = getBoolean(output, "pingOnBlacklistWarning", DEFAULT_PING_ON_BLACKLIST_WARNING);

        return new RuntimeSettings(minimumRiskLevel, mutePatterns, pingOnRiskWarning, pingOnBlacklistWarning);
    }

    private static void applySettings(JsonObject root, String minimumRiskLevel, Set<String> mutePatterns,
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
