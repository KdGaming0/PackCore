package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.command.PackCoreCommands;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.util.diagnostics.DiagnosticsCollector;
import com.github.kd_gaming1.packcore.gui.screen.PackCoreTitleScreen;
import com.github.kd_gaming1.packcore.gui.screen.SBETitleScreen;
import com.github.kd_gaming1.packcore.gui.screen.WelcomeWizardScreen;
import com.github.kd_gaming1.packcore.playtime.PlaytimeTracker;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.util.RamWarningHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class PackCore implements ClientModInitializer {

    public static final String MOD_ID = "packcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Path PACKCORE_DIR =
            FabricLoader.getInstance().getGameDir().resolve("packcore");

    public static boolean migratedFromV3 = false;

    /** True once CLIENT_STARTED has fired and the initial screen swap is done. */
    private static boolean clientFullyStarted = false;
    private static boolean replacingTitleScreen = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PackCore] Initialized\n{}", DiagnosticsCollector.buildFullReport());

        RamWarningHelper.init();
        UpdateChecker.checkAsync();

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> PackCoreCommands.register(dispatcher));

        // Only handles subsequent returns to TitleScreen (e.g. disconnect from server).
        // The initial startup swap is done in CLIENT_STARTED to avoid racing the loading overlay.
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen) || screen instanceof PackCoreTitleScreen) return;
            RamWarningHelper.onMainMenu();
            if (!clientFullyStarted) return;
            if (PackCoreConfig.menuStyle != PackCoreConfig.MenuStyle.MINIMAL) {
                scheduleConfiguredTitleScreen(client, screen);
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen) || screen instanceof PackCoreTitleScreen) return;
            if (!PackCoreConfig.successfulWelcomeWizard) return;
            if (PackCoreConfig.menuStyle != PackCoreConfig.MenuStyle.MINIMAL) return;
            PackCoreTitleScreen.decorateExisting((TitleScreen) screen, scaledWidth, scaledHeight);
        });

        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> client.execute(RamWarningHelper::onWorldJoin));

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            PlaytimeTracker.onSessionStart();

            // Safe to swap now — resources are loaded and the overlay is gone.
            clientFullyStarted = true;
            if (client.screen instanceof TitleScreen ts && !(ts instanceof PackCoreTitleScreen)) {
                if (PackCoreConfig.menuStyle != PackCoreConfig.MenuStyle.MINIMAL) {
                    applyConfiguredTitleScreen(client, ts);
                }
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PlaytimeTracker.onSessionEnd());
    }

    private static void scheduleConfiguredTitleScreen(Minecraft client, Screen screen) {
        if (replacingTitleScreen) return;
        replacingTitleScreen = true;
        client.execute(() -> {
            replacingTitleScreen = false;
            if (client.screen != screen) return;
            applyConfiguredTitleScreen(client, screen);
        });
    }

    private static void applyConfiguredTitleScreen(Minecraft client, Screen screen) {
        if (!PackCoreConfig.successfulWelcomeWizard) {
            client.setScreen(new WelcomeWizardScreen(screen));
            return;
        }
        switch (PackCoreConfig.menuStyle) {
            case MODERN -> client.setScreen(new SBETitleScreen());
            case MODERN_MINIMAL -> client.setScreen(new SBETitleScreen(false));
            case MINIMAL -> client.setScreen(new PackCoreTitleScreen());
        }
    }
}