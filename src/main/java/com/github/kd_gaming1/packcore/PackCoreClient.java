package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.help.introduction.IntroductionScreenPage;
import com.github.kd_gaming1.packcore.gui.titlescreen.fancy.FancyMainMenuScreen;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class PackCoreClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        // Initialize MidnightLib data for client-side access
        MidnightConfig.init(PackCore.MOD_ID, PackCoreConfig.class);

        // Check if the Custom Menu is enabled using MidnightLib
        if (PackCoreConfig.enableCustomMenu) {
            // Register screenoOld event to replace the main menu after initialization
            ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                // Check if the screenoOld being opened is the vanilla main menu
                if (screen instanceof TitleScreen) {
                    // Replace it with your custom menu on the next tick
                    if (!PackCoreConfig.haveShowWelcomeWizard) {
                        client.execute(() -> client.setScreen(new IntroductionScreenPage()));
                    }
                    client.execute(() -> client.setScreen(new FancyMainMenuScreen()));
                }
            });
        }
    }
}