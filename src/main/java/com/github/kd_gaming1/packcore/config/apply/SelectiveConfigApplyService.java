package com.github.kd_gaming1.packcore.config. apply;

import com.github.kd_gaming1.packcore. PackCore;
import com.github. kd_gaming1.packcore.config.backup.BackupManager;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.github.kd_gaming1.packcore. util.GsonUtils;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent. CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Service for applying specific files from a config package
 * instead of the entire configuration.
 */
public class SelectiveConfigApplyService {
    private static final Gson GSON = GsonUtils. GSON;
    private static final String PENDING_SELECTIVE_CONFIG_FILE = "packcore_pending_selective_config.json";

    /**
     * Represents a file that can be selectively applied from a config
     */
    public record SelectableFile(
            String path,           // Path within the zip
            String displayName,    // User-friendly name
            FileType type,         // Type of file
            long size,             // File size in bytes
            String description,    // What this file does
            boolean isDirectory
    ) {
        public enum FileType {
            MOD_CONFIG("Mod Configuration"),
            GAME_OPTIONS("Game Settings"),
            KEYBINDINGS("Keybindings"),
            RESOURCE_PACK("Resource Pack"),
            SERVER_LIST("Server List"),
            OTHER("Other");

            private final String displayName;
            FileType(String displayName) { this.displayName = displayName; }
            public String getDisplayName() { return displayName; }
        }
    }

    /**
     * Data class for pending selective config info
     */
    private static class PendingSelectiveConfig {
        String configPath;
        String configName;
        Set<String> selectedPaths;

        PendingSelectiveConfig(String configPath, String configName, Set<String> selectedPaths) {
            this.configPath = configPath;
            this.configName = configName;
            this.selectedPaths = selectedPaths;
        }
    }

    /**
     * Scan a config zip file and build a tree of selectable files
     */
    public static CompletableFuture<List<SelectableFile>> scanConfigFiles(
            ConfigFileRepository. ConfigFile config) {
        return CompletableFuture.supplyAsync(() -> {
            List<SelectableFile> files = new ArrayList<>();

            try (ZipFile zipFile = new ZipFile(config. path(). toFile())) {
                Enumeration<?  extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // Skip metadata file
                    if (entryName.equals(ConfigFileRepository.METADATA_FILE)) {
                        continue;
                    }

                    SelectableFile. FileType type = determineFileType(entryName);
                    String description = getFileDescription(entryName, type);
                    String displayName = getDisplayName(entryName);

                    files.add(new SelectableFile(
                            entryName,
                            displayName,
                            type,
                            entry.getSize(),
                            description,
                            entry.isDirectory()
                    ));
                }

                // Sort by type and then name
                files.sort(Comparator
                        .comparing((SelectableFile f) -> f.type.ordinal())
                        .thenComparing(f -> f.displayName));

            } catch (IOException e) {
                PackCore.LOGGER.error("[Selective Config Apply Service] Failed to scan config files", e);
            }

            return files;
        });
    }

    /**
     * Schedule selected files to be applied on next game start
     */
    public static CompletableFuture<Boolean> scheduleSelectiveConfigApplication(
            ConfigFileRepository. ConfigFile config,
            Set<String> selectedPaths) {
        return CompletableFuture. supplyAsync(() -> {
            try {
                Path gameDir = FabricLoader.getInstance().getGameDir();
                Path pendingFile = gameDir.resolve(PENDING_SELECTIVE_CONFIG_FILE);

                // Create pending config info
                PendingSelectiveConfig pending = new PendingSelectiveConfig(
                        config.path(). toString(),
                        config.getDisplayName(),
                        selectedPaths
                );

                // Write to file
                String json = GSON.toJson(pending);
                Files.writeString(pendingFile, json, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                PackCore.LOGGER.info("[Selective Config Apply Service] Scheduled {} files for application: {}",
                        selectedPaths.size(), config.getDisplayName());

                // Schedule game shutdown
                MinecraftClient.getInstance().scheduleStop();

                return true;

            } catch (IOException e) {
                PackCore.LOGGER.error("[Selective Config Apply Service] Failed to schedule selective config application", e);
                return false;
            }
        });
    }

    /**
     * Check and apply pending selective config during pre-launch
     *
     * @return true if a selective config was applied
     */
    public static boolean checkAndApplyPendingSelectiveConfig(Path gameDir) {
        Path pendingFile = gameDir.resolve(PENDING_SELECTIVE_CONFIG_FILE);

        if (!Files.exists(pendingFile)) {
            return false;
        }

        try {
            // Read pending config info
            String json = Files.readString(pendingFile, StandardCharsets.UTF_8);
            PendingSelectiveConfig pending = GSON.fromJson(json, PendingSelectiveConfig.class);

            if (pending == null || pending.configPath == null || pending.selectedPaths == null) {
                PackCore.LOGGER.warn("[Selective Config Apply Service] Invalid pending selective config file");
                Files.deleteIfExists(pendingFile);
                return false;
            }

            PackCore.LOGGER.info("[Selective Config Apply Service] Found pending selective config: {} ({} files)",
                    pending.configName, pending.selectedPaths.size());

            // Create backup using backup manager
            Path backup = BackupManager.createAutoBackup();
            if (backup != null) {
                PackCore.LOGGER.info("[Selective Config Apply Service] Created auto-backup before applying: {}", backup);
            }

            // Apply the selected files
            boolean success = applySelectedFilesSync(
                    Path.of(pending.configPath),
                    pending.selectedPaths,
                    gameDir,
                    msg -> PackCore.LOGGER.info("[Selective Config Apply Service] {}", msg)
            );

            if (success) {
                PackCore.LOGGER.info("[Selective Config Apply Service] Successfully applied {} selected files",
                        pending. selectedPaths.size());
            } else {
                PackCore. LOGGER.error("[Selective Config Apply Service] Failed to apply selected files");
            }

            // Clean up pending file
            Files.deleteIfExists(pendingFile);

            return success;

        } catch (Exception e) {
            PackCore. LOGGER.error("[Selective Config Apply Service] Error processing pending selective config", e);
            try {
                Files.deleteIfExists(pendingFile);
            } catch (IOException ex) {
                PackCore.LOGGER.warn("[Selective Config Apply Service] Failed to clean up pending file", ex);
            }
            return false;
        }
    }

    /**
     * Apply selected files synchronously (for pre-launch)
     */
    private static boolean applySelectedFilesSync(
            Path zipPath,
            Set<String> selectedPaths,
            Path gameDir,
            Consumer<String> progressCallback) {

        try {
            progressCallback.accept("Extracting selected files.. .");

            // Extract only selected files
            Path tempDir = Files.createTempDirectory("packcore_selective");
            try {
                extractSelectedFiles(zipPath, selectedPaths, tempDir, progressCallback);

                progressCallback.accept("Applying files...");

                // Copy extracted files to game directory
                copySelectedFilesToGameDir(tempDir, gameDir);

                PackCore.LOGGER.info("[Selective Config Apply Service] Successfully applied {} selected files",
                        selectedPaths.size());
                progressCallback.accept("Complete!");
                return true;

            } finally {
                // Cleanup temp directory
                deleteDirectory(tempDir);
            }

        } catch (Exception e) {
            PackCore.LOGGER. error("[Selective Config Apply Service] Failed to apply selected files", e);
            progressCallback.accept("Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extract only the selected files from the zip
     */
    private static void extractSelectedFiles(
            Path zipPath,
            Set<String> selectedPaths,
            Path tempDir,
            Consumer<String> progressCallback) throws IOException {

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            int processed = 0;
            int total = selectedPaths.size();

            for (String selectedPath : selectedPaths) {
                ZipEntry entry = zipFile.getEntry(selectedPath);
                if (entry == null) {
                    PackCore.LOGGER.warn("[Selective Config Apply Service] Entry not found in zip: {}", selectedPath);
                    continue;
                }

                Path targetPath = tempDir.resolve(selectedPath);

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);

                    // Extract all files in this directory
                    extractDirectory(zipFile, selectedPath, tempDir);
                } else {
                    Files.createDirectories(targetPath.getParent());

                    try (var is = zipFile.getInputStream(entry)) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                processed++;
                int percentage = (processed * 100) / total;
                progressCallback.accept(String. format("Extracting: %d%%", percentage));
            }
        }
    }

    /**
     * Extract all files in a directory from the zip
     */
    private static void extractDirectory(ZipFile zipFile, String dirPath, Path tempDir)
            throws IOException {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (entryName.startsWith(dirPath) && !entry.isDirectory()) {
                Path targetPath = tempDir.resolve(entryName);
                Files.createDirectories(targetPath. getParent());

                try (var is = zipFile.getInputStream(entry)) {
                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Copy extracted files to game directory
     */
    private static void copySelectedFilesToGameDir(Path tempDir, Path gameDir)
            throws IOException {
        try (var paths = Files.walk(tempDir)) {
            paths.filter(Files::isRegularFile). forEach(sourcePath -> {
                try {
                    Path relativePath = tempDir.relativize(sourcePath);
                    Path targetPath = gameDir.resolve(relativePath);

                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    PackCore.LOGGER.debug("[Selective Config Apply Service] Copied: {}", relativePath);
                } catch (IOException e) {
                    PackCore.LOGGER.warn("[Selective Config Apply Service] Failed to copy file: {}", sourcePath, e);
                }
            });
        }
    }

    /**
     * Determine the file type based on path
     */
    private static SelectableFile.FileType determineFileType(String path) {
        path = path.toLowerCase();

        if (path.equals("options.txt")) {
            return SelectableFile.FileType.GAME_OPTIONS;
        } else if (path.equals("servers.dat")) {
            return SelectableFile.FileType.SERVER_LIST;
        } else if (path. startsWith("config/")) {
            return SelectableFile.FileType.MOD_CONFIG;
        } else if (path.startsWith("resourcepacks/")) {
            return SelectableFile.FileType.RESOURCE_PACK;
        }

        return SelectableFile.FileType.OTHER;
    }

    /**
     * Get display name from path
     */
    private static String getDisplayName(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }

        return path;
    }

    /**
     * Get description for a file
     */
    private static String getFileDescription(String path, SelectableFile.FileType type) {
        // Check for well-known files first
        String description = KNOWN_FILE_DESCRIPTIONS.get(path. toLowerCase());
        if (description != null) {
            return description;
        }

        // Extract mod name from config path
        if (path.startsWith("config/")) {
            String fileName = path.substring("config/".length());
            int dotIndex = fileName.indexOf('.');
            if (dotIndex > 0) {
                String modName = fileName.substring(0, dotIndex);
                // Capitalize first letter
                modName = modName.substring(0, 1).toUpperCase() + modName.substring(1);
                return "Configuration for " + modName;
            }
        }

        // Fall back to generic description based on type
        return switch (type) {
            case MOD_CONFIG -> "Mod configuration file";
            case GAME_OPTIONS -> "Minecraft game settings";
            case KEYBINDINGS -> "Control bindings";
            case RESOURCE_PACK -> "Resource pack";
            case SERVER_LIST -> "Multiplayer server list";
            case OTHER -> "Configuration file";
        };
    }

    /**
     * Known file descriptions for common configs
     * This map can be expanded as you add more mods
     */
    private static final Map<String, String> KNOWN_FILE_DESCRIPTIONS = Map.ofEntries(
            // Game files
            Map.entry("options. txt", "Minecraft video settings, controls, and preferences"),
            Map.entry("servers.dat", "Multiplayer server list"),

            // Popular mod configs
            Map.entry("config/skyblocker.json", "SkyBlocker - Item highlighting, macro features"),
            Map.entry("config/firmament.json", "Firmament - Repository integration, features"),
            Map.entry("config/skyhanni.json", "SkyHanni - Events, diana helper, farming"),
            Map.entry("config/dungeons-guide.json", "Dungeons Guide - Dungeon secrets and routing"),
            Map.entry("config/neu.json", "NotEnoughUpdates - Auction house, recipes, overlays"),
            Map.entry("config/skytils.json", "Skytils - Dungeons, kuudra, slayer features"),

            // UI mods
            Map.entry("config/owo-ui.json", "owo-ui library configuration"),
            Map.entry("config/isxander-main-menu-credits.json", "Main menu credits display"),

            // Performance mods
            Map.entry("config/sodium-options.json", "Sodium - Performance optimization settings"),
            Map.entry("config/iris. json5", "Iris Shaders - Shader pack settings")
    );

    /**
     * Delete directory recursively
     */
    private static void deleteDirectory(Path directory) {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            PackCore.LOGGER. debug("[Selective Config Apply Service] Could not delete: {}", path);
                        }
                    });
        } catch (IOException e) {
            PackCore.LOGGER.warn("[Selective Config Apply Service] Failed to delete temp directory: {}", directory);
        }
    }

    /**
     * Get groups of related files (e.g., all files for a specific mod)
     */
    public static Map<String, List<SelectableFile>> groupFilesByMod(
            List<SelectableFile> files) {
        Map<String, List<SelectableFile>> groups = new LinkedHashMap<>();

        for (SelectableFile file : files) {
            String groupName = determineGroup(file.path);
            groups.computeIfAbsent(groupName, k -> new ArrayList<>()).add(file);
        }

        return groups;
    }

    /**
     * Determine which mod group a file belongs to
     */
    private static String determineGroup(String path) {
        if (path.equals("options.txt")) return "Minecraft Settings";
        if (path.equals("servers.dat")) return "Server List";
        if (path. startsWith("config/")) {
            String fileName = path.substring("config/".length());
            int dotIndex = fileName.indexOf('.');
            if (dotIndex > 0) {
                String modName = fileName.substring(0, dotIndex);
                // Capitalize and format
                return formatModName(modName);
            }
        }
        return "Other Files";
    }

    /**
     * Format mod name for display
     */
    private static String formatModName(String modName) {
        // Handle common patterns
        modName = modName. replace("-", " ");
        modName = modName.replace("_", " ");

        // Capitalize each word
        String[] words = modName.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (! word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word. substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return formatted.toString().trim();
    }
}