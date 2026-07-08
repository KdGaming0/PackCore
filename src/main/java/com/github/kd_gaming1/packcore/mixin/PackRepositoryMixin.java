package com.github.kd_gaming1.packcore.mixin;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.config.PackCoreConfig.KeepAboveServerPack;
import com.github.kd_gaming1.packcore.integration.ResourcePackManager;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps PackCore-applied resource packs above a server's own pack.
 *
 * <p>Minecraft marks a downloaded server pack {@code required} + {@code fixedPosition} at the TOP
 * (highest-priority) slot, and {@link net.minecraft.server.packs.repository.Pack.Position#insert}
 * makes every other pack skip over it — so a local pack can never be ordered above it through the
 * pack screen. On join this runs {@code Minecraft.reloadResourcePacks() -> Options
 * .loadSelectedResourcePacks() -> PackRepository.setSelected() -> rebuildSelected()}, so hooking the
 * result of {@code rebuildSelected} lets us re-assert the desired order automatically on every join,
 * with no server-detection needed.
 */
@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    @Inject(method = "rebuildSelected", at = @At("RETURN"), cancellable = true)
    private void packcore$forcePacksAboveServer(Collection<String> ids,
                                                CallbackInfoReturnable<List<Pack>> cir) {
        KeepAboveServerPack mode = PackCoreConfig.keepPacksAboveServerPack;
        if (mode == KeepAboveServerPack.OFF) return;
        // ON_APPLY_ONLY: act only during a PackCore apply, so ordinary reloads and the vanilla
        // pack menu are left untouched (persistence across joins is handled by another mod).
        if (mode == KeepAboveServerPack.ON_APPLY_ONLY && !ResourcePackManager.applyingFromPackCore) return;

        Set<String> forced = parse(PackCoreConfig.packsAboveServer);
        if (forced.isEmpty()) return;

        List<Pack> selected = cir.getReturnValue();
        boolean hasServerPack = selected.stream()
                .anyMatch(p -> p.getPackSource() == PackSource.SERVER);
        if (!hasServerPack) return;

        // Everything else keeps its order (the server pack included); pull out the forced packs.
        List<Pack> kept = new ArrayList<>(selected.size());
        Map<String, Pack> forcedPacks = new HashMap<>();
        for (Pack pack : selected) {
            if (forced.contains(pack.getId())) {
                forcedPacks.put(pack.getId(), pack);
            } else {
                kept.add(pack);
            }
        }
        if (forcedPacks.isEmpty()) return;

        // Re-append forced packs at the end (last = highest priority = above the server pack),
        // in the persisted priority order (highest last).
        for (String id : forced) {
            Pack pack = forcedPacks.get(id);
            if (pack != null) kept.add(pack);
        }
        cir.setReturnValue(List.copyOf(kept));
    }

    private static Set<String> parse(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
