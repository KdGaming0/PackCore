package com.kd_gaming1.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {

    // Category constants
    public static final String DIALOG = "dialog";
    public static final String UI = "ui";
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

    // Debug options (completely hidden)
    @Hidden
    public static boolean debugMode = false;

    @Hidden
    public static String lastConfigApplied = "";
}