package com.github.kd_gaming1.packcore.migration;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Carries the modpack's Sound Controller volumes over to Enhanced Sound Control when the modpack
 * swaps one mod for the other, so an update does not silently reset the tuned sound setup.
 *
 * <p>Sound Controller stores {@code { "version": 4, "sounds": [ { "soundId", "volume" } ] }} with
 * volume as a {@code 0.0-1.0} float (1.0 = untouched). Enhanced Sound Control stores
 * {@code { "sounds": { "ns:path": { "volume", "frequency" } } }} with volume as an integer percent
 * (100 = untouched), so the conversion is {@code round(volume * 100)} with frequency left at its
 * default. Per-island overrides have no Sound Controller equivalent and are not written.
 *
 * <p>An existing tweaks file is merged into rather than left alone: imported sounds overwrite the
 * {@code volume} of a conflicting entry, every other entry and root key (notably {@code islandNames})
 * is carried over untouched, and a conflicting entry keeps its {@code frequency} — Sound Controller
 * has no frequency concept, so overwriting it would destroy a setting rather than migrate one.
 *
 * <p><b>Why this is not a {@link ConfigMigration}.</b> {@link ConfigMigrationRunner} runs at
 * {@code CLIENT_STARTED}, which is after Enhanced Sound Control's client initializer has already
 * read its tweaks file. A file written that late would be loaded by nothing and then overwritten by
 * Enhanced Sound Control's own (empty) in-memory store the first time it flushes — which happens as
 * soon as it learns a SkyBlock island name. So this runs at pre-launch instead, and guards itself
 * with {@link PackCoreConfig#soundControllerImported} rather than {@code appliedConfigMigrations}.
 */
public final class SoundControllerImport {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/SoundControllerImport");

    private static final String MOD_ID = "packcore";
    private static final String OLD_MOD_ID = "soundcontroller";
    private static final String NEW_MOD_ID = "enhancedsoundcontrol";
    private static final String OLD_FILE = "soundcontroller.json";
    private static final String NEW_FILE = "enhancedsoundcontrol.tweaks.json";

    /** Enhanced Sound Control's untouched values; entries matching these carry no information. */
    private static final int DEFAULT_VOLUME = 100;
    private static final int DEFAULT_FREQUENCY = 100;
    private static final int MAX_VOLUME = 200;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SoundControllerImport() {}

    /**
     * Converts the old config once, if all of the following hold: Enhanced Sound Control is present,
     * Sound Controller is not (both installed would stack their attenuation), the import has not run
     * before and the old file exists. An existing tweaks file does not stand the import down — it is
     * merged into instead.
     */
    public static void runIfNeeded(Path gameDir) {
        if (PackCoreConfig.soundControllerImported) return;

        FabricLoader loader = FabricLoader.getInstance();
        if (!loader.isModLoaded(NEW_MOD_ID)) return;
        if (loader.isModLoaded(OLD_MOD_ID)) {
            LOGGER.info("Sound Controller is still installed, skipping import to avoid stacked volume changes");
            return;
        }

        Path configDir = gameDir.resolve("config");
        Path source = configDir.resolve(OLD_FILE);
        Path target = configDir.resolve(NEW_FILE);
        if (!Files.isRegularFile(source)) return;

        Map<String, Integer> volumes;
        try (Reader reader = Files.newBufferedReader(source)) {
            volumes = read(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception e) {
            LOGGER.warn("Could not read {}, leaving Enhanced Sound Control at its defaults", OLD_FILE, e);
            return;
        }

        try {
            write(target, volumes);
        } catch (Exception e) {
            LOGGER.warn("Could not write {}, leaving Enhanced Sound Control at its defaults", NEW_FILE, e);
            return;
        }

        PackCoreConfig.soundControllerImported = true;
        MidnightConfig.write(MOD_ID);
        LOGGER.info("Imported {} sound volume(s) from Sound Controller into Enhanced Sound Control", volumes.size());
    }

    /**
     * Reads either the v4 shape (a {@code sounds} array of {@code { soundId, volume }}) or the
     * pre-v4 shape (a flat {@code { "ns:path": volume }} object at the root), which Sound Controller
     * still accepts. Sorted so the written file has a stable order.
     */
    private static Map<String, Integer> read(JsonObject root) {
        Map<String, Integer> volumes = new TreeMap<>();
        if (root.has("sounds") && root.get("sounds").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("sounds")) {
                JsonObject entry = element.getAsJsonObject();
                accept(volumes, entry.get("soundId").getAsString(), entry.get("volume").getAsFloat());
            }
            return volumes;
        }
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) continue;
            if ("version".equals(entry.getKey()) || "subtitlesEnabled".equals(entry.getKey())) continue;
            accept(volumes, entry.getKey(), entry.getValue().getAsFloat());
        }
        return volumes;
    }

    /**
     * Records one converted sound. Entries that are malformed, or that sit at the default volume and
     * so carry nothing over, are dropped rather than failing the whole import.
     */
    private static void accept(Map<String, Integer> volumes, String soundId, float volume) {
        if (soundId == null || soundId.isBlank()) return;
        int percent = Math.max(0, Math.min(MAX_VOLUME, Math.round(volume * 100f)));
        if (percent == DEFAULT_VOLUME) return;
        volumes.put(soundId, percent);
    }

    /**
     * Writes Enhanced Sound Control's format, merged on top of whatever is already at {@code target}.
     * When nothing is there, {@code islandNames} is omitted, which its reader tolerates — it omits the
     * key itself when no island names have been learned.
     */
    private static void write(Path target, Map<String, Integer> volumes) throws IOException {
        JsonObject root = readExisting(target);
        JsonObject sounds = root.has("sounds") && root.get("sounds").isJsonObject()
                ? root.getAsJsonObject("sounds")
                : new JsonObject();

        volumes.forEach((soundId, percent) -> {
            JsonObject existing = sounds.has(soundId) && sounds.get(soundId).isJsonObject()
                    ? sounds.getAsJsonObject(soundId)
                    : null;
            JsonObject tweak = new JsonObject();
            tweak.addProperty("volume", percent);
            // A conflicting entry keeps its frequency: Sound Controller never had one to migrate.
            tweak.addProperty("frequency", frequencyOf(existing));
            sounds.add(soundId, tweak);
        });
        root.add("sounds", sounds);

        // The config directory necessarily exists: the source file was just read from it.
        try (Writer writer = Files.newBufferedWriter(target)) {
            GSON.toJson(root, writer);
        }
    }

    /**
     * Returns the tweaks file already on disk so its untouched sounds and root keys survive the
     * merge, or an empty object when there is none. An unreadable file is reported and replaced —
     * Enhanced Sound Control would not have loaded it either.
     */
    private static JsonObject readExisting(Path target) {
        if (!Files.isRegularFile(target)) return new JsonObject();
        try (Reader reader = Files.newBufferedReader(target)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed.isJsonObject()) return parsed.getAsJsonObject();
        } catch (Exception e) {
            LOGGER.warn("Could not read existing {}, writing a fresh one", NEW_FILE, e);
            return new JsonObject();
        }
        LOGGER.warn("Existing {} was not a JSON object, writing a fresh one", NEW_FILE);
        return new JsonObject();
    }

    private static int frequencyOf(JsonObject existing) {
        if (existing == null || !existing.has("frequency")) return DEFAULT_FREQUENCY;
        try {
            return existing.get("frequency").getAsInt();
        } catch (RuntimeException e) {
            return DEFAULT_FREQUENCY;
        }
    }
}
