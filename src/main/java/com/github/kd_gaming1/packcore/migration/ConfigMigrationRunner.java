package com.github.kd_gaming1.packcore.migration;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.PackCorePreLaunch;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import eu.midnightdust.lib.config.MidnightConfig;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applies one-shot forced config changes exactly once for players who <em>update</em> the modpack.
 *
 * <p><b>New installs are skipped.</b> The shipped default configs already carry the intended values,
 * so there is nothing to change. "New vs updating" is decided by
 * {@link PackCorePreLaunch#getPreviousModpackVersion()}: it holds the {@code lastSeenModpackVersion}
 * captured before this launch overwrote it, and a blank value means this is a first launch.
 *
 * <p>Each migration is recorded by id in {@link PackCoreConfig#appliedConfigMigrations} after its
 * single attempt and never runs again — so a player who later changes one of these settings back is
 * not overridden on the next launch. This applied-set is the sole "run once" guard, which is why a
 * migration is marked applied even if its target mod was absent or the change threw: retrying every
 * launch would re-fight the player's own later choices.
 *
 * <p>Runs at {@code CLIENT_STARTED}, by which point every mod's config has been initialized.
 */
public final class ConfigMigrationRunner {

    private static final List<ConfigMigration> MIGRATIONS = List.of(
            new ConfigMigration("prices-skyblocker-over-sbe", PriceTooltipMigration::apply),
            new ConfigMigration("enhanced-chat-compact-off", CompactChatMigration::apply)
    );

    private ConfigMigrationRunner() {}

    public static void run() {
        String previous = PackCorePreLaunch.getPreviousModpackVersion();
        boolean newUser = previous == null || previous.isBlank();

        Set<String> applied = parse(PackCoreConfig.appliedConfigMigrations);
        boolean changed = false;

        for (ConfigMigration migration : MIGRATIONS) {
            if (applied.contains(migration.id())) continue;

            if (newUser) {
                PackCore.LOGGER.info("[Migration] New install — baselining '{}' as applied without running",
                        migration.id());
            } else {
                PackCore.LOGGER.info("[Migration] Modpack updated — applying one-shot config migration '{}'",
                        migration.id());
                try {
                    migration.action().run();
                } catch (Exception e) {
                    PackCore.LOGGER.warn("[Migration] '{}' failed; marking applied so it is not retried",
                            migration.id(), e);
                }
            }
            applied.add(migration.id());
            changed = true;
        }

        if (changed) {
            PackCoreConfig.appliedConfigMigrations = String.join(",", applied);
            MidnightConfig.write(PackCore.MOD_ID);
        }
    }

    private static Set<String> parse(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashSet<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
