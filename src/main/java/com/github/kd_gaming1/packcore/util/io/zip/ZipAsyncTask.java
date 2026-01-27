package com.github.kd_gaming1.packcore.util.io.zip;

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

public class ZipAsyncTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipAsyncTask.class);
    private static final int BUFFER_SIZE = 32768; // Increased buffer (32KB)
    private static final ExecutorService ZIP_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("AsyncZip-" + thread.threadId());
        thread.setDaemon(true);
        return thread;
    });

    public interface ProgressCallback {
        void onProgress(long bytesProcessed, long totalBytes, int percentage);
    }

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

    public void zipDirectory(File dir, String zipFilePath,
                             ProgressCallback progressCallback) throws IOException {
        Path basePath = dir.toPath();

        // OPTIMIZATION: Removed the Files.walk pre-scan.
        // It causes massive delays on large modpacks just to get a total size.
        // We will report bytes processed, but total might be estimated or specific logic needed.
        long estimatedTotal = -1;

        AtomicLong processedBytes = new AtomicLong(0);
        // Use an array to hold state regarding last progress report
        int[] lastReportedProgress = {-1};

        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            zos.setLevel(3);
            byte[] buffer = new byte[BUFFER_SIZE];

            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
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
                    String entryName = basePath.relativize(file).toString().replace(File.separatorChar, '/');
                    ZipEntry fileEntry = new ZipEntry(entryName);
                    fileEntry.setTime(attrs.lastModifiedTime().toMillis());
                    // Storing size is optional but good for unzip progress later
                    fileEntry.setSize(attrs.size());

                    zos.putNextEntry(fileEntry);

                    try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) > 0) {
                            zos.write(buffer, 0, bytesRead);
                            long processed = processedBytes.addAndGet(bytesRead);

                            // Progress logic: If we don't know total, just tick every 1MB or similar
                            // Or relies on the callback handling unknown (-1) totals
                            if (progressCallback != null) {
                                // Rate limit updates to avoid spamming the UI thread (e.g. every 1MB)
                                if (processed / (1024 * 1024) != (processed - bytesRead) / (1024 * 1024)) {
                                    progressCallback.onProgress(processed, estimatedTotal, -1);
                                }
                            }
                        }
                    }
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });

            // Final callback
            if (progressCallback != null) {
                progressCallback.onProgress(processedBytes.get(), processedBytes.get(), 100);
            }
            LOGGER.info("Successfully zipped {} to {}", dir.getAbsolutePath(), zipFilePath);
        }
    }

    public static void shutdown() {
        ZIP_EXECUTOR.shutdown();
    }
}