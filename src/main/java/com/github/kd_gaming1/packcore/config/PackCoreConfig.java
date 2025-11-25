package com.github.kd_gaming1.packcore.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {

    // Category constants (short, user-friendly internal ids)
    public static final String CATEGORY_INTERFACE = "interface";
    public static final String CATEGORY_BACKUPS = "backups";
    public static final String CATEGORY_CUSTOMIZATION = "customization";
    public static final String CATEGORY_SCAM_PROTECTION = "scam_protection";
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

    // ---------------------
    // Advanced / telemetry-like trackers (hidden flags visible to pack devs)
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

    // ---------------------
    // Scam protection / security
    // ---------------------
    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.enable_scamshield")
    public static boolean enableScamShield = true;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.enable_scamshield_debugging")
    public static boolean enableScamShieldDebugging = false;

    @Comment(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scam_spacer_1")
    public static Comment scamshieldSpacer1;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_trigger_threshold", min = 50, max = 500)
    public static int scamShieldTriggerThreshold = 100;

    @Comment(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scam_spacer_2")
    public static Comment scamshieldSpacer2;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_show_notifications")
    public static boolean scamShieldShowNotifications = true;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_notification_cooldown", min = 0, max = 60)
    public static int scamShieldNotificationCooldownSeconds = 5;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_max_recent_detections", min = 5, max = 50)
    public static int scamShieldMaxRecentDetections = 10;

    @Comment(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scam_spacer_3")
    public static Comment scamshieldSpacer3;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_max_history_size", min = 10, max = 1000)
    public static int scamShieldMaxHistorySize = 100;

    @Comment(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scam_spacer_4")
    public static Comment scamshieldSpacer4;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_regex_timeout_ms", min = 50, max = 500)
    public static int scamShieldRegexTimeoutMs = 100;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_cache_size", min = 50, max = 500)
    public static int scamShieldCacheSize = 100;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_cache_ttl_seconds", min = 10, max = 300)
    public static int scamShieldCacheTTLSeconds = 30;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_conversation_timeout_minutes", min = 5, max = 60)
    public static int scamShieldConversationTimeoutMinutes = 15;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_max_messages_per_user", min = 10, max = 100)
    public static int scamShieldMaxMessagesPerUser = 30;

    @Entry(category = CATEGORY_SCAM_PROTECTION, name = "packcore.midnightconfig.scamshield_max_progression_bonus", min = 50, max = 200)
    public static int scamShieldMaxProgressionBonus = 150;
}