package com.github.kd_gaming1.packcore.mixin.compat.bobby;

import it.unimi.dsi.fastutil.longs.Long2LongArrayMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ReportedNbtException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Backports Bobby's region-metadata corruption recovery from upstream PR #463.
 * Applied only to Bobby 5.2.13 by {@code PackCoreCompatMixinPlugin}.
 */
@Mixin(targets = "de.johni0702.minecraft.bobby.Worlds$Region", remap = false)
public class RegionMixin {

    @Redirect(
            method = "read",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;readCompressed(Ljava/io/InputStream;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;"
            ),
            require = 1
    )
    private static CompoundTag packcore$readRegionMetadataSafely(
            InputStream input,
            NbtAccounter accounter,
            Path file,
            @Coerce Object unusedPos
    ) throws IOException {
        try {
            return NbtIo.readCompressed(input, accounter);
        } catch (IOException | ReportedNbtException e) {
            packcore$deleteInvalidMetadata(file, e);
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Invalid compressed NBT in Bobby region metadata", e);
        }
    }

    @Redirect(
            method = "read",
            at = @At(value = "NEW", target = "it/unimi/dsi/fastutil/longs/Long2LongArrayMap"),
            require = 2
    )
    private static Long2LongArrayMap packcore$validateRegionArrays(
            long[] coordinates,
            long[] values,
            Path file,
            @Coerce Object unusedPos
    ) throws IOException {
        if (coordinates.length != values.length) {
            IOException exception = new IOException(
                    "Invalid region metadata: chunk array lengths differ (coordinates="
                            + coordinates.length + ", values=" + values.length + ")"
            );
            packcore$deleteInvalidMetadata(file, exception);
            throw exception;
        }
        return new Long2LongArrayMap(coordinates, values);
    }

    @Unique
    private static void packcore$deleteInvalidMetadata(Path file, Exception cause) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException deleteException) {
            cause.addSuppressed(deleteException);
        }
    }
}
