package com.github.kd_gaming1.packcore.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {

    // Category constants
    public static final String DIALOG = "dialog";
    public static final String UI = "ui";
    public static final String CUSTOMIZATION = "customization";
    public static final String ADVANCED = "advanced";

    // Dialog Settings Category
    @Comment(category = DIALOG, name = "packcore.midnightconfig.dialog_header")
    public static Comment dialogHeader;

    @Entry(category = DIALOG, name = "packcore.midnightconfig.prompt_default_config")
    public static boolean promptSetDefaultConfig = true;

    @Entry(category = DIALOG, name = "packcore.midnightconfig.dialog_timeout", min = 1, max = 60)
    public static int dialogTimeoutMinutes = 10;

    // UI Customization Settings Category
    @Comment(category = UI, name = "packcore.midnightconfig.ui_header")
    public static Comment uiHeader;

    @Entry(category = UI, name = "packcore.midnightconfig.enable_custom_menu")
    public static boolean enableCustomMenu = true;

    @Entry(category = UI, name = "packcore.midnightconfig.enable_custom_panorama")
    public static boolean enableCustomPanorama = true;

    // Spacer
    @Comment(category = UI)
    public static Comment spacer1;

    @Entry(category = CUSTOMIZATION, name = "Server Address For Quick Join Button")
    public static String serverAddressForQuickJoinButton = "mc.hypixel.net";

    @Entry(category = CUSTOMIZATION, name = "Replace Realms Button With Quick Server Join Button")
    public static boolean replaceRealmsButtonWithQuickServerJoinButton = true;

    @Entry(category = CUSTOMIZATION, name = "Use Custom Panorama")
    public static boolean useCustomPanorama = true;

    @Entry(category = CUSTOMIZATION, name = "Override Vanilla Panorama")
    public static boolean overrideVanillaPanorama = true;

    @Entry(category = CUSTOMIZATION, name = "Enable Update Notifications")
    public static boolean enableUpdateNotifications = true;

    @Entry(category = CUSTOMIZATION, name = "Show Update Notifications On Title Screen")
    public static boolean showUpdateNotificationsOnTitleScreen = true;

    @Entry(category = CUSTOMIZATION, name = "Update Check Delay Seconds")
    public static int updateCheckDelaySeconds = 5; // Wait 5 seconds after reaching the main menu

    /**
     * Hidden data value to track if this is the first time the game has started.
     * Used to show different extraction confirmation messages.
     * This is automatically set to false after the first ever startup.
     */
    @Hidden()
    @Entry(category = ADVANCED, name = "packcore.midnightconfig.first_startup")
    public static boolean isFirstStartup = true;

    /**
     * Tracks the last applied configuration name for reference
     */
    @Hidden
    @Entry(category = ADVANCED, name = "packcore.midnightconfig.last_config_applied")
    public static String lastConfigApplied = "";

    @Hidden
    @Entry(category = ADVANCED, name = "Have Setup Wizard shown")
    public static boolean haveSetupWizardShown = true;

    @Hidden
    @Entry(category = ADVANCED, name = "Have Setup Wizard completed successfully")
    public static boolean haveSetupWizardCompletedSuccessfully = true;

    @Hidden
    @Entry(category = ADVANCED, name = "What config was applied?")
    public static String appliedConfigName = "";
}