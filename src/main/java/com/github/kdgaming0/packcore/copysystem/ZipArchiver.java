package com.github.kdgaming0.packcore.copysystem;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class provides functionality to create ZIP archives from selected files and folders
 * in the Minecraft root directory.
 */
public class ZipArchiver {

    /**
     * Creates a ZIP archive containing the specified files and folders.
     *
     * @param outputZipPath The path where the ZIP file should be created
     * @param selectedPaths List of paths (files/folders) to include in the ZIP
     * @param minecraftRoot The root directory of the Minecraft instance
     * @param listener The listener to receive progress updates
     * @return true if archive creation was successful, false otherwise
     */
    public static boolean createZipArchive(String zipFileName, String outputZipPath, List<Path> selectedPaths,
                                           File minecraftRoot, ArchiveProgressListener listener) {
        // Combine the output path and zip file name
        String fullZipPath = Paths.get(outputZipPath, zipFileName).toString();

        try (FileOutputStream fos = new FileOutputStream(fullZipPath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // Count total files for progress tracking
            int totalFiles = countFiles(selectedPaths);
            AtomicInteger processedFiles = new AtomicInteger();

            // Process each selected path
            for (Path path : selectedPaths) {
                if (!path.toFile().exists()) {
                    System.err.println("Path does not exist: " + path);
                    continue;
                }

                // Get the relative path from Minecraft root
                Path relativePath = minecraftRoot.toPath().relativize(path);

                if (Files.isDirectory(path)) {
                    // Walk through directory and add all files
                    try (Stream<Path> walker = Files.walk(path)) {
                        walker.forEach(filePath -> {
                            if (Files.isRegularFile(filePath)) {
                                try {
                                    addFileToZip(filePath, minecraftRoot.toPath().relativize(filePath), zos);
                                    updateProgress(processedFiles.incrementAndGet(), totalFiles, listener);
                                } catch (IOException e) {
                                    System.err.println("Error adding file to ZIP: " + filePath);
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } else {
                    // Add single file
                    addFileToZip(path, relativePath, zos);
                    updateProgress(processedFiles.incrementAndGet(), totalFiles, listener);
                }
            }

            return true;
        } catch (IOException e) {
            System.err.println("Error creating ZIP archive: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds a single file to the ZIP archive.
     */
    private static void addFileToZip(Path filePath, Path relativePath, ZipOutputStream zos)
            throws IOException {
        ZipEntry zipEntry = new ZipEntry(relativePath.toString().replace("\\", "/"));
        zos.putNextEntry(zipEntry);

        Files.copy(filePath, zos);
        zos.closeEntry();
    }

    /**
     * Counts total number of files to be processed for progress tracking.
     */
    private static int countFiles(List<Path> paths) {
        int count = 0;
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                try (Stream<Path> walker = Files.walk(path)) {
                    count += walker.filter(Files::isRegularFile).count();
                } catch (IOException e) {
                    System.err.println("Error counting files in directory: " + path);
                }
            } else {
                count++;
            }
        }
        return count;
    }

    /**
     * Updates the progress through the listener.
     */
    private static void updateProgress(int processed, int total, ArchiveProgressListener listener) {
        if (listener != null) {
            int progress = (int)(((double)processed / total) * 100);
            listener.onProgress(progress);
        }
    }

    /**
     * Interface for progress updates during archive creation.
     */
    public interface ArchiveProgressListener {
        void onProgress(int percentComplete);
    }
}