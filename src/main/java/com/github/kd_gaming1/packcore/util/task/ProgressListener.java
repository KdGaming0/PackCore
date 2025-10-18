package com.github.kd_gaming1.packcore.util.task;

/**
 * Generic progress listener for long-running tasks (import/export/backup/zip).
 */
public interface ProgressListener {
    /**
     * Report progress. Percentage should be 0..100 when applicable, or -1 for indeterminate.
     */
    void onProgress(String message, int percentage);

    /**
     * Called when the task completes. If success is false, message should describe the failure.
     */
    void onComplete(boolean success, String message);
}