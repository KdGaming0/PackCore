package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.util.ToastHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows a low-RAM toast once on the main menu and once on the first world join,
 * if the JVM max heap is under {@value RAM_THRESHOLD_GB}GB.
 */
public final class RamWarningHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/RamWarning");

    private static final long RAM_THRESHOLD_BYTES = 3L * 1024 * 1024 * 1024;
    private static final int  RAM_THRESHOLD_GB = 3;

    private static boolean lowRam = false;
    private static boolean shownMainMenu = false;
    private static boolean shownInGame = false;

    private RamWarningHelper() {}

    /** Call once in onInitializeClient to evaluate RAM. */
    public static void init() {
        long maxRam = Runtime.getRuntime().maxMemory();
        if (maxRam < RAM_THRESHOLD_BYTES) {
            lowRam = true;
            LOGGER.warn("Low RAM detected: {}MB allocated ({}GB+ recommended)", maxRam / (1024 * 1024), RAM_THRESHOLD_GB);
        }
    }

    /** Call when the main menu is first shown. */
    public static void onMainMenu() {
        if (lowRam && !shownMainMenu && PackCoreConfig.showRamWarningToast) {
            shownMainMenu = true;
            ToastHelper.showLowRam();
        }
    }

    /** Call when the player first joins a world or server. */
    public static void onWorldJoin() {
        if (lowRam && !shownInGame && PackCoreConfig.showRamWarningToast) {
            shownInGame = true;
            ToastHelper.showLowRam();
        }
    }
}