package com.kd_gaming1;

import com.kd_gaming1.commands.PackCoreCommands;
import com.kd_gaming1.config.PackCoreConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackCore implements ModInitializer {
	public static final String MOD_ID = "packcore";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Register commands
		PackCoreCommands.registerCommands();

		LOGGER.info("PackCore initialized!");
	}
}