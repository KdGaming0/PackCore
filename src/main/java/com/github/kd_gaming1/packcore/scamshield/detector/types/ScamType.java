package com.github.kd_gaming1.packcore.scamshield.detector.types;

import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.context.ConversationContext;

public interface ScamType {
    String getId();

    String getDisplayName();

    void analyze(String message, String rawMessage, String sender,
                 ConversationContext context, DetectionResult.Builder result);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    default double getBaseSensitivity() {
        return 1.0;
    }

    default void reload() {
        // Override to reload configuration from files
    }
}