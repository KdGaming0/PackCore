package com.github.kd_gaming1.packcore.mixin;

import com.github.kd_gaming1.packcore.integration.HypixelPanoramaGuard;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wraps a server-sent pack's resources so PackCore's title-screen panorama is not overridden.
 *
 * <p>The Hypixel server pack ({@link PackSource#SERVER}) ships its own panorama at the same vanilla
 * path as ours; since every resource pack outranks mod resources, it replaces our branded panorama on
 * the title screen. Wrapping the opened resources in {@link HypixelPanoramaGuard} hides only those
 * title-background textures, so our panorama shows through while the rest of the server pack applies
 * normally. Other user packs ({@link PackSource#DEFAULT}) are never wrapped, so they can still override
 * the panorama if they choose to.
 */
@Mixin(Pack.class)
public class PackMixin {

    @Inject(method = "open", at = @At("RETURN"), cancellable = true)
    private void packcore$guardServerPackPanorama(CallbackInfoReturnable<PackResources> cir) {
        if (((Pack) (Object) this).getPackSource() != PackSource.SERVER) return;
        cir.setReturnValue(new HypixelPanoramaGuard(cir.getReturnValue()));
    }
}
