package com.kd_gaming1.copysystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Handles the actual extraction of ZIP files with proper error handling and progress reporting.
 */
public class ConfigExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigExtractor.class);

    /**
     * Extracts a ZIP file to the target directory with progress reporting
     */
    public boolean extractZipToDirectory(File zipFile, File targetDirectory, Consumer<Integer> progressCallback)
            throws IOException {

        if (!zipFile.exists()) {
            throw new IOException("ZIP file does not exist: " + zipFile.getAbsolutePath());
        }

        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            throw new IOException("Could not create target directory: " + targetDirectory.getAbsolutePath());
        }

        try (ZipFile zip = new ZipFile(zipFile)) {
            List<ZipEntry> entries = collectEntries(zip);

            if (entries.isEmpty()) {
                LOGGER.warn("ZIP file is empty: {}", zipFile.getName());
                return true;
            }

            return extractEntries(zip, entries, targetDirectory, progressCallback);

        } catch (ZipException e) {
            throw new IOException("Invalid ZIP file: " + zipFile.getName(), e);
        }
    }

    private List<ZipEntry> collectEntries(ZipFile zipFile) {
        List<ZipEntry> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();
            // Skip directory entries, we'll create them as needed
            if (!entry.isDirectory()) {
                entries.add(entry);
            }
        }

        return entries;
    }

    private boolean extractEntries(ZipFile zipFile, List<ZipEntry> entries, File targetDirectory,
                                   Consumer<Integer> progressCallback) throws IOException {

        int totalEntries = entries.size();
        int processedEntries = 0;

        for (ZipEntry entry : entries) {
            try {
                extractSingleEntry(zipFile, entry, targetDirectory);
                processedEntries++;

                // Report progress
                int progress = (int) ((double) processedEntries / totalEntries * 100);
                progressCallback.accept(progress);

            } catch (IOException e) {
                LOGGER.error("Failed to extract entry: {}", entry.getName(), e);
                throw e;
            }
        }

        LOGGER.info("Successfully extracted {} entries from ZIP file", processedEntries);
        return true;
    }

    private void extractSingleEntry(ZipFile zipFile, ZipEntry entry, File targetDirectory) throws IOException {
        // Validate entry name to prevent directory traversal attacks
        String entryName = validateEntryName(entry.getName());

        Path targetPath = new File(targetDirectory, entryName).toPath();

        // Create parent directories if they don't exist
        Path parentPath = targetPath.getParent();
        if (parentPath != null && !Files.exists(parentPath)) {
            Files.createDirectories(parentPath);
        }

        // Extract the file
        try (InputStream inputStream = zipFile.getInputStream(entry)) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        LOGGER.debug("Extracted: {}", entryName);
    }

    private String validateEntryName(String entryName) throws IOException {
        // Normalize the entry name and check for directory traversal
        String normalizedName = entryName.replace('\\', '/');

        if (normalizedName.contains("../") || normalizedName.startsWith("/")) {
            throw new IOException("Invalid entry name (potential directory traversal): " + entryName);
        }

        return normalizedName;
    }
}