package com.github.kd_gaming1.packcore.scamshield.storage;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.detector.PatternStats;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamPattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Manages persistent storage for ScamShield data including patterns and detection history
 */
public class ScamShieldDataManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final ScamShieldDataManager INSTANCE = new ScamShieldDataManager();

    private final Path scamShieldDir;
    private final Path patternsFile;
    private final Path detectionsFile;
    private final Path statsFile;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private ScamShieldDataManager() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        this.scamShieldDir = gameDir.resolve("packcore/scamshield");
        this.patternsFile = scamShieldDir.resolve("patterns.json");
        this.detectionsFile = scamShieldDir.resolve("detections.json");
        this.statsFile = scamShieldDir.resolve("pattern-stats.json");

        initializeDirectories();
    }

    public static ScamShieldDataManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize ScamShield directories
     */
    private void initializeDirectories() {
        try {
            Files.createDirectories(scamShieldDir);
            PackCore.LOGGER.info("[ScamShield] Data directory initialized at: {}", scamShieldDir);
        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to create ScamShield directories", e);
        }
    }

    // ==================== PATTERN STORAGE ====================

    /**
     * Save custom patterns to file
     *
     * @param patterns List of patterns to save
     */
    public void savePatternsAsync(List<ScamPattern> patterns) {
        CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                List<SerializedPattern> serialized = new ArrayList<>();
                for (ScamPattern pattern : patterns) {
                    serialized.add(SerializedPattern.fromPattern(pattern));
                }

                Map<String, Object> envelope = new HashMap<>();
                envelope.put("patterns", serialized);

                String json = GSON.toJson(envelope);

                // Write to temporary file first (atomic operation)
                Path tempFile = patternsFile.resolveSibling(patternsFile.getFileName() + ".tmp");
                Files.writeString(tempFile, json, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // Atomic move (replace existing file)
                Files.move(tempFile, patternsFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);

                PackCore.LOGGER.info("[ScamShield] Saved {} custom patterns", patterns.size());
            } catch (IOException e) {
                PackCore.LOGGER.error("[ScamShield] Failed to save patterns", e);
                throw new RuntimeException("[ScamShield] Failed to save patterns", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }


    /**
     * Load custom patterns from file
     * @return List of loaded patterns
     */
    public List<ScamPattern> loadPatterns() {
        lock.readLock().lock();
        try {
            if (!Files.exists(patternsFile)) {
                PackCore.LOGGER.info("[ScamShield] No custom patterns file found, using defaults");
                return new ArrayList<>();
            }

            String json = Files.readString(patternsFile, StandardCharsets.UTF_8);
            JsonElement root;
            try {
                root = JsonParser.parseString(json);
            } catch (JsonSyntaxException e) {
                PackCore.LOGGER.error("[ScamShield] patterns.json is not valid JSON, ignoring file", e);
                return new ArrayList<>();
            }

            if (!root.isJsonObject()) {
                PackCore.LOGGER.warn("[ScamShield] patterns.json root is not an object, expected { \"patterns\": [...] }, ignoring file");
                return new ArrayList<>();
            }

            JsonObject obj = root.getAsJsonObject();
            if (!obj.has("patterns") || !obj.get("patterns").isJsonArray()) {
                PackCore.LOGGER.warn("[ScamShield] patterns.json missing required \"patterns\" array, ignoring file");
                return new ArrayList<>();
            }

            SerializedPattern[] arr = GSON.fromJson(obj.get("patterns"), SerializedPattern[].class);
            List<ScamPattern> patterns = new ArrayList<>();
            if (arr != null) {
                for (SerializedPattern sp : arr) {
                    try {
                        patterns.add(sp.toPattern());
                    } catch (Exception ex) {
                        PackCore.LOGGER.error("[ScamShield] Failed to convert a serialized pattern to runtime pattern, skipping entry", ex);
                    }
                }
            }

            PackCore.LOGGER.info("[ScamShield] Loaded {} custom patterns", patterns.size());
            return patterns;

        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to read patterns file", e);
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== DETECTION HISTORY ====================

    /**
     * Save a detected scam message to history
     *
     * @param detection The detection to save
     */
    public void saveDetectionAsync(DetectedScam detection) {
        CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                List<DetectedScam> history = loadDetectionHistory();
                history.add(detection);

                int maxHistory = PackCoreConfig.scamShieldMaxHistorySize;
                if (history.size() > maxHistory) {
                    history = history.subList(history.size() - maxHistory, history.size());
                }

                String json = GSON.toJson(history);

                // Atomic write to prevent corruption
                Path tempFile = detectionsFile.resolveSibling(detectionsFile.getFileName() + ".tmp");
                Files.writeString(tempFile, json, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Files.move(tempFile, detectionsFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);

                if (PackCoreConfig.enableScamShieldDebugging) {
                    PackCore.LOGGER.debug("[ScamShield] Saved detection to history (total: {})", history.size());
                }
            } catch (IOException e) {
                PackCore.LOGGER.error("[ScamShield] Failed to save detection", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }


    /**
     * Load detection history from file
     * @return List of detected scams
     */
    public List<DetectedScam> loadDetectionHistory() {
        lock.readLock().lock();
        try {
            if (!Files.exists(detectionsFile)) {
                return new ArrayList<>();
            }

            String json = Files.readString(detectionsFile, StandardCharsets.UTF_8);
            DetectedScam[] detections = GSON.fromJson(json, DetectedScam[].class);

            if (detections == null) {
                return new ArrayList<>();
            }

            return new ArrayList<>(List.of(detections));

        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to load detection history", e);
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clear all detection history
     */
    public CompletableFuture<Void> clearHistoryAsync() {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                Files.deleteIfExists(detectionsFile);
                PackCore.LOGGER.info("[ScamShield] Cleared detection history");
            } catch (IOException e) {
                PackCore.LOGGER.error("[ScamShield] Failed to clear history", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    /**
     * Get statistics about detections
     * @return Detection statistics
     */
    public DetectionStats getStats() {
        List<DetectedScam> history = loadDetectionHistory();
        return new DetectionStats(history);
    }

    // ==================== PATTERN STATS ====================

    /**
     * Save pattern statistics to disk.
     */
    public CompletableFuture<Void> saveStatsAsync(Map<String, PatternStats> stats) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                // Convert to serializable format
                Map<String, PatternStats.SerializedStats> serialized = new HashMap<>();
                stats.forEach((id, stat) -> serialized.put(id, stat.toSerialized()));

                String json = GSON.toJson(serialized);

                // Atomic write to prevent corruption
                Path tempFile = statsFile.resolveSibling(statsFile.getFileName() + ".tmp");
                Files.writeString(tempFile, json, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Files.move(tempFile, statsFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);

                PackCore.LOGGER.debug("[ScamShield] Saved statistics for {} patterns", stats.size());
            } catch (IOException e) {
                PackCore.LOGGER.error("[ScamShield] Failed to save pattern statistics", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    /**
     * Load pattern statistics from disk.
     */
    public Map<String, PatternStats.SerializedStats> loadStats() {
        lock.readLock().lock();
        try {
            if (!Files.exists(statsFile)) {
                PackCore.LOGGER.info("[ScamShield] No statistics file found, starting fresh");
                return new HashMap<>();
            }

            String json = Files.readString(statsFile, StandardCharsets.UTF_8);

            Type type = new TypeToken<
                                Map<String, PatternStats.SerializedStats>>(){}.getType();

            Map<String, PatternStats.SerializedStats> loaded = GSON.fromJson(json, type);

            if (loaded == null) {
                return new HashMap<>();
            }

            PackCore.LOGGER.info("[ScamShield] Loaded statistics for {} patterns", loaded.size());
            return loaded;

        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to load pattern statistics", e);
            return new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== UTILITY ====================

    /**
     * Export all data for backup or debugging
     * @return Path to exported data file
     */
    public Path exportData() throws IOException {
        lock.readLock().lock();
        try {
            Path exportFile = scamShieldDir.resolve("scamshield_export_" +
                    System.currentTimeMillis() + ".json");

            ExportData export = new ExportData(
                    loadPatterns(),
                    loadDetectionHistory()
            );

            String json = GSON.toJson(export);
            Files.writeString(exportFile, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            PackCore.LOGGER.info("[ScamShield] Exported ScamShield data to: {}", exportFile);
            return exportFile;

        } finally {
            lock.readLock().unlock();
        }
    }

    public Path getScamShieldDirectory() {
        return scamShieldDir;
    }
}