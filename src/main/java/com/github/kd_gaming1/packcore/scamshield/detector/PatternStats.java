package com.github.kd_gaming1.packcore.scamshield.detector;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks statistics for a single pattern.
 */
public class PatternStats {
    private final AtomicLong matchCount = new AtomicLong(0);
    private final AtomicLong timeoutCount = new AtomicLong(0);
    private volatile long lastMatchTimestamp = 0;
    private volatile long firstMatchTimestamp = 0;

    public void recordMatch() {
        matchCount.incrementAndGet();
        long now = System.currentTimeMillis();
        lastMatchTimestamp = now;

        // Set first match timestamp if this is the first match
        if (firstMatchTimestamp == 0) {
            firstMatchTimestamp = now;
        }
    }

    public void recordTimeout() {
        timeoutCount.incrementAndGet();
    }

    // Getters
    public long getMatchCount() {
        return matchCount.get();
    }

    public long getTimeoutCount() {
        return timeoutCount.get();
    }

    public long getLastMatchTimestamp() {
        return lastMatchTimestamp;
    }

    public long getFirstMatchTimestamp() {
        return firstMatchTimestamp;
    }

    public void reset() {
        matchCount.set(0);
        timeoutCount.set(0);
        lastMatchTimestamp = 0;
        firstMatchTimestamp = 0;
    }

    /**
     * Convert to a serializable format for JSON.
     */
    public SerializedStats toSerialized() {
        return new SerializedStats(
                matchCount.get(),
                timeoutCount.get(),
                lastMatchTimestamp,
                firstMatchTimestamp
        );
    }

    /**
     * Load from serialized format.
     */
    public void fromSerialized(SerializedStats serialized) {
        matchCount.set(serialized.matchCount);
        timeoutCount.set(serialized.timeoutCount);
        lastMatchTimestamp = serialized.lastMatchTimestamp;
        firstMatchTimestamp = serialized.firstMatchTimestamp;
    }

    /**
     * JSON-friendly representation of stats.
     */
    public static class SerializedStats {
        private final long matchCount;
        private final long timeoutCount;
        private final long lastMatchTimestamp;
        private final long firstMatchTimestamp;

        public SerializedStats(long matchCount, long timeoutCount,
                               long lastMatchTimestamp, long firstMatchTimestamp) {
            this.matchCount = matchCount;
            this.timeoutCount = timeoutCount;
            this.lastMatchTimestamp = lastMatchTimestamp;
            this.firstMatchTimestamp = firstMatchTimestamp;
        }

        // Getters for Gson
        public long getMatchCount() { return matchCount; }
        public long getTimeoutCount() { return timeoutCount; }
        public long getLastMatchTimestamp() { return lastMatchTimestamp; }
        public long getFirstMatchTimestamp() { return firstMatchTimestamp; }
    }
}