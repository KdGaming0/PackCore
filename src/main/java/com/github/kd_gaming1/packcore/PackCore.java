package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.command.PackCoreCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class PackCore implements ClientModInitializer {
    public static final String MOD_ID = "packcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Path PACKCORE_DIR = FabricLoader.getInstance().getGameDir().resolve("packcore");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PackCore] Initialized");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                PackCoreCommands.register(dispatcher)
        );
    }
}