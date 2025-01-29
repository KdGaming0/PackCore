package com.github.kdgaming0.packcore.mixin;

import com.github.kdgaming0.packcore.config.ModConfig;
import com.github.kdgaming0.packcore.copysystem.ZipSelectionDialog;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(Minecraft.class)
public class PackCoreShowConfigMixin {

    @Inject(method = "startGame", at = @At("HEAD"))
    private void onGameStart(CallbackInfo ci) {
        // Get Minecraft instance and game directory
        Minecraft mc = (Minecraft)(Object)this;
        File gameDir = mc.mcDataDir;

        // Load and check config
        if (packCore$shouldShowDialog()) {
            // Show the ZIP selection dialog
            ZipSelectionDialog.showDialog(gameDir);
        }
    }

    @Unique
    private boolean packCore$shouldShowDialog() {
        return ModConfig.getPromptSetDefaultConfig();
    }
}