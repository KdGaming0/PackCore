package com.github.kd_gaming1.packcore.util.io.file;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Low-level file operations used by higher-level backup logic.
 */
public class FileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    /**
     * Recursively copies a directory and its contents to a target location.
     *
     * @param source The source directory to copy.
     * @param target The target directory.
     * @throws IOException If an I/O error occurs during copying.
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
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
     * Recursively copies a directory while excluding specified subfolders.
     * Uses the centralized exclusion patterns.
     *
     * @param source  The source directory to copy.
     * @param target  The target directory.
     * @param gameDir The game directory (for calculating relative paths).
     * @throws IOException If an I/O error occurs during copying.
     */
    public static void copyDirectoryWithExclusions(Path source, Path target, Path gameDir) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                // Check if this directory should be excluded
                if (ExclusionPatterns.shouldExclude(gameDir, dir)) {
                    LOGGER.debug("Skipping excluded folder: {}", gameDir.relativize(dir));
                    return FileVisitResult.SKIP_SUBTREE;
                }

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
     * Recursively deletes a directory and all its contents. Ignored when directory does not exist.
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
}
