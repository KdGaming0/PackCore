package com.github.kd_gaming1.packcore.util.copysystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;

/**
 * Async version of ZipFiles that performs zipping operations without blocking the main thread
 */
public class AsyncZipFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncZipFiles.class);
    private static final int BUFFER_SIZE = 16384; // Larger buffer for better performance
    private static final ExecutorService ZIP_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("AsyncZip-" + thread.getId());
        thread.setDaemon(true);
        return thread;
    });

    public interface ProgressCallback {
        void onProgress(long bytesProcessed, long totalBytes, int percentage);
    }

    /**
     * Asynchronously zip a directory with progress callback
     */
    public CompletableFuture<Void> zipDirectoryAsync(File dir, String zipFilePath,
                                                     ProgressCallback progressCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                zipDirectory(dir, zipFilePath, progressCallback);
            } catch (IOException e) {
                throw new RuntimeException("Failed to zip directory", e);
            }
        }, ZIP_EXECUTOR);
    }

    /**
     * Zip the contents of the directory 'dir' into a zip file at 'zipFilePath'.
     * Optimized for large directories with progress reporting.
     */
    public void zipDirectory(File dir, String zipFilePath,
                             ProgressCallback progressCallback) throws IOException {
        Path basePath = dir.toPath();

        // First, calculate total size for progress reporting
        AtomicLong totalSize = new AtomicLong(0);
        try (Stream<Path> walk = Files.walk(basePath)) {
            walk.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            totalSize.addAndGet(Files.size(path));
                        } catch (IOException e) {
                            LOGGER.debug("Could not get size for: {}", path);
                        }
                    });
        }

        AtomicLong processedBytes = new AtomicLong(0);
        int lastReportedProgress = 0;

        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            // Set compression level for better performance
            zos.setLevel(6); // Balance between speed and compression

            byte[] buffer = new byte[BUFFER_SIZE];

            // Walk the directory tree and add entries
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    // Add directory entry
                    Path relativePath = basePath.relativize(dir);
                    if (!relativePath.toString().isEmpty()) {
                        String entryName = relativePath.toString().replace(File.separatorChar, '/') + '/';
                        ZipEntry dirEntry = new ZipEntry(entryName);
                        dirEntry.setTime(attrs.lastModifiedTime().toMillis());
                        zos.putNextEntry(dirEntry);
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    // Add file entry
                    Path relativePath = basePath.relativize(file);
                    String entryName = relativePath.toString().replace(File.separatorChar, '/');

                    ZipEntry fileEntry = new ZipEntry(entryName);
                    fileEntry.setTime(attrs.lastModifiedTime().toMillis());
                    fileEntry.setSize(attrs.size());

                    zos.putNextEntry(fileEntry);

                    // Copy file content with progress tracking
                    try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) > 0) {
                            zos.write(buffer, 0, bytesRead);

                            long processed = processedBytes.addAndGet(bytesRead);

                            // Report progress (but not too frequently to avoid overhead)
                            if (progressCallback != null && totalSize.get() > 0) {
                                int currentProgress = (int) ((processed * 100) / totalSize.get());
                                if (currentProgress != lastReportedProgress) {
                                    progressCallback.onProgress(processed, totalSize.get(), currentProgress);
                                }
                            }
                        }
                    }

                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    LOGGER.warn("Failed to zip file: {} - {}", file, exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });

            // Final progress callback
            if (progressCallback != null) {
                progressCallback.onProgress(processedBytes.get(), totalSize.get(), 100);
            }

            LOGGER.info("Successfully zipped {} to {}", dir.getAbsolutePath(), zipFilePath);
        }
    }

    /**
     * Asynchronously zip a single file
     */
    public CompletableFuture<Void> zipSingleFileAsync(File file, String zipFileName,
                                                      ProgressCallback progressCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                zipSingleFile(file, zipFileName, progressCallback);
            } catch (IOException e) {
                throw new RuntimeException("Failed to zip file", e);
            }
        }, ZIP_EXECUTOR);
    }

    /**
     * Zip a single file with progress reporting
     */
    public void zipSingleFile(File file, String zipFileName,
                              ProgressCallback progressCallback) throws IOException {
        long totalSize = file.length();
        AtomicLong processedBytes = new AtomicLong(0);
        int lastReportedProgress = 0;

        try (FileOutputStream fos = new FileOutputStream(zipFileName);
             BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);
             ZipOutputStream zos = new ZipOutputStream(bos);
             FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {

            ZipEntry ze = new ZipEntry(file.getName());
            ze.setTime(file.lastModified());
            ze.setSize(file.length());
            zos.putNextEntry(ze);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) > 0) {
                zos.write(buffer, 0, bytesRead);

                long processed = processedBytes.addAndGet(bytesRead);

                if (progressCallback != null && totalSize > 0) {
                    int currentProgress = (int) ((processed * 100) / totalSize);
                    if (currentProgress != lastReportedProgress) {
                        lastReportedProgress = currentProgress;
                        progressCallback.onProgress(processed, totalSize, currentProgress);
                    }
                }
            }

            zos.closeEntry();

            // Final progress callback
            if (progressCallback != null) {
                progressCallback.onProgress(totalSize, totalSize, 100);
            }

            LOGGER.info("Successfully zipped {} to {}", file.getCanonicalPath(), zipFileName);
        }
    }

    /**
     * Shutdown the executor service (call this when your application is closing)
     */
    public static void shutdown() {
        ZIP_EXECUTOR.shutdown();
    }
}