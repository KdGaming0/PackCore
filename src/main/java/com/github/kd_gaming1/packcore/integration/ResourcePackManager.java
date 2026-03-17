package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ResourcePackManager {

    /**
     * Applies the given pack IDs on top of any currently enabled non-wizard packs,
     * then triggers a full resource reload.
     * <p>
     * Pack order: existing user packs first, wizard-selected packs on top (last = highest priority).
     *
     * @throws RuntimeException if the reload fails
     */
    public static void apply(Set<String> packIds) {
        Minecraft client = Minecraft.getInstance();
        PackRepository repo = client.getResourcePackRepository();

        repo.reload();

        Collection<String> availableIds = repo.getAvailableIds();

        // Validate — warn about any selected IDs that no longer exist on disk
        for (String id : packIds) {
            if (!availableIds.contains(id)) {
                PackCore.LOGGER.warn("ResourcePack: selected pack '{}' is not available, skipping", id);
            }
        }

        // Keep existing enabled packs that we didn't touch, then append wizard packs on top
        List<String> finalOrder = new ArrayList<>();

        for (String id : client.options.resourcePacks) {
            if (!packIds.contains(id)) {
                finalOrder.add(id);
            }
        }

        for (String id : packIds) {
            if (availableIds.contains(id) && !finalOrder.contains(id)) {
                finalOrder.add(id);
            }
        }

        PackCore.LOGGER.info("ResourcePack: applying order: {}", finalOrder);

        repo.setSelected(finalOrder);
        client.options.resourcePacks.clear();
        client.options.resourcePacks.addAll(finalOrder);
        client.options.save();

        client.reloadResourcePacks().whenComplete((res, ex) -> {
            if (ex != null) {
                PackCore.LOGGER.error("ResourcePack: reload failed", ex);
            }
        });
    }
}