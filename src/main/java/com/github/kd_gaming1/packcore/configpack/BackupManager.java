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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class BackupManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/BackupManager");
    private static final Path BACKUP_DIR = PackCore.PACKCORE_DIR.resolve("backups");
    private static final DateTimeFormatter FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final Pattern MODPACK_UPDATE_PATTERN =
            Pattern.compile("modpack_update_(.+)_to_(.+)_\\d{8}_\\d{6}\\.zip");

    private static final Map<BackupEntry.BackupType, Integer> MAX_PER_TYPE =
            new EnumMap<>(Map.of(
                    BackupEntry.BackupType.MANUAL,         10,
                    BackupEntry.BackupType.AUTO,            5,
                    BackupEntry.BackupType.MODPACK_UPDATE, 10,
                    BackupEntry.BackupType.CONFIG_SWITCH,   5
            ));

    private static final Set<String> BACKUP_ROOTS = Set.of(
            "config", "options.txt", "servers.dat"
    );

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

    private record BackupFile(Path path, Instant modifiedAt) {}

    public static List<BackupEntry> listBackups() throws IOException {
        if (!Files.exists(BACKUP_DIR)) {
            return List.of();
        }

        List<BackupFile> backupFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.list(BACKUP_DIR)) {
            stream.filter(p -> p.toString().endsWith(".zip"))
                    .forEach(p -> {
                        try {
                            backupFiles.add(new BackupFile(p, Files.getLastModifiedTime(p).toInstant()));
                        } catch (IOException ignored) {}
                    });
        }

        backupFiles.sort(Comparator.comparing(BackupFile::modifiedAt).reversed());

        List<BackupEntry> result = new ArrayList<>(backupFiles.size());
        for (BackupFile bf : backupFiles) {
            String name = bf.path().getFileName().toString();
            BackupEntry.BackupType type = parseType(name);
            result.add(new BackupEntry(bf.path(), resolveDisplayName(name, bf.modifiedAt()), bf.modifiedAt(), type));
        }
        return result;
    }

    /** Manual backup — triggered by the user from the UI. Requires Minecraft to be initialized. */
    public static void createBackup(Path gameDir) throws IOException {
        String timestamp = FILE_FORMAT.format(Instant.now());
        List<String> paths = collectBackupPaths(gameDir);
        if (paths.isEmpty()) {
            LOGGER.warn("No files found to back up.");
            return;
        }
        int guiScale = Minecraft.getInstance().options.guiScale().get();
        ConfigPackMeta meta = ConfigPackMeta.builder("backup", 0, 0, guiScale)
                .name("Manual")
                .build();
        ConfigPackBuilder.zipFiles(gameDir, paths, BACKUP_DIR, "manual_" + timestamp + ".zip", meta);
        pruneOldBackups(BackupEntry.BackupType.MANUAL);
        LOGGER.info("Manual backup created: manual_{}.zip", timestamp);
    }

    /** Automatic timer-based backup. Requires Minecraft to be initialized. */
    public static void createAutoBackup(Path gameDir) throws IOException {
        String timestamp = FILE_FORMAT.format(Instant.now());
        List<String> paths = collectBackupPaths(gameDir);
        if (paths.isEmpty()) {
            LOGGER.warn("No files found to back up.");
            return;
        }
        int guiScale = Minecraft.getInstance().options.guiScale().get();
        ConfigPackMeta meta = ConfigPackMeta.builder("backup", 0, 0, guiScale)
                .name("Auto backup")
                .build();
        ConfigPackBuilder.zipFiles(gameDir, paths, BACKUP_DIR, "auto_" + timestamp + ".zip", meta);
        pruneOldBackups(BackupEntry.BackupType.AUTO);
        LOGGER.info("Auto backup created: auto_{}.zip", timestamp);
    }

    /**
     * Pre-update backup when the modpack version changes.
     * Safe to call from PreLaunch — does not require Minecraft to be initialized.
     */
    public static void createModpackUpdateBackup(Path gameDir, String fromVersion, String toVersion) throws IOException {
        String timestamp = FILE_FORMAT.format(Instant.now());
        String zipName = "modpack_update_" + fromVersion + "_to_" + toVersion + "_" + timestamp + ".zip";
        List<String> paths = collectBackupPaths(gameDir);
        if (paths.isEmpty()) {
            LOGGER.warn("No files found to back up.");
            return;
        }
        ConfigPackMeta meta = ConfigPackMeta.builder("backup", 0, 0, 0)
                .name("Modpack update: v" + fromVersion + " → v" + toVersion)
                .build();
        ConfigPackBuilder.zipFiles(gameDir, paths, BACKUP_DIR, zipName, meta);
        pruneOldBackups(BackupEntry.BackupType.MODPACK_UPDATE);
        LOGGER.info("Modpack update backup created: {}", zipName);
    }

    /**
     * Pre-switch backup when the user applies a different config pack.
     * Safe to call from PreLaunch — does not require Minecraft to be initialized.
     */
    public static void createConfigSwitchBackup(Path gameDir) throws IOException {
        String timestamp = FILE_FORMAT.format(Instant.now());
        List<String> paths = collectBackupPaths(gameDir);
        if (paths.isEmpty()) {
            LOGGER.warn("No files found to back up.");
            return;
        }
        ConfigPackMeta meta = ConfigPackMeta.builder("backup", 0, 0, 0)
                .name("Config switch")
                .build();
        ConfigPackBuilder.zipFiles(gameDir, paths, BACKUP_DIR, "config_switch_" + timestamp + ".zip", meta);
        pruneOldBackups(BackupEntry.BackupType.CONFIG_SWITCH);
        LOGGER.info("Config switch backup created: config_switch_{}.zip", timestamp);
    }

    private static BackupEntry.BackupType parseType(String fileName) {
        if (fileName.startsWith("auto_")) return BackupEntry.BackupType.AUTO;
        if (fileName.startsWith("modpack_update_")) return BackupEntry.BackupType.MODPACK_UPDATE;
        if (fileName.startsWith("config_switch_")) return BackupEntry.BackupType.CONFIG_SWITCH;
        return BackupEntry.BackupType.MANUAL; // "manual_*" and legacy "backup_*"
    }

    private static String resolveDisplayName(String fileName, Instant modifiedAt) {
        String date = DISPLAY_FORMAT.format(modifiedAt);
        if (fileName.startsWith("auto_")) return "Auto backup  ·  " + date;
        if (fileName.startsWith("config_switch_")) return "Config switch  ·  " + date;
        Matcher m = MODPACK_UPDATE_PATTERN.matcher(fileName);
        if (m.matches()) return "Modpack update: v" + m.group(1) + " → v" + m.group(2) + "  ·  " + date;
        return "Manual  ·  " + date; // "manual_*" and legacy "backup_*"
    }

    /** Deletes the oldest backups of {@code type} beyond its configured cap. */
    private static void pruneOldBackups(BackupEntry.BackupType type) {
        int max = MAX_PER_TYPE.getOrDefault(type, Integer.MAX_VALUE);
        try (Stream<Path> stream = Files.list(BACKUP_DIR)) {
            List<Path> typed = stream
                    .filter(p -> p.toString().endsWith(".zip") && parseType(p.getFileName().toString()) == type)
                    .sorted(Comparator.comparingLong(p -> {
                        try { return Files.getLastModifiedTime(p).toMillis(); }
                        catch (IOException e) { return 0L; }
                    }))
                    .toList();

            for (int i = 0; i < typed.size() - max; i++) {
                Files.deleteIfExists(typed.get(i));
                LOGGER.info("Pruned old {} backup: {}", type, typed.get(i).getFileName());
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to prune old {} backups: {}", type, e.getMessage());
        }
    }

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
                    paths.add(gameDir.relativize(file).toString().replace('\\', '/'));
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