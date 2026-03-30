package com.github.kd_gaming1.packcore.util.diagnostics;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.update.UpdateStatus;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import com.github.kd_gaming1.packcore.util.ScreenResolution;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Collects modpack, config, and runtime diagnostics. All reporting surfaces
 * (startup log, crash reports, /packcore diagnose) delegate here so the data
 * is always consistent.
 */
public final class DiagnosticsCollector {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private DiagnosticsCollector() {}

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /** Full multi-section report for the startup log and crash reports. */
    public static String buildFullReport() {
        ModpackMetadata meta = ModpackMetadata.getInstance();
        Runtime rt = Runtime.getRuntime();
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        var os = ManagementFactory.getOperatingSystemMXBean();

        return section("Modpack", new String[][] {
                {"Name",              meta.getModpackName()},
                {"Version",           meta.getModpackVersion()},
                {"Minecraft Version", meta.getMinecraftVersion()},
                {"Author",            meta.getAuthor()},
                {"Description",       meta.getDescription()},
                {"Modrinth ID",       meta.getModrinthProjectId()},
        })
                + section("Config Pack", new String[][] {
                {"Last Applied Version", blankOr(PackCoreConfig.lastAppliedVersion)},
                {"Last Applied File",    blankOr(PackCoreConfig.lastAppliedPackFile)},
        })
                + section("Settings", new String[][] {
                {"Menu Style",          PackCoreConfig.menuStyle.name()},
                {"Wizard Complete",     String.valueOf(PackCoreConfig.successfulWelcomeWizard)},
                {"Auto Backup",         PackCoreConfig.autoBackupEnabled
                        ? "enabled (every " + PackCoreConfig.autoBackupIntervalDays + " days)"
                        : "disabled"},
                {"Last Backup",         formatEpoch(PackCoreConfig.lastBackupEpochMs)},
        })
                + section("Runtime", new String[][] {
                {"RAM Allocated",   mb(rt.maxMemory()) + " MB"},
                {"RAM Used",        mb(rt.totalMemory() - rt.freeMemory()) + " MB"},
                {"Heap Used / Max",
                        mb(mem.getHeapMemoryUsage().getUsed())
                                + " MB / "
                                + mb(mem.getHeapMemoryUsage().getMax())
                                + " MB"},
                {"CPU Cores",    String.valueOf(rt.availableProcessors())},
                {"Java Version", System.getProperty("java.version")},
                {"JVM",          System.getProperty("java.vm.name")},
                {"Fabric Loader",
                        FabricLoader.getInstance()
                                .getModContainer("fabricloader")
                                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                                .orElse("unknown")},
        })
                + section("System", new String[][] {
                {"OS",           System.getProperty("os.name") + " " + System.getProperty("os.version")},
                {"Architecture", System.getProperty("os.arch")},
                {"CPU Load",     String.format("%.1f%%", os.getSystemLoadAverage() * 100)},
                {"Physical RAM", physicalRam(os)},
        });
    }

    /**
     * Compact report for /packcore diagnose — only what a developer needs when a
     * player reports an issue.
     */
    public static String buildCompactReport() {
        ModpackMetadata meta = ModpackMetadata.getInstance();
        Runtime rt = Runtime.getRuntime();

        // Detect screen resolution
        var screen = ScreenResolution.detect();
        String screenLine = screen.width() + "x" + screen.height();

        return String.join(
                "\n",
                "=== PackCore Diagnostics ===",
                "Modpack : " + meta.getModpackName() + " " + meta.getModpackVersion(),
                "MC      : " + meta.getMinecraftVersion(),
                "Update  : " + updateLine(),
                "RAM     : " + mb(rt.totalMemory() - rt.freeMemory()) + " MB used / " + mb(rt.maxMemory()) + " MB allocated",
                "Config  : " + blankOr(PackCoreConfig.lastAppliedVersion) + " (" + blankOr(PackCoreConfig.lastAppliedPackFile) + ")",
                "Backup  : " + formatEpoch(PackCoreConfig.lastBackupEpochMs),
                "Menu    : " + PackCoreConfig.menuStyle.name(),
                "OS      : " + System.getProperty("os.name") + " " + System.getProperty("os.arch"),
                "Screen  : " + screenLine,
                "Java    : " + System.getProperty("java.version"),
                "============================");
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static String section(String title, String[][] fields) {
        int keyWidth = Arrays.stream(fields).mapToInt(f -> f[0].length()).max().orElse(0);
        StringBuilder sb = new StringBuilder("  -- ").append(title).append(" --\n");
        for (String[] f : fields) {
            sb.append(String.format("  %-" + keyWidth + "s : %s%n", f[0], f[1]));
        }
        return sb.append('\n').toString();
    }

    private static long mb(long bytes) {
        return bytes / 1024 / 1024;
    }

    private static String physicalRam(java.lang.management.OperatingSystemMXBean os) {
        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            return mb(sunOs.getTotalMemorySize()) + " MB";
        }
        return "unavailable";
    }

    /** Returns the cached update status line, or "not yet checked" if unavailable. */
    private static String updateLine() {
        UpdateStatus cached = UpdateChecker.getCachedStatus();
        return switch (cached.state()) {
            case UP_TO_DATE       -> "up to date (" + cached.installedVersion() + ")";
            case UPDATE_AVAILABLE -> "update available → " + cached.latestVersion();
            case UNKNOWN          -> "not yet checked";
        };
    }
    private static String formatEpoch(long epochMs) {
        if (epochMs == 0) return "never";
        return DATE_FMT.format(Instant.ofEpochMilli(epochMs));
    }

    /** Returns the value if non-blank, otherwise {@code "none"}. */
    private static String blankOr(String value) {
        return (value == null || value.isBlank()) ? "none" : value;
    }
}