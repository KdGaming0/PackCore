package com.github.kdgaming0.packcore.copysystem.utils;
/**
 * Interface to listen for extraction progress updates.
 */
public interface ExtractionProgressListener {
    /**
     * Called to update the progress percentage.
     *
     * @param progress The current progress percentage (0-100).
     */
    void onProgress(int progress);
}
