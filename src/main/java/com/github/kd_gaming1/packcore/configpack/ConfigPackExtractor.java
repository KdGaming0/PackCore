package com.github.kd_gaming1.packcore.configpack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ConfigPackExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ConfigPackExtractor");

    private ConfigPackExtractor() {}

    /** Extracts the entire ZIP to the destination. */
    public static void extractAll(Path zipPath, Path extractionRoot, OverwriteMode overwriteMode) throws IOException {
        extractFromZip(zipPath, extractionRoot, overwriteMode, null);
    }

    /** Extracts only the specified internal ZIP paths (files or directories). */
    public static void extractSelective(Path zipPath, Path extractionRoot, OverwriteMode overwriteMode, Collection<String> targets) throws IOException {
        extractFromZip(zipPath, extractionRoot, overwriteMode, targets);
    }

    private static void extractFromZip(Path zipPath, Path extractionRoot, OverwriteMode overwriteMode, Collection<String> targetPaths) throws IOException {
        Path absoluteRoot = extractionRoot.toAbsolutePath().normalize();
        Files.createDirectories(absoluteRoot);

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (targetPaths != null && !isTargetedEntry(entry.getName(), targetPaths)) {
                    continue;
                }

                extractEntry(zipFile, entry, absoluteRoot, overwriteMode);
            }
        }

        LOGGER.info("Finished extraction from '{}'", zipPath.getFileName());
    }

    private static boolean isTargetedEntry(String entryName, Collection<String> targetPaths) {
        for (String target : targetPaths) {
            if (entryName.equals(target) || entryName.startsWith(target)) {
                return true;
            }
        }
        return false;
    }

    private static void extractEntry(ZipFile zipFile, ZipEntry entry, Path extractionRoot, OverwriteMode overwriteMode) throws IOException {
        Path targetPath = extractionRoot.resolve(entry.getName()).normalize();

        // Prevent zip slip attacks
        if (!targetPath.startsWith(extractionRoot)) {
            throw new IOException("Zip slip detected: " + entry.getName());
        }

        if (entry.isDirectory()) {
            Files.createDirectories(targetPath);
        } else {
            Files.createDirectories(targetPath.getParent());
            writeEntry(zipFile, entry, targetPath, overwriteMode);
        }
    }

    private static void writeEntry(ZipFile zipFile, ZipEntry entry, Path targetPath, OverwriteMode overwriteMode) throws IOException {
        if (overwriteMode == OverwriteMode.SKIP_EXISTING && Files.exists(targetPath)) return;

        CopyOption[] options = (overwriteMode == OverwriteMode.REPLACE_EXISTING)
                ? new CopyOption[]{ StandardCopyOption.REPLACE_EXISTING }
                : new CopyOption[0];

        try (InputStream in = zipFile.getInputStream(entry)) {
            Files.copy(in, targetPath, options);
        }
    }

    public enum OverwriteMode {
        SKIP_EXISTING, REPLACE_EXISTING, FAIL_IF_EXISTS
    }
}