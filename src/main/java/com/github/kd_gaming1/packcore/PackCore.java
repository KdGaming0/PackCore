package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.command.PackCoreCommands;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.screen.SBETitleScreen;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.TitleScreen;
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

        UpdateChecker.checkAsync();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                PackCoreCommands.register(dispatcher)
        );

        if (PackCoreConfig.enableCustomTitleScreen) {
            ScreenEvents.BEFORE_INIT.register(((client, screen, scaledWidth, scaledHeight) -> {
                if (screen instanceof TitleScreen) {
                    client.execute(() -> client.setScreen(new SBETitleScreen()));
                }
            }));
        }
    }
}