package com.github.kdgaming0.packcore.copysystem;

import com.github.kdgaming0.packcore.copysystem.utils.ExtractionProgressListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class provides functionality to extract the contents of a ZIP file
 * located in the "Skyblock Enhanced" folder and place those contents at the root of
 * the Minecraft directory before Minecraft finishes loading.
 */
public class ZipExtractor {

    /**
     * Extracts a ZIP file into the root of the Minecraft instance.
     *
     * @param zipFileName   The name of the ZIP file located in the Official or Custom folder.
     * @param subfolderName The subfolder name ("Official" or "Custom") where the ZIP is located.
     * @param minecraftRoot The root directory of the Minecraft instance.
     * @param listener      The listener to receive progress updates.
     * @return true if extraction was successful, false otherwise.
     */
    public static boolean extractZipContents(String zipFileName, String subfolderName, File minecraftRoot, ExtractionProgressListener listener) {
        File skyblockFolder = new File(minecraftRoot, "Skyblock Enhanced");
        File subfolder = new File(skyblockFolder, subfolderName);
        File zipFilePath = new File(subfolder, zipFileName);

        // Ensure the ZIP file exists
        if (!zipFilePath.exists()) {
            System.err.println("ZIP file not found at: " + zipFilePath.getAbsolutePath());
            return false;
        }

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entriesEnum = zipFile.entries();
            List<ZipEntry> zipEntries = new ArrayList<>();
            while (entriesEnum.hasMoreElements()) {
                zipEntries.add(entriesEnum.nextElement());
            }

            int totalCount = zipEntries.size();
            int extractedCount = 0;

            for (ZipEntry entry : zipEntries) {
                // Perform the actual file extraction
                Path outPath = new File(minecraftRoot, entry.getName()).toPath();

                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    // Create parent directories if they don't exist
                    if (outPath.getParent() != null) {
                        Files.createDirectories(outPath.getParent());
                    }
                    // Copy file contents
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, outPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                // Update progress
                extractedCount++;
                int progress = (int)(((double)extractedCount / totalCount) * 100);
                listener.onProgress(progress);
            }

            System.out.println("Successfully extracted contents of: " + zipFilePath.getName());
            return true;

        } catch (IOException e) {
            System.err.println("An error occurred during extraction: " + e.getMessage());
            return false;
        }
    }
}