package com.github.kd_gaming1.packcore.util.config;

import com.github.kd_gaming1.packcore.PackCore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ResourcePackUtil {

    private static final Map<String, String> PACK_KEYWORDS = Map.of(
            "HypixelPlus", "hypixel",                    // Matches "Hypixel+ 0.23.4"
            "FurfSkyOverlay", "overlay",                 // Matches "§aFurf§bSky §6Reborn §f§lOVERLAY§r"
            "FurfSkyFull", "furfsky",                    // Better match for FurfSky packs
            "SkyBlockDarkUI", "skyblock dark ui",        // Matches "Skyblock Dark UI 1.5"
            "Defrosted", "defrosted",                    // Matches "! §3defrosted §8[§f16x8]"
            "Looshy", "looshy"                           // Matches "§dlooshy §7[§f1.21.8§7]"
    );

    // Pack name mappings - updated to match actual pack names
    private static final Map<String, String[]> MULTI_PACK_KEYWORDS = Map.of(
            "HypixelPlus", new String[]{"hypixel"},
            "FurfSkyOverlay", new String[]{"overlay", "furfsky"},
            "FurfSkyFull", new String[]{"full", "furfsky"},        // Now looks for both!
            "SkyBlockDarkUI", new String[]{"skyblock", "dark ui", "dark_ui"},
            "Defrosted", new String[]{"defrosted"},
            "Looshy", new String[]{"looshy"}
    );

    public static CompletableFuture<Boolean> applyResourcePacksOrdered(List<String> packKeysOrdered) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                result.complete(false);
                return result.completeOnTimeout(false, 10, TimeUnit.SECONDS);
            }

            // Find actual pack IDs that exist - PRESERVING ORDER
            List<String> foundPackIds = findAvailablePackIdsOrderedList(packKeysOrdered);
            if (foundPackIds.isEmpty()) {
                PackCore.LOGGER.warn("No matching resource packs found for keys: {}", packKeysOrdered);
                result.complete(false);
                return result.completeOnTimeout(false, 10, TimeUnit.SECONDS);
            }

            PackCore.LOGGER.info("Found {} packs in order: {}", foundPackIds.size(), foundPackIds);

            // Execute UI / pack-manager changes on the main thread and complete the result asynchronously.
            client.execute(() -> {
                try {
                    ResourcePackManager packManager = client.getResourcePackManager();

                    // Get current packs
                    List<String> currentPacks = new ArrayList<>(client.options.resourcePacks);
                    List<String> newPacks = new ArrayList<>();

                    // Get ALL possible pack IDs from our keywords
                    Set<String> allKnownPackIds = getAllKnownPackIds();

                    PackCore.LOGGER.info("Current packs from options.txt: {}", currentPacks);
                    PackCore.LOGGER.info("All known managed pack IDs: {}", allKnownPackIds);
                    PackCore.LOGGER.info("Packs to add (in order): {}", foundPackIds);

                    // Keep packs that aren't in our known list (vanilla, mods, etc)
                    for (String pack : currentPacks) {
                        if (!allKnownPackIds.contains(pack)) {
                            newPacks.add(pack);
                            PackCore.LOGGER.info("Keeping non-managed pack: {}", pack);
                        } else {
                            PackCore.LOGGER.info("Removing previously enabled managed pack: {}", pack);
                        }
                    }

                    PackCore.LOGGER.info("Base packs (after removing managed): {}", newPacks);

                    // Add selected packs IN THE ORDER THEY WERE SELECTED
                    // First selected = first added = higher priority in resource loading
                    Collections.reverse(foundPackIds);

                    for (int i = 0; i < foundPackIds.size(); i++) {
                        String packId = foundPackIds.get(i);
                        if (!newPacks.contains(packId)) {
                            newPacks.add(packId);
                            PackCore.LOGGER.info("Adding resource pack: {} (reversed index: {}, final position: {})",
                                    packId, i + 1, newPacks.size() - 1);
                        } else {
                            PackCore.LOGGER.warn("Pack {} was already in the list, skipping duplicate", packId);
                        }
                    }

                    PackCore.LOGGER.info("Final pack order for options.txt: {}", newPacks);

                    // Apply the changes
                    packManager.setEnabledProfiles(newPacks);
                    client.options.resourcePacks.clear();
                    client.options.resourcePacks.addAll(newPacks);
                    client.options.write();

                    // Reload resources and complete result accordingly
                    client.reloadResources().thenRun(() -> {
                        PackCore.LOGGER.info("Resource reload completed successfully");
                        result.complete(true);
                    }).exceptionally(e -> {
                        PackCore.LOGGER.error("Resource reload failed", e);
                        result.complete(false);
                        return null;
                    });

                } catch (Exception e) {
                    PackCore.LOGGER.error("Failed to apply packs", e);
                    result.complete(false);
                }
            });

        } catch (Exception e) {
            PackCore.LOGGER.error("Failed to apply resource packs", e);
            result.complete(false);
        }

        // Fail-fast: if reload never completes, treat as failed after timeout
        return result.completeOnTimeout(false, 10, TimeUnit.SECONDS);
    }

    private static List<String> findAvailablePackIdsOrderedList(List<String> packKeysOrdered) {
        List<String> found = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return found;

        ResourcePackManager packManager = client.getResourcePackManager();
        Collection<ResourcePackProfile> allProfiles = packManager.getProfiles();

        for (int index = 0; index < packKeysOrdered.size(); index++) {
            String key = packKeysOrdered.get(index).trim();
            if (key.isEmpty()) continue;

            String[] keywords = MULTI_PACK_KEYWORDS.get(key);
            if (keywords == null) {
                String keyword = PACK_KEYWORDS.get(key);
                if (keyword != null) {
                    keywords = new String[]{keyword};
                } else {
                    PackCore.LOGGER.warn("No keywords found for pack key: {}", key);
                    continue;
                }
            }

            PackCore.LOGGER.info("Looking for pack '{}' (selection #{}) with keywords: {}",
                    key, index + 1, Arrays.toString(keywords));

            ResourcePackProfile bestMatch = null;
            int bestScore = -1;
            String matchedKeyword = null;

            for (ResourcePackProfile profile : allProfiles) {
                String name = stripMinecraftColors(profile.getDisplayName().getString().toLowerCase());
                String id = stripMinecraftColors(profile.getId().toLowerCase());
                String desc = stripMinecraftColors(profile.getDescription().getString().toLowerCase());

                for (String keyword : keywords) {
                    String kw = keyword.toLowerCase();

                    int score = -1;
                    // Highest priority: exact ID contains keyword
                    if (id.contains(kw)) score = 3;
                        // Next: display name contains keyword
                    else if (name.contains(kw)) score = 2;
                        // Last: description contains keyword
                    else if (desc.contains(kw)) score = 1;

                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = profile;
                        matchedKeyword = kw;
                    }
                }
            }

            if (bestMatch != null) {
                found.add(bestMatch.getId());
                PackCore.LOGGER.info("MATCHED '{}' -> '{}' (display: '{}') via keyword '{}' [Selection #{} -> Pack position #{}]",
                        key, bestMatch.getId(), bestMatch.getDisplayName().getString(),
                        matchedKeyword, index + 1, found.size());
            } else {
                PackCore.LOGGER.warn("No match found for pack '{}' (selection #{}) with keywords: {}",
                        key, index + 1, Arrays.toString(keywords));
            }
        }

        return found;
    }


    private static Set<String> getAllKnownPackIds() {
        Set<String> allKnown = new HashSet<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return allKnown;

        ResourcePackManager packManager = client.getResourcePackManager();

        // Check both single keywords AND multi keywords
        Set<String> allKeywords = new HashSet<>();

        // Add single keywords
        allKeywords.addAll(PACK_KEYWORDS.values());

        // Add multi keywords
        for (String[] keywords : MULTI_PACK_KEYWORDS.values()) {
            allKeywords.addAll(Arrays.asList(keywords));
        }

        // Find all packs that match any of our keywords
        for (String keyword : allKeywords) {
            for (ResourcePackProfile profile : packManager.getProfiles()) {
                String name = stripMinecraftColors(profile.getDisplayName().getString().toLowerCase());
                String id = stripMinecraftColors(profile.getId().toLowerCase());
                String desc = stripMinecraftColors(profile.getDescription().getString().toLowerCase());

                if (name.contains(keyword) || id.contains(keyword) || desc.contains(keyword)) {
                    allKnown.add(profile.getId());
                }
            }
        }

        PackCore.LOGGER.debug("All known managed pack IDs: {}", allKnown);
        return allKnown;
    }

    private static Set<String> findAvailablePackIds(Set<String> packKeys) {
        Set<String> found = new HashSet<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return found;

        ResourcePackManager packManager = client.getResourcePackManager();
        Collection<ResourcePackProfile> allProfiles = packManager.getProfiles();

        for (String key : packKeys) {
            String[] keywords = MULTI_PACK_KEYWORDS.get(key);
            if (keywords == null) {
                // Fallback to single keyword
                String keyword = PACK_KEYWORDS.get(key);
                if (keyword != null) {
                    keywords = new String[]{keyword};
                } else {
                    continue;
                }
            }

            PackCore.LOGGER.info("Looking for pack '{}' with keywords: {}", key, Arrays.toString(keywords));
            boolean foundMatch = false;

            for (ResourcePackProfile profile : allProfiles) {
                String id = stripMinecraftColors(profile.getId().toLowerCase());

                // Check if any keyword matches the filename only
                for (String keyword : keywords) {
                    if (id.contains(keyword)) {
                        found.add(profile.getId());
                        PackCore.LOGGER.info("MATCHED '{}' -> '{}' via keyword '{}' in filename",
                                key, profile.getId(), keyword);
                        foundMatch = true;
                        break;
                    }
                }
                if (foundMatch) break;
            }

            if (!foundMatch) {
                PackCore.LOGGER.warn("No match found for key '{}' with keywords: {}", key, Arrays.toString(keywords));
            }
        }
        return found;
    }

    // Helper method to remove Minecraft color codes (§x)
    private static String stripMinecraftColors(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}