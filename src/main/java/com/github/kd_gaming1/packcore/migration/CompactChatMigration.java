package com.github.kd_gaming1.packcore.migration;

import com.github.kd_gaming1.packcore.PackCore;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;

/**
 * Turns Enhanced Chat's duplicate-message compaction off, so updating players match the modpack's
 * shipped default instead of keeping the mod's own {@code true} default. Best-effort — a missing
 * Enhanced Chat is skipped.
 *
 * <p>Enhanced Chat is MidnightConfig-based, so this is a static field write followed by
 * {@code MidnightConfig.write}, the same pattern {@link PriceTooltipMigration} uses for Skyblock
 * Enhancements. That rewrites Enhanced Chat's whole config file from its in-memory state, which is
 * safe here: {@link ConfigMigrationRunner} runs at {@code CLIENT_STARTED}, by which point the file
 * has already been read in.
 */
final class CompactChatMigration {

    private static final String MOD_ID = "enhanced_chat";
    private static final String CONFIG_CLASS =
            "com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig";

    private CompactChatMigration() {}

    /** Sets the static {@code compactDuplicateMessages} field false and persists via MidnightConfig. */
    static void apply() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            PackCore.LOGGER.info("[Migration] Enhanced Chat not loaded, skipping compact-chat disable");
            return;
        }
        try {
            Class<?> configClass = Class.forName(CONFIG_CLASS);
            Field field = configClass.getDeclaredField("compactDuplicateMessages");
            field.setAccessible(true);
            field.setBoolean(null, false);
            MidnightConfig.write(MOD_ID);
            PackCore.LOGGER.info("[Migration] Enhanced Chat: compactDuplicateMessages disabled");
        } catch (Exception e) {
            PackCore.LOGGER.warn("[Migration] Enhanced Chat: failed to disable compactDuplicateMessages", e);
        }
    }
}
