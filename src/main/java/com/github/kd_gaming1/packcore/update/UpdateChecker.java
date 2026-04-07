package com.github.kd_gaming1.packcore.update;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import com.github.kd_gaming1.packcore.update.ModrinthClient.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public final class UpdateChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/UpdateChecker");
    private static final Executor NETWORK_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private static final AtomicReference<UpdateStatus> CACHED_STATUS =
            new AtomicReference<>(UpdateStatus.unknown());
    private static final AtomicReference<CompletableFuture<UpdateStatus>> IN_FLIGHT =
            new AtomicReference<>();

    private UpdateChecker() {}

    public static UpdateStatus getCachedStatus() {
        return CACHED_STATUS.get();
    }

    public static CompletableFuture<UpdateStatus> checkAsync() {
        CompletableFuture<UpdateStatus> existing = IN_FLIGHT.get();
        if (existing != null) {
            return existing;
        }

        CompletableFuture<UpdateStatus> created = CompletableFuture
                .supplyAsync(UpdateChecker::check, NETWORK_EXECUTOR)
                .thenApply(status -> {
                    CACHED_STATUS.set(status);
                    return status;
                })
                .whenComplete((result, throwable) -> IN_FLIGHT.set(null));

        if (IN_FLIGHT.compareAndSet(null, created)) {
            return created;
        }

        CompletableFuture<UpdateStatus> winner = IN_FLIGHT.get();
        return winner != null ? winner : created;
    }

    private static UpdateStatus check() {
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
            boolean includeBeta = PackCoreConfig.showBetaUpdateNotifications;
            Optional<VersionInfo> fetched = ModrinthClient.fetchLatestVersion(projectId, includeBeta);
            if (fetched.isEmpty()) {
                return UpdateStatus.unknown();
            }

            versionInfo = fetched.get();
            UpdateCache.set(versionInfo);
            LOGGER.info("Fetched latest version from Modrinth: {}", versionInfo.versionNumber());
        }

        UpdateStatus status = isNewerVersion(versionInfo.versionNumber(), installedVersion)
                ? UpdateStatus.updateAvailable(installedVersion, versionInfo.versionNumber(), versionInfo.changelog())
                : UpdateStatus.upToDate(installedVersion, versionInfo.changelog());

        LOGGER.info(
                "Update check result: {} (installed: {}, latest: {})",
                status.state(),
                installedVersion,
                versionInfo.versionNumber()
        );
        return status;
    }

    public static boolean isNewerVersion(String available, String installed) {
        Version a = Version.parse(available);
        Version b = Version.parse(installed);
        return a.compareTo(b) > 0;
    }

    private record Version(int major, int minor, int patch, int betaNumber) implements Comparable<Version> {
        static Version parse(String raw) {
            // Split off optional -beta.N suffix
            int betaNumber = -1;
            String base = raw;

            int betaIdx = raw.indexOf("-beta.");
            if (betaIdx != -1) {
                try {
                    betaNumber = Integer.parseInt(raw.substring(betaIdx + 6).trim());
                } catch (NumberFormatException ignored) {
                    betaNumber = 0;
                }
                base = raw.substring(0, betaIdx);
            }

            String[] parts = base.split("\\.");
            int major = parsePart(parts, 0);
            int minor = parsePart(parts, 1);
            int patch = parsePart(parts, 2);

            return new Version(major, minor, patch, betaNumber);
        }

        private static int parsePart(String[] parts, int i) {
            if (i >= parts.length) return 0;
            try { return Integer.parseInt(parts[i].trim()); }
            catch (NumberFormatException e) { return 0; }
        }

        @Override
        public int compareTo(Version o) {
            if (this.major != o.major) return Integer.compare(this.major, o.major);
            if (this.minor != o.minor) return Integer.compare(this.minor, o.minor);
            if (this.patch != o.patch) return Integer.compare(this.patch, o.patch);

            // Same base version — stable beats beta, beta.2 beats beta.1
            if (this.betaNumber == o.betaNumber) return 0;
            if (this.betaNumber == -1) return 1;
            if (o.betaNumber == -1)    return -1;
            return Integer.compare(this.betaNumber, o.betaNumber);
        }
    }
}