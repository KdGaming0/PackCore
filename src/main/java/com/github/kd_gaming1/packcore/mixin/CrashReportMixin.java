package com.github.kd_gaming1.packcore.mixin;

import com.github.kd_gaming1.packcore.crash.CrashBrandingHandler;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.crash.ReportType;
import org.spongepowered. asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered. asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm. mixin.injection.Inject;
import org.spongepowered.asm.mixin. injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.List;

/**
 * Injects modpack branding information into crash reports.
 * This helps users and support teams quickly identify the modpack version.
 *
 * Compatible with Minecraft 1.21.10+ (updated writeToFile signature)
 */
@Mixin(CrashReport.class)
public class CrashReportMixin {
    @Shadow @Final private List<CrashReportSection> otherSections;

    /**
     * Inject at HEAD of writeToFile to add modpack branding section
     * before the crash report is written to disk.
     */
    @Inject(method = "writeToFile", at = @At("HEAD"))
    private void packcore$addModpackBranding(
            Path path,
            ReportType reportType,
            List<?> list,
            CallbackInfoReturnable<Boolean> cir
    ) {
        try {
            CrashReportSection section = new CrashReportSection("Modpack Information");
            CrashBrandingHandler.addBranding(section);
            otherSections.add(section);
        } catch (Exception e) {
            // Don't let branding injection cause crashes
            System.err.println("PackCore: Failed to add modpack branding to crash report: " + e.getMessage());
        }
    }
}