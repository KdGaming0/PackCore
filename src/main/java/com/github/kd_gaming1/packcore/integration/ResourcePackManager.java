package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.util.CaxtonFontDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies a set of resource pack IDs on top of the user's current pack
 * selection, preserving order, then triggers a full client reload.
 *
 * <p>Final pack order: existing user packs first, the requested packs
 * appended at the end (last entry = highest priority).
 */
public final class ResourcePackManager {

    private ResourcePackManager() {}

    public static void apply(Set<String> packIds) {
        apply(packIds, Set.of());
    }

    /**
     * @param packIds    packs to ensure are enabled (added to the end of the order)
     * @param excludeIds packs to remove from the existing selection (unless also in {@code packIds})
     */
    public static void apply(Set<String> packIds, Set<String> excludeIds) {
        Minecraft client = Minecraft.getInstance();
        PackRepository repo = client.getResourcePackRepository();

        // Synchronously re-scan pack sources so repo.getAvailableIds() is a
        // fresh snapshot for the rest of this method. This does not rebuild
        // the client resource manager; the expensive full reload happens via
        // client.reloadResourcePacks() at the end.
        repo.reload();

        Collection<String> availableIds = repo.getAvailableIds();
        for (String id : packIds) {
            if (!availableIds.contains(id)) {
                PackCore.LOGGER.warn("ResourcePack: '{}' is not available, skipping", id);
            }
        }

        // Build the final order: keep existing entries (minus excludes that
        // weren't explicitly requested), then append the requested packs.
        // LinkedHashSet preserves insertion order and dedupes in one pass.
        LinkedHashSet<String> finalOrder = new LinkedHashSet<>();
        for (String id : client.options.resourcePacks) {
            if (excludeIds.contains(id) && !packIds.contains(id)) continue;
            finalOrder.add(id);
        }
        for (String id : packIds) {
            if (availableIds.contains(id)) {
                finalOrder.add(id);
            }
        }

        List<String> orderedList = new ArrayList<>(finalOrder);
        PackCore.LOGGER.info("ResourcePack: applying order: {}", orderedList);

        repo.setSelected(orderedList);
        client.options.resourcePacks.clear();
        client.options.resourcePacks.addAll(orderedList);
        client.options.save();

        // Recompute Caxton state once, after the reload finishes — the
        // FontSet.setFonts mixin will also fire, but this is a safety net
        // for the case where no font set is rebuilt (e.g. the new packs
        // don't touch fonts at all).
        client.reloadResourcePacks().whenComplete((res, ex) -> {
            if (ex != null) {
                PackCore.LOGGER.error("ResourcePack: reload failed", ex);
                return;
            }
            CaxtonFontDetector.recompute();
        });
    }
}