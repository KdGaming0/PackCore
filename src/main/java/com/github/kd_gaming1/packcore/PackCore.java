package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.command.PackCoreCommands;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
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

        UpdateChecker.checkAsync().thenAccept(status -> {
                    switch (status.state()) {
                        case UPDATE_AVAILABLE ->
                                LOGGER.info("Update available: {} -> {} | Changelog: {}",
                                        status.installedVersion(), status.latestVersion(), status.changelog());
                        case UP_TO_DATE ->
                                LOGGER.info("Modpack is up to date: {}", status.installedVersion());
                        case UNKNOWN ->
                                LOGGER.debug("Unable to determine update status");
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.error("Failed to check for updates", throwable);
                    return null;
                });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                PackCoreCommands.register(dispatcher)
        );
    }
}