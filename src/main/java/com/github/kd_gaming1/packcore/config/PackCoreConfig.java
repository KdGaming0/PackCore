package com.github.kd_gaming1.packcore.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {

    // Category constants
    public static final String UI = "ui";
    public static final String CUSTOMIZATION = "customization";
    public static final String ADVANCED = "advanced";
    public static final String BACKUP = "backup";

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

    @Hidden
    @Entry(category = ADVANCED, name = "packcore.midnightconfig.setup_wizard_completed")
    public static boolean defaultConfigSuccessfullyApplied = false;
}