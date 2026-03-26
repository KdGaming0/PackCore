package com.github.kd_gaming1.packcore.integration;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Reads and writes ModernUI config files to reflect the user's wizard selections.
 *
 * <p>Font changes require a restart -- ModernUI states "only read once when the game is loaded".
 * Text engine changes also require a restart; the pipeline is bootstrapped before the game loads.
 * All other changes are picked up live via ModernUI's file watcher.
 *
 * <p>Config files used:
 * <ul>
 *   <li>{@code config/ModernUI/client.toml} -- tooltip, general (ding), font sections
 *   <li>{@code config/ModernUI/text.toml} -- text rendering (defaultFontBehavior)
 *   <li>{@code config/ModernUI/bootstrap.properties} -- pre-launch flags including text engine toggle
 * </ul>
 *
 * <p>The text engine toggle is {@code modernui_mc_disableTextEngine} in bootstrap.properties.
 * The key is inverted: {@code false} = engine ON, {@code true} = engine OFF.
 *
 * <p>To toggle fonts, we switch {@code text.defaultFontBehavior} between {@code ONLY_INCLUDE}
 * (vanilla) and {@code KEEP_OTHER} (custom). Font family fields are never touched.
 */
public class ModernUIConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ModernUIConfigurator");

    private static final Path CONFIG_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("ModernUI");
    private static final Path CLIENT_TOML = CONFIG_DIR.resolve("client.toml");
    private static final Path TEXT_TOML = CONFIG_DIR.resolve("text.toml");
    private static final Path BOOTSTRAP = CONFIG_DIR.resolve("bootstrap.properties");

    private static final String FONT_BEHAVIOR_VANILLA = "ONLY_INCLUDE";
    private static final String FONT_BEHAVIOR_CUSTOM = "KEEP_OTHER";
    private static final String TEXT_ENGINE_DISABLE_KEY = "modernui_mc_disableTextEngine";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Applies all feature toggles. Throws {@link RuntimeException} on write failure so the wizard
     * surfaces a red row.
     *
     * <p>Features: {@code textEngine}, {@code customFont}, {@code fancyTooltip}, {@code dingSound}.
     */
    public static void apply(Set<String> enabledFeatures) {
        boolean ok = true;

        ok &= applyTextEngine(enabledFeatures.contains("textEngine"));
        ok &= applyCustomFont(enabledFeatures.contains("customFont"));
        ok &= patchToml(CLIENT_TOML, "tooltip", "enable", bool(enabledFeatures.contains("fancyTooltip")), false);
        ok &= patchToml(CLIENT_TOML, "general", "ding", bool(enabledFeatures.contains("dingSound")), false);

        if (!ok) {
            throw new RuntimeException(
                    "One or more Modern UI settings could not be written -- check the log for details.");
        }
    }

    // ── State readers ─────────────────────────────────────────────────────────

    /**
     * Returns true if the Modern Text Engine is currently enabled.
     * Reads {@code modernui_mc_disableTextEngine} from bootstrap.properties.
     * Defaults to {@code true} (engine on) if the key or file is missing.
     */
    public static boolean isTextEngineEnabled() {
        String val = readBootstrapValue(TEXT_ENGINE_DISABLE_KEY);
        // Key absent on fresh install = engine on by default
        return !"true".equals(val);
    }

    /** Returns true if the custom font is currently active (defaultFontBehavior != ONLY_INCLUDE). */
    public static boolean isCustomFontEnabled() {
        String behavior = readTomlValue(TEXT_TOML, "text", "defaultFontBehavior");
        // Fresh install default is KEEP_OTHER (custom on), so treat unknown/missing as enabled.
        return !FONT_BEHAVIOR_VANILLA.equals(behavior);
    }

    /** Returns true if {@code tooltip.enable} is set to true in client.toml. */
    public static boolean isTooltipEnabled() {
        return "true".equals(readTomlValue(CLIENT_TOML, "tooltip", "enable"));
    }

    /** Returns true if {@code general.ding} is set to true in client.toml. */
    public static boolean isDingEnabled() {
        return "true".equals(readTomlValue(CLIENT_TOML, "general", "ding"));
    }

    // ── Feature application ───────────────────────────────────────────────────

    /**
     * Toggles the Modern Text Engine by writing {@code modernui_mc_disableTextEngine}
     * in bootstrap.properties. The key is inverted: engine ON = {@code false}.
     */
    private static boolean applyTextEngine(boolean enable) {
        return patchBootstrap(bool(!enable));
    }

    /**
     * Switches between vanilla and custom font rendering by toggling
     * {@code text.defaultFontBehavior} in {@code text.toml}.
     *
     * <ul>
     *   <li>Custom: {@code KEEP_OTHER}
     *   <li>Vanilla: {@code ONLY_INCLUDE}
     * </ul>
     */
    private static boolean applyCustomFont(boolean enable) {
        return patchToml(
                TEXT_TOML,
                "text",
                "defaultFontBehavior",
                str(enable ? FONT_BEHAVIOR_CUSTOM : FONT_BEHAVIOR_VANILLA),
                true /* required -- missing key is a real error */);
    }

    // ── Bootstrap properties patching ─────────────────────────────────────────

    /**
     * Reads a single value from bootstrap.properties (flat key=value, no sections).
     * Returns null if the file or key does not exist.
     */
    private static String readBootstrapValue(String key) {
        if (!Files.exists(BOOTSTRAP)) return null;
        try {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(BOOTSTRAP)) {
                props.load(in);
            }
            return props.getProperty(key);
        } catch (IOException e) {
            LOGGER.warn("Could not read bootstrap.properties key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Patches a single key in bootstrap.properties, preserving all other lines and comments.
     * If the key does not exist, it is appended at the end of the file.
     *
     * <p>We patch line-by-line (rather than Properties.store) to preserve the comment
     * header and any other keys ModernUI has written.
     */
    private static boolean patchBootstrap(String value) {
        if (!Files.exists(BOOTSTRAP)) {
            // Create the file with just this key -- ModernUI will fill the rest on next launch.
            try {
                Files.createDirectories(BOOTSTRAP.getParent());
                Files.writeString(BOOTSTRAP, "#Modern UI bootstrap file\n" + ModernUIConfigurator.TEXT_ENGINE_DISABLE_KEY + "=" + value + "\n");
                LOGGER.info("Created bootstrap.properties with {}={}", ModernUIConfigurator.TEXT_ENGINE_DISABLE_KEY, value);
                return true;
            } catch (IOException e) {
                LOGGER.error("Failed to create bootstrap.properties: {}", e.getMessage(), e);
                return false;
            }
        }

        try {
            List<String> lines = Files.readAllLines(BOOTSTRAP);
            List<String> out = new ArrayList<>(lines.size() + 1);
            boolean replaced = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("#") && trimmed.startsWith(ModernUIConfigurator.TEXT_ENGINE_DISABLE_KEY + "=")) {
                    out.add(ModernUIConfigurator.TEXT_ENGINE_DISABLE_KEY + "=" + value);
                    replaced = true;
                } else {
                    out.add(line);
                }
            }

            if (!replaced) {
                out.add(ModernUIConfigurator.TEXT_ENGINE_DISABLE_KEY + "=" + value);
            }

            Files.write(BOOTSTRAP, out);
            LOGGER.info("bootstrap.properties patched: {}={}", ModernUIConfigurator.TEXT_ENGINE_DISABLE_KEY, value);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to patch bootstrap.properties [{}]: {}", ModernUIConfigurator.TEXT_ENGINE_DISABLE_KEY, e.getMessage(), e);
            return false;
        }
    }

    // ── TOML patching ─────────────────────────────────────────────────────────

    /**
     * Patches a scalar {@code key = value} inside the given TOML section.
     *
     * @param required if true, a missing key is treated as an error (returns false); if false, a
     *     missing key is silently skipped (returns true).
     */
    private static boolean patchToml(
            Path file, String section, String key, String value, boolean required) {
        if (!Files.exists(file)) {
            if (required) {
                LOGGER.error("{} not found; cannot patch [{}.{}]", file.getFileName(), section, key);
                return false;
            }
            LOGGER.warn("{} not found; skipping [{}.{}]", file.getFileName(), section, key);
            return true;
        }
        try {
            List<String> lines = Files.readAllLines(file);
            List<String> out = new ArrayList<>(lines.size());
            String header = "[" + section + "]";
            boolean inSection = false;
            boolean replaced = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("[")) inSection = trimmed.equals(header);
                if (inSection && !replaced && trimmed.startsWith(key + " =")) {
                    String indent = line.substring(0, line.length() - line.stripLeading().length());
                    out.add(indent + key + " = " + value);
                    replaced = true;
                } else {
                    out.add(line);
                }
            }

            if (!replaced) {
                if (required) {
                    LOGGER.error(
                            "Required key '{}' not found in [{}] of {}; cannot apply change",
                            key, section, file.getFileName());
                    return false;
                }
                LOGGER.warn("Key '{}' not found in [{}] of {}; skipping", key, section, file.getFileName());
                return true;
            }

            Files.write(file, out);
            LOGGER.info("ModernUI {} patched: [{}.{}] = {}", file.getFileName(), section, key, value);
            return true;
        } catch (IOException e) {
            LOGGER.error(
                    "Failed to patch {} [{}.{}]: {}", file.getFileName(), section, key, e.getMessage(), e);
            return false;
        }
    }

    // ── TOML readers ──────────────────────────────────────────────────────────

    /** Reads a scalar string value from the given TOML file, stripping surrounding quotes. */
    private static String readTomlValue(Path file, String section, String key) {
        if (!Files.exists(file)) return null;
        try {
            String header = "[" + section + "]";
            boolean inSection = false;
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("[")) {
                    inSection = trimmed.equals(header);
                    continue;
                }
                if (inSection && trimmed.startsWith(key + " =")) {
                    String raw = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
                        raw = raw.substring(1, raw.length() - 1);
                    }
                    return raw;
                }
            }
        } catch (IOException e) {
            LOGGER.warn(
                    "Could not read [{}.{}] from {}: {}",
                    section, key, file.getFileName(), e.getMessage());
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String bool(boolean v) {
        return v ? "true" : "false";
    }

    private static String str(String v) {
        return "\"" + v + "\"";
    }
}