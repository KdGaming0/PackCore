package com.github.kd_gaming1.packcore.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {
    public static final String CATEGORY_INTERFACE = "interface";
    public static final String CATEGORY_BACKUPS = "backups";
    public static final String CATEGORY_CUSTOMIZATION = "customization";
    public static final String CATEGORY_ADVANCED = "advanced";

    // ---------------------
    // Interface
    // ---------------------
    @Comment(category = CATEGORY_INTERFACE, name = "packcore.midnightconfig.interface_info")
    public static Comment interfaceInfo;

    @Entry(category = CATEGORY_INTERFACE, name = "packcore.midnightconfig.enable_custom_menu")
    public static boolean enableCustomMenu = true;

    // ---------------------
    // Backups
    // ---------------------
    @Comment(category = CATEGORY_BACKUPS, name = "packcore.midnightconfig.backup_info")
    public static Comment backupInfo;

    @Entry(category = CATEGORY_BACKUPS, name = "packcore.midnightconfig.enable_auto_backups")
    public static boolean enableAutoBackups = true;

    @Entry(category = CATEGORY_BACKUPS, name = "packcore.midnightconfig.enable_scheduled_backups")
    public static boolean enableScheduledBackups = true;

    @Entry(category = CATEGORY_BACKUPS, name = "packcore.midnightconfig.max_backups", min = 1, max = 20)
    public static int maxBackups = 5;

    // the key says days; variable name reflects that
    @Entry(category = CATEGORY_BACKUPS, name = "packcore.midnightconfig.backup_interval_days", min = 1, max = 14)
    public static int backupIntervalDays = 3;

    @Entry(category = CATEGORY_BACKUPS, name = "packcore.midnightconfig.enable_backup_debug_logging")
    public static boolean enableBackupDebugLogging = false;

    @Comment(category = CATEGORY_BACKUPS, name = "packcore.midnightconfig.backup_spacer_1")
    public static Comment backupSpacer;

    // ---------------------
    // Customization & Updates
    // ---------------------
    @Entry(category = CATEGORY_CUSTOMIZATION, name = "packcore.midnightconfig.server_address")
    public static String serverAddressForQuickJoinButton = "mc.hypixel.net";

    @Entry(category = CATEGORY_CUSTOMIZATION, name = "packcore.midnightconfig.enable_update_notifications")
    public static boolean enableUpdateNotifications = true;

    @Entry(category = CATEGORY_CUSTOMIZATION, name = "packcore.midnightconfig.show_update_notifications_title")
    public static boolean showUpdateNotificationsOnTitleScreen = true;

    @Comment(category = CATEGORY_CUSTOMIZATION, name = "packcore.midnightconfig.customization_spacer_1")
    public static Comment customizationSpacer;

    @Entry(category = CATEGORY_CUSTOMIZATION, name = "packcore.midnightconfig.show_low_memory_warning")
    public static boolean showLowMemoryWarning = true;

    @Entry(category = CATEGORY_CUSTOMIZATION, name = "packcore.midnightconfig.minimum_ram_gb", min = 2, max = 8)
    public static int minimumRamGB = 3;

    // ---------------------
    // Advanced / telemetry-like trackers
    // ---------------------
    @Entry(category = CATEGORY_ADVANCED, name = "packcore.midnightconfig.first_startup")
    public static boolean isFirstStartup = true;

    @Entry(category = CATEGORY_ADVANCED, name = "packcore.midnightconfig.welcome_wizard_shown")
    public static boolean haveShownWelcomeWizard = false;

    @Entry(category = CATEGORY_ADVANCED, name = "packcore.midnightconfig.have_set_bobby_config")
    public static boolean haveSetBobbyConfig = false;

    @Hidden
    @Entry(category = CATEGORY_ADVANCED, name = "packcore.midnightconfig.setup_wizard_completed")
    public static boolean defaultConfigSuccessfullyApplied = false;
}