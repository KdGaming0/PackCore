package com.github.kdgaming0.packcore;

import com.github.kdgaming0.packcore.screen.JavaTestGui;
import com.github.kdgaming0.packcore.screen.MainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "packcore", useMetadata=true)
public class PackCore {
    public static KeyBinding OPEN_GUI_KEY;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Create and register the key binding
        OPEN_GUI_KEY = new KeyBinding("Open Pack Menu", Keyboard.KEY_K, "Pack Core");
        ClientRegistry.registerKeyBinding(OPEN_GUI_KEY);

        // Register the event handler
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_GUI_KEY.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new MainMenu());
        }
    }
}
