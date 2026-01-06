package com.github.kd_gaming1.packcore.util.io.zip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UnzipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnzipService.class);
    private static final int BUFFER_SIZE = 16384; // Larger buffer for better I/O performance

    public interface ProgressCallback {
        void onProgress(long bytesProcessed, long totalBytes, int percentage);
    }

    public void unzip(String zipFilePath, String destDir, ProgressCallback progressCallback) throws IOException {
        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();

        // Use ZipFile instead of ZipInputStream - allows random access and pre-calculated sizes
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            // Calculate total size from ZIP entries (no second pass needed)
            long totalSize = zipFile.stream()
                    .filter(e -> !e.isDirectory())
                    .mapToLong(ZipEntry::getSize)
                    .filter(size -> size > 0)
                    .sum();

            long processedBytes = 0;
            byte[] buffer = new byte[BUFFER_SIZE];

            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String fileName = entry.getName();
                File newFile = new File(destDir + File.separator + fileName);

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    LOGGER.debug("Unzipping: {}", fileName);

                    File parent = newFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();

                    try (InputStream is = new BufferedInputStream(zipFile.getInputStream(entry), BUFFER_SIZE);
                         FileOutputStream fos = new FileOutputStream(newFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE)) {

                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                            processedBytes += len;

                            if (progressCallback != null && totalSize > 0) {
                                int percentage = (int) ((processedBytes * 100) / totalSize);
                                progressCallback.onProgress(processedBytes, totalSize, percentage);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to unzip files", e);
            throw e;
        }
    }
}