package com.github.kd_gaming1.packcore.gui.component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public final class FileTreeBuilder {

    private FileTreeBuilder() {}

    public static FileTreeNode fromZip(Path zipPath) throws IOException {
        FileTreeNode root = new FileTreeNode("root", "", true);
        root.setExpanded(true);

        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            zip.entries().asIterator().forEachRemaining(entry -> {
                if (!entry.getName().equals("pack.json")) {
                    insertPath(root, entry.getName(), entry.isDirectory());
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

        if (!Files.exists(dir)) return root;

        try (Stream<Path> stream = Files.walk(dir, 8)) {
            stream.filter(p -> !p.equals(dir))
                    .sorted()
                    .forEach(p -> {
                        String rel = dir.relativize(p).toString().replace('\\', '/');
                        if (hidden != null && hidden.stream().anyMatch(h -> rel.equals(h) || rel.startsWith(h + "/"))) return;
                        insertPath(root, rel, Files.isDirectory(p));
                    });
        }

        sortTree(root);
        return root;
    }

    private static void insertPath(FileTreeNode root, String path, boolean isDir) {
        String[] parts = path.split("/");
        FileTreeNode current = root;
        StringBuilder accum = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            if (!accum.isEmpty()) accum.append('/');
            accum.append(part);

            boolean isLast = (i == parts.length - 1);
            String fullPath = accum.toString();
            boolean nodeIsDir = !isLast || isDir;

            FileTreeNode existing = current.children().stream()
                    .filter(c -> c.name().equals(part))
                    .findFirst().orElse(null);

            if (existing == null) {
                FileTreeNode node = new FileTreeNode(part, fullPath, nodeIsDir);
                current.addChild(node);
                current = node;
            } else {
                current = existing;
            }
        }
    }

    private static void sortTree(FileTreeNode node) {
        node.children().sort((a, b) -> {
            if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
            return a.name().compareToIgnoreCase(b.name());
        });
        for (FileTreeNode child : node.children()) sortTree(child);
    }
}