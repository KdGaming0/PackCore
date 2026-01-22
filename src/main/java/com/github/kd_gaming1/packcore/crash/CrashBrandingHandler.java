package com.github.kd_gaming1.packcore.crash;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.modpack.ModpackInfo;
import net.minecraft.util.crash.CrashReportSection;

/**
 * Handles adding modpack branding information to crash reports.
 * This makes it easier for users and support teams to identify the exact
 * modpack version and get support resources.
 */
public class CrashBrandingHandler {

    /**
     * Adds modpack branding information to a crash report section.
     *
     * @param section The crash report section to add information to
     */
    public static void addBranding(CrashReportSection section) {
        ModpackInfo info = PackCore.getModpackInfo();

        if (info == null) {
            section.add("Status", "Modpack information not available");
            return;
        }

        // Add modpack identification
        String name = info.getName();
        String version = info. getVersion();

        if (name != null && ! name.equals("YOUR_MODPACK_NAME_HERE") && ! name.isBlank()) {
            section. add("Name", name);
        }

        if (version != null && !version.isBlank()) {
            section. add("Version", version);
        }

        // Add Minecraft version
        String mcVersion = info.getMinecraftVersion();
        if (mcVersion != null && !mcVersion.isBlank()) {
            section. add("Minecraft Version", mcVersion);
        }

        // Add author info
        String author = info.getAuthor();
        if (author != null && !author.equals("YOUR_NAME_HERE") && !author.isBlank()) {
            section.add("Author", author);
        }

        // Add support resources (most important for users!)
        String discord = info.getDiscord();
        if (discord != null && ! discord.contains("your-invite") && !discord.isBlank()) {
            section.add("Discord Support", discord);
        }

        String issueTracker = info.getIssueTracker();
        if (issueTracker != null && !issueTracker.contains("yourname/yourmod") && !issueTracker.isBlank()) {
            section.add("Issue Tracker", issueTracker);
        }

        String website = info.getWebsite();
        if (website != null && ! website.contains("your-website") && !website.isBlank()) {
            section.add("Website", website);
        }

        String wiki = info.getWiki();
        if (wiki != null && !wiki.contains("your-wiki") && !wiki.isBlank()) {
            section.add("Wiki", wiki);
        }

        // Add description if available
        String description = info. getDescription();
        if (description != null && !description.equals("A brief description of your modpack") && !description.isBlank()) {
            section.add("Description", description);
        }

        // Add Modrinth project info
        String modrinthId = info.getModrinthProjectId();
        if (modrinthId != null && !modrinthId.equals("YOUR_PROJECT_ID_FROM_MODRINTH_URL") && !modrinthId.isBlank()) {
            section.add("Modrinth Project", "https://modrinth.com/modpack/" + modrinthId);
        }
    }
}