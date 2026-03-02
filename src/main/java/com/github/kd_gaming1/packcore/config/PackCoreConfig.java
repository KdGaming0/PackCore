package com.github.kd_gaming1.packcore.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PackCoreConfig extends MidnightConfig {
    public static final String META = "meta";

    @Hidden
    @Entry(category = META)
    public static String lastAppliedVersion = "";
}