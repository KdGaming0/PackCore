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

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class ConfigExportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Set<String> HIDDEN_FOLDERS = Set.of("packcore", "logs", "crash-reports", "screenshots", ".git", ".minecraft");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
     * Build a limited-depth file tree suitable for UI display.
     * Depth is intentionally limited to avoid expensive recursion on big directories.
     */
    public FileTreeNode buildFileTree() {
        FileTreeNode root = new FileTreeNode(gameDir, "Game Directory", true);
        root.setExpanded(true);

        try (Stream<Path> entries = Files.list(gameDir)) {
            entries.filter(Files::exists)
                    .sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p))
                            .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(path -> {
                        FileTreeNode node = createNode(path, 0);
                        if (node != null) root.addChild(node);
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

        // Hide certain folders by name
        if (HIDDEN_FOLDERS.contains(fileName.toLowerCase())) {
            node.setHidden(true);
            return node;
        }

        // Explore children up to a depth of 3 to keep UI snappy
        if (isDirectory && depth < 3) {
            try (Stream<Path> children = Files.list(path)) {
                children.filter(Files::exists)
                        .filter(childPath -> !HIDDEN_FOLDERS.contains(childPath.getFileName().toString().toLowerCase()))
                        .sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p))
                                .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                        .limit(50)
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

    public Set<Path> getPresetPaths(String presetType) {
        Set<Path> paths = new HashSet<>();
        if (presetType == null) return paths;

        switch (presetType.toLowerCase(Locale.ROOT)) {
            case "mod_only":
                addIfExists(paths, gameDir.resolve("config"));
                addIfExists(paths, gameDir.resolve("mods"));
                break;
            case "mc_only":
                addIfExists(paths, gameDir.resolve("options.txt"));
                addIfExists(paths, gameDir.resolve("servers.dat"));
                addIfExists(paths, gameDir.resolve("resourcepacks"));
                addIfExists(paths, gameDir.resolve("shaderpacks"));
                break;
            case "both":
                addIfExists(paths, gameDir.resolve("config"));
                addIfExists(paths, gameDir.resolve("mods"));
                addIfExists(paths, gameDir.resolve("options.txt"));
                addIfExists(paths, gameDir.resolve("servers.dat"));
                addIfExists(paths, gameDir.resolve("resourcepacks"));
                addIfExists(paths, gameDir.resolve("shaderpacks"));
                break;
            case "clear":
            default:
                // empty
                break;
        }

        return paths;
    }

    private void addIfExists(Set<Path> paths, Path path) {
        if (Files.exists(path)) paths.add(path);
    }

    public long calculateSelectionSize(Set<Path> selectedPaths) {
        long totalSize = 0;
        for (Path path : selectedPaths) {
            try {
                if (Files.isDirectory(path)) totalSize += calculateDirectorySize(path);
                else if (Files.isRegularFile(path)) totalSize += Files.size(path);
            } catch (IOException e) {
                LOGGER.debug("Could not calculate size for: {}", path);
            }
        }
        return totalSize;
    }

    private long calculateDirectorySize(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); }
                        catch (IOException e) { return 0L; }
                    })
                    .sum();
        } catch (IOException e) {
            LOGGER.debug("Could not calculate directory size: {}", directory);
            return 0L;
        }
    }

    /**
     * Export config to a zip. Accepts features, requirements and mods lists so those end up in metadata.
     */
    public Path exportConfig(Set<Path> selectedPaths, String name, String description,
                             String version, String author, String resolution,
                             List<String> features, List<String> requirements, List<String> mods) throws IOException {

        Path tempDir = Files.createTempDirectory("packcore_export");

        try {
            LOGGER.info("Starting export process for {} selected paths", selectedPaths.size());

            for (Path selectedPath : selectedPaths) {
                if (!Files.exists(selectedPath)) {
                    LOGGER.warn("Selected path does not exist: {}", selectedPath);
                    continue;
                }

                Path relativePath = gameDir.relativize(selectedPath);
                Path targetPath = tempDir.resolve(relativePath);

                LOGGER.info("Copying {} to {}", selectedPath, targetPath);

                if (Files.isDirectory(selectedPath)) {
                    copyDirectoryRecursively(selectedPath, targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(selectedPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            ConfigMetadata metadata = new ConfigMetadata(
                    name, description, version, author,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    resolution, features != null ? features : new ArrayList<>(),
                    requirements != null ? requirements : new ArrayList<>(),
                    mods != null ? mods : new ArrayList<>()
            );
            metadata.setSource("Community");

            Path metadataPath = tempDir.resolve(ConfigFileUtils.METADATA_FILE);
            String metadataJson = GSON.toJson(metadata);
            Files.writeString(metadataPath, metadataJson, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            LOGGER.info("Created metadata file: {}", metadataPath);

            String sanitizedName = name.replaceAll("[^a-zA-Z0-9\\-_]", "_");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String zipFileName = sanitizedName + "_" + timestamp + ".zip";
            Path zipPath = exportDir.resolve(zipFileName);

            ZipFiles zipFiles = new ZipFiles();
            zipFiles.zipDirectory(tempDir.toFile(), zipPath.toString(), (bytesProcessed, totalBytes, percentage) -> {
                if (percentage % 25 == 0) LOGGER.info("Export progress: {}%", percentage);
            });

            LOGGER.info("Config exported successfully to: {}", zipPath);
            return zipPath;
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void copyDirectoryRecursively(Path source, Path target) throws IOException {
        LOGGER.info("Copying directory recursively: {} -> {}", source, target);

        try (Stream<Path> paths = Files.walk(source)) {
            paths.forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) Files.createDirectories(targetPath);
                    else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to copy: {} -> {}", sourcePath, target.resolve(source.relativize(sourcePath)), e);
                }
            });
        }
    }

    private void deleteDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); }
                        catch (IOException e) { LOGGER.debug("Could not delete: {}", path); }
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
}