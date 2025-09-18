package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.gui.configscreen.util.FileTreeNode;
import com.github.kd_gaming1.packcore.wizard.copysystem.ZipFiles;
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
import java.util.stream.Stream;

public class ConfigExportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigExportManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> HIDDEN_FOLDERS = Set.of(
            "packcore", "logs", "crash-reports", "screenshots", ".git", ".minecraft", "saves", "assets", "mods"
    );
    private static final int MAX_TREE_DEPTH = 3;
    private static final int MAX_CHILDREN_PER_NODE = 50;

    private final Path gameDir;
    private final Path exportDir;

    public ConfigExportManager() {
        this.gameDir = MinecraftClient.getInstance().runDirectory.toPath();
        this.exportDir = gameDir.resolve("packcore/modpack_config/custom_configs");

        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create export directory", e);
        }
    }

    /**
     * Build a file tree for UI display with limited depth to prevent performance issues
     */
    public FileTreeNode buildFileTree() {
        FileTreeNode root = new FileTreeNode(gameDir, "Game Directory", true);
        root.setExpanded(true);

        try (Stream<Path> entries = Files.list(gameDir)) {
            entries.filter(Files::exists)
                    .filter(path -> !isHidden(path))
                    .sorted(comparePaths())
                    .limit(MAX_CHILDREN_PER_NODE)
                    .forEach(path -> {
                        FileTreeNode node = createNode(path, 0);
                        if (node != null && !node.isHidden()) {
                            root.addChild(node);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Failed to build file tree", e);
        }

        return root;
    }

    private FileTreeNode createNode(Path path, int depth) {
        String fileName = path.getFileName().toString();
        boolean isDirectory = Files.isDirectory(path);

        FileTreeNode node = new FileTreeNode(path, fileName, isDirectory);

        if (isHidden(path)) {
            node.setHidden(true);
            return node;
        }

        // Only explore directories up to max depth
        if (isDirectory && depth < MAX_TREE_DEPTH) {
            try (Stream<Path> children = Files.list(path)) {
                children.filter(Files::exists)
                        .filter(child -> !isHidden(child))
                        .sorted(comparePaths())
                        .limit(MAX_CHILDREN_PER_NODE)
                        .forEach(childPath -> {
                            FileTreeNode childNode = createNode(childPath, depth + 1);
                            if (childNode != null && !childNode.isHidden()) {
                                node.addChild(childNode);
                            }
                        });
            } catch (IOException e) {
                LOGGER.debug("Could not list directory: {}", path);
            }
        }

        return node;
    }

    private boolean isHidden(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return HIDDEN_FOLDERS.contains(name) || name.startsWith(".");
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
                addIfExists(paths, "mods");
            }
            case MINECRAFT_ONLY -> {
                addIfExists(paths, "options.txt");
                addIfExists(paths, "servers.dat");
                addIfExists(paths, "resourcepacks");
                addIfExists(paths, "shaderpacks");
            }
            case ALL_CONFIGS -> {
                addIfExists(paths, "config");
                addIfExists(paths, "mods");
                addIfExists(paths, "options.txt");
                addIfExists(paths, "servers.dat");
                addIfExists(paths, "resourcepacks");
                addIfExists(paths, "shaderpacks");
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
     * Calculate total size of selected paths
     */
    public long calculateSelectionSize(Set<Path> selectedPaths) {
        return selectedPaths.stream()
                .mapToLong(this::getPathSize)
                .sum();
    }

    private long getPathSize(Path path) {
        try {
            if (Files.isRegularFile(path)) {
                return Files.size(path);
            } else if (Files.isDirectory(path)) {
                try (Stream<Path> paths = Files.walk(path)) {
                    return paths.filter(Files::isRegularFile)
                            .mapToLong(p -> {
                                try {
                                    return Files.size(p);
                                } catch (IOException e) {
                                    return 0L;
                                }
                            })
                            .sum();
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Could not calculate size for: {}", path);
        }
        return 0L;
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
                        .map(p -> p.getFileName().toString().replaceAll("\\.(jar|zip)$", ""))
                        .sorted()
                        .forEach(mods::add);
            } catch (IOException e) {
                LOGGER.error("Failed to scan mods folder", e);
            }
        }

        return mods;
    }

    /**
     * Export configuration to a zip file with metadata
     */
    public Path exportConfig(ExportRequest request) throws IOException {
        validateExportRequest(request);

        Path tempDir = Files.createTempDirectory("packcore_export");

        try {
            LOGGER.info("Starting export for {} selected paths", request.selectedPaths.size());

            // Copy selected paths to temp directory
            copySelectedPaths(request.selectedPaths, tempDir);

            // Create metadata
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
            String zipFileName = generateZipFileName(request.name);
            Path zipPath = exportDir.resolve(zipFileName);

            ZipFiles zipFiles = new ZipFiles();
            zipFiles.zipDirectory(tempDir.toFile(), zipPath.toString(),
                    (bytesProcessed, totalBytes, percentage) -> {
                        if (percentage % 25 == 0) {
                            LOGGER.info("Export progress: {}%", percentage);
                        }
                    });

            LOGGER.info("Config exported successfully to: {}", zipPath);
            return zipPath;

        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void validateExportRequest(ExportRequest request) {
        if (request.selectedPaths == null || request.selectedPaths.isEmpty()) {
            throw new IllegalArgumentException("No paths selected for export");
        }
        if (request.name == null || request.name.isBlank()) {
            throw new IllegalArgumentException("Config name is required");
        }
    }

    private void copySelectedPaths(Set<Path> selectedPaths, Path targetDir) throws IOException {
        for (Path selectedPath : selectedPaths) {
            if (!Files.exists(selectedPath)) {
                LOGGER.warn("Selected path does not exist: {}", selectedPath);
                continue;
            }

            Path relativePath = gameDir.relativize(selectedPath);
            Path targetPath = targetDir.resolve(relativePath);

            LOGGER.debug("Copying {} to {}", selectedPath, targetPath);

            if (Files.isDirectory(selectedPath)) {
                copyDirectoryRecursively(selectedPath, targetPath);
            } else {
                Files.createDirectories(targetPath.getParent());
                Files.copy(selectedPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void copyDirectoryRecursively(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            paths.forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to copy: {}", sourcePath, e);
                }
            });
        }
    }

    private String generateZipFileName(String configName) {
        String sanitized = configName.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return sanitized + "_" + timestamp + ".zip";
    }

    private void deleteDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            LOGGER.debug("Could not delete: {}", path);
                        }
                    });
        } catch (IOException e) {
            LOGGER.debug("Could not delete temp directory: {}", directory);
        }
    }

    public void openExportFolder() {
        try {
            java.awt.Desktop.getDesktop().open(exportDir.toFile());
        } catch (Exception e) {
            LOGGER.error("Failed to open export folder", e);
        }
    }

    /**
     * Export request data class for cleaner API
     */
    public static class ExportRequest {
        public final Set<Path> selectedPaths;
        public final String name;
        public final String description;
        public final String version;
        public final String author;
        public final String targetResolution;
        public final List<String> includedMods;

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