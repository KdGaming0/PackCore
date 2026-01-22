package com.github.kd_gaming1.packcore.crash;

import com.github.kd_gaming1.packcore.PackCore;
import com.github. kd_gaming1.packcore.modpack.ModpackInfo;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Logs modpack branding information at startup.
 * Helps with log-based debugging when crash reports aren't available.
 */
public class CrashBrandingLogger {

    /**
     * Print modpack branding information to logs on startup.
     * Call this from PreLaunchEntrypoint or ClientModInitializer.
     */
    public static void logBrandingInfo() {
        ModpackInfo info = PackCore.getModpackInfo();

        if (info == null) {
            PackCore.LOGGER.warn("╔══════════════════════════════════════════════════════════════╗");
            PackCore. LOGGER.warn("║           Modpack Information - Not Available                ║");
            PackCore. LOGGER.warn("╚══════════════════════════════════════════════════════════════╝");
            return;
        }

        // Build the branding info display
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║                   MODPACK INFORMATION                        ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");

        // Add modpack identification
        appendField(sb, "Name", info.getName(), "YOUR_MODPACK_NAME_HERE");
        appendField(sb, "Version", info.getVersion(), null);
        appendField(sb, "Minecraft", info.getMinecraftVersion(), null);
        appendField(sb, "Author", info.getAuthor(), "YOUR_NAME_HERE");

        // Add separator if we have support links
        boolean hasLinks = hasValidField(info. getDiscord(), "your-invite") ||
                hasValidField(info.getIssueTracker(), "yourname/yourmod") ||
                hasValidField(info.getWebsite(), "your-website");

        if (hasLinks) {
            sb.append("╟──────────────────────────────────────────────────────────────╢\n");
            appendField(sb, "Discord", info.getDiscord(), "your-invite");
            appendField(sb, "Issue Tracker", info.getIssueTracker(), "yourname/yourmod");
            appendField(sb, "Website", info.getWebsite(), "your-website");
            appendField(sb, "Wiki", info.getWiki(), "your-wiki");
        }

        // Add technical info
        sb.append("╟──────────────────────────────────────────────────────────────╢\n");

        // Fabric Loader version
        String loaderVersion = FabricLoader. getInstance()
                .getModContainer("fabricloader")
                .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
                .orElse("Unknown");
        appendInfoLine(sb, "Fabric Loader", loaderVersion);

        // PackCore version
        String packcoreVersion = FabricLoader.getInstance()
                .getModContainer("packcore")
                .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
                .orElse("Unknown");
        appendInfoLine(sb, "PackCore", packcoreVersion);

        // Total mods
        int modCount = FabricLoader.getInstance().getAllMods().size();
        appendInfoLine(sb, "Total Mods", String.valueOf(modCount));

        // Java version
        String javaVersion = System.getProperty("java.version");
        appendInfoLine(sb, "Java", javaVersion);

        // Add description if available
        String description = info.getDescription();
        if (description != null && ! description.equals("A brief description of your modpack") && !description.isBlank()) {
            sb.append("╟────────────────────────────��────────────────────────────────╢\n");
            // Wrap description to 60 chars
            String wrappedDesc = wrapText(description, 58);
            for (String line : wrappedDesc.split("\n")) {
                sb.append(String.format("║ %-60s ║\n", line));
            }
        }

        sb.append("╚══════════════════════════════════════════════════════════════╝");

        PackCore.LOGGER.info(sb.toString());
    }

    /**
     * Append a field to the output if it has a valid value.
     */
    private static void appendField(StringBuilder sb, String label, String value, String invalidPattern) {
        if (hasValidField(value, invalidPattern)) {
            appendInfoLine(sb, label, value);
        }
    }

    /**
     * Append a line with proper formatting.
     */
    private static void appendInfoLine(StringBuilder sb, String label, String value) {
        String line = String.format("%s: %s", label, value);
        if (line.length() > 58) {
            line = line.substring(0, 55) + "...";
        }
        sb.append(String.format("║ %-60s ║\n", line));
    }

    /**
     * Check if a field has a valid (non-default) value.
     */
    private static boolean hasValidField(String value, String invalidPattern) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (invalidPattern != null && value.contains(invalidPattern)) {
            return false;
        }
        return true;
    }

    /**
     * Wrap text to a maximum line length.
     */
    private static String wrapText(String text, int maxLength) {
        if (text. length() <= maxLength) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine. length() + word.length() + 1 > maxLength) {
                if (currentLine.length() > 0) {
                    result. append(currentLine).append("\n");
                    currentLine = new StringBuilder();
                }
            }

            if (currentLine.length() > 0) {
                currentLine. append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            result.append(currentLine);
        }

        return result.toString();
    }
}