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
import java.util.zip.ZipInputStream;

/**
 * Async version of UnzipFiles that performs extraction operations without blocking the main thread
 */
public class UnzipAsyncTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnzipAsyncTask.class);
    private static final int BUFFER_SIZE = 16384; // Larger buffer for better performance
    private static final ExecutorService UNZIP_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("AsyncUnzip-" + thread.threadId());
        thread.setDaemon(true);
        return thread;
    });

    public interface ProgressCallback {
        void onProgress(long bytesProcessed, long totalBytes, int percentage);
    }

    /**
     * Asynchronously unzip a file with progress callback
     */
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

    /**
     * Optimized unzip with better performance and progress reporting
     */
    public void unzip(String zipFilePath, String destDir,
                      ProgressCallback progressCallback) throws IOException {
        // Calculate total uncompressed size for progress reporting
        long totalSize = calculateTotalUncompressedSize(zipFilePath);
        AtomicLong processedBytes = new AtomicLong(0);
        int lastReportedProgress = 0;

        Path destPath = Path.of(destDir);
        Files.createDirectories(destPath);

        byte[] buffer = new byte[BUFFER_SIZE];

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            var entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = destPath.resolve(entry.getName()).normalize();

                // Security check: ensure the entry doesn't escape the destination directory
                if (!entryPath.startsWith(destPath)) {
                    LOGGER.warn("Skipping entry with path outside destination: {}", entry.getName());
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                    LOGGER.debug("Created directory: {}", entryPath);
                } else {
                    // Ensure parent directory exists
                    Files.createDirectories(entryPath.getParent());

                    // Extract file with progress tracking
                    try (InputStream is = zipFile.getInputStream(entry);
                         BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE);
                         OutputStream os = Files.newOutputStream(entryPath);
                         BufferedOutputStream bos = new BufferedOutputStream(os, BUFFER_SIZE)) {

                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) > 0) {
                            bos.write(buffer, 0, bytesRead);

                            long processed = processedBytes.addAndGet(bytesRead);

                            // Report progress (but not too frequently)
                            if (progressCallback != null && totalSize > 0) {
                                int currentProgress = (int) ((processed * 100) / totalSize);
                                if (currentProgress != lastReportedProgress) {
                                    lastReportedProgress = currentProgress;
                                    progressCallback.onProgress(processed, totalSize, currentProgress);
                                }
                            }
                        }
                    }

                    // Preserve file modification time
                    if (entry.getTime() != -1) {
                        Files.setLastModifiedTime(entryPath,
                                java.nio.file.attribute.FileTime.fromMillis(entry.getTime()));
                    }

                    LOGGER.debug("Extracted: {}", entryPath);
                }
            }

            // Final progress callback
            if (progressCallback != null) {
                progressCallback.onProgress(processedBytes.get(), totalSize, 100);
            }

            LOGGER.info("Successfully unzipped {} to {}", zipFilePath, destDir);
        }
    }

    /**
     * Calculate the total uncompressed size of all entries in the zip file
     */
    private long calculateTotalUncompressedSize(String zipFilePath) {
        long totalSize = 0;

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    long size = entry.getSize();
                    if (size > 0) {
                        totalSize += size;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to calculate total size", e);
            // If we can't calculate, try alternate method
            return calculateTotalSizeAlternate(zipFilePath);
        }

        return totalSize;
    }

    /**
     * Alternate method to calculate total size using ZipInputStream
     */
    private long calculateTotalSizeAlternate(String zipFilePath) {
        long totalSize = 0;

        try (FileInputStream fis = new FileInputStream(zipFilePath);
             BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);
             ZipInputStream zis = new ZipInputStream(bis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getSize() > 0) {
                    totalSize += entry.getSize();
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to calculate total size (alternate method)", e);
        }

        return totalSize;
    }

    /**
     * Extract a single file from a zip archive asynchronously
     */
    public CompletableFuture<Void> extractFileAsync(String zipFilePath, String entryName,
                                                    String destPath, ProgressCallback progressCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                extractFile(zipFilePath, entryName, destPath, progressCallback);
            } catch (IOException e) {
                throw new RuntimeException("Failed to extract file", e);
            }
        }, UNZIP_EXECUTOR);
    }

    /**
     * Extract a single file from a zip archive
     */
    public void extractFile(String zipFilePath, String entryName, String destPath,
                            ProgressCallback progressCallback) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            ZipEntry entry = zipFile.getEntry(entryName);

            if (entry == null) {
                throw new IOException("Entry not found in zip: " + entryName);
            }

            Path destFile = Path.of(destPath).getParent();

            if (entry.isDirectory()) {
                Files.createDirectories(destFile);
                return;
            }

            Files.createDirectories(destFile.getParent());

            long totalSize = entry.getSize();
            AtomicLong processedBytes = new AtomicLong(0);
            int lastReportedProgress = 0;

            byte[] buffer = new byte[BUFFER_SIZE];

            try (InputStream is = zipFile.getInputStream(entry);
                 BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE);
                 OutputStream os = Files.newOutputStream(destFile);
                 BufferedOutputStream bos = new BufferedOutputStream(os, BUFFER_SIZE)) {

                int bytesRead;
                while ((bytesRead = bis.read(buffer)) > 0) {
                    bos.write(buffer, 0, bytesRead);

                    long processed = processedBytes.addAndGet(bytesRead);

                    if (progressCallback != null && totalSize > 0) {
                        int currentProgress = (int) ((processed * 100) / totalSize);
                        if (currentProgress != lastReportedProgress) {
                            lastReportedProgress = currentProgress;
                            progressCallback.onProgress(processed, totalSize, currentProgress);
                        }
                    }
                }
            }

            // Preserve file modification time
            if (entry.getTime() != -1) {
                Files.setLastModifiedTime(destFile,
                        java.nio.file.attribute.FileTime.fromMillis(entry.getTime()));
            }

            // Final progress callback
            if (progressCallback != null) {
                progressCallback.onProgress(totalSize, totalSize, 100);
            }

            LOGGER.info("Successfully extracted {} from {}", entryName, zipFilePath);
        }
    }

    /**
     * List all entries in a zip file without extracting
     */
    public CompletableFuture<List<String>> listEntriesAsync(String zipFilePath) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> entries = new ArrayList<>();

            try (ZipFile zipFile = new ZipFile(zipFilePath)) {
                var enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    entries.add(enumeration.nextElement().getName());
                }
            } catch (IOException e) {
                LOGGER.error("Failed to list entries", e);
            }

            return entries;
        }, UNZIP_EXECUTOR);
    }

    /**
     * Shutdown the executor service (call this when your application is closing)
     */
    public static void shutdown() {
        UNZIP_EXECUTOR.shutdown();
    }
}