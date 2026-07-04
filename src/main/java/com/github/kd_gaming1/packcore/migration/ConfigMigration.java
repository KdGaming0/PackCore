package com.github.kd_gaming1.packcore.migration;

/**
 * A one-shot forced config change, identified by a stable {@code id} and run exactly once for
 * players who update the modpack. See {@link ConfigMigrationRunner} for how and when it runs.
 *
 * @param id     stable, never-reused identifier recorded once the migration has been applied
 * @param action the change to perform; must be best-effort (a missing target mod is skipped, not fatal)
 */
public record ConfigMigration(String id, Runnable action) {}
