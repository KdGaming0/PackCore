package com.github.kdgaming0.packcore.command;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Command to create ZIP archives of selected Minecraft files and folders.
 * Usage: /packcore archive <preset|folder_name> [filename]
 * Presets: vanilla-configs, mod-configs, all-configs
 */
public class ArchiveCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "packcore-archive";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/packcore archive <target> [filename]";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true; // Required for client commands
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /packcore archive <target> [filename]"));
            return;
        }

        String target = args[0];
        String customFilename = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Starting archive creation for: " + EnumChatFormatting.YELLOW + target));

        // Run archive creation in a separate thread to avoid blocking
        Thread archiveThread = new Thread(() -> {
            try {
                createArchive(sender, target, customFilename);
            } catch (Exception e) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Error creating archive: " + e.getMessage()));
                e.printStackTrace();
            }
        });
        archiveThread.start();
    }

    private void createArchive(ICommandSender sender, String target, String customFilename) {
        File minecraftRoot = Minecraft.getMinecraft().mcDataDir;
        File skyblockFolder = new File(minecraftRoot, "Skyblock Enhanced");
        File customConfigFolder = new File(skyblockFolder, "CustomConfigs");

        // Ensure CustomConfigs folder exists
        if (!customConfigFolder.exists()) {
            customConfigFolder.mkdirs();
        }

        // Generate filename if not provided
        String filename = customFilename != null ? customFilename : generateFilename(target);
        if (!filename.endsWith(".zip")) {
            filename += ".zip";
        }

        // Get paths based on target (preset or folder name)
        List<Path> pathsToArchive = getPathsForTarget(target, minecraftRoot, sender);

        if (pathsToArchive.isEmpty()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No valid paths found for: " + target));
            return;
        }

        // Create the archive
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Creating archive with " + pathsToArchive.size() + " items..."));

        boolean success = createZipArchive(filename, customConfigFolder.getAbsolutePath(), pathsToArchive, minecraftRoot, sender);

        if (success) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Archive created successfully: " + EnumChatFormatting.WHITE + filename));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Location: " + EnumChatFormatting.WHITE + customConfigFolder.getAbsolutePath()));
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Failed to create archive. Check console for errors."));
        }
    }

    private List<Path> getPathsForTarget(String target, File minecraftRoot, ICommandSender sender) {
        List<Path> paths = new ArrayList<>();
        Path rootPath = minecraftRoot.toPath();

        switch (target.toLowerCase()) {
            case "vanilla-configs":
                // Vanilla Minecraft configuration files
                addIfExists(paths, rootPath.resolve("options.txt"), sender);
                addIfExists(paths, rootPath.resolve("servers.dat"), sender);
                break;

            case "mod-configs":
                // Only the config folder (mod configurations)
                addIfExists(paths, rootPath.resolve("config"), sender);
                break;

            case "all-configs":
                // Both vanilla and mod configs
                addIfExists(paths, rootPath.resolve("options.txt"), sender);
                addIfExists(paths, rootPath.resolve("servers.dat"), sender);
                addIfExists(paths, rootPath.resolve("config"), sender);
                break;

            default:
                // Treat as folder name
                Path targetPath = rootPath.resolve(target);
                if (targetPath.toFile().exists()) {
                    paths.add(targetPath);
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Found folder: " + EnumChatFormatting.YELLOW + target));
                } else {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Folder not found: " + EnumChatFormatting.YELLOW + target));
                }
                break;
        }

        return paths;
    }

    private void addIfExists(List<Path> paths, Path path, ICommandSender sender) {
        if (path.toFile().exists()) {
            paths.add(path);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Found: " + EnumChatFormatting.YELLOW + path.getFileName()));
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Skipped (not found): " + EnumChatFormatting.YELLOW + path.getFileName()));
        }
    }

    private String generateFilename(String target) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return target.replace("-", "_") + "_backup_" + timestamp;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "vanilla-configs", "mod-configs", "all-configs", "config", "resourcepacks", "shaderpacks", "screenshots");
        }
        return Arrays.asList();
    }

    // Simple ZIP archiver implementation
    private boolean createZipArchive(String filename, String outputDir, List<Path> pathsToArchive, File rootDir, ICommandSender sender) {
        try {
            File outputFile = new File(outputDir, filename);
            FileOutputStream fos = new FileOutputStream(outputFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            int totalFiles = 0;
            for (Path path : pathsToArchive) {
                totalFiles += countFiles(path.toFile());
            }

            int currentFile = 0;
            for (Path path : pathsToArchive) {
                currentFile = addToZip(zos, path.toFile(), rootDir, "", currentFile, totalFiles, sender);
            }

            zos.close();
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int addToZip(ZipOutputStream zos, File file, File rootDir, String parentPath, int currentFile, int totalFiles, ICommandSender sender) throws IOException {
        if (file.isDirectory()) {
            String dirPath = parentPath + file.getName() + "/";
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    currentFile = addToZip(zos, subFile, rootDir, dirPath, currentFile, totalFiles, sender);
                }
            }
        } else {
            String filePath = parentPath + file.getName();
            ZipEntry entry = new ZipEntry(filePath);
            zos.putNextEntry(entry);

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            fis.close();
            zos.closeEntry();

            currentFile++;
            int progress = (currentFile * 100) / totalFiles;
            if (progress % 20 == 0) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Archive progress: " + EnumChatFormatting.YELLOW + progress + "%"));
            }
        }
        return currentFile;
    }

    private int countFiles(File file) {
        if (file.isDirectory()) {
            int count = 0;
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    count += countFiles(subFile);
                }
            }
            return count;
        } else {
            return 1;
        }
    }
}