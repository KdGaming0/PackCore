package com.github.kd_gaming1.packcore.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {

    public static final String MENU = "menu";
    public static final String BACKUP = "backup";
    public static final String TOAST = "toast";
    public static final String RESOURCE_PACKS = "resource_packs";
    public static final String META = "meta";

    // ── Toast ─────────────────────────────────────────────────────────────────

    @Entry(category = TOAST)
    public static boolean showRamWarningToast = true;

    @Entry(category = TOAST)
    public static boolean showUpdateToast = true;

    @Entry(category = TOAST)
    public static boolean showBetaUpdateNotifications = false;

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

    // ── Resource packs ──────────────────────────────────────────────────────────

    @Entry(category = RESOURCE_PACKS)
    public static KeepAboveServerPack keepPacksAboveServerPackv2 = KeepAboveServerPack.ON_APPLY_ONLY;

    /**
     * Controls when PackCore forces its applied packs above a server's own resource pack
     * (see {@code PackRepositoryMixin}).
     */
    public enum KeepAboveServerPack {
        /** Re-assert the order on every reload/join. Self-contained; survives rejoins on its own. */
        ALWAYS,
        /** Only reorder when applying from PackCore. */
        ON_APPLY_ONLY,
        /** Never reorder; behave like vanilla. */
        OFF
    }

    /**
     * Comma-separated ids of the packs PackCore last applied, in priority order (highest priority
     * last). {@code PackRepositoryMixin} moves these above the server pack.
     */
    @Hidden
    @Entry(category = RESOURCE_PACKS)
    public static String packsAboveServer = "";

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

    /** Comma-separated ids of one-shot config migrations already applied (see ConfigMigrationRunner). */
    @Hidden
    @Entry(category = META)
    public static String appliedConfigMigrations = "";

    @Hidden
    @Entry(category = META)
    public static long lastBackupEpochMs = 0L;

    @Hidden
    @Entry(category = META)
    public static long lastSeenEpochMs = 0L;

    @Hidden
    @Entry(category = META)
    public static boolean isFirstStartup = true;

    @Hidden
    @Entry(category = META)
    public static String lastAppliedOverwriteMode = "";
}
