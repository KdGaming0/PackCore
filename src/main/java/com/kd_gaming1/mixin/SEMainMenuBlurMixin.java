package com.kd_gaming1.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import com.kd_gaming1.screen.SEMainMenu;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Screen.class)
public abstract class SEMainMenuBlurMixin {

    @WrapWithCondition(
            method = "renderBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;applyBlur()V")
    )
    private boolean shouldApplyBlur(Screen instance) {
        // Only disable blur for your custom main menu
        return !(instance instanceof SEMainMenu);
    }

    @WrapWithCondition(
            method = "renderBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderDarkening(Lnet/minecraft/client/gui/DrawContext;)V")
    )
    private boolean shouldRenderDarkening(Screen instance, DrawContext context) {
        // Only disable darkening for your custom main menu
        return !(instance instanceof SEMainMenu);
    }
}