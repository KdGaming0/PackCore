package com.github.kd_gaming1.packcore.update;

import com.github.kd_gaming1.packcore.update.ModrinthClient.VersionInfo;

import java.util.Optional;

/** In-memory cache — resets on every restart. */
public final class UpdateCache {

    private static VersionInfo cachedVersionInfo = null;

    private UpdateCache() {}

    public static Optional<VersionInfo> get() {
        return Optional.ofNullable(cachedVersionInfo);
    }

    public static void set(VersionInfo versionInfo) {
        cachedVersionInfo = versionInfo;
    }

    public static void invalidate() {
        cachedVersionInfo = null;
    }
}