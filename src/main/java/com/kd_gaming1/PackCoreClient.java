package com.kd_gaming1;

import com.kd_gaming1.config.PackCoreConfig;
import com.kd_gaming1.integration.VistasIntegration;
import com.kd_gaming1.screen.SEMainMenu;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.TitleScreen;

public class PackCoreClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Initialize MidnightLib config for client-side access
        MidnightConfig.init(PackCore.MOD_ID, PackCoreConfig.class);

        // Check if the Custom Menu is enabled using MidnightLib
        if (PackCoreConfig.enableCustomMenu) {
            // Register screen event to replace the main menu after initialization
            ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                // Check if the screen being opened is the vanilla main menu
                if (screen instanceof TitleScreen) {
                    // Apply Vistas configuration before showing the screen
                    VistasIntegration.applyConfiguration();

                    // Replace it with your custom menu on the next tick
                    client.execute(() -> client.setScreen(new SEMainMenu()));
                }
            });
        }
    }
}