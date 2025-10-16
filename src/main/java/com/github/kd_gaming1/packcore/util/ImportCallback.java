package com.github.kd_gaming1.packcore.util;

/**
 * Callback interface for config import operations
 */
public interface ImportCallback {
    /**
     * Called during import to report progress
     *
     * @param message    Progress message
     * @param percentage Progress percentage (0-100)
     */
    void onProgress(String message, int percentage);

    /**
     * Called when import operation completes
     *
     * @param success Whether the import was successful
     * @param message Result message
     */
    void onComplete(boolean success, String message);
}