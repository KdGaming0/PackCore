package com.github.kd_gaming1.packcore.util.config;

import com.github.kd_gaming1.packcore.PackCore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ResourcePackUtil {
    private static final Map<String, String[]> MULTI_PACK_KEYWORDS = Map.of(
            "HypixelPlus", new String[]{"hypixel"},
            "FurfSkyOverlay", new String[]{"overlay", "furfsky"},
            "FurfSkyFull", new String[]{"full", "furfsky"},
            "SkyBlockDarkUI", new String[]{"skyblock", "dark ui", "dark_ui"},
            "Defrosted", new String[]{"defrosted"},
            "Looshy", new String[]{"looshy"}
    );

    private static final Map<String, String> PACK_KEYWORDS = Map.of(
            "HypixelPlus", "hypixel",
            "FurfSkyOverlay", "overlay",
            "FurfSkyFull", "full",
            "SkyBlockDarkUI", "skyblock dark ui",
            "Defrosted", "defrosted",
            "Looshy", "looshy"
    );

    public static CompletableFuture<Boolean> applyResourcePacksOrdered(List<String> packKeysOrdered) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return CompletableFuture.completedFuture(false);
            }

            List<String> foundPackIds = findAvailablePackIdsOrderedList(packKeysOrdered);
            if (foundPackIds.isEmpty()) {
                PackCore.LOGGER.warn("No matching resource packs found for keys: {}", packKeysOrdered);
                return CompletableFuture.completedFuture(false);
            }

            PackCore.LOGGER.info("Found {} packs in order: {}", foundPackIds.size(), foundPackIds);

            client.execute(() -> applyPacksOnMainThread(foundPackIds, result));

        } catch (Exception e) {
            PackCore.LOGGER.error("Failed to apply resource packs", e);
            return CompletableFuture.completedFuture(false);
        }

        return result.completeOnTimeout(false, 10, TimeUnit.SECONDS);
    }

    private static void applyPacksOnMainThread(List<String> foundPackIds, CompletableFuture<Boolean> result) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            ResourcePackManager packManager = client.getResourcePackManager();

            List<String> currentPacks = new ArrayList<>(client.options.resourcePacks);
            List<String> newPacks = new ArrayList<>();
            Set<String> allKnownPackIds = getAllKnownPackIds();

            PackCore.LOGGER.info("Current packs from options.txt: {}", currentPacks);
            PackCore.LOGGER.info("All known managed pack IDs: {}", allKnownPackIds);
            PackCore.LOGGER.info("Packs to add (in order): {}", foundPackIds);

            for (String pack : currentPacks) {
                if (!allKnownPackIds.contains(pack)) {
                    newPacks.add(pack);
                    PackCore.LOGGER.info("Keeping non-managed pack: {}", pack);
                } else {
                    PackCore.LOGGER.info("Removing previously enabled managed pack: {}", pack);
                }
            }

            PackCore.LOGGER.info("Base packs (after removing managed): {}", newPacks);

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

            packManager.setEnabledProfiles(newPacks);
            client.options.resourcePacks.clear();
            client.options.resourcePacks.addAll(newPacks);
            client.options.write();

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

            String[] keywords = getKeywordsForPack(key);
            if (keywords == null) {
                PackCore.LOGGER.warn("No keywords found for pack key: {}", key);
                continue;
            }

            PackCore.LOGGER.info("Looking for pack '{}' (selection #{}) with keywords: {}", key, index + 1, Arrays.toString(keywords));

            ResourcePackProfile bestMatch = findBestMatch(allProfiles, keywords);

            if (bestMatch != null) {
                found.add(bestMatch.getId());
                PackCore.LOGGER.info("MATCHED '{}' -> '{}' (display: '{}') [Selection #{} -> Pack position #{}]",
                        key, bestMatch.getId(), bestMatch.getDisplayName().getString(),
                        index + 1, found.size());
            } else {
                PackCore.LOGGER.warn("No match found for pack '{}' (selection #{}) with keywords: {}", key, index + 1, Arrays.toString(keywords));
            }
        }

        return found;
    }



    private static String[] getKeywordsForPack(String key) {
        String[] keywords = MULTI_PACK_KEYWORDS.get(key);
        if (keywords == null) {
            String keyword = PACK_KEYWORDS.get(key);
            if (keyword != null) {
                keywords = new String[]{keyword};
            }
        }
        return keywords;
    }

    private static ResourcePackProfile findBestMatch(Collection<ResourcePackProfile> allProfiles, String[] keywords) {
        ResourcePackProfile bestMatch = null;
        int bestScore = -1;

        for (ResourcePackProfile profile : allProfiles) {
            String name = stripMinecraftColors(profile.getDisplayName().getString().toLowerCase());
            String id = stripMinecraftColors(profile.getId().toLowerCase());
            String desc = stripMinecraftColors(profile.getDescription().getString().toLowerCase());

            int totalScore = 0;
            int matched = 0;
            for (String kw : keywords) {
                int score = getMatchScore(id, name, desc, kw.toLowerCase());
                if (score > 0) {
                    totalScore += score;
                    matched++;
                }
            }
            totalScore += matched > 1 ? matched * 2 : 0;

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestMatch = profile;
            }
        }
        return bestMatch;
    }

    private static int getMatchScore(String id, String name, String desc, String keyword) {
        if (id.contains(keyword)) return 3;
        if (name.contains(keyword)) return 2;
        if (desc.contains(keyword)) return 1;
        return -1;
    }

    private static Set<String> getAllKnownPackIds() {
        Set<String> allKnown = new HashSet<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return allKnown;

        ResourcePackManager packManager = client.getResourcePackManager();

        // Add single keywords
        Set<String> allKeywords = new HashSet<>(PACK_KEYWORDS.values());

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
            String[] keywords = getKeywordsForPack(key);
            if (keywords == null) continue;

            PackCore.LOGGER.info("Looking for pack '{}' with keywords: {}", key, Arrays.toString(keywords));
            boolean foundMatch = false;

            for (ResourcePackProfile profile : allProfiles) {
                String id = stripMinecraftColors(profile.getId().toLowerCase());

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

    private static String stripMinecraftColors(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}