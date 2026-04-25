package com.github.kd_gaming1.packcore.warning;

import com.github.kd_gaming1.packcore.util.CaxtonFontDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Warns the player in chat when a Caxton font pack and Iris shaders are both
 * active — the two combinations render fonts incorrectly.
 *
 * <p>Warning cadence: 5, 10, 15, ... 45 minutes (caps at 45). The player can
 * click {@code [Ignore]} to suppress warnings for the rest of the session.
 */
public final class CaxtonShaderConflictWarner {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/CaxtonShaderWarner");

    private static final long FIRST_INTERVAL_MIN = 5L;
    private static final long INTERVAL_STEP_MIN = 5L;
    private static final long MAX_INTERVAL_MIN = 45L;
    private static final long ONE_MINUTE_MS = 60_000L;
    private static final int CHECK_INTERVAL_TICKS = 20;

    private static long lastWarnedAt = 0L;
    private static int warningsSent = 0;
    private static boolean conflictOnLastCheck = false;
    private static boolean ignored = false;

    private CaxtonShaderConflictWarner() {}

    /** Register the per-tick listener. Call once during mod init. */
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && client.player != null) {
                tick();
            }
        });
        LOGGER.info("Initialised");
    }

    /** Suppress warnings for the rest of this session. */
    public static void ignoreWarning() {
        ignored = true;
        LOGGER.info("Warnings suppressed for this session");
    }

    /** Re-enable warnings and reset the cadence. */
    public static void enableWarnings() {
        ignored = false;
        reset();
        LOGGER.info("Warnings re-enabled");
    }

    public static boolean isWarningIgnored() {
        return ignored;
    }

    public static boolean hasConflict() {
        return CaxtonFontDetector.isActive() && IrisProbe.isPackActive();
    }

    private static void tick() {
        boolean conflict = hasConflict();

        if (conflict != conflictOnLastCheck) {
            conflictOnLastCheck = conflict;
            reset();
            if (conflict) sendWarning();
            return;
        }

        if (!conflict || ignored) return;

        if (System.currentTimeMillis() - lastWarnedAt >= currentIntervalMs()) {
            sendWarning();
        }
    }

    private static void sendWarning() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

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
                .append(Component.literal(" and disable the active Caxton font pack.")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(Component.literal("[Ignore]").withStyle(
                        Style.EMPTY
                                .withColor(ChatFormatting.BLUE)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.RunCommand(
                                        "/packcore ignore-shader-warning"))));

        mc.player.displayClientMessage(message, false);

        lastWarnedAt = System.currentTimeMillis();
        warningsSent++;
        LOGGER.debug("Warning #{} sent (next in {} ms)", warningsSent, currentIntervalMs());
    }

    /** 5, 10, 15, ... 45 minutes — capped. */
    private static long currentIntervalMs() {
        long minutes = Math.min(
                FIRST_INTERVAL_MIN + (long) warningsSent * INTERVAL_STEP_MIN,
                MAX_INTERVAL_MIN);
        return minutes * ONE_MINUTE_MS;
    }

    private static void reset() {
        warningsSent = 0;
        lastWarnedAt = 0L;
    }

    // ---------------------------------------------------------------------
    // Iris probe — reflection handle resolved once.
    // ---------------------------------------------------------------------

    private static final class IrisProbe {

        private static volatile boolean resolved = false;
        private static volatile MethodHandle isPackInUseQuick = null;
        private static volatile boolean unavailable = false;

        static boolean isPackActive() {
            if (unavailable) return false;
            if (!FabricLoader.getInstance().isModLoaded("iris")) {
                unavailable = true;
                return false;
            }
            if (!resolved) {
                synchronized (IrisProbe.class) {
                    if (!resolved) resolve();
                }
            }
            MethodHandle mh = isPackInUseQuick;
            if (mh == null) return false;
            try {
                return (boolean) mh.invokeExact();
            } catch (Throwable t) {
                LOGGER.debug("Iris.isPackInUseQuick threw, assuming inactive", t);
                return false;
            }
        }

        private static void resolve() {
            try {
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
                isPackInUseQuick = MethodHandles.publicLookup()
                        .findStatic(iris, "isPackInUseQuick",
                                MethodType.methodType(boolean.class));
            } catch (ClassNotFoundException e) {
                unavailable = true;
            } catch (Throwable t) {
                LOGGER.debug("Failed to resolve Iris probe handle", t);
                unavailable = true;
            }
            resolved = true;
        }
    }
}