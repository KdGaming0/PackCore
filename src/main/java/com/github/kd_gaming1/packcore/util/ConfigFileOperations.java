package com.github.kd_gaming1.packcore.util;

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
 * Centralized file operations for config management.
 * Reduces code duplication across config utilities.
 */
public class ConfigFileOperations {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileOperations.class);
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Create a timestamped backup of important configuration files
     * @param gameDir The game directory
     * @return Path to the backup directory, or null if backup failed
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
     * Backup a file or directory if it exists
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
     * Copy a directory recursively
     * Uses NIO Files.walkFileTree for better performance and error handling
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.warn("Failed to copy file: {} - {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Delete a directory recursively
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
     * Calculate the size of a file or directory
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
     * Clean up old backups, keeping only the most recent N backups
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