package com.github.kd_gaming1.packcore.mixin.compat.bobby;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ReportedNbtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Backports Bobby's world-index corruption recovery from upstream PR #463.
 * Applied only to Bobby 5.2.13 by {@code PackCoreCompatMixinPlugin}.
 */
@Mixin(targets = "de.johni0702.minecraft.bobby.Worlds", remap = false)
public abstract class WorldsMixin {

    @Unique
    private static final Logger PACKCORE_LOGGER = LoggerFactory.getLogger("PackCore/BobbyCompat");

    @Shadow @Final private Path metaFile;
    @Shadow @Final private Int2ObjectMap<?> worlds;
    @Shadow @Final private List<?> outdatedWorlds;

    @Shadow
    private CompoundTag readFromDisk() {
        throw new AssertionError();
    }

    @Shadow
    private void load(CompoundTag root) {}

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lde/johni0702/minecraft/bobby/Worlds;readFromDisk()Lnet/minecraft/nbt/CompoundTag;"
            ),
            require = 1
    )
    private CompoundTag packcore$readWorldMetadataSafely(@Coerce Object unusedInstance) {
        try {
            CompoundTag root = readFromDisk();
            if (root == null && Files.exists(metaFile)) {
                packcore$discardCorruptMetadata(
                        metaFile,
                        new IOException("Bobby could not decode its world metadata")
                );
            }
            return root;
        } catch (ReportedNbtException e) {
            packcore$discardCorruptMetadata(metaFile, e);
            return null;
        }
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lde/johni0702/minecraft/bobby/Worlds;load(Lnet/minecraft/nbt/CompoundTag;)V"
            ),
            require = 1
    )
    private void packcore$loadWorldMetadataSafely(@Coerce Object unusedInstance, CompoundTag root) {
        if (root == null) {
            load(null);
            return;
        }

        try {
            load(root);
        } catch (RuntimeException e) {
            packcore$discardCorruptMetadata(metaFile, e);
            worlds.clear();
            outdatedWorlds.clear();
            load(null);
        }
    }

    @Unique
    private static void packcore$discardCorruptMetadata(Path file, Exception cause) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException deleteException) {
            cause.addSuppressed(deleteException);
        }
        PACKCORE_LOGGER.error("Failed to read {}, discarding it", file, cause);
    }
}
