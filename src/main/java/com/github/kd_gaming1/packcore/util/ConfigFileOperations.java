package com.github.kd_gaming1.packcore.util;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Utility class providing centralized file operations for configuration management,
 * including backup, restore, copy, delete, and size calculation functionalities.
 */
public class ConfigFileOperations {
    /**
     * Logger instance for logging operation details and errors.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileOperations.class);

    /**
     * Formatter for backup directory timestamps.
     */
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Creates a timestamped backup of important configuration files and directories.
     *
     * @param gameDir The root directory of the game.
     * @return The path to the created backup directory, or {@code null} if the backup failed.
     */
    public static Path createBackup(Path gameDir) {
        try {
            Path backupDir = gameDir.resolve("packcore/backups");
            Files.createDirectories(backupDir);

            String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP);
            Path backupPath = backupDir.resolve("config_backup_" + timestamp);
            Files.createDirectories(backupPath);

            LOGGER.info("Creating backup at: {}", backupPath);

            // Backup key configuration files and folders
            backupIfExists(gameDir.resolve("config"), backupPath.resolve("config"));
            backupIfExists(gameDir.resolve("options.txt"), backupPath.resolve("options.txt"));
            backupIfExists(gameDir.resolve("servers.dat"), backupPath.resolve("servers.dat"));

            // Backup current metadata if it exists
            Path currentMetadata = gameDir.resolve(ConfigFileUtils.METADATA_FILE);
            if (Files.exists(currentMetadata)) {
                Files.copy(currentMetadata,
                        backupPath.resolve(ConfigFileUtils.METADATA_FILE),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            LOGGER.info("Backup created successfully at: {}", backupPath);
            return backupPath;

        } catch (IOException e) {
            LOGGER.error("Failed to create backup", e);
            return null;
        }
    }

    /**
     * Backs up a file or directory if it exists.
     * If the source is a directory, it is copied recursively.
     * If the source is a file, it is copied directly.
     *
     * @param source The source file or directory to back up.
     * @param target The target backup location.
     */
    private static void backupIfExists(Path source, Path target) {
        try {
            if (Files.exists(source)) {
                if (Files.isDirectory(source)) {
                    copyDirectory(source, target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
                LOGGER.debug("Backed up: {}", source);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not backup: {} - {}", source, e.getMessage());
        }
    }

    /**
     * Recursively copies a directory and its contents to a target location.
     * Uses {@link Files#walkFileTree} for efficient traversal and copying.
     *
     * @param source The source directory to copy.
     * @param target The target directory.
     * @throws IOException If an I/O error occurs during copying.
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.warn("Failed to copy file: {} - {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Recursively deletes a directory and all its contents.
     * Ignores non-existent directories.
     *
     * @param directory The directory to delete.
     */
    public static void deleteDirectory(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            LOGGER.debug("Could not delete: {}", path);
                        }
                    });
        } catch (IOException e) {
            LOGGER.warn("Could not fully delete directory: {}", directory);
        }
    }

    /**
     * Calculates the total size (in bytes) of a file or directory.
     * For directories, sums the sizes of all contained files.
     *
     * @param path The file or directory to measure.
     * @return The total size in bytes, or 0 if the size could not be determined.
     */
    public static long calculateSize(Path path) {
        try {
            if (Files.isRegularFile(path)) {
                return Files.size(path);
            } else if (Files.isDirectory(path)) {
                try (Stream<Path> paths = Files.walk(path)) {
                    return paths
                            .filter(Files::isRegularFile)
                            .mapToLong(p -> {
                                try {
                                    return Files.size(p);
                                } catch (IOException e) {
                                    return 0L;
                                }
                            })
                            .sum();
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Could not calculate size for: {}", path);
        }
        return 0L;
    }

    /**
     * Cleans up old backup directories, keeping only the most recent {@code keepCount} backups.
     * Older backups are deleted.
     *
     * @param gameDir   The root directory of the game.
     * @param keepCount The number of most recent backups to keep.
     */
    public static void cleanOldBackups(Path gameDir, int keepCount) {
        Path backupDir = gameDir.resolve("packcore/backups");

        if (!Files.exists(backupDir)) {
            return;
        }

        try (Stream<Path> backups = Files.list(backupDir)) {
            backups.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> {
                        try {
                            return Files.getLastModifiedTime((Path) path);
                        } catch (IOException e) {
                            return FileTime.fromMillis(0);
                        }
                    }).reversed())
                    .skip(keepCount)
                    .forEach(backup -> {
                        LOGGER.info("Removing old backup: {}", backup.getFileName());
                        deleteDirectory(backup);
                    });
        } catch (IOException e) {
            LOGGER.warn("Failed to clean old backups", e);
        }
    }
}
