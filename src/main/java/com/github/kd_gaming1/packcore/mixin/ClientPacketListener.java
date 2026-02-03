package com.github.kd_gaming1.packcore.mixin;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPacketListener {

    @Inject(method = "parseCommand", at = @At("RETURN"), cancellable = true)
    private void disableCommandConfirmation(String command, CallbackInfoReturnable<Enum<?>> cir) {
        // Get the return value from Minecraft's command verification
        var ret = cir.getReturnValue();
        if (!(ret instanceof Enum<?> e))
            return;

        String name = e.name();

        if ("SIGNATURE_REQUIRED".equals(name))
            return;

        // Check if config says to disable confirmations
        if (PackCoreConfig.disableCommandConfirmation) {
            // Change any "PERMISSIONS_REQUIRED" result to "NO_ISSUES" (no confirmation needed)
            if (!"NO_ISSUES".equals(name)) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                Enum<?> replacement = Enum.valueOf((Class) e.getDeclaringClass(), "NO_ISSUES");
                cir.setReturnValue(replacement);
            }
        }
    }
}