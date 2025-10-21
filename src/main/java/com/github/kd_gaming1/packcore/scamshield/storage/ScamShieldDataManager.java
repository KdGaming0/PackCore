package com.github.kd_gaming1.packcore.scamshield.storage;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScamShieldDataManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final ScamShieldDataManager INSTANCE = new ScamShieldDataManager();

    private final Path scamShieldDir;
    private final Path detectionsFile;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private ScamShieldDataManager() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        this.scamShieldDir = gameDir.resolve("packcore/scamshield");
        this.detectionsFile = scamShieldDir.resolve("detections.json");

        initializeDirectories();
    }

    public static ScamShieldDataManager getInstance() {
        return INSTANCE;
    }

    private void initializeDirectories() {
        try {
            Files.createDirectories(scamShieldDir);
            PackCore.LOGGER.info("[ScamShield] Data directory initialized at: {}", scamShieldDir);
        } catch (IOException e) {
            PackCore.LOGGER.error("[ScamShield] Failed to create ScamShield directories", e);
        }
    }

    public void saveDetectionAsync(DetectedScam detection) {
        CompletableFuture.runAsync(() -> {
            try {
                List<DetectedScam> history = loadDetectionHistory();
                history.add(detection);

                int maxHistory = PackCoreConfig.scamShieldMaxHistorySize;
                if (history.size() > maxHistory) {
                    history = new ArrayList<>(
                            history.subList(history.size() - maxHistory, history.size())
                    );
                }

                String json = GSON.toJson(history);

                lock.writeLock().lock();
                try {
                    Path tempFile = detectionsFile.resolveSibling(
                            detectionsFile.getFileName() + ".tmp"
                    );
                    Files.writeString(tempFile, json, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                    Files.move(tempFile, detectionsFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);

                    if (PackCoreConfig.enableScamShieldDebugging) {
                        PackCore.LOGGER.debug(
                                "[ScamShield] Saved detection to history (total: {})",
                                history.size()
                        );
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            } catch (IOException e) {
                PackCore.LOGGER.error("[ScamShield] Failed to save detection", e);
            }
        }, getExecutor());
    }

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

    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ScamShield-Saver");
        t.setDaemon(true);
        return t;
    });

    private ExecutorService getExecutor() {
        return saveExecutor;
    }

    public DetectionStats getStats() {
        List<DetectedScam> history = loadDetectionHistory();
        return new DetectionStats(history);
    }

    public Path getScamShieldDirectory() {
        return scamShieldDir;
    }

    public void shutdown() {
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            saveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}