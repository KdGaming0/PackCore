package com.github.kd_gaming1.packcore.warning;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.repository.Pack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Warns the player in chat when a Caxton font resource pack and Iris shaders
 * are enabled at the same time — the two are incompatible and fonts will
 * render incorrectly.
 *
 * <p>Warning cadence starts at 5 minutes and increases to 45 minutes:
 * 5 → 10 → 15 → 20 → 25 → 30 → 35 → 40 → 45 (then repeats at 45).
 *
 * <p>The player can click [Ignore] in chat (runs {@code /packcore ignore-shader-warning})
 * to suppress warnings for the remainder of the session.
 */
public class CaxtonShaderConflictWarner {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/CaxtonShaderWarner");

    /**
     * The exact resource-pack IDs of the Caxton font packs.
     * The base {@code caxton} mod pack is NOT a font pack and should not trigger warnings.
     */
    private static final Set<String> CAXTON_FONT_PACK_IDS = Set.of(
            "caxton:inter",
            "caxton:opensans"
    );

    // ── Warning intervals (ascending, in milliseconds) ───────────────────────

    private static final long[] INTERVALS_MS = {
            5 * 60_000L, //  5 min
            10 * 60_000L, // 10 min
            15 * 60_000L, // 15 min
            20 * 60_000L, // 20 min
            25 * 60_000L, // 25 min
            30 * 60_000L, // 30 min
            35 * 60_000L, // 35 min
            40 * 60_000L, // 40 min
            45 * 60_000L  // 45 min (repeats)
    };

    // ── State ────────────────────────────────────────────────────────────────

    /** Epoch millis of the last warning sent (0 = never). */
    private static long lastWarnedAt = 0L;

    /** How many warnings have been sent since the conflict started. */
    private static int warningsSent = 0;

    /** Whether the conflict was present on the previous tick check. */
    private static boolean conflictOnLastCheck = false;

    /** User clicked [Ignore] — suppress for the rest of the session. */
    private static boolean ignored = false;

    // ── Public API ───────────────────────────────────────────────────────────

    /** Register the per-tick listener. Call once during mod init. */
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;
            tick();
        });
        LOGGER.info("CaxtonShaderConflictWarner initialised");
    }

    /** Suppress all warnings for the rest of this session. */
    public static void ignoreWarning() {
        ignored = true;
        LOGGER.info("User ignored Caxton/shader conflict warnings for this session");
    }

    /** Re-enable warnings (e.g. via command). */
    public static void enableWarnings() {
        ignored = false;
        reset();
        LOGGER.info("Caxton/shader conflict warnings re-enabled");
    }

    /** Whether the user has chosen to ignore warnings. */
    public static boolean isWarningIgnored() {
        return ignored;
    }

    /** Live check: is a Caxton font pack enabled at the same time as Iris shaders? */
    public static boolean hasConflict() {
        return isCaxtonFontEnabled() && isIrisShadersActive();
    }

    // ── Tick logic ───────────────────────────────────────────────────────────

    private static void tick() {
        boolean conflict = hasConflict();

        // Conflict state just changed
        if (conflict != conflictOnLastCheck) {
            conflictOnLastCheck = conflict;
            if (conflict) {
                reset();
                sendWarning();
            } else {
                reset();
            }
            return;
        }

        if (!conflict || ignored) return;

        long now = System.currentTimeMillis();
        if (now - lastWarnedAt >= currentInterval()) {
            sendWarning();
        }
    }

    // ── Warning message ─────────────────────────────────────────────────────

    private static void sendWarning() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String activeFontName = getActiveCaxtonFontName();

        Component message = Component.literal("")
                .append(Component.literal("⚠ ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("[PackCore] ").withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal("Caxton fonts and shaders are incompatible! ")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("Press ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("K").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" to disable shaders, or go to ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("ESC → Options → Resource Packs")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" and disable the ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(activeFontName)
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" pack. ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("[Ignore]").withStyle(
                        Style.EMPTY
                                .withColor(ChatFormatting.BLUE)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.RunCommand(
                                        "/packcore ignore-shader-warning"))));

        mc.player.displayClientMessage(message, false);

        lastWarnedAt = System.currentTimeMillis();
        warningsSent++;

        LOGGER.debug("Caxton/shader conflict warning #{} sent (next in {}ms)",
                warningsSent, currentInterval());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static long currentInterval() {
        int idx = Math.min(warningsSent, INTERVALS_MS.length - 1);
        return INTERVALS_MS[idx];
    }

    private static void reset() {
        warningsSent = 0;
        lastWarnedAt = 0L;
    }

    /**
     * Returns the display name of the currently active Caxton font pack
     * (e.g. "Inter" or "Open Sans"), or a generic fallback.
     */
    private static String getActiveCaxtonFontName() {
        try {
            for (Pack pack : Minecraft.getInstance().getResourcePackRepository().getSelectedPacks()) {
                String id = pack.getId();
                if (CAXTON_FONT_PACK_IDS.contains(id)) {
                    return switch (id) {
                        case "caxton:inter" -> "Inter";
                        case "caxton:opensans" -> "Open Sans";
                        default -> pack.getTitle().getString();
                    };
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error determining active Caxton font name", e);
        }
        return "Caxton font";
    }

    /**
     * True if any of the known Caxton font packs ({@code caxton:inter},
     * {@code caxton:opensans}) are currently selected.
     * Does NOT match the base {@code caxton} mod resource pack.
     */
    private static boolean isCaxtonFontEnabled() {
        if (!FabricLoader.getInstance().isModLoaded("caxton")) return false;
        try {
            for (Pack pack : Minecraft.getInstance().getResourcePackRepository().getSelectedPacks()) {
                if (CAXTON_FONT_PACK_IDS.contains(pack.getId())) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error checking Caxton font status", e);
        }
        return false;
    }

    /**
     * True if Iris shaders are currently active (a shader pipeline is in use).
     * Uses {@code Iris.isPackInUseQuick()} via reflection.
     */
    private static boolean isIrisShadersActive() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return false;
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            return (boolean) irisClass.getMethod("isPackInUseQuick").invoke(null);
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (Exception e) {
            LOGGER.debug("Error checking Iris shader status", e);
            return false;
        }
    }
}