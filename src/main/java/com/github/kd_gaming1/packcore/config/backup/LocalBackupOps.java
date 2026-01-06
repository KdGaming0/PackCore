package com.github.kd_gaming1.packcore.config.backup;

import com.github.kd_gaming1.packcore.util.io.file.FileUtils;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * High-level local backup operations. Delegates file system tasks to FileUtils.
 */
public class LocalBackupOps {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalBackupOps.class);
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

            // Backup key configuration files and folders using centralized utilities
            backupIfExists(gameDir.resolve("config"), backupPath.resolve("config"), gameDir);
            backupIfExists(gameDir.resolve("options.txt"), backupPath.resolve("options.txt"), gameDir);
            backupIfExists(gameDir.resolve("servers.dat"), backupPath.resolve("servers.dat"), gameDir);

            // Backup current metadata if it exists
            Path currentMetadata = gameDir.resolve(ConfigFileRepository.METADATA_FILE);
            if (Files.exists(currentMetadata)) {
                Files.copy(currentMetadata,
                        backupPath.resolve(ConfigFileRepository.METADATA_FILE),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            LOGGER.info("Backup created successfully at: {}", backupPath);
            return backupPath;

        } catch (IOException e) {
            LOGGER.error("Failed to create backup", e);
            return null;
        }
    }

    private static void backupIfExists(Path source, Path target, Path gameDir) {
        try {
            if (Files.exists(source)) {
                if (Files.isDirectory(source)) {
                    // Use the centralized FileUtils method with exclusions
                    FileUtils.copyDirectoryWithExclusions(source, target, gameDir);
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
     * Cleans up old backup directories, keeping only the most recent {@code keepCount} backups.
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
                        FileUtils.deleteDirectory(backup);
                    });
        } catch (IOException e) {
            LOGGER.warn("Failed to clean old backups", e);
        }
    }
}