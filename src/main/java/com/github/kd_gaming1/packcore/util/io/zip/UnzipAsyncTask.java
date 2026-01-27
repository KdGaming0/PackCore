package com.github.kd_gaming1.packcore.util.io.zip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UnzipAsyncTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnzipAsyncTask.class);
    private static final int BUFFER_SIZE = 32768; // 32KB buffer
    private static final ExecutorService UNZIP_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("AsyncUnzip-" + thread.threadId());
        thread.setDaemon(true);
        return thread;
    });

    public interface ProgressCallback {
        void onProgress(long bytesProcessed, long totalBytes, int percentage);
    }

    public CompletableFuture<Void> unzipAsync(String zipFilePath, String destDir,
                                              ProgressCallback progressCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                unzip(zipFilePath, destDir, progressCallback);
            } catch (IOException e) {
                throw new RuntimeException("Failed to unzip file", e);
            }
        }, UNZIP_EXECUTOR);
    }

    public void unzip(String zipFilePath, String destDir,
                      ProgressCallback progressCallback) throws IOException {

        Path destPath = Path.of(destDir);
        Files.createDirectories(destPath); // fast

        // Optimization: Open ZipFile ONCE for both calculation and extraction
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {

            // 1. Fast Size Calculation
            long totalSize = 0;
            var sizeEnum = zipFile.entries();
            while (sizeEnum.hasMoreElements()) {
                ZipEntry e = sizeEnum.nextElement();
                if (!e.isDirectory()) totalSize += e.getSize();
            }

            // 2. Extraction
            AtomicLong processedBytes = new AtomicLong(0);
            int lastReportedProgress = -1;
            byte[] buffer = new byte[BUFFER_SIZE];

            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = destPath.resolve(entry.getName()).normalize();

                if (!entryPath.normalize().toAbsolutePath().startsWith(destPath.toAbsolutePath())) {
                    LOGGER.warn("Zip entry outside destination: {}", entry.getName());
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());

                    try (InputStream is = zipFile.getInputStream(entry);
                         BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE);
                         OutputStream os = Files.newOutputStream(entryPath);
                         BufferedOutputStream bos = new BufferedOutputStream(os, BUFFER_SIZE)) {

                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) > 0) {
                            bos.write(buffer, 0, bytesRead);
                            long processed = processedBytes.addAndGet(bytesRead);

                            if (progressCallback != null && totalSize > 0) {
                                int currentProgress = (int) ((processed * 100) / totalSize);

                                // Optimization: Only callback if percent changes OR every 5MB
                                // Prevents spamming on small files, fixes "stuck" feeling
                                if (currentProgress != lastReportedProgress) {
                                    lastReportedProgress = currentProgress;
                                    progressCallback.onProgress(processed, totalSize, currentProgress);
                                }
                            }
                        }
                    }

                    // Handle file times...
                    if (entry.getTime() != -1) {
                        try {
                            Files.setLastModifiedTime(entryPath,
                                    java.nio.file.attribute.FileTime.fromMillis(entry.getTime()));
                        } catch (IOException e) {
                            LOGGER.debug("Failed to set file time for {}: {}", entryPath, e.getMessage());
                        }
                    }
                }
            }

            if (progressCallback != null) {
                progressCallback.onProgress(processedBytes.get(), totalSize, 100);
            }
        }
    }

    public static void shutdown() {
        UNZIP_EXECUTOR.shutdown();
    }
}