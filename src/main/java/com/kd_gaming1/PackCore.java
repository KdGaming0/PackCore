package com.kd_gaming1;

import com.kd_gaming1.screen.SEMainMenu;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackCore implements ModInitializer {
	public static final String MOD_ID = "packcore";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

		// Register screen event to replace the main menu after initialization
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			// Check if the screen being opened is the vanilla main menu
			if (screen instanceof TitleScreen) {
				// Replace it with your custom menu on the next tick
				client.execute(() -> client.setScreen(new SEMainMenu()));
			}
		});
	}
}