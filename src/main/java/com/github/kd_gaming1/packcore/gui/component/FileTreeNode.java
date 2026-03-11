package com.github.kd_gaming1.packcore.gui.component;

import java.util.ArrayList;
import java.util.List;

public final class FileTreeNode {

    private final String name;
    private final String path;
    private final boolean directory;
    private final List<FileTreeNode> children = new ArrayList<>();

    private boolean expanded = false;
    private boolean selected = false;

    public FileTreeNode(String name, String path, boolean directory) {
        this.name = name;
        this.path = path;
        this.directory = directory;
    }

    public void addChild(FileTreeNode child) { children.add(child); }

    public String name() { return name; }
    public String path() { return path; }
    public boolean isDirectory() { return directory; }
    public List<FileTreeNode> children() { return children; }
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public List<String> collectSelectedPaths() {
        List<String> result = new ArrayList<>();
        collectInto(result);
        return result;
    }

    private void collectInto(List<String> result) {
        if (!directory && selected) result.add(path);
        for (FileTreeNode child : children) child.collectInto(result);
    }

    public void setSelectedRecursive(boolean selected) {
        if (!directory) this.selected = selected;
        for (FileTreeNode child : children) child.setSelectedRecursive(selected);
    }

    public boolean isAllSelected() {
        if (!directory) return selected;
        if (children.isEmpty()) return false;
        return children.stream().allMatch(FileTreeNode::isAllSelected);
    }

    public boolean isAnySelected() {
        if (!directory) return selected;
        return children.stream().anyMatch(FileTreeNode::isAnySelected);
    }

}