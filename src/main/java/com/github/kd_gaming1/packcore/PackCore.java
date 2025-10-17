package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.commands.PackCoreCommand;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.help.introduction.IntroductionScreenPage;
import com.github.kd_gaming1.packcore.gui.titlescreen.fancy.FancyMainMenuScreen;
import com.github.kd_gaming1.packcore.util.config.BobbyConfigModifier;
import com.github.kd_gaming1.packcore.util.modpack.ModpackInfo;
import com.github.kd_gaming1.packcore.util.api.UpdateCacheManager;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class PackCore implements ClientModInitializer {
    public static final String MOD_ID = "packcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModpackInfo modpackInfo;
    private static UpdateCacheManager updateManager;
    private static final Path packcoreDir = FabricLoader.getInstance().getGameDir().resolve("packcore");

    @Override
    public void onInitializeClient() {
        LOGGER.info("PackCore initialized!");

        try {
            modpackInfo = ModpackInfo.loadFromFile(packcoreDir);
            updateManager = new UpdateCacheManager();

            LOGGER.info("Loaded modpack info for: {}", modpackInfo.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to load modpack info: {}", e.getMessage());
        }

        MidnightConfig.init(MOD_ID, PackCoreConfig.class);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                PackCoreCommand.registerCommands(dispatcher));

        if (PackCoreConfig.enableCustomMenu) {
            ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                if (screen instanceof TitleScreen) {
                    client.execute(() -> client.setScreen(PackCoreConfig.haveShownWelcomeWizard
                            ? new FancyMainMenuScreen()
                            : new IntroductionScreenPage())
                    );
                }
            });
        }

        if (!PackCoreConfig.haveSetBobbyConfig) {
            BobbyConfigModifier.enableDynamicMultiWorld();
            PackCoreConfig.haveSetBobbyConfig = true;
            PackCoreConfig.write(MOD_ID);
        }
    }

    public static ModpackInfo getModpackInfo() {
        return modpackInfo;
    }

    public static UpdateCacheManager getUpdateManager() {
        return updateManager;
    }
}