package com.github.kdgaming0.packcore;

import com.github.kdgaming0.packcore.command.CommandOpenGui;
import com.github.kdgaming0.packcore.config.ModConfig;
import com.github.kdgaming0.packcore.screen.SEMainMenu;
import com.github.kdgaming0.packcore.utils.ModpackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.GuiOpenEvent;

import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

@Mod(modid = PackCore.MOD_ID, useMetadata=true)
public class PackCore {
    public static final String MOD_ID = "packcore";
    public static KeyBinding OPEN_GUI_KEY;
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static GuiScreen screenToOpenNextTick = null;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new CommandOpenGui());
        ModpackInfo.loadModpackInfo();
        ModConfig.loadConfig();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Check config setting first
        if (!ModConfig.getEnableCustomMenu()) {
            return;
        }

        if (event.gui instanceof GuiMainMenu) {
            if (!event.isCanceled()) {
                event.setCanceled(true);
                Minecraft.getMinecraft().displayGuiScreen(new SEMainMenu());
                LOGGER.info("PackCore: Loading custom main menu");
            } else {
                LOGGER.info("PackCore: Another mod has already modified the main menu");
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (screenToOpenNextTick != null) {
            Minecraft.getMinecraft().displayGuiScreen(screenToOpenNextTick);
            screenToOpenNextTick = null;
        }
    }
}