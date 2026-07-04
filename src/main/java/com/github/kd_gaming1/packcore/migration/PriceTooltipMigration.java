package com.github.kd_gaming1.packcore.migration;

import com.github.kd_gaming1.packcore.PackCore;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Moves item price tooltips from Skyblock Enhancements to Skyblocker so the two mods no longer stack
 * duplicate price lines: enables Skyblocker's NPC / AvgBIN / LowestBIN / Bazaar price lines and
 * disables Skyblock Enhancements' own price tooltips. Best-effort per mod — a missing mod is skipped.
 *
 * <p>Both edits mirror config-write patterns already used in this mod: Skyblocker via
 * {@code SkyblockerConfigManager.update(Consumer)} reflection (which persists on its own), and the
 * MidnightConfig-based Skyblock Enhancements via a static field write + {@code MidnightConfig.write}.
 */
final class PriceTooltipMigration {

    private static final String SKYBLOCKER_MOD_ID = "skyblocker";
    private static final String SBE_MOD_ID = "skyblock_enhancements";
    private static final String SBE_CONFIG_CLASS =
            "com.github.kd_gaming1.skyblockenhancements.config.SkyblockEnhancementsConfig";

    private PriceTooltipMigration() {}

    static void apply() {
        enableSkyblockerPrices();
        disableSkyblockEnhancementsPrices();
    }

    /** Sets {@code general.itemTooltip.{enableNPCPrice,enableAvgBIN,enableLowestBIN,enableBazaarPrice}} true. */
    private static void enableSkyblockerPrices() {
        if (!FabricLoader.getInstance().isModLoaded(SKYBLOCKER_MOD_ID)) {
            PackCore.LOGGER.info("[Migration] Skyblocker not loaded, skipping price-tooltip enable");
            return;
        }
        try {
            Class<?> configManager = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
            Method update = configManager.getDeclaredMethod("update", Consumer.class);
            update.setAccessible(true);
            update.invoke(null, (Consumer<Object>) config -> {
                try {
                    Object itemTooltip = getField(getField(config, "general"), "itemTooltip");
                    setBoolean(itemTooltip, "enableNPCPrice", true);
                    setBoolean(itemTooltip, "enableAvgBIN", true);
                    setBoolean(itemTooltip, "enableLowestBIN", true);
                    setBoolean(itemTooltip, "enableBazaarPrice", true);
                } catch (Exception e) {
                    PackCore.LOGGER.warn("[Migration] Skyblocker: failed to enable price tooltips", e);
                }
            });
            PackCore.LOGGER.info("[Migration] Skyblocker: NPC/AvgBIN/LowestBIN/Bazaar price tooltips enabled");
        } catch (Exception e) {
            PackCore.LOGGER.warn("[Migration] Skyblocker: price-tooltip update failed", e);
        }
    }

    /** Sets the static {@code enablePriceTooltips} field false and persists via MidnightConfig. */
    private static void disableSkyblockEnhancementsPrices() {
        if (!FabricLoader.getInstance().isModLoaded(SBE_MOD_ID)) {
            PackCore.LOGGER.info("[Migration] Skyblock Enhancements not loaded, skipping price-tooltip disable");
            return;
        }
        try {
            Class<?> configClass = Class.forName(SBE_CONFIG_CLASS);
            Field field = configClass.getDeclaredField("enablePriceTooltips");
            field.setAccessible(true);
            field.setBoolean(null, false);
            MidnightConfig.write(SBE_MOD_ID);
            PackCore.LOGGER.info("[Migration] Skyblock Enhancements: enablePriceTooltips disabled");
        } catch (Exception e) {
            PackCore.LOGGER.warn("[Migration] Skyblock Enhancements: failed to disable price tooltips", e);
        }
    }

    private static Object getField(Object instance, String name) throws Exception {
        Field f = instance.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(instance);
    }

    private static void setBoolean(Object instance, String name, boolean value) throws Exception {
        Field f = instance.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setBoolean(instance, value);
    }
}
