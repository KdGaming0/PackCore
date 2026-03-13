package com.github.kd_gaming1.packcore.gui.component;

import java.util.ArrayList;
import java.util.List;

public final class FileTreeNode {

    private enum SelectionState {
        NONE,
        PARTIAL,
        ALL
    }

    private final String name;
    private final String path;
    private final boolean directory;
    private final List<FileTreeNode> children = new ArrayList<>();

    private FileTreeNode parent;
    private boolean expanded = false;
    private boolean selected = false;
    private SelectionState selectionState = SelectionState.NONE;

    public FileTreeNode(String name, String path, boolean directory) {
        this.name = name;
        this.path = path;
        this.directory = directory;
    }

    public void addChild(FileTreeNode child) {
        child.parent = this;
        children.add(child);
    }

    public String name() {
        return name;
    }

    public String path() {
        return path;
    }

    public boolean isDirectory() {
        return directory;
    }

    public List<FileTreeNode> children() {
        return children;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if (directory) {
            setSelectedRecursive(selected);
            return;
        }

        if (this.selected == selected) {
            return;
        }

        this.selected = selected;
        recalculateSelectionStateUpward();
    }

    public List<String> collectSelectedPaths() {
        List<String> result = new ArrayList<>();
        collectInto(result);
        return result;
    }

    private void collectInto(List<String> result) {
        if (!directory && selected) {
            result.add(path);
        }
        for (FileTreeNode child : children) {
            child.collectInto(result);
        }
    }

    public void setSelectedRecursive(boolean selected) {
        if (directory) {
            for (FileTreeNode child : children) {
                child.setSelectedRecursive(selected);
            }
        } else {
            this.selected = selected;
        }

        recalculateSelectionState();
        if (parent != null) {
            parent.recalculateSelectionStateUpward();
        }
    }

    public boolean isAllSelected() {
        return selectionState == SelectionState.ALL;
    }

    public boolean isAnySelected() {
        return selectionState != SelectionState.NONE;
    }

    private void recalculateSelectionStateUpward() {
        FileTreeNode current = this;
        while (current != null) {
            SelectionState before = current.selectionState;
            current.recalculateSelectionState();
            if (before == current.selectionState) {
                break;
            }
            current = current.parent;
        }
    }

    private void recalculateSelectionState() {
        if (!directory) {
            selectionState = selected ? SelectionState.ALL : SelectionState.NONE;
            return;
        }

        if (children.isEmpty()) {
            selectionState = SelectionState.NONE;
            return;
        }

        boolean anySelected = false;
        boolean allSelected = true;

        for (FileTreeNode child : children) {
            SelectionState state = child.selectionState;
            if (state != SelectionState.NONE) {
                anySelected = true;
            }
            if (state != SelectionState.ALL) {
                allSelected = false;
            }

            if (anySelected && !allSelected) {
                selectionState = SelectionState.PARTIAL;
                return;
            }
        }

        selectionState = allSelected ? SelectionState.ALL : SelectionState.NONE;
    }
}