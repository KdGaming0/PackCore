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
 * <p>When the custom font is disabled, the original font values are backed up to
 * {@code packcore_font_backup.properties} so they can be restored if the user re-enables it.
 */
public class ModernUIConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ModernUIConfigurator");

    private static final Path CONFIG_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("ModernUI");
    private static final Path CLIENT_TOML = CONFIG_DIR.resolve("client.toml");
    private static final Path BOOTSTRAP = CONFIG_DIR.resolve("bootstrap.properties");
    /** Stores original font values before we clear them, so they can be restored later. */
    private static final Path FONT_BACKUP = CONFIG_DIR.resolve("packcore_font_backup.properties");

    private static final String KEY_DISABLE_TEXT_ENGINE = "modernui_mc_disableTextEngine";
    private static final String BACKUP_KEY_FIRST = "firstFontFamily";
    private static final String BACKUP_KEY_FALLBACK = "fallbackFontFamilyList";

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
        ok &= patchToml("tooltip", "enable", bool(enabledFeatures.contains("fancyTooltip")));
        ok &= patchToml("general", "ding", bool(enabledFeatures.contains("dingSound")));

        if (!ok) {
            throw new RuntimeException(
                    "One or more Modern UI settings could not be written -- check the log for details.");
        }
    }

    // ── State readers (used by ModernUIPage to seed defaults) ─────────────────

    /**
     * Returns true if the ModernUI text engine is currently enabled.
     * Reads bootstrap.properties directly -- reliable and avoids reflection issues.
     */
    public static boolean isTextEngineEnabled() {
        try {
            if (!Files.exists(BOOTSTRAP)) return true;
            Properties props = new Properties();
            try (Reader r = Files.newBufferedReader(BOOTSTRAP)) {
                props.load(r);
            }
            return !"true".equalsIgnoreCase(props.getProperty(KEY_DISABLE_TEXT_ENGINE, "false"));
        } catch (IOException e) {
            LOGGER.warn("Could not read bootstrap.properties: {}", e.getMessage());
            return true;
        }
    }

    /** Returns true if {@code tooltip.enable} is set in client.toml. */
    public static boolean isTooltipEnabled() {
        return "true".equals(readTomlValue("tooltip", "enable"));
    }

    /** Returns true if {@code general.ding} is set in client.toml. */
    public static boolean isDingEnabled() {
        return "true".equals(readTomlValue("general", "ding"));
    }

    // ── Feature application ───────────────────────────────────────────────────

    private static boolean applyCustomFont(boolean enable) {
        if (enable) {
            // Re-enable: turn engine back on and restore backed-up font values if available.
            boolean ok = applyBootstrap(false);
            ok &= restoreFontBackup();
            return ok;
        } else {
            // Disable: back up current values first, then clear them.
            backupFontValues();
            boolean ok = applyBootstrap(true);
            ok &= patchToml("font", "firstFontFamily", str(""));
            ok &= patchTomlList("font", "fallbackFontFamilyList", "[]");
            return ok;
        }
    }

    // ── Font backup / restore ─────────────────────────────────────────────────

    /**
     * Saves the current {@code firstFontFamily} and {@code fallbackFontFamilyList} values to
     * {@code packcore_font_backup.properties} before we clear them. Skips if a backup already
     * exists (we never overwrite an existing backup with empty values).
     */
    private static void backupFontValues() {
        // Don't overwrite an existing backup -- if the user disabled and re-enabled and
        // disabled again, the backup from the first disable is still the "real" original.
        if (Files.exists(FONT_BACKUP)) return;

        String first = readTomlValue("font", "firstFontFamily");
        String fallback = readTomlRawList("font", "fallbackFontFamilyList");

        if (first == null && fallback == null) return;

        try {
            Properties props = new Properties();
            if (first != null) props.setProperty(BACKUP_KEY_FIRST, first);
            if (fallback != null) props.setProperty(BACKUP_KEY_FALLBACK, fallback);
            Files.createDirectories(FONT_BACKUP.getParent());
            try (Writer w = Files.newBufferedWriter(FONT_BACKUP)) {
                props.store(w, "PackCore font backup -- do not edit manually");
            }
            LOGGER.info("ModernUI font values backed up to {}", FONT_BACKUP.getFileName());
        } catch (IOException e) {
            LOGGER.warn("Could not back up font values: {}", e.getMessage());
        }
    }

    /**
     * Restores {@code firstFontFamily} and {@code fallbackFontFamilyList} from the backup file,
     * then deletes the backup so the next disable cycle creates a fresh one.
     */
    private static boolean restoreFontBackup() {
        if (!Files.exists(FONT_BACKUP)) {
            LOGGER.info("No font backup found -- font fields left as-is");
            return true;
        }
        try {
            Properties props = new Properties();
            try (Reader r = Files.newBufferedReader(FONT_BACKUP)) {
                props.load(r);
            }

            boolean ok = true;
            String first = props.getProperty(BACKUP_KEY_FIRST);
            String fallback = props.getProperty(BACKUP_KEY_FALLBACK);

            if (first != null) {
                ok &= patchToml("font", "firstFontFamily", str(first));
            }
            if (fallback != null) {
                // fallback is stored as the raw TOML list value, e.g. ["Foo", "Bar"]
                ok &= patchTomlList("font", "fallbackFontFamilyList", fallback);
            }

            if (ok) {
                Files.delete(FONT_BACKUP);
                LOGGER.info("ModernUI font values restored from backup");
            }
            return ok;
        } catch (IOException e) {
            LOGGER.error("Could not restore font backup: {}", e.getMessage(), e);
            return false;
        }
    }

    // ── bootstrap.properties ──────────────────────────────────────────────────

    private static boolean applyBootstrap(boolean disableEngine) {
        try {
            Properties props = new Properties();
            if (Files.exists(BOOTSTRAP)) {
                try (Reader r = Files.newBufferedReader(BOOTSTRAP)) {
                    props.load(r);
                }
            }
            props.setProperty(KEY_DISABLE_TEXT_ENGINE, disableEngine ? "true" : "false");
            Files.createDirectories(BOOTSTRAP.getParent());
            try (Writer w = Files.newBufferedWriter(BOOTSTRAP)) {
                props.store(w, "ModernUI Bootstrap -- managed by PackCore wizard");
            }
            LOGGER.info("ModernUI bootstrap: disableTextEngine={}", disableEngine);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to write bootstrap.properties: {}", e.getMessage(), e);
            return false;
        }
    }

    // ── client.toml patching ──────────────────────────────────────────────────

    /** Patches a scalar {@code key = value} inside the given TOML section. */
    private static boolean patchToml(String section, String key, String value) {
        if (!Files.exists(CLIENT_TOML)) {
            LOGGER.warn("client.toml not found; skipping [{}.{}]", section, key);
            return true;
        }
        try {
            List<String> lines = Files.readAllLines(CLIENT_TOML);
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
                LOGGER.warn("Key '{}' not found in [{}]; skipping", key, section);
                return true;
            }

            Files.write(CLIENT_TOML, out);
            LOGGER.info("ModernUI client.toml patched: [{}.{}] = {}", section, key, value);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to patch client.toml [{}.{}]: {}", section, key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Patches a TOML list value, handling both single-line and multi-line arrays.
     * Replaces everything from {@code key = [} to the closing {@code ]} with {@code newValue}.
     */
    private static boolean patchTomlList(String section, String key, String newValue) {
        if (!Files.exists(CLIENT_TOML)) {
            LOGGER.warn("client.toml not found; skipping [{}.{}]", section, key);
            return true;
        }
        try {
            List<String> lines = Files.readAllLines(CLIENT_TOML);
            List<String> out = new ArrayList<>(lines.size());
            String header = "[" + section + "]";
            boolean inSection = false;
            boolean inList = false;
            boolean replaced = false;

            for (String line : lines) {
                String trimmed = line.trim();

                if (!inList && trimmed.startsWith("[")) inSection = trimmed.equals(header);

                if (inSection && !replaced && !inList && trimmed.startsWith(key + " =")) {
                    String indent = line.substring(0, line.length() - line.stripLeading().length());
                    out.add(indent + key + " = " + newValue);
                    replaced = true;
                    if (!trimmed.contains("]")) inList = true; // multi-line, skip until close
                    continue;
                }

                if (inList) {
                    if (trimmed.contains("]")) inList = false;
                    continue;
                }

                out.add(line);
            }

            if (!replaced) {
                LOGGER.warn("Key '{}' not found in [{}]; skipping", key, section);
                return true;
            }

            Files.write(CLIENT_TOML, out);
            LOGGER.info("ModernUI client.toml patched: [{}.{}] = {}", section, key, newValue);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to patch client.toml [{}.{}]: {}", section, key, e.getMessage(), e);
            return false;
        }
    }

    // ── TOML readers ──────────────────────────────────────────────────────────

    /**
     * Reads a scalar string value from client.toml, stripping surrounding quotes.
     * Returns null if missing.
     */
    private static String readTomlValue(String section, String key) {
        if (!Files.exists(CLIENT_TOML)) return null;
        try {
            String header = "[" + section + "]";
            boolean inSection = false;
            for (String line : Files.readAllLines(CLIENT_TOML)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("[")) { inSection = trimmed.equals(header); continue; }
                if (inSection && trimmed.startsWith(key + " =")) {
                    String raw = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
                        raw = raw.substring(1, raw.length() - 1);
                    }
                    return raw;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read [{}.{}] from client.toml: {}", section, key, e.getMessage());
        }
        return null;
    }

    /**
     * Reads a TOML list value (possibly multi-line) and returns it as a single-line string
     * suitable for storing in the backup and for re-patching, e.g. {@code ["Foo", "Bar"]}.
     * Returns null if missing.
     */
    private static String readTomlRawList(String section, String key) {
        if (!Files.exists(CLIENT_TOML)) return null;
        try {
            List<String> lines = Files.readAllLines(CLIENT_TOML);
            String header = "[" + section + "]";
            boolean inSection = false;
            boolean inList = false;
            StringBuilder collected = new StringBuilder();

            for (String line : lines) {
                String trimmed = line.trim();

                if (!inList && trimmed.startsWith("[")) {
                    inSection = trimmed.equals(header);
                    continue;
                }

                if (inSection && !inList && trimmed.startsWith(key + " =")) {
                    // Grab everything after the '='
                    String rest = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                    collected.append(rest);
                    if (rest.contains("]")) break; // single-line list
                    inList = true;
                    continue;
                }

                if (inList) {
                    collected.append(" ").append(trimmed);
                    if (trimmed.contains("]")) break;
                }
            }

            String result = collected.toString().trim();
            return result.isEmpty() ? null : result;
        } catch (IOException e) {
            LOGGER.warn("Could not read list [{}.{}] from client.toml: {}", section, key, e.getMessage());
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String bool(boolean v) { return v ? "true" : "false"; }
    private static String str(String v) { return "\"" + v + "\""; }
}