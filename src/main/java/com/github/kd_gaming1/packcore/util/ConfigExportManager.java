package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.gui.configscreen.util.FileTreeNode;
import com.github.kd_gaming1.packcore.util.copysystem.AsyncZipFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Optimized ConfigExportManager with async operations and progress reporting
 */
public class ConfigExportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigExportManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Set<String> HIDDEN_FOLDERS = Set.of(
            "packcore", "logs", "crash-reports", "screenshots",
            ".git", ".minecraft", "saves", "assets", "mods", ".firmament"
    );

    private static final Set<String> EXCLUDED_CONFIG_SUBFOLDERS = Set.of(
            "firmament/profiles", "skyhanni/backup", "skyhanni/repo",
            "skyblocker/item-repo", "skyocean/data"
    );

    private static final int MAX_TREE_DEPTH = 10;
    private static final int MAX_CHILDREN_PER_NODE = 100;
    private static final int BATCH_SIZE = 50; // Process files in batches

    private final Path gameDir;
    private final Path exportDir;
    private final Map<Path, Long> sizeCache = new ConcurrentHashMap<>();

    public ConfigExportManager() {
        this.gameDir = MinecraftClient.getInstance().runDirectory.toPath();
        this.exportDir = gameDir.resolve(ConfigFileUtils.CUSTOM_CONFIGS_PATH);

        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create export directory", e);
        }
    }

    /**
     * Build initial file tree structure (only top level, lazy load the rest)
     */
    public FileTreeNode buildFileTree() {
        FileTreeNode root = new FileTreeNode(gameDir, "Game Directory", true);
        root.setExpanded(true);

        try (Stream<Path> entries = Files.list(gameDir)) {
            List<Path> sortedEntries = entries
                    .filter(Files::exists)
                    .filter(path -> !isHidden(path))
                    .sorted(comparePaths())
                    .limit(MAX_CHILDREN_PER_NODE)
                    .toList();

            for (Path path : sortedEntries) {
                FileTreeNode node = createLazyNode(path);
                if (node != null && !node.isHidden()) {
                    root.addChild(node);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to build file tree", e);
        }

        return root;
    }

    /**
     * Create a lazy node that doesn't load children immediately
     */
    private FileTreeNode createLazyNode(Path path) {
        String fileName = path.getFileName().toString();
        boolean isDirectory = Files.isDirectory(path);

        FileTreeNode node = new FileTreeNode(path, fileName, isDirectory);

        if (isHidden(path)) {
            node.setHidden(true);
            return node;
        }

        if (isDirectory) {
            // Just check if it has children without loading them
            try (Stream<Path> children = Files.list(path)) {
                boolean hasChildren = children
                        .anyMatch(child -> !isHidden(child));
                node.setHasUnloadedChildren(hasChildren);
            } catch (IOException e) {
                LOGGER.debug("Could not check directory: {}", path);
            }
        }

        return node;
    }

    /**
     * Load children for a directory node asynchronously
     */
    public void loadNodeChildren(FileTreeNode node) {
        if (!node.isDirectory() || node.isChildrenLoaded()) {
            return;
        }

        try (Stream<Path> children = Files.list(node.getPath())) {
            List<Path> sortedChildren = children
                    .filter(Files::exists)
                    .filter(child -> !isHidden(child))
                    .sorted(comparePaths())
                    .limit(MAX_CHILDREN_PER_NODE)
                    .toList();

            for (Path childPath : sortedChildren) {
                FileTreeNode childNode = createLazyNode(childPath);
                if (childNode != null && !childNode.isHidden()) {
                    node.addChild(childNode);
                }
            }

            node.setChildrenLoaded(true);
            node.setHasUnloadedChildren(false);
        } catch (IOException e) {
            LOGGER.debug("Could not list directory: {}", node.getPath());
        }
    }

    private boolean isHidden(Path path) {
        String name = path.getFileName().toString().toLowerCase();

        if (HIDDEN_FOLDERS.contains(name) || name.startsWith(".")) {
            return true;
        }

        Path relativePath = gameDir.relativize(path);
        String relativePathStr = relativePath.toString().replace("\\", "/");

        return EXCLUDED_CONFIG_SUBFOLDERS.stream()
                .anyMatch(excluded -> relativePathStr.equals(excluded) || relativePathStr.startsWith(excluded + "/"));
    }

    private Comparator<Path> comparePaths() {
        return Comparator.comparing((Path p) -> !Files.isDirectory(p))
                .thenComparing(p -> p.getFileName().toString().toLowerCase());
    }

    /**
     * Get preset paths for common configuration combinations
     */
    public Set<Path> getPresetPaths(PresetType presetType) {
        Set<Path> paths = new HashSet<>();

        switch (presetType) {
            case MODS_ONLY -> {
                addIfExists(paths, "config");
            }
            case MINECRAFT_ONLY -> {
                addIfExists(paths, "options.txt");
                addIfExists(paths, "servers.dat");
            }
            case ALL_CONFIGS -> {
                addIfExists(paths, "config");
                addIfExists(paths, "options.txt");
                addIfExists(paths, "servers.dat");
            }
            case CLEAR -> paths.clear();
        }

        return paths;
    }

    public enum PresetType {
        MODS_ONLY("Mod Configs Only"),
        MINECRAFT_ONLY("MC Configs Only"),
        ALL_CONFIGS("Both Configs"),
        CLEAR("Clear All");

        private final String displayName;

        PresetType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private void addIfExists(Set<Path> paths, String relativePath) {
        Path path = gameDir.resolve(relativePath);
        if (Files.exists(path)) {
            paths.add(path);
        }
    }

    /**
     * Calculate total size with caching for performance
     */
    public long calculateSelectionSize(Set<Path> selectedPaths) {
        return selectedPaths.parallelStream()
                .mapToLong(this::getCachedSize)
                .sum();
    }

    private long getCachedSize(Path path) {
        return sizeCache.computeIfAbsent(path, p -> {
            try {
                if (Files.isRegularFile(p)) {
                    return Files.size(p);
                } else if (Files.isDirectory(p)) {
                    try (Stream<Path> paths = Files.walk(p)) {
                        return paths
                                .filter(Files::isRegularFile)
                                .mapToLong(file -> {
                                    try {
                                        return Files.size(file);
                                    } catch (IOException e) {
                                        return 0L;
                                    }
                                })
                                .sum();
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("Could not calculate size for: {}", p);
            }
            return 0L;
        });
    }

    /**
     * Scan mods folder and return list of mod names
     */
    public List<String> scanInstalledMods() {
        List<String> mods = new ArrayList<>();
        Path modsDir = gameDir.resolve("mods");

        if (Files.exists(modsDir) && Files.isDirectory(modsDir)) {
            try (Stream<Path> stream = Files.list(modsDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.endsWith(".jar") || name.endsWith(".zip");
                        })
                        .map(p -> p.getFileName().toString()
                                .replaceAll("\\.(jar|zip)$", ""))
                        .sorted()
                        .forEach(mods::add);
            } catch (IOException e) {
                LOGGER.error("Failed to scan mods folder", e);
            }
        }

        return mods;
    }

    /**
     * Export configuration asynchronously with progress reporting
     */
    public Path exportConfigAsync(ExportRequest request, Consumer<String> progressCallback) throws IOException {
        validateExportRequest(request);

        Path tempDir = Files.createTempDirectory("packcore_export");

        try {
            LOGGER.info("Starting async export for {} selected paths", request.selectedPaths.size());

            // Copy selected paths to temp directory
            progressCallback.accept("Copying files...");
            copySelectedPathsAsync(request.selectedPaths, tempDir, progressCallback);

            // Create metadata
            progressCallback.accept("Creating metadata...");
            ConfigMetadata metadata = ConfigMetadata.builder()
                    .name(request.name)
                    .description(request.description)
                    .version(request.version)
                    .author(request.author)
                    .targetResolution(request.targetResolution)
                    .mods(request.includedMods)
                    .source("Community")
                    .createdNow()
                    .build();

            // Write metadata file
            Path metadataPath = tempDir.resolve(ConfigFileUtils.METADATA_FILE);
            Files.writeString(metadataPath, GSON.toJson(metadata),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Create zip file
            progressCallback.accept("Creating zip archive...");
            String zipFileName = generateZipFileName(request.name);
            Path zipPath = exportDir.resolve(zipFileName);

            // Use async zip utility
            AsyncZipFiles zipFiles = new AsyncZipFiles();
            CompletableFuture<Void> zipFuture = zipFiles.zipDirectoryAsync(
                    tempDir.toFile(),
                    zipPath.toString(),
                    (bytesProcessed, totalBytes, percentage) -> {
                        progressCallback.accept(String.format("Zipping: %d%%", percentage));
                    }
            );

            // Wait for zipping to complete
            zipFuture.join();

            progressCallback.accept("Export complete!");
            LOGGER.info("Config exported successfully to: {}", zipPath);
            return zipPath;

        } finally {
            // Clean up temp directory in background
            CompletableFuture.runAsync(() -> {
                try {
                    ConfigFileOperations.deleteDirectory(tempDir);
                } catch (Exception e) {
                    LOGGER.warn("Failed to clean up temp directory", e);
                }
            });
        }
    }

    /**
     * Export configuration synchronously (fallback method)
     */
    public Path exportConfig(ExportRequest request) throws IOException {
        return exportConfigAsync(request, message -> {
        });
    }

    private void validateExportRequest(ExportRequest request) {
        if (request.selectedPaths == null || request.selectedPaths.isEmpty()) {
            throw new IllegalArgumentException("No paths selected for export");
        }
        if (request.name == null || request.name.isBlank()) {
            throw new IllegalArgumentException("Config name is required");
        }
    }

    private void copySelectedPathsAsync(Set<Path> selectedPaths, Path targetDir,
                                        Consumer<String> progressCallback) throws IOException {
        int total = selectedPaths.size();
        int processed = 0;

        // Process in batches for better performance
        List<Path> pathList = new ArrayList<>(selectedPaths);
        for (int i = 0; i < pathList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, pathList.size());
            List<Path> batch = pathList.subList(i, end);

            // Process batch in parallel
            batch.parallelStream().forEach(selectedPath -> {
                try {
                    if (!Files.exists(selectedPath)) {
                        LOGGER.warn("Selected path does not exist: {}", selectedPath);
                        return;
                    }

                    Path relativePath = gameDir.relativize(selectedPath);
                    Path targetPath = targetDir.resolve(relativePath);

                    if (Files.isDirectory(selectedPath)) {
                        ConfigFileOperations.copyDirectory(selectedPath, targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(selectedPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to copy: {}", selectedPath, e);
                }
            });

            processed = end;
            int percentage = (processed * 100) / total;
            progressCallback.accept(String.format("Copying files: %d%%", percentage));
        }
    }

    private String generateZipFileName(String configName) {
        String sanitized = configName.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return sanitized + "_" + timestamp + ".zip";
    }

    public void openExportFolder() {
        try {
            java.awt.Desktop.getDesktop().open(exportDir.toFile());
        } catch (Exception e) {
            LOGGER.error("Failed to open export folder", e);
        }
    }

    /**
         * Export request data class
         */
        public record ExportRequest(Set<Path> selectedPaths, String name, String description, String version, String author,
                                    String targetResolution, List<String> includedMods) {
            public ExportRequest(Set<Path> selectedPaths, String name, String description,
                                 String version, String author, String targetResolution,
                                 List<String> includedMods) {
                this.selectedPaths = selectedPaths;
                this.name = name;
                this.description = description;
                this.version = version;
                this.author = author;
                this.targetResolution = targetResolution;
                this.includedMods = includedMods != null ? includedMods : new ArrayList<>();
            }
        }
}