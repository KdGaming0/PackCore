package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.api.widget.IWidget;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Scrollable file tree with expandable directories and selectable files.
 */
public class FileTreeComponent extends EmptyComponent {

    private static final int ROW_HEIGHT = 16;
    private static final int INDENT_WIDTH = 12;
    private static final int SCROLL_BAR_ROOM = 8;

    private static final int COLOR_HOVER = 0x22FFFFFF;
    private static final int COLOR_TEXT_FILE = 0xFFAAAAAA;
    private static final int COLOR_TEXT_FILE_SELECTED = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIR = 0xFFCCCCCC;
    private static final int COLOR_CHECKBOX_BORDER = 0xFF555555;
    private static final int COLOR_CHECKBOX_FILL = 0xFF2196F3;

    private final FileTreeNode root;
    private ScrollContainerWidget scrollContainer;
    private Runnable onSelectionChanged;

    public FileTreeComponent(int x, int y, int width, int height, FileTreeNode root) {
        super(x, y, width, height);
        this.root = root;
        build();
    }

    private void build() {
        double savedScroll = scrollContainer != null ? scrollContainer.scrollAmount() : 0;
        this.clearComponents();

        List<VisibleNode> visible = new ArrayList<>();
        collectVisible(root, 0, visible);

        int rowWidth = getWidth() - SCROLL_BAR_ROOM;
        EmptyComponent rows = new EmptyComponent(0, 0, rowWidth, visible.size() * ROW_HEIGHT);

        int nodeY = 0;
        for (VisibleNode vn : visible) {
            rows.addWidget(new FileTreeRowWidget(0, nodeY, rowWidth, ROW_HEIGHT, vn.node, vn.depth));
            nodeY += ROW_HEIGHT;
        }

        scrollContainer = new ScrollContainerWidget(getWidth(), getHeight());
        scrollContainer.addComponent(rows);
        scrollContainer.setScrollAmount(savedScroll);

        EmptyComponent wrapper = new EmptyComponent(0, 0, getWidth(), getHeight());
        wrapper.addWidget(scrollContainer);
        this.addComponent(wrapper);
        this.updateParentPosition(getParentX(), getParentY(), getWidth(), getHeight());
    }

    private void collectVisible(FileTreeNode node, int depth, List<VisibleNode> result) {
        for (FileTreeNode child : node.children()) {
            result.add(new VisibleNode(child, depth));
            if (child.isDirectory() && child.isExpanded()) collectVisible(child, depth + 1, result);
        }
    }

    public List<String> getSelectedPaths() { return root.collectSelectedPaths(); }

    public void setOnSelectionChanged(Runnable callback) { this.onSelectionChanged = callback; }

    private record VisibleNode(FileTreeNode node, int depth) {}

    private class FileTreeRowWidget extends AbstractContainerWidget implements IWidget {

        private final FileTreeNode node;
        private final int depth;

        FileTreeRowWidget(int x, int y, int width, int height, FileTreeNode node, int depth) {
            super(x, y, width, height, Component.empty());
            this.node = node;
            this.depth = depth;
        }

        @Override
        protected void renderWidget(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            var font = Minecraft.getInstance().font;
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int indent = INDENT_WIDTH * depth + 4;
            int midY = y + (h - font.lineHeight) / 2;

            if (isHovered()) graphics.fill(x, y, x + w, y + h, COLOR_HOVER);

            if (node.isDirectory()) {
                int checkSize = 8;
                int checkX = x + indent;
                int checkY = y + (h - checkSize) / 2;

                // Draw directory checkbox border
                graphics.fill(checkX, checkY, checkX + checkSize, checkY + 1, COLOR_CHECKBOX_BORDER);
                graphics.fill(checkX, checkY + checkSize - 1, checkX + checkSize, checkY + checkSize, COLOR_CHECKBOX_BORDER);
                graphics.fill(checkX, checkY, checkX + 1, checkY + checkSize, COLOR_CHECKBOX_BORDER);
                graphics.fill(checkX + checkSize - 1, checkY, checkX + checkSize, checkY + checkSize, COLOR_CHECKBOX_BORDER);

                if (node.isAllSelected()) {
                    // Fully selected: solid fill
                    graphics.fill(checkX + 1, checkY + 1, checkX + checkSize - 1, checkY + checkSize - 1, COLOR_CHECKBOX_FILL);
                } else if (node.isAnySelected()) {
                    // Partially selected: dimmer fill to indicate indeterminate
                    graphics.fill(checkX + 2, checkY + 2, checkX + checkSize - 2, checkY + checkSize - 2, COLOR_CHECKBOX_FILL);
                }

                String prefix = node.isExpanded() ? "v " : "> ";
                graphics.drawString(font, prefix + node.name(), checkX + checkSize + 3, midY, COLOR_TEXT_DIR, false);
            } else {
                int checkSize = 8;
                int checkX = x + indent;
                int checkY = y + (h - checkSize) / 2;

                graphics.fill(checkX, checkY, checkX + checkSize, checkY + 1, COLOR_CHECKBOX_BORDER);
                graphics.fill(checkX, checkY + checkSize - 1, checkX + checkSize, checkY + checkSize, COLOR_CHECKBOX_BORDER);
                graphics.fill(checkX, checkY, checkX + 1, checkY + checkSize, COLOR_CHECKBOX_BORDER);
                graphics.fill(checkX + checkSize - 1, checkY, checkX + checkSize, checkY + checkSize, COLOR_CHECKBOX_BORDER);

                if (node.isSelected()) {
                    graphics.fill(checkX + 1, checkY + 1, checkX + checkSize - 1, checkY + checkSize - 1, COLOR_CHECKBOX_FILL);
                }

                int textColor = node.isSelected() ? COLOR_TEXT_FILE_SELECTED : COLOR_TEXT_FILE;
                graphics.drawString(font, node.name(), checkX + checkSize + 3, midY, textColor, false);
            }
        }

        @Override
        public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean bl) {
            if (event.button() != 0 || !isMouseOver(event.x(), event.y())) return false;
            int indent = INDENT_WIDTH * depth + 4;
            int checkX = getX() + indent;
            int checkSize = 8;

            if (node.isDirectory()) {
                if (event.x() >= checkX && event.x() < checkX + checkSize) {
                    node.setSelectedRecursive(!node.isAllSelected());
                    if (onSelectionChanged != null) onSelectionChanged.run();
                } else {
                    node.setExpanded(!node.isExpanded());
                }
            } else {
                node.setSelected(!node.isSelected());
                if (onSelectionChanged != null) onSelectionChanged.run();
            }
            FileTreeComponent.this.build();
            return true;
        }

        @Override protected int contentHeight() { return 0; }
        @Override protected double scrollRate() { return 0; }
        @Override protected void updateWidgetNarration(@NotNull NarrationElementOutput n) {}
        @Override public @NotNull ScreenRectangle getBorderForArrowNavigation(@NotNull ScreenDirection d) { return getRectangle(); }
        @Override public @NotNull List<? extends GuiEventListener> children() { return List.of(); }
        @Override public @NotNull Collection<? extends net.minecraft.client.gui.narration.NarratableEntry> getNarratables() { return List.of(); }
    }
}