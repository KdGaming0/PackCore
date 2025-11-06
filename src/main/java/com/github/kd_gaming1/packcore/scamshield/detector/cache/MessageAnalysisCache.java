package com.github.kd_gaming1.packcore.scamshield.detector.cache;

import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class MessageAnalysisCache {
    private final long ttlMillis;

    private final Map<String, CachedEntry> cache;

    public MessageAnalysisCache(int maxSize, long ttlMillis) {
        this.ttlMillis = ttlMillis;

        this.cache = new LinkedHashMap<String, CachedEntry>(maxSize + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedEntry> eldest) {
                return size() > maxSize;
            }
        };
    }

    public DetectionResult get(String key) {
        synchronized (cache) {
            CachedEntry entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                return entry.result;
            }
            return null;
        }
    }

    public void put(String key, DetectionResult result) {
        synchronized (cache) {
            cache.put(key, new CachedEntry(result, System.currentTimeMillis() + ttlMillis));
        }
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    private static class CachedEntry {
        final DetectionResult result;
        final long expiryTime;

        CachedEntry(DetectionResult result, long expiryTime) {
            this.result = result;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}