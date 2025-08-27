package com.github.kd_gaming1.packcore.mixin;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

@Mixin(Screen.class)
public class ScreenMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Shadow
    @Final
    protected static CubeMapRenderer PANORAMA_RENDERER;

    @Shadow
    @Mutable
    @Final
    protected static RotatingCubeMapRenderer ROTATING_PANORAMA_RENDERER;

    @Unique
    private static boolean customPanoramaApplied = false;



    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void packcore$useCustomPanoramaRenderer(CallbackInfo ci) {
        packcore$applyCustomPanorama();
    }

    // Inject into the init method to ensure panorama is updated when screens are created
    @Inject(method = "init", at = @At("HEAD"))
    private void packcore$ensureCustomPanorama(CallbackInfo ci) {
        if (PackCoreConfig.enableCustomMenu && !customPanoramaApplied) {
            packcore$applyCustomPanorama();
        }
    }

    @Unique
    private static void packcore$applyCustomPanorama() {
        if (customPanoramaApplied) return;

        try {
            Identifier base = Identifier.of("packcore", "textures/gui/title/background/panorama");
            CubeMapRenderer cubeMapRenderer = new CubeMapRenderer(base);

            // Pre-register the textures to avoid render pass conflicts
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getTextureManager() != null) {
                cubeMapRenderer.registerTextures(client.getTextureManager());
            }

            ROTATING_PANORAMA_RENDERER = new RotatingCubeMapRenderer(cubeMapRenderer);
            customPanoramaApplied = true;
        } catch (Exception e) {
            LOGGER.error("Failed to apply custom panorama renderer", e);
        }
    }
}