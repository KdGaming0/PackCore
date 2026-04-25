package com.github.kd_gaming1.packcore.mixin.compat.skyblocker;

import com.github.kd_gaming1.packcore.util.CaxtonFontDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gates Skyblocker's Caxton {@code drawOutlinedText} entry point behind
 * {@link CaxtonFontDetector#isActive()}. When no Caxton font pack is
 * selected the method short-circuits to {@code false} so Skyblocker falls
 * back to vanilla rendering.
 *
 * <p>The pipeline accessor methods ({@code getTextPipeline},
 * {@code getSeeThroughTextPipeline}) are intentionally <b>not</b>
 * intercepted here. Instead, {@link TextPrimitiveRendererMixin}
 * dynamically nulls out the cached pipeline fields at their usage sites,
 * which lets Skyblocker initialise normally and switch behaviour live
 * without a restart.
 *
 * <p>Applied only when both {@code skyblocker} and {@code caxton} are
 * loaded (gated by {@code PackCoreCompatMixinPlugin}).
 */
@Mixin(targets = "de.hysky.skyblocker.compatibility.CaxtonCompatibility", remap = false)
public class CaxtonCompatibilityMixin {

    @Inject(
            method = "drawOutlinedText",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private static void packcore$skipDrawWhenInactive(CallbackInfoReturnable<Boolean> cir) {
        if (!CaxtonFontDetector.isActive()) {
            cir.setReturnValue(false);
        }
    }
}