package com.github.kd_gaming1.packcore.wizard.copysystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipFiles.class);
    private List<String> filesListInDir = new ArrayList<>();

    public interface ProgressCallback {
        void onProgress(long bytesProcessed, long totalBytes, int percentage);
    }

    public void zipDirectory(File dir, String zipDirName, ProgressCallback progressCallback) {
        try {
            populateFilesList(dir);
            long totalSize = calculateTotalSize();
            long processedBytes = 0;

            try (FileOutputStream fos = new FileOutputStream(zipDirName);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                byte[] buffer = new byte[4096];

                for (String filePath : filesListInDir) {
                    LOGGER.info("Zipping {}", filePath);
                    ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length() + 1));
                    zos.putNextEntry(ze);

                    try (FileInputStream fis = new FileInputStream(filePath)) {
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                            processedBytes += len;

                            if (progressCallback != null && totalSize > 0) {
                                int percentage = (int) ((processedBytes * 100) / totalSize);
                                progressCallback.onProgress(processedBytes, totalSize, percentage);
                            }
                        }
                    }
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to zip files", e);
        } finally {
            filesListInDir.clear();
        }
    }

    public static void zipSingleFile(File file, String zipFileName, ProgressCallback progressCallback) {
        try {
            long totalSize = file.length();
            long processedBytes = 0;

            try (FileOutputStream fos = new FileOutputStream(zipFileName);
                 ZipOutputStream zos = new ZipOutputStream(fos);
                 FileInputStream fis = new FileInputStream(file)) {

                ZipEntry ze = new ZipEntry(file.getName());
                zos.putNextEntry(ze);

                byte[] buffer = new byte[4096];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                    processedBytes += len;

                    if (progressCallback != null && totalSize > 0) {
                        int percentage = (int) ((processedBytes * 100) / totalSize);
                        progressCallback.onProgress(processedBytes, totalSize, percentage);
                    }
                }

                zos.closeEntry();
                LOGGER.info("{} is zipped to {}", file.getCanonicalPath(), zipFileName);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to zip file", e);
        }
    }

    private long calculateTotalSize() {
        long totalSize = 0;
        for (String filePath : filesListInDir) {
            File file = new File(filePath);
            totalSize += file.length();
        }
        return totalSize;
    }

    private void populateFilesList(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    filesListInDir.add(file.getAbsolutePath());
                } else if (file.isDirectory()) {
                    String dirPath = file.getAbsolutePath() + File.separator;
                    filesListInDir.add(dirPath);
                    populateFilesList(file);
                }
            }
        }
    }
}