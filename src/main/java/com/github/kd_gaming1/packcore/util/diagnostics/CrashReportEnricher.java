package com.github.kd_gaming1.packcore.util.diagnostics;

import com.github.kd_gaming1.packcore.mixin.CrashReportMixin;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;

/**
 * Appends a "PackCore Diagnostics" section to Minecraft crash reports.
 *
 * <p>Invoked automatically via {@link CrashReportMixin}. The try/catch ensures
 * this can never make a crash report worse.
 */
public final class CrashReportEnricher {

    private CrashReportEnricher() {}

    public static void register(CrashReport report) {
        CrashReportCategory cat = report.addCategory("PackCore Diagnostics");
        try {
            cat.setDetail("Report", DiagnosticsCollector.buildFullReport());
        } catch (Exception e) {
            cat.setDetail("Error", "Failed to collect diagnostics: " + e.getMessage());
        }
    }
}