package com.github.kd_gaming1.packcore.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {

    // Category constants
    public static final String UI = "ui";
    public static final String CUSTOMIZATION = "customization";
    public static final String ADVANCED = "advanced";
    public static final String BACKUP = "backup";
    public static final String SCAMSHIELD  = "scamshield";

    // UI CATEGORY
    @Entry(category = UI, name = "packcore.midnightconfig.enable_custom_menu")
    public static boolean enableCustomMenu = true;

    // Spacer
    @Comment(category = UI)
    public static Comment spacer1;

    // BACKUP CATEGORY
    @Entry(category = BACKUP, name = "packcore.midnightconfig.enable_auto_backups")
    public static boolean enableAutoBackups = true;

    @Entry(category = BACKUP, name = "packcore.midnightconfig.max_backups", min = 1, max = 20)
    public static int maxBackups = 5;

    // Spacer
    @Comment(category = BACKUP)
    public static Comment backupSpacer;

    // CUSTOMIZATION CATEGORY
    @Entry(category = CUSTOMIZATION, name = "packcore.midnightconfig.server_address")
    public static String serverAddressForQuickJoinButton = "mc.hypixel.net";

    @Entry(category = CUSTOMIZATION, name = "packcore.midnightconfig.enable_update_notifications")
    public static boolean enableUpdateNotifications = true;

    @Entry(category = CUSTOMIZATION, name = "packcore.midnightconfig.show_update_notifications_title")
    public static boolean showUpdateNotificationsOnTitleScreen = true;

    // ADVANCED CATEGORY
    @Entry(category = ADVANCED, name = "packcore.midnightconfig.first_startup")
    public static boolean isFirstStartup = true;

    @Entry(category = ADVANCED, name = "packcore.midnightconfig.welcome_wizard_shown")
    public static boolean haveShownWelcomeWizard = false;

    @Entry(category = ADVANCED, name = "packcore.midnightconfig.have_set_bobby_config")
    public static boolean haveSetBobbyConfig = false;

    @Hidden
    @Entry(category = ADVANCED, name = "packcore.midnightconfig.setup_wizard_completed")
    public static boolean defaultConfigSuccessfullyApplied = false;

    // SCAMSHIELD CATEGORY
    @Entry(category = SCAMSHIELD , name = "packcore.midnightconfig.enable_scamshield")
    public static boolean enableScamShield = true;

    @Entry(category = SCAMSHIELD , name = "packcore.midnightconfig.enable_scamshield_debugging")
    public static boolean enableScamShieldDebugging = false;

    @Comment(category = SCAMSHIELD)
    public static Comment scamshieldSpacer1;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_trigger_threshold", min = 50, max = 500)
    public static int scamShieldTriggerThreshold = 100;

    @Comment(category = SCAMSHIELD)
    public static Comment scamshieldSpacer2;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_show_notifications")
    public static boolean scamShieldShowNotifications = true;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_notification_cooldown", min = 0, max = 60)
    public static int scamShieldNotificationCooldownSeconds = 5;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_max_recent_detections", min = 5, max = 50)
    public static int scamShieldMaxRecentDetections = 10;

    @Comment(category = SCAMSHIELD)
    public static Comment scamshieldSpacer3;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_max_history_size", min = 10, max = 1000)
    public static int scamShieldMaxHistorySize = 100;

    @Comment(category = SCAMSHIELD)
    public static Comment scamshieldSpacer4;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_regex_timeout_ms", min = 50, max = 500)
    public static int scamShieldRegexTimeoutMs = 100;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_cache_size", min = 50, max = 500)
    public static int scamShieldCacheSize = 100;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_cache_ttl_seconds", min = 10, max = 300)
    public static int scamShieldCacheTTLSeconds = 30;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_conversation_timeout_minutes", min = 5, max = 60)
    public static int scamShieldConversationTimeoutMinutes = 15;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_max_messages_per_user", min = 10, max = 100)
    public static int scamShieldMaxMessagesPerUser = 30;

    @Entry(category = SCAMSHIELD, name = "packcore.midnightconfig.scamshield_max_progression_bonus", min = 50, max = 200)
    public static int scamShieldMaxProgressionBonus = 150;
}