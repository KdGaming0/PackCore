package com.github.kd_gaming1.packcore.configpack;

import com.github.kd_gaming1.packcore.PackCore;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class BackupManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/BackupManager");
    private static final Path BACKUP_DIR = PackCore.PACKCORE_DIR.resolve("backups");
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss").withZone(ZoneId.systemDefault());

    /** Top-level paths (relative to game dir) to include in every backup. */
    private static final Set<String> BACKUP_ROOTS = Set.of(
            "config", "options.txt", "servers.dat"
    );

    /**
     * Subpaths excluded from the backup. These are large or auto-regenerated
     * directories that don't need to be saved.
     */
    private static final Set<String> BACKUP_EXCLUDED = Set.of(
            "config/skyhanni/repo",
            "config/skyhanni/logs",
            "config/skyhanni/backup",
            "config/skyblocker/item-repo",
            "config/skyblocker/config_backups",
            "config/skyblocker/backpack-preview",
            "config/SBO",
            "config/notenoughupdates",
            "config/firmament/profiles",
            "config/skyocean/data"
    );

    private BackupManager() {}

    public static List<BackupEntry> listBackups() throws IOException {
        if (!Files.exists(BACKUP_DIR)) return List.of();

        List<BackupEntry> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(BACKUP_DIR)) {
            stream.filter(p -> p.toString().endsWith(".zip"))
                    .sorted((a, b) -> {
                        try { return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a)); }
                        catch (IOException e) { return 0; }
                    })
                    .forEach(p -> {
                        try {
                            Instant time = Files.getLastModifiedTime(p).toInstant();
                            result.add(new BackupEntry(p, DISPLAY_FORMAT.format(time), time));
                        } catch (IOException ignored) {}
                    });
        }
        return result;
    }

    public static void createBackup(Path gameDir) throws IOException {
        String timestamp = FILE_FORMAT.format(Instant.now());
        String zipName = "backup_" + timestamp + ".zip";

        List<String> paths = collectBackupPaths(gameDir);
        if (paths.isEmpty()) {
            LOGGER.warn("No files found to back up.");
            return;
        }

        int guiScale = Minecraft.getInstance().options.guiScale().get();

        ConfigPackMeta meta = ConfigPackMeta.builder("backup", 0, 0, guiScale)
                .name("Manual backup " + timestamp)
                .build();

        ConfigPackBuilder.zipFiles(gameDir, paths, BACKUP_DIR, zipName, meta);
        LOGGER.info("Backup created: {}", zipName);
    }

    /**
     * Collects all files to back up by walking each root in {@link #BACKUP_ROOTS}.
     * Directories listed in {@link #BACKUP_EXCLUDED} are skipped entirely via
     * {@link FileVisitResult#SKIP_SUBTREE} so their contents are never read.
     */
    private static List<String> collectBackupPaths(Path gameDir) throws IOException {
        List<String> paths = new ArrayList<>();

        for (String root : BACKUP_ROOTS) {
            Path rootPath = gameDir.resolve(root);
            if (!Files.exists(rootPath)) continue;

            if (Files.isRegularFile(rootPath)) {
                paths.add(root);
                continue;
            }

            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public @NonNull FileVisitResult preVisitDirectory(@NonNull Path dir, @NonNull BasicFileAttributes attrs) {
                    String rel = gameDir.relativize(dir).toString().replace('\\', '/');
                    if (BACKUP_EXCLUDED.stream().anyMatch(ex -> rel.equals(ex) || rel.startsWith(ex + "/"))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) {
                    String rel = gameDir.relativize(file).toString().replace('\\', '/');
                    paths.add(rel);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFileFailed(@NonNull Path file, @NonNull IOException e) {
                    LOGGER.warn("Could not read file during backup: {}", file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return paths;
    }
}