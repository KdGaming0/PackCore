package com.github.kd_gaming1.packcore;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class PackCore implements ClientModInitializer {
    public static final String MOD_ID = "packcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Path packcoreDir = FabricLoader.getInstance().getGameDir().resolve("packcore");

    @Override
    public void onInitializeClient() {


    }
}