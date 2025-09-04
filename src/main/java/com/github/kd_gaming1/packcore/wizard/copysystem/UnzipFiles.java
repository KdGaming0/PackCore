package com.github.kd_gaming1.packcore.wizard.copysystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnzipFiles.class);

    public interface ProgressCallback {
        void onProgress(long bytesProcessed, long totalBytes, int percentage);
    }

    public void unzip(String zipFilePath, String destDir, ProgressCallback progressCallback) throws IOException {
        long totalSize = calculateTotalSize(zipFilePath);
        long processedBytes = 0;

        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();

        byte[] buffer = new byte[8192];

        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {

            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);

                if (ze.isDirectory()) {
                    LOGGER.info("Creating directory {}", newFile.getAbsolutePath());
                    newFile.mkdirs();
                } else {
                    LOGGER.info("Unzipping to {}", newFile.getAbsolutePath());

                    File parent = newFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                            processedBytes += len;

                            if (progressCallback != null && totalSize > 0) {
                                int percentage = (int) ((processedBytes * 100) / totalSize);
                                progressCallback.onProgress(processedBytes, totalSize, percentage);
                            }
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to unzip files", e);
            throw e;
        }
    }

    private long calculateTotalSize(String zipFilePath) {
        long totalSize = 0;
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {

            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getSize() > 0) totalSize += ze.getSize();
                zis.closeEntry();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to calculate total size", e);
        }
        return totalSize;
    }
}