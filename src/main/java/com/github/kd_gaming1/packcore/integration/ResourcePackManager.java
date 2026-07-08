package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.util.CaxtonFontDetector;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applies a set of resource pack IDs on top of the user's current pack
 * selection, preserving order, then triggers a full client reload.
 *
 * <p>Final pack order: existing user packs first, the requested packs
 * appended at the end (last entry = highest priority).
 */
public final class ResourcePackManager {

    /**
     * True while a PackCore-initiated pack reorder is in flight (set around {@code setSelected} +
     * the synchronous reload it triggers). {@code PackRepositoryMixin} reads this in
     * {@link PackCoreConfig.KeepAboveServerPack#ON_APPLY_ONLY} mode so it lifts packs above the
     * server pack only during a PackCore apply, never on ordinary reloads or the vanilla menu.
     */
    public static volatile boolean applyingFromPackCore = false;

    private ResourcePackManager() {}

    /**
     * Whether {@code pack} is a pack the user picks in the wizard — a loose pack from the
     * {@code resourcepacks/} folder ({@link PackSource#DEFAULT}), excluding vanilla. Mod-provided
     * and built-in packs (e.g. Caxton fonts, which use a namespaced id) are handled elsewhere and
     * must never be touched by the resource-pack selection.
     */
    public static boolean isUserSelectable(Pack pack) {
        return pack.getPackSource() == PackSource.DEFAULT && !pack.getId().equals("vanilla");
    }

    /** Ids of every currently-available user-selectable pack (see {@link #isUserSelectable}). */
    public static Set<String> availableUserSelectablePackIds() {
        return Minecraft.getInstance()
                .getResourcePackRepository()
                .getAvailablePacks()
                .stream()
                .filter(ResourcePackManager::isUserSelectable)
                .map(Pack::getId)
                .collect(Collectors.toSet());
    }

    public static void apply(Set<String> packIds) {
        apply(packIds, Set.of());
    }

    /**
     * @param packIds    packs to enable, in the order they should be appended — since the last entry
     *                   wins conflicts, the highest-priority pack must come last. Excluded packs are
     *                   stripped from their existing position first, so this order is authoritative
     *                   for every pack it contains.
     * @param excludeIds packs to remove from the existing selection entirely; any that also appear in
     *                   {@code packIds} are re-added at their requested position, giving the caller
     *                   full control over ordering rather than preserving the old position.
     */
    public static void apply(Collection<String> packIds, Set<String> excludeIds) {
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

        // Build the final order: keep existing entries except every excluded id, then append the
        // requested packs in the given order. Dropping excluded packs unconditionally (even ones
        // also in packIds) lets the append order fully control their final priority, so reordering
        // a re-selected pack takes effect. LinkedHashSet preserves order and dedupes in one pass.
        LinkedHashSet<String> finalOrder = new LinkedHashSet<>();
        for (String id : client.options.resourcePacks) {
            if (excludeIds.contains(id)) continue;
            finalOrder.add(id);
        }
        for (String id : packIds) {
            if (availableIds.contains(id)) {
                finalOrder.add(id);
            }
        }

        List<String> orderedList = new ArrayList<>(finalOrder);
        PackCore.LOGGER.info("ResourcePack: applying order: {}", orderedList);

        // Remember which packs PackCore applied, in priority order (highest last), so
        // PackRepositoryMixin can keep them above a server's own pack on every reload.
        List<String> appliedPacks = new ArrayList<>();
        for (String id : packIds) {
            if (availableIds.contains(id)) {
                appliedPacks.add(id);
            }
        }
        PackCoreConfig.packsAboveServer = String.join(",", appliedPacks);
        MidnightConfig.write(PackCore.MOD_ID);

        // Mark this as a PackCore-initiated reorder so PackRepositoryMixin lifts packs above the
        // server pack even in ON_APPLY_ONLY mode. setSelected and the synchronous reload() inside
        // reloadResourcePacks() both run rebuildSelected before the returned future completes, so
        // clearing the flag once reloadResourcePacks() returns still covers both.
        applyingFromPackCore = true;
        java.util.concurrent.CompletableFuture<Void> reload;
        try {
            repo.setSelected(orderedList);
            client.options.resourcePacks.clear();
            client.options.resourcePacks.addAll(orderedList);
            client.options.save();
            reload = client.reloadResourcePacks();
        } finally {
            applyingFromPackCore = false;
        }

        // Recompute Caxton state once, after the reload finishes — the
        // FontSet.setFonts mixin will also fire, but this is a safety net
        // for the case where no font set is rebuilt (e.g. the new packs
        // don't touch fonts at all).
        reload.whenComplete((res, ex) -> {
            if (ex != null) {
                PackCore.LOGGER.error("ResourcePack: reload failed", ex);
                return;
            }
            CaxtonFontDetector.recompute();
        });
    }
}