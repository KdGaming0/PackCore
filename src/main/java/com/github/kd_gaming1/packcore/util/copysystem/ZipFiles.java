package com.github.kd_gaming1.packcore.util.copysystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;

public class ZipFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipFiles.class);

    public interface ProgressCallback {
        void onProgress(long bytesProcessed, long totalBytes, int percentage);
    }

    /**
     * Zip the contents of the directory 'dir' into a zip file at 'zipFilePath'.
     * The method walks the directory tree and writes entries relative to the base directory.
     */
    public void zipDirectory(File dir, String zipFilePath, ProgressCallback progressCallback) {
        Path base = dir.toPath();

        try (Stream<Path> walk = Files.walk(base)) {
            // Calculate total size (sum of regular files)
            long totalSize = walk.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (IOException e) { return 0L; }
                    }).sum();

            long[] processed = {0L}; // mutable holder for lambda

            try (FileOutputStream fos = new FileOutputStream(zipFilePath);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 ZipOutputStream zos = new ZipOutputStream(bos)) {

                byte[] buffer = new byte[8192];

                try (Stream<Path> files = Files.walk(base)) {
                    files.forEach(path -> {
                        try {
                            Path rel = base.relativize(path);
                            String entryName = rel.toString().replace(File.separatorChar, '/');
                            if (Files.isDirectory(path)) {
                                if (!entryName.endsWith("/")) entryName = entryName + "/";
                                ZipEntry dirEntry = new ZipEntry(entryName);
                                zos.putNextEntry(dirEntry);
                                zos.closeEntry();
                            } else {
                                ZipEntry ze = new ZipEntry(entryName);
                                zos.putNextEntry(ze);
                                try (InputStream in = Files.newInputStream(path)) {
                                    int len;
                                    while ((len = in.read(buffer)) > 0) {
                                        zos.write(buffer, 0, len);
                                        processed[0] += len;
                                        if (progressCallback != null && totalSize > 0) {
                                            int percentage = (int) ((processed[0] * 100) / totalSize);
                                            progressCallback.onProgress(processed[0], totalSize, percentage);
                                        }
                                    }
                                }
                                zos.closeEntry();
                            }
                        } catch (IOException e) {
                            LOGGER.error("Failed zipping path: {}", path, e);
                        }
                    });
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to zip files", e);
        }
    }

    public static void zipSingleFile(File file, String zipFileName, ProgressCallback progressCallback) {
        try {
            long totalSize = file.length();
            long processed = 0;

            try (FileOutputStream fos = new FileOutputStream(zipFileName);
                 ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
                 FileInputStream fis = new FileInputStream(file)) {

                ZipEntry ze = new ZipEntry(file.getName());
                zos.putNextEntry(ze);

                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                    processed += len;

                    if (progressCallback != null && totalSize > 0) {
                        int percentage = (int) ((processed * 100) / totalSize);
                        progressCallback.onProgress(processed, totalSize, percentage);
                    }
                }

                zos.closeEntry();
                LOGGER.info("{} is zipped to {}", file.getCanonicalPath(), zipFileName);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to zip file", e);
        }
    }
}