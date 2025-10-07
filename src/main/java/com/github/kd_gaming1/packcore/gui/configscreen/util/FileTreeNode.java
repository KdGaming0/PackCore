package com.github.kd_gaming1.packcore.gui.configscreen.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileTreeNode {
    private final Path path;
    private final String name;
    private final boolean isDirectory;
    private boolean expanded = false;
    private boolean hidden = false;
    private List<FileTreeNode> children = new ArrayList<>();
    private boolean childrenLoaded = false;
    private boolean hasUnloadedChildren = false;

    public FileTreeNode(Path path, String name, boolean isDirectory) {
        this.path = path;
        this.name = name;
        this.isDirectory = isDirectory;
    }

    public Path getPath() { return path; }
    public String getName() { return name; }
    public boolean isDirectory() { return isDirectory; }
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public List<FileTreeNode> getChildren() { return children; }
    public void addChild(FileTreeNode child) { this.children.add(child); }
    public boolean isChildrenLoaded() { return childrenLoaded; }
    public void setChildrenLoaded(boolean loaded) { this.childrenLoaded = loaded; }
    public boolean hasUnloadedChildren() { return hasUnloadedChildren; }
    public void setHasUnloadedChildren(boolean hasChildren) { this.hasUnloadedChildren = hasChildren; }
}
