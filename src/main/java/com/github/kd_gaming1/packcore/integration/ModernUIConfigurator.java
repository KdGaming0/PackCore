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
 *
 * <ul>
 *   <li>{@code config/ModernUI/client.toml} -- tooltip, general (ding)
 *   <li>{@code config/ModernUI/text.toml} -- defaultFontBehavior, allowShadow
 *   <li>{@code config/ModernUI/bootstrap.properties} -- modernui_mc_disableTextEngine
 * </ul>
 *
 * <p>Font mode behaviour:
 *
 * <ul>
 *   <li>{@code INTER} -- engine ON, sets {@code defaultFontBehavior = KEEP_OTHER}
 *   <li>{@code VANILLA} -- engine OFF, {@code text.toml} is left untouched
 * </ul>
 */
public class ModernUIConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ModernUIConfigurator");

    private static final Path CONFIG_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("ModernUI");
    private static final Path CLIENT_TOML = CONFIG_DIR.resolve("client.toml");
    private static final Path TEXT_TOML   = CONFIG_DIR.resolve("text.toml");
    private static final Path BOOTSTRAP   = CONFIG_DIR.resolve("bootstrap.properties");

    private static final String TEXT_ENGINE_DISABLE_KEY = "modernui_mc_disableTextEngine";

    // ── Font mode ─────────────────────────────────────────────────────────────

    public enum FontMode {
        INTER("inter"),
        VANILLA("vanilla");

        private final String id;

        FontMode(String id) { this.id = id; }

        public String id() { return id; }

        public static FontMode fromId(String id) {
            for (FontMode m : values()) {
                if (m.id.equals(id)) return m;
            }
            return INTER;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Applies all Modern UI wizard selections.
     *
     * <ul>
     *   <li>{@code INTER} -- turns the text engine on and sets {@code defaultFontBehavior = KEEP_OTHER}
     *   <li>{@code VANILLA} -- turns the text engine off; {@code text.toml} is not modified
     * </ul>
     *
     * @param enabledFeatures IDs from {@code modernuiFeatures} multi-select (fancyTooltip, dingSound)
     * @param fontModeId      ID from {@code modernuiFontMode} single-select (inter, vanilla)
     */
    public static void apply(Set<String> enabledFeatures, String fontModeId) {
        FontMode mode = FontMode.fromId(fontModeId);
        boolean ok = true;

        ok &= patchBootstrap(bool(mode != FontMode.INTER)); // disable engine unless INTER
        if (mode == FontMode.INTER) {
            ok &= patchToml(TEXT_TOML, "text", "defaultFontBehavior", str("KEEP_OTHER"), true);
        }
        ok &= patchToml(CLIENT_TOML, "tooltip", "enable", bool(enabledFeatures.contains("fancyTooltip")), false);
        ok &= patchToml(CLIENT_TOML, "general",  "ding",   bool(enabledFeatures.contains("dingSound")),    false);

        if (!ok) {
            throw new RuntimeException(
                    "One or more Modern UI settings could not be written -- check the log for details.");
        }
    }

    /**
     * Enforces modpack-standard defaults. Safe to call unconditionally; silently skips if
     * ModernUI config files are absent.
     */
    public static void enforceModpackDefaults() {
        // Text shadows create readability issues with the modpack's custom font at small sizes.
        if (!patchToml(TEXT_TOML, "text", "allowShadow", "false", false)) {
            LOGGER.warn("Could not enforce ModernUI modpack defaults -- check log above for details.");
        }
    }

    /**
     * Enforces modpack-standard screen defaults. Safe to call unconditionally; silently skips if
     * ModernUI config files are absent.
     */
    public static void enforceModpackScreenDefaults() {
        boolean ok = patchToml(CLIENT_TOML, "screen", "animationDuration",   "0",     false);
        ok        &= patchToml(CLIENT_TOML, "screen", "overrideVanillaBlur", "false", false);
        if (!ok) {
            LOGGER.warn("Could not enforce ModernUI screen defaults -- check log above for details.");
        }
    }

    // ── State readers ─────────────────────────────────────────────────────────

    /**
     * Returns the current {@link FontMode} by checking whether the text engine is enabled in
     * {@code bootstrap.properties}. Defaults to {@code INTER} if the file or key is missing
     * (engine on by default).
     */
    public static FontMode currentFontMode() {
        String val = readBootstrapValue();
        // engine disabled = "true" → VANILLA; anything else → INTER
        return "true".equals(val) ? FontMode.VANILLA : FontMode.INTER;
    }

    /** Returns {@code true} if {@code tooltip.enable} is set to {@code true} in client.toml. */
    public static boolean isTooltipEnabled() {
        return "true".equals(readTomlValue(CLIENT_TOML, "tooltip", "enable"));
    }

    /** Returns {@code true} if {@code general.ding} is set to {@code true} in client.toml. */
    public static boolean isDingEnabled() {
        return "true".equals(readTomlValue(CLIENT_TOML, "general", "ding"));
    }

    // ── Bootstrap properties ──────────────────────────────────────────────────

    private static String readBootstrapValue() {
        if (!Files.exists(BOOTSTRAP)) return null;
        try {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(BOOTSTRAP)) {
                props.load(in);
            }
            return props.getProperty(TEXT_ENGINE_DISABLE_KEY);
        } catch (IOException e) {
            LOGGER.warn("Could not read bootstrap.properties key '{}': {}", TEXT_ENGINE_DISABLE_KEY, e.getMessage());
            return null;
        }
    }

    /**
     * Patches a key in bootstrap.properties, preserving all other lines and comments.
     * Appends the key if it doesn't exist. Creates the file if missing.
     */
    private static boolean patchBootstrap(String value) {
        if (!Files.exists(BOOTSTRAP)) {
            try {
                Files.createDirectories(BOOTSTRAP.getParent());
                Files.writeString(BOOTSTRAP,
                        "#Modern UI bootstrap file\n" + TEXT_ENGINE_DISABLE_KEY + "=" + value + "\n");
                LOGGER.info("Created bootstrap.properties with {}={}", TEXT_ENGINE_DISABLE_KEY, value);
                return true;
            } catch (IOException e) {
                LOGGER.error("Failed to create bootstrap.properties: {}", e.getMessage(), e);
                return false;
            }
        }
        try {
            List<String> lines = Files.readAllLines(BOOTSTRAP);
            List<String> out   = new ArrayList<>(lines.size() + 1);
            boolean replaced   = false;
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("#") && trimmed.startsWith(TEXT_ENGINE_DISABLE_KEY + "=")) {
                    out.add(TEXT_ENGINE_DISABLE_KEY + "=" + value);
                    replaced = true;
                } else {
                    out.add(line);
                }
            }
            if (!replaced) out.add(TEXT_ENGINE_DISABLE_KEY + "=" + value);
            Files.write(BOOTSTRAP, out);
            LOGGER.info("bootstrap.properties patched: {}={}", TEXT_ENGINE_DISABLE_KEY, value);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to patch bootstrap.properties [{}]: {}", TEXT_ENGINE_DISABLE_KEY, e.getMessage(), e);
            return false;
        }
    }

    // ── TOML patching ─────────────────────────────────────────────────────────

    private static boolean patchToml(Path file, String section, String key, String value, boolean required) {
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
            List<String> out   = new ArrayList<>(lines.size());
            String header      = "[" + section + "]";
            boolean inSection  = false;
            boolean replaced   = false;
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
                    LOGGER.error("Required key '{}' not found in [{}] of {}", key, section, file.getFileName());
                    return false;
                }
                LOGGER.warn("Key '{}' not found in [{}] of {}; skipping", key, section, file.getFileName());
                return true;
            }
            Files.write(file, out);
            LOGGER.info("ModernUI {} patched: [{}.{}] = {}", file.getFileName(), section, key, value);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to patch {} [{}.{}]: {}", file.getFileName(), section, key, e.getMessage(), e);
            return false;
        }
    }

    // ── TOML readers ──────────────────────────────────────────────────────────

    private static String readTomlValue(Path file, String section, String key) {
        if (!Files.exists(file)) return null;
        try {
            String header     = "[" + section + "]";
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
            LOGGER.warn("Could not read [{}.{}] from {}: {}", section, key, file.getFileName(), e.getMessage());
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String bool(boolean v) { return v ? "true" : "false"; }
    private static String str(String v)   { return "\"" + v + "\""; }
}