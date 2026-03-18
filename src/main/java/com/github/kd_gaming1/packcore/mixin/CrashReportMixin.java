package com.github.kd_gaming1.packcore.mixin;

import com.github.kd_gaming1.packcore.util.diagnostics.CrashReportEnricher;
import net.minecraft.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Injects PackCore diagnostics into every crash report automatically. */
@Mixin(CrashReport.class)
public class CrashReportMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void packcore$enrichCrashReport(String message, Throwable cause, CallbackInfo ci) {
        CrashReportEnricher.register((CrashReport) (Object) this);
    }
}