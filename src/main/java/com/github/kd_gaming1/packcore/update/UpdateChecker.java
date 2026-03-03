package com.github.kd_gaming1.packcore.update;

import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import com.github.kd_gaming1.packcore.update.ModrinthClient.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class UpdateChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/UpdateChecker");

    // Dedicated executor for network I/O operations
    private static final Executor NETWORK_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private UpdateChecker() {}

    /**
     * Runs the update check on a background thread — never blocks the game thread.
     * Usage: UpdateChecker.checkAsync().thenAccept(status -> { ... });
     */
    public static CompletableFuture<UpdateStatus> checkAsync() {
        return CompletableFuture.supplyAsync(UpdateChecker::check, NETWORK_EXECUTOR);
    }

    public static UpdateStatus check() {
        ModpackMetadata metadata = ModpackMetadata.getInstance();
        String projectId = metadata.getModrinthProjectId();
        String installedVersion = metadata.getModpackVersion();

        if (projectId == null || projectId.isBlank()) {
            LOGGER.warn("No Modrinth project ID set in modpack.json, skipping update check.");
            return UpdateStatus.unknown();
        }

        Optional<VersionInfo> cached = UpdateCache.get();
        VersionInfo versionInfo;

        if (cached.isPresent()) {
            versionInfo = cached.get();
            LOGGER.info("Using cached latest version: {}", versionInfo.versionNumber());
        } else {
            Optional<VersionInfo> fetched = ModrinthClient.fetchLatestVersion(projectId);
            if (fetched.isEmpty()) return UpdateStatus.unknown();

            versionInfo = fetched.get();
            UpdateCache.set(versionInfo);
            LOGGER.info("Fetched latest version from Modrinth: {}", versionInfo.versionNumber());
        }

        UpdateStatus status = isNewerVersion(versionInfo.versionNumber(), installedVersion)
                ? UpdateStatus.updateAvailable(installedVersion, versionInfo.versionNumber(), versionInfo.changelog())
                : UpdateStatus.upToDate(installedVersion);

        LOGGER.info("Update check result: {} (installed: {}, latest: {})",
                status.state(), installedVersion, versionInfo.versionNumber());

        return status;
    }

    public static boolean isNewerVersion(String available, String installed) {
        String[] availableParts = available.split("\\.");
        String[] installedParts = installed.split("\\.");

        int segmentCount = Math.max(availableParts.length, installedParts.length);

        for (int i = 0; i < segmentCount; i++) {
            int availableSegment = i < availableParts.length ? parseSegment(availableParts[i]) : 0;
            int installedSegment = i < installedParts.length ? parseSegment(installedParts[i]) : 0;

            if (availableSegment != installedSegment) {
                return availableSegment > installedSegment;
            }
        }

        return false;
    }

    private static int parseSegment(String segment) {
        try {
            return Integer.parseInt(segment.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}