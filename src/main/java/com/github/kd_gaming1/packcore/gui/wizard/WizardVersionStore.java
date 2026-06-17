package com.github.kd_gaming1.packcore.gui.wizard;

import com.github.kd_gaming1.packcore.PackCore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the version of each wizard step a user has applied, persisted to
 * {@code packcore/wizard.json} as a {@code {stepId: version}} map. This is the single source of
 * truth for whether the wizard opens on startup.
 *
 * <p>A step is "pending" (should be shown) when its id is absent or its stored version is lower than
 * the step's current {@link WizardStep#version()}. An empty store means a brand-new user (every page
 * pending → full wizard); a populated store means only newly added or version-bumped pages show.
 */
public final class WizardVersionStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/WizardVersionStore");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final Path FILE = PackCore.PACKCORE_DIR.resolve("wizard.json");

    private final Map<String, Integer> appliedVersions;

    private WizardVersionStore(Map<String, Integer> appliedVersions) {
        this.appliedVersions = appliedVersions;
    }

    /** Whether the store file exists — i.e. the user has been through the wizard at least once. */
    public static boolean fileExists() {
        return Files.exists(FILE);
    }

    /** Loads the store, returning an empty one if the file is missing or unreadable. */
    public static WizardVersionStore load() {
        if (!Files.exists(FILE)) {
            return new WizardVersionStore(new HashMap<>());
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Map<String, Integer> map = GSON.fromJson(reader, MAP_TYPE);
            return new WizardVersionStore(map != null ? map : new HashMap<>());
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Failed to read wizard.json, treating as empty", e);
            return new WizardVersionStore(new HashMap<>());
        }
    }

    /** True when nothing has been applied yet — a brand-new user who should see the full wizard. */
    public boolean isEmpty() {
        return appliedVersions.isEmpty();
    }

    /** A step is pending when it has never been applied, or was applied at an older version. */
    public boolean isPending(WizardStep step) {
        return appliedVersions.getOrDefault(step.id(), 0) < step.version();
    }

    /** Records the given steps as applied at their current version and persists the store. */
    public void markApplied(Collection<WizardStep> steps) {
        for (WizardStep step : steps) {
            appliedVersions.put(step.id(), step.version());
        }
        save();
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(appliedVersions, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write wizard.json", e);
        }
    }
}
