package com.kd_gaming1.copysystem.utils;

/**
 * Interface for receiving progress updates during ZIP extraction.
 */
public interface ExtractionProgressListener {
    /**
     * Called when extraction progress is updated.
     * @param progress The current progress percentage (0-100)
     */
    void onProgress(int progress);
}