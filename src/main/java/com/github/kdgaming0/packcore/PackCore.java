package com.github.kdgaming0.packcore;

import com.github.kdgaming0.packcore.config.ModConfig;
import com.github.kdgaming0.packcore.screen.SEMainMenu;
import com.github.kdgaming0.packcore.utils.ModpackInfo;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

@Mod(modid = PackCore.MOD_ID, useMetadata=true)
public class PackCore {
    public static final String MOD_ID = "packcore";
    public static KeyBinding OPEN_GUI_KEY;
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        OPEN_GUI_KEY = new KeyBinding("Open Pack Menu", Keyboard.KEY_K, "Pack Core");
        ClientRegistry.registerKeyBinding(OPEN_GUI_KEY);
        ModpackInfo.loadModpackInfo();
        ModConfig.loadConfig();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Check if the GUI being opened is the main menu
        if (event.gui instanceof GuiMainMenu) {
            // Replace it with your custom menu
            event.gui = new SEMainMenu();
        }
    }
}