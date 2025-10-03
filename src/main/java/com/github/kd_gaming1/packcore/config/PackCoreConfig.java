package com.github.kd_gaming1.packcore.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {

    // Category constants
    public static final String DIALOG = "dialog";
    public static final String UI = "ui";
    public static final String CUSTOMIZATION = "customization";
    public static final String ADVANCED = "advanced";

    // DIALOG CATEGORY
    @Entry(category = DIALOG, name = "packcore.midnightconfig.dialog_timeout", min = 1, max = 60)
    public static int dialogTimeoutMinutes = 10;

    // UI CATEGORY
    @Entry(category = UI, name = "packcore.midnightconfig.enable_custom_menu")
    public static boolean enableCustomMenu = true;

    @Entry(category = UI, name = "packcore.midnightconfig.enable_custom_panorama")
    public static boolean enableCustomPanorama = true;

    // Spacer
    @Comment(category = UI)
    public static Comment spacer1;

    // CUSTOMIZATION CATEGORY
    @Entry(category = CUSTOMIZATION, name = "packcore.midnightconfig.server_address")
    public static String serverAddressForQuickJoinButton = "mc.hypixel.net";

    @Entry(category = CUSTOMIZATION, name = "packcore.midnightconfig.override_vanilla_panorama")
    public static boolean overrideVanillaPanorama = true;

    @Entry(category = CUSTOMIZATION, name = "packcore.midnightconfig.enable_update_notifications")
    public static boolean enableUpdateNotifications = true;

    @Entry(category = CUSTOMIZATION, name = "packcore.midnightconfig.show_update_notifications_title")
    public static boolean showUpdateNotificationsOnTitleScreen = true;

    // ADVANCED CATEGORY (Hidden options)
    @Hidden
    @Entry(category = ADVANCED, name = "packcore.midnightconfig.first_startup")
    public static boolean isFirstStartup = true;

    @Hidden
    @Entry(category = ADVANCED, name = "packcore.midnightconfig.welcome_wizard_shown")
    public static boolean haveShownWelcomeWizard = false;

    @Hidden
    @Entry(category = ADVANCED, name = "packcore.midnightconfig.setup_wizard_shown")
    public static boolean haveConfigApplied = false;

    @Hidden
    @Entry(category = ADVANCED, name = "packcore.midnightconfig.setup_wizard_completed")
    public static boolean defaultConfigSuccessfullyApplied = false;
}