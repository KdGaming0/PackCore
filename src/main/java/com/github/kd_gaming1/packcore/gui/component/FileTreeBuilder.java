package com.github.kd_gaming1.packcore.gui.component;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

public final class FileTreeBuilder {

    private FileTreeBuilder() {}

    public static FileTreeNode fromZip(Path zipPath) throws IOException {
        FileTreeNode root = new FileTreeNode("root", "", true);
        root.setExpanded(true);

        Map<FileTreeNode, Map<String, FileTreeNode>> childIndex = new IdentityHashMap<>();
        childIndex.put(root, new HashMap<>());

        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            zip.entries().asIterator().forEachRemaining(entry -> {
                if (!entry.getName().equals("pack.json")) {
                    insertPath(root, entry.getName(), entry.isDirectory(), childIndex);
                }
            });
        }

        sortTree(root);
        return root;
    }

    public static FileTreeNode fromDirectory(Path dir, Set<String> hidden) throws IOException {
        String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "root";
        FileTreeNode root = new FileTreeNode(dirName, "", true);
        root.setExpanded(true);

        if (!Files.exists(dir)) {
            return root;
        }

        Map<FileTreeNode, Map<String, FileTreeNode>> childIndex = new IdentityHashMap<>();
        childIndex.put(root, new HashMap<>());

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {

            @Override
            public @NonNull FileVisitResult preVisitDirectory(@NonNull Path path, @NonNull BasicFileAttributes attrs) {
                if (path.equals(dir)) {
                    return FileVisitResult.CONTINUE;
                }

                String rel = dir.relativize(path).toString().replace('\\', '/');
                String topLevel = rel.split("/")[0];

                if (topLevel.startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (hidden != null && hidden.stream().anyMatch(h -> rel.equals(h) || rel.startsWith(h + "/"))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                insertPath(root, rel, true, childIndex);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NonNull FileVisitResult visitFile(@NonNull Path path, @NonNull BasicFileAttributes attrs) {
                String rel = dir.relativize(path).toString().replace('\\', '/');
                String topLevel = rel.split("/")[0];

                if (topLevel.startsWith(".")) {
                    return FileVisitResult.CONTINUE;
                }
                if (hidden != null && hidden.stream().anyMatch(h -> rel.equals(h) || rel.startsWith(h + "/"))) {
                    return FileVisitResult.CONTINUE;
                }

                insertPath(root, rel, false, childIndex);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NonNull FileVisitResult visitFileFailed(@NonNull Path path, @NonNull IOException e) {
                return FileVisitResult.CONTINUE;
            }
        });

        sortTree(root);
        return root;
    }

    private static void insertPath(
            FileTreeNode root,
            String path,
            boolean isDirectory,
            Map<FileTreeNode, Map<String, FileTreeNode>> childIndex
    ) {
        String[] parts = path.split("/");
        FileTreeNode current = root;
        StringBuilder accumulatedPath = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }

            if (!accumulatedPath.isEmpty()) {
                accumulatedPath.append('/');
            }
            accumulatedPath.append(part);

            boolean isLastPart = i == parts.length - 1;
            boolean nodeIsDirectory = !isLastPart || isDirectory;
            String fullPath = accumulatedPath.toString();

            Map<String, FileTreeNode> indexForCurrent =
                    childIndex.computeIfAbsent(current, ignored -> new HashMap<>());
            FileTreeNode existing = indexForCurrent.get(part);

            if (existing == null) {
                FileTreeNode created = new FileTreeNode(part, fullPath, nodeIsDirectory);
                current.addChild(created);
                indexForCurrent.put(part, created);
                childIndex.put(created, new HashMap<>());
                current = created;
            } else {
                current = existing;
            }
        }
    }

    private static void sortTree(FileTreeNode node) {
        node.children().sort((a, b) -> {
            if (a.isDirectory() != b.isDirectory()) {
                return a.isDirectory() ? -1 : 1;
            }
            return a.name().compareToIgnoreCase(b.name());
        });

        for (FileTreeNode child : node.children()) {
            sortTree(child);
        }
    }
}