package com.github.kd_gaming1.packcore.mixin.compat.bobby;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.configurate.reference.ValueReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Backports Bobby's configuration-corruption recovery from upstream PR #463.
 * Applied only to Bobby 5.2.13 by {@code PackCoreCompatMixinPlugin}.
 */
@Mixin(targets = "de.johni0702.minecraft.bobby.Bobby", remap = false)
public abstract class BobbyMixin {

    @Unique
    private static final Logger PACKCORE_LOGGER = LoggerFactory.getLogger("PackCore/BobbyCompat");

    @Shadow
    private ValueReference<?, ?> configReference;

    @Shadow
    private void cleanupOldWorlds() {}

    @Redirect(
            method = "onInitializeClient",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/IOException;printStackTrace()V"
            ),
            require = 1
    )
    private void packcore$preserveCorruptConfig(IOException cause) {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("bobby.conf");
        Path corruptConfigPath = configPath.resolveSibling(configPath.getFileName() + ".corrupt");
        try {
            Files.move(configPath, corruptConfigPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException moveException) {
            cause.addSuppressed(moveException);
        }
        PACKCORE_LOGGER.error("Failed to initialize Bobby configuration at {}, using defaults", configPath, cause);
    }

    @Inject(
            method = "onInitializeClient",
            at = @At(
                    value = "INVOKE",
                    target = "Lde/johni0702/minecraft/bobby/util/FlawlessFrames;onClientInitialization()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            require = 1
    )
    private void packcore$skipConfigSubscriptionsWhenUnavailable(CallbackInfo ci) {
        if (configReference == null) {
            Util.ioPool().execute(this::cleanupOldWorlds);
            ci.cancel();
        }
    }

    @Inject(method = "createConfigScreen", at = @At("HEAD"), cancellable = true, require = 1)
    private void packcore$skipConfigScreenWhenUnavailable(
            Screen unusedParent,
            CallbackInfoReturnable<Screen> cir
    ) {
        if (configReference == null) {
            cir.setReturnValue(null);
        }
    }
}
