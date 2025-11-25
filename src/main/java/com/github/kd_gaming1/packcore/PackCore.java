package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.command.packcore.PackCoreCommand;
import com.github.kd_gaming1.packcore.command.scamshield.ScamShieldCommand;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.config.backup.ScheduledBackupManager;
import com.github.kd_gaming1.packcore.scamshield.ChatMessageInterceptor;
import com.github.kd_gaming1.packcore.scamshield.ScamShieldChatHandler;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamDetector;
import com.github.kd_gaming1.packcore.ui.screen.wizard.pages.WelcomeWizardPage;
import com.github.kd_gaming1.packcore.ui.screen.title.SBEStyledTitleScreen;
import com.github.kd_gaming1.packcore.integration.bobby.BobbyConfigModifier;
import com.github.kd_gaming1.packcore.modpack.ModpackInfo;
import com.github.kd_gaming1.packcore.util.HypixelEventUtil;
import com.github.kd_gaming1.packcore.util.update.modrinth.UpdateCache;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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
    private static UpdateCache updateManager;
    private static final Path packcoreDir = FabricLoader.getInstance().getGameDir().resolve("packcore");

    private static ScamDetector scamDetector;

    @Override
    public void onInitializeClient() {
        LOGGER.info("PackCore initialized!");

        HypixelEventUtil.init();

        // Initialize the scam detection engine
        scamDetector = ScamDetector.getInstance();
        LOGGER.info("[ScamShield] Initialized with {} scam types", scamDetector.getScamTypes().size());

        // Register chat message interceptor
        ChatMessageInterceptor.getInstance().register();

        // Cleanup on shutdown
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            scamDetector.shutdown();
            ScamShieldChatHandler.getInstance().shutdown();
        });

        try {
            modpackInfo = ModpackInfo.loadFromFile(packcoreDir);
            updateManager = new UpdateCache();

            LOGGER.info("Loaded modpack info for: {}", modpackInfo.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to load modpack info: {}", e.getMessage());
        }

        MidnightConfig.init(MOD_ID, PackCoreConfig.class);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            PackCoreCommand.registerCommands(dispatcher);
            ScamShieldCommand.register(dispatcher);
        });

        // Initialize scheduled backups
        if (PackCoreConfig.enableScheduledBackups) {
            ScheduledBackupManager.initialize();
        }

        // try catch just in case something goes wrong with title screen
        try {
            if (PackCoreConfig.enableCustomMenu) {
                ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                    if (screen instanceof TitleScreen) {
                        client.execute(() -> client.setScreen(PackCoreConfig.haveShownWelcomeWizard
                                ? new SBEStyledTitleScreen()
                                : new WelcomeWizardPage())
                        );
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.error("Failed to show custom title screen: {}", e.getMessage());
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

    public static UpdateCache getUpdateManager() {
        return updateManager;
    }

    public static ScamDetector getScamDetector() {
        return scamDetector;
    }
}