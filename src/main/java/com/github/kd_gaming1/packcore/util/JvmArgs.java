package com.github.kd_gaming1.packcore.util;

import java.lang.management.ManagementFactory;
import java.util.Locale;

public final class JvmArgs {

    private JvmArgs() {}

    // ---------------------------------------------------------------
    // Stack-size check
    // ---------------------------------------------------------------

    public static boolean hasXssAtLeast(long thresholdBytes) {
        return ManagementFactory.getRuntimeMXBean().getInputArguments()
                .stream()
                .anyMatch(arg -> xssBytes(arg) >= thresholdBytes);
    }

    private static long xssBytes(String arg) {
        if (arg == null || !arg.startsWith("-Xss")) return -1L;

        String val = arg.substring(4).trim().toLowerCase(Locale.ROOT);
        if (val.isEmpty()) return -1L;

        try {
            char suffix = val.charAt(val.length() - 1);
            long multiplier = 1L;
            String number = val;

            if (suffix == 'k' || suffix == 'm' || suffix == 'g') {
                number = val.substring(0, val.length() - 1);
                multiplier = switch (suffix) {
                    case 'k' -> 1024L;
                    case 'm' -> 1024L * 1024L;
                    case 'g' -> 1024L * 1024L * 1024L;
                    default  -> 1L;
                };
            }

            return Long.parseLong(number) * multiplier;
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    // ---------------------------------------------------------------
    // Launcher detection — best-effort, used for hint messages only
    // ---------------------------------------------------------------

    public enum Launcher {
        PRISM_POLYMC,
        CURSEFORGE,
        ATLAUNCHER,
        MODRINTH,
        OFFICIAL,
        UNKNOWN;

        /** Human-readable display name. */
        public String displayName() {
            return switch (this) {
                case PRISM_POLYMC  -> "Prism / PolyMC";
                case CURSEFORGE    -> "CurseForge";
                case ATLAUNCHER    -> "ATLauncher";
                case MODRINTH      -> "Modrinth App";
                case OFFICIAL      -> "Official Launcher";
                case UNKNOWN       -> "your launcher";
            };
        }
    }

    /**
     * Attempts to identify the launcher by inspecting system properties and
     * JVM arguments set by known launchers.
     */
    public static Launcher detectLauncher() {
        // Prism / PolyMC set a dedicated system property
        String prism = System.getProperty("org.prismlauncher.instance.name");
        if (prism != null) return Launcher.PRISM_POLYMC;

        // ATLauncher sets this property
        String atl = System.getProperty("atlauncher.instance.name");
        if (atl != null) return Launcher.ATLAUNCHER;

        // CurseForge passes a recognisable classpath or agent path
        String classPath = System.getProperty("java.class.path", "");
        if (classPath.toLowerCase(Locale.ROOT).contains("curseforge")) return Launcher.CURSEFORGE;

        // Modrinth App (theseus) sets launcher metadata in the classpath too
        if (classPath.toLowerCase(Locale.ROOT).contains("modrinth")
                || classPath.toLowerCase(Locale.ROOT).contains("theseus")) {
            return Launcher.MODRINTH;
        }

        // Official launcher leaves a distinctive version-type argument
        boolean hasOfficialArg = ManagementFactory.getRuntimeMXBean().getInputArguments()
                .stream()
                .anyMatch(a -> a.contains("minecraft-launcher") || a.contains("launcher_name"));
        if (hasOfficialArg) return Launcher.OFFICIAL;

        return Launcher.UNKNOWN;
    }

    /**
     * Returns step-by-step instructions for adding {@code -Xss4M} in the
     * detected (or provided) launcher.
     */
    public static String xss4MInstructions(Launcher launcher) {
        return switch (launcher) {
            case PRISM_POLYMC ->
                    "In Prism / PolyMC:\n" +
                            "  1. Right-click your instance → Edit\n" +
                            "  2. Go to the \"Settings\" tab → \"Java\" section\n" +
                            "  3. Add  -Xss4M  to the \"JVM Arguments\" field\n" +
                            "  4. Click OK and relaunch";
            case CURSEFORGE ->
                    "In CurseForge:\n" +
                            "  1. Open the profile → click the three-dot menu → Profile Options\n" +
                            "  2. Enable \"Additional Java Arguments\"\n" +
                            "  3. Add  -Xss4M  to the field\n" +
                            "  4. Save and relaunch";
            case ATLAUNCHER ->
                    "In ATLauncher:\n" +
                            "  1. Open Settings → Java/Minecraft tab\n" +
                            "  2. Add  -Xss4M  to \"Extra Java Parameters\"\n" +
                            "  3. Save and relaunch";
            case MODRINTH ->
                    "In the Modrinth App:\n" +
                            "  1. Click the instance → setting icon in the top right corner\n" +
                            "  2. Under \"Java Settings\", find \"Java arguments\"\n" +
                            "  3. Add  -Xss4M\n" +
                            "  4. Relaunch";
            case OFFICIAL ->
                    "In the Official Launcher:\n" +
                            "  1. Open Installations → hover your profile → click the pencil icon\n" +
                            "  2. Click \"More Options\" at the bottom\n" +
                            "  3. In \"JVM Arguments\", add  -Xss4M  before the existing flags\n" +
                            "  4. Save and relaunch";
            default ->
                    "To add the JVM argument:\n" +
                            "  1. Open your launcher and find the instance/profile settings\n" +
                            "  2. Look for a \"JVM Arguments\" or \"Java Arguments\" field\n" +
                            "  3. Add  -Xss4M  to the field\n" +
                            "  4. Save and relaunch";
        };
    }

    /** Convenience overload — auto-detects the launcher. */
    public static String xss4MInstructions() {
        return xss4MInstructions(detectLauncher());
    }
}