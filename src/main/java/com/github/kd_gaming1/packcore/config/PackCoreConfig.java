package com.github.kd_gaming1.packcore.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {

    public static final String MENU = "menu";
    public static final String BACKUP = "backup";
    public static final String TOAST = "toast";
    public static final String META = "meta";

    // ── Toast ─────────────────────────────────────────────────────────────────

    @Entry(category = TOAST)
    public static boolean showRamWarningToast = true;

    @Entry(category = TOAST)
    public static boolean showUpdateToast = true;

    @Entry(category = TOAST)
    public static boolean showBackupToast = true;

    // ── Menu ──────────────────────────────────────────────────────────────────

    @Entry(category = MENU)
    public static MenuStyle menuStyle = MenuStyle.MODERN_MINIMAL;

    @Entry(category = MENU)
    public static String serverAddressForQuickJoinButton = "mc.hypixel.net";

    public enum MenuStyle {
        MODERN,
        MODERN_MINIMAL,
        MINIMAL
    }

    // ── Backup ────────────────────────────────────────────────────────────────

    @Entry(category = BACKUP)
    public static boolean autoBackupEnabled = true;

    @Entry(category = BACKUP, min = 1, max = 90)
    public static int autoBackupIntervalDays = 3;

    // ── Meta (hidden) ─────────────────────────────────────────────────────────

    @Hidden
    @Entry(category = META)
    public static String lastAppliedVersion = "";

    @Hidden
    @Entry(category = META)
    public static String lastAppliedPackFile = "";

    @Hidden
    @Entry(category = META)
    public static String pendingConfigPack = "";

    @Hidden
    @Entry(category = META)
    public static String pendingRestoreBackup = "";

    @Hidden
    @Entry(category = META)
    public static String pendingConfigPackFiles = "";

    @Hidden
    @Entry(category = META)
    public static String pendingRestoreBackupFiles = "";

    @Hidden
    @Entry(category = META)
    public static String lastSeenModpackVersion = "";

    @Hidden
    @Entry(category = META)
    public static long lastBackupEpochMs = 0L;

    @Hidden
    @Entry(category = META)
    public static long lastSeenEpochMs = 0L;

    @Hidden
    @Entry(category = META)
    public static boolean successfulWelcomeWizard = false;

    @Hidden
    @Entry(category = META)
    public static boolean isFirstStartup = true;

    @Hidden
    @Entry(category = META)
    public static boolean modernUIDefaultsEnforced = false;

    @Hidden
    @Entry(category = META)
    public static boolean modernUIScreenDefaultsEnforced = false;
}