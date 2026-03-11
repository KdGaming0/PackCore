package com.github.kd_gaming1.packcore.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {
    public static final String MENU = "menu";
    public static final String META = "meta";

    @Entry(category = MENU)
    public static Boolean enableCustomTitleScreen = true;

    @Entry(category = MENU)
    public static String serverAddressForQuickJoinButton = "mc.hypixel.net";

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

    @Entry(category = META)
    public static boolean successfulWelcomeWizard =  false;
}