package com.github.kd_gaming1.packcore.ui.component.tree;

import com.github.kd_gaming1.packcore.config.apply.FileDescriptionRegistry;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Shared helper for rendering file tree UI components.
 * Promotes DRY by centralizing tree rendering logic.
 */
public class FileTreeUIHelper {

    /**
     * Create a tree node row with expand/collapse, checkbox, and label
     *
     * @param node The tree node to render
     * @param depth Indentation depth
     * @param isSelected Whether this node is currently selected
     * @param onExpand Callback when expand button is clicked
     * @param onSelect Callback when checkbox is changed (node, checked)
     * @param onHover Optional callback when node is hovered
     * @return Configured FlowLayout for the tree row
     */
    public static FlowLayout createTreeNodeRow(
            FileTreeNode node,
            int depth,
            boolean isSelected,
            Runnable onExpand,
            BiConsumer<FileTreeNode, Boolean> onSelect,
            Consumer<FileTreeNode> onHover) {

        FlowLayout row = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .padding(Insets.left(depth * 16))
                .verticalAlignment(VerticalAlignment.CENTER);

        // Expand/collapse button for directories
        if (node.isDirectory() && (!node.getChildren().isEmpty() || node.hasUnloadedChildren())) {
            ButtonComponent expandBtn = (ButtonComponent) Components.button(
                            Text.literal(node.isExpanded() ? "▼" : "▶"),
                            btn -> onExpand.run()
                    ).renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ACCENT_SECONDARY, ENTRY_BORDER))
                    .sizing(Sizing.fixed(16), Sizing.fixed(16));
            row.child(expandBtn);
        } else {
            // Spacer for alignment
            BoxComponent spacer = Components.box(Sizing.fixed(16), Sizing.fixed(16));
            spacer.fill(true);
            spacer.color(Color.ofArgb(0x00000000));
            row.child(spacer);
        }

        // Checkbox
        if (onSelect != null) {
            CheckboxComponent checkbox = Components.checkbox(Text.empty())
                    .checked(isSelected)
                    .onChanged(checked -> onSelect.accept(node, checked));
            row.child(checkbox);
        }

        // Icon
        String icon = getNodeIcon(node);
        row.child(Components.label(Text.literal(icon))
                .color(color(ACCENT_SECONDARY)));

        // Label
        row.child(Components.label(Text.literal(node.getName()))
                .color(color(isSelected ? ACCENT_SECONDARY : TEXT_PRIMARY))
                .horizontalSizing(Sizing.expand()));

        // File count for directories
        if (node.isDirectory() && !node.getChildren().isEmpty()) {
            int fileCount = countFiles(node);
            if (fileCount > 0) {
                row.child(Components.label(Text.literal("[" + fileCount + "]"))
                        .color(color(TEXT_SECONDARY)));
            }
        }

        // Hover effects
        if (onHover != null) {
            row.mouseEnter().subscribe(() -> {
                row.surface(Surface.flat(ENTRY_HOVER));
                onHover.accept(node);
            });
            row.mouseLeave().subscribe(() ->
                    row.surface(Surface.BLANK));
        }

        return row;
    }

    /**
     * Create a simple tree node row without selection (for display only)
     */
    public static FlowLayout createTreeNodeRowReadOnly(
            FileTreeNode node,
            int depth,
            Runnable onExpand,
            Consumer<FileTreeNode> onHover) {

        return createTreeNodeRow(node, depth, false, onExpand, null, onHover);
    }

    /**
     * Get appropriate icon for a tree node
     */
    private static String getNodeIcon(FileTreeNode node) {
        if (node.isDirectory()) {
            return node.isExpanded() ? "📂" : "📁";
        }

        // Get icon from registry based on path
        return FileDescriptionRegistry.getIcon(node.getPath().toString());
    }

    /**
     * Count total files in a directory tree
     */
    public static int countFiles(FileTreeNode node) {
        if (!node.isDirectory()) {
            return 1;
        }

        int count = 0;
        for (FileTreeNode child : node.getChildren()) {
            if (!child.isHidden()) {
                count += countFiles(child);
            }
        }
        return count;
    }

    /**
     * Calculate total size of files in a tree
     */
    public static long calculateSize(FileTreeNode node) {
        if (!node.isDirectory()) {
            try {
                return java.nio.file.Files.size(node.getPath());
            } catch (Exception e) {
                return 0;
            }
        }

        long total = 0;
        for (FileTreeNode child : node.getChildren()) {
            if (!child.isHidden()) {
                total += calculateSize(child);
            }
        }
        return total;
    }

    /**
     * Apply selection to all children of a node
     */
    public static void selectNodeAndChildren(FileTreeNode node, boolean selected,
                                             java.util.Set<String> selectedPaths) {
        String path = node.getPath().toString();

        if (selected) {
            selectedPaths.add(path);
        } else {
            selectedPaths.remove(path);
        }

        if (node.isDirectory()) {
            for (FileTreeNode child : node.getChildren()) {
                selectNodeAndChildren(child, selected, selectedPaths);
            }
        }
    }

    /**
     * Check if node or any children are selected
     */
    public static boolean hasSelectedChildren(FileTreeNode node, java.util.Set<String> selectedPaths) {
        if (selectedPaths.contains(node.getPath().toString())) {
            return true;
        }

        if (node.isDirectory()) {
            for (FileTreeNode child : node.getChildren()) {
                if (hasSelectedChildren(child, selectedPaths)) {
                    return true;
                }
            }
        }

        return false;
    }
}