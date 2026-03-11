package com.github.kd_gaming1.packcore.configpack;

import com.github.kd_gaming1.packcore.PackCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final Set<String> BACKUP_PATHS = Set.of(
            "config", "options.txt", "servers.dat"
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
                            String display = DISPLAY_FORMAT.format(time);
                            result.add(new BackupEntry(p, display, time));
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

        ConfigPackMeta meta = ConfigPackMeta.builder("backup", 0, 0)
                .name("Manual backup " + timestamp)
                .build();

        ConfigPackBuilder.zipFiles(gameDir, paths, BACKUP_DIR, zipName, meta);
        LOGGER.info("Backup created: {}", zipName);
    }

    private static List<String> collectBackupPaths(Path gameDir) throws IOException {
        List<String> paths = new ArrayList<>();
        for (String target : BACKUP_PATHS) {
            Path p = gameDir.resolve(target);
            if (!Files.exists(p)) continue;
            if (Files.isRegularFile(p)) {
                paths.add(target);
            } else if (Files.isDirectory(p)) {
                try (Stream<Path> walk = Files.walk(p)) {
                    walk.filter(Files::isRegularFile).forEach(f -> paths.add(gameDir.relativize(f).toString().replace('\\', '/')));
                }
            }
        }
        return paths;
    }
}