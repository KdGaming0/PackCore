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
 * All other changes are picked up live via ModernUI's file watcher.
 *
 * <p>To toggle between vanilla and custom fonts, we switch {@code text.defaultFontBehavior}
 * between {@code ONLY_INCLUDE} (vanilla -- includes the vanilla ASCII providers) and
 * {@code ONLY_EXCLUDE} (custom -- uses the Modern UI typeface list with exclusions).
 * This avoids touching the font family fields, so user-configured fonts are never lost.
 *
 * <p>ModernUI splits its config across two files: {@code client.toml} (screen, tooltip, general,
 * font sections) and {@code text.toml} (text engine section). {@code defaultFontBehavior} lives in
 * {@code text.toml}.
 */
public class ModernUIConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ModernUIConfigurator");

    private static final Path CONFIG_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("ModernUI");
    private static final Path CLIENT_TOML = CONFIG_DIR.resolve("client.toml");
    private static final Path TEXT_TOML = CONFIG_DIR.resolve("text.toml");

    private static final String FONT_BEHAVIOR_VANILLA = "ONLY_INCLUDE";
    private static final String FONT_BEHAVIOR_CUSTOM = "KEEP_OTHER";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Applies all feature toggles. Throws {@link RuntimeException} on write failure so the wizard
     * surfaces a red row.
     *
     * <p>Features: {@code customFont}, {@code fancyTooltip}, {@code dingSound}.
     */
    public static void apply(Set<String> enabledFeatures) {
        boolean ok = true;

        ok &= applyCustomFont(enabledFeatures.contains("customFont"));
        ok &= patchToml(CLIENT_TOML, "tooltip", "enable", bool(enabledFeatures.contains("fancyTooltip")), false);
        ok &= patchToml(CLIENT_TOML, "general", "ding", bool(enabledFeatures.contains("dingSound")), false);

        if (!ok) {
            throw new RuntimeException(
                    "One or more Modern UI settings could not be written -- check the log for details.");
        }
    }

    // ── State readers ─────────────────────────────────────────────────────────

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
     * Switches between vanilla and custom font rendering by toggling
     * {@code text.defaultFontBehavior} in {@code text.toml}.
     *
     * <ul>
     *   <li>Custom: {@code KEEP_OTHER}
     *   <li>Vanilla: {@code ONLY_INCLUDE}
     * </ul>
     *
     * <p>The Modern Text Engine is left untouched -- PackCore never modifies that setting.
     */
    private static boolean applyCustomFont(boolean enable) {
        return patchToml(
                TEXT_TOML,
                "text",
                "defaultFontBehavior",
                str(enable ? FONT_BEHAVIOR_CUSTOM : FONT_BEHAVIOR_VANILLA),
                true /* required -- missing key is a real error */);
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