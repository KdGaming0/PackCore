package com.github.kd_gaming1.packcore.update;

import com.github.kd_gaming1.packcore.update.ModrinthClient.VersionInfo;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** In-memory cache — resets on every restart. */
public final class UpdateCache {

    private static final AtomicReference<VersionInfo> cachedVersionInfo = new AtomicReference<>();

    private UpdateCache() {}

    public static Optional<VersionInfo> get() {
        return Optional.ofNullable(cachedVersionInfo.get());
    }

    public static void set(VersionInfo versionInfo) {
        cachedVersionInfo.set(versionInfo);
    }

    public static void invalidate() {
        cachedVersionInfo.set(null);
    }
}