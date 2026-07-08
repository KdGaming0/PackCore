package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.api.component.IComponent;
import com.daqem.uilib.api.widget.IWidget;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.color.ColorComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.gui.component.MultiSelectList.RowDescriptor;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * A scrollable list split into two sections: <b>selected</b> rows shown at the top in priority order
 * (highest first) with move-up/down controls and a position number, followed by the remaining
 * <b>available</b> rows in the order supplied.
 *
 * <p>Clicking an available row selects it (appended at the bottom of the selected section = lowest
 * priority); clicking a selected row's body deselects it; the ▲/▼ controls reorder it. The scroll
 * position is preserved across rebuilds.
 *
 * @param <T> the option type
 */
public class ReorderableSelectList<T> extends EmptyComponent {

    private static final int ROW_GAP               = 4;
    private static final int ROW_PADDING_X         = 10;
    private static final int ROW_PADDING_Y         = 8;
    private static final int SCROLL_BAR_WIDTH      = 8;
    private static final int CHECKMARK_RIGHT_MARGIN = 8;
    private static final int CHECKMARK_SIZE        = 9;
    private static final int ARROW_SIZE            = 11;
    private static final int ARROW_GAP             = 4;
    private static final int NUMBER_WIDTH          = 18;
    private static final int HEADER_GAP_ABOVE      = 8;

    private final List<T> options;
    private final RowDescriptor<T> descriptor;
    private final Consumer<T> onSelect;
    private final Consumer<T> onDeselect;
    private final Consumer<T> onMoveUp;
    private final Consumer<T> onMoveDown;
    private final Component selectedHeader;
    private final Component availableHeader;
    private final Component priorityHint;

    /** Selected ids in priority order, highest first. Mirrors the owner's ordered state. */
    private final List<String> selectedOrder;

    private ScrollContainerWidget scrollContainer;

    public ReorderableSelectList(
            int x, int y,
            int width, int height,
            List<T> options,
            RowDescriptor<T> descriptor,
            List<String> selectedOrder,
            Consumer<T> onSelect,
            Consumer<T> onDeselect,
            Consumer<T> onMoveUp,
            Consumer<T> onMoveDown,
            Component selectedHeader,
            Component availableHeader,
            Component priorityHint
    ) {
        super(x, y, width, height);
        this.options = options;
        this.descriptor = descriptor;
        this.selectedOrder = new ArrayList<>(selectedOrder);
        this.onSelect = onSelect;
        this.onDeselect = onDeselect;
        this.onMoveUp = onMoveUp;
        this.onMoveDown = onMoveDown;
        this.selectedHeader = selectedHeader;
        this.availableHeader = availableHeader;
        this.priorityHint = priorityHint;

        buildList();
    }

    private void buildList() {
        double savedScroll = scrollContainer != null ? scrollContainer.scrollAmount() : 0;
        this.clearComponents();

        int listWidth = getWidth();
        int listHeight = getHeight();
        int rowWidth = listWidth - SCROLL_BAR_WIDTH;
        int textX = ROW_PADDING_X + NUMBER_WIDTH;
        int textWidth = rowWidth - textX - ROW_PADDING_X
                - CHECKMARK_SIZE - CHECKMARK_RIGHT_MARGIN
                - 2 * ARROW_SIZE - 2 * ARROW_GAP;

        scrollContainer = new ScrollContainerWidget(listWidth, listHeight);
        EmptyComponent rowContainer = new EmptyComponent(0, 0, rowWidth, 0);

        List<T> selected = selectedEntries();
        List<T> available = availableEntries();

        int currentY = 0;

        if (!selected.isEmpty()) {
            currentY = addHeader(rowContainer, currentY, rowWidth, selectedHeader, priorityHint, false);
            for (int i = 0; i < selected.size(); i++) {
                T option = selected.get(i);
                int rowHeight = rowHeight(option, textWidth);
                rowContainer.addWidget(new PackRow<>(
                        0, currentY, rowWidth, rowHeight, textX, textWidth,
                        option, descriptor, true, i + 1, i > 0, i < selected.size() - 1,
                        this::onRowToggle, this::onRowMoveUp, this::onRowMoveDown,
                        scrollbarSettings()));
                currentY += rowHeight + ROW_GAP;
            }
        }

        if (!available.isEmpty()) {
            currentY = addHeader(rowContainer, currentY, rowWidth, availableHeader, null, !selected.isEmpty());
            for (T option : available) {
                int rowHeight = rowHeight(option, textWidth);
                rowContainer.addWidget(new PackRow<>(
                        0, currentY, rowWidth, rowHeight, textX, textWidth,
                        option, descriptor, false, 0, false, false,
                        this::onRowToggle, this::onRowMoveUp, this::onRowMoveDown,
                        scrollbarSettings()));
                currentY += rowHeight + ROW_GAP;
            }
        }

        rowContainer.setHeight(currentY);
        scrollContainer.addComponent(rowContainer);
        scrollContainer.setScrollAmount(savedScroll);

        EmptyComponent scrollWrapper = new EmptyComponent(0, 0, listWidth, listHeight);
        scrollWrapper.addWidget(scrollContainer);
        this.addComponent(scrollWrapper);

        this.updateParentPosition(getParentX(), getParentY(), listWidth, listHeight);
    }

    private List<T> selectedEntries() {
        List<T> result = new ArrayList<>();
        for (String id : selectedOrder) {
            for (T option : options) {
                if (descriptor.id(option).equals(id)) {
                    result.add(option);
                    break;
                }
            }
        }
        return result;
    }

    private List<T> availableEntries() {
        List<T> result = new ArrayList<>();
        for (T option : options) {
            if (!selectedOrder.contains(descriptor.id(option))) {
                result.add(option);
            }
        }
        return result;
    }

    private int rowHeight(T option, int textWidth) {
        int lineHeight = Minecraft.getInstance().font.lineHeight;
        int descHeight = 0;
        Component desc = descriptor.description(option);
        if (desc != null) {
            descHeight = new MultiLineTextComponent(0, 0, textWidth, desc, GuiColors.DESCRIPTION).getHeight();
        }
        return ROW_PADDING_Y + lineHeight + (descHeight > 0 ? 2 + descHeight : 0) + ROW_PADDING_Y;
    }

    private int addHeader(EmptyComponent container, int currentY, int rowWidth,
                          Component title, Component hint, boolean gapAbove) {
        int y = currentY + (gapAbove ? HEADER_GAP_ABOVE : 0);
        HeaderRow header = new HeaderRow(0, y, rowWidth, title, hint);
        container.addComponent(header);
        return y + header.getHeight() + ROW_GAP;
    }

    private AbstractScrollArea.ScrollbarSettings scrollbarSettings() {
        return new AbstractScrollArea.ScrollbarSettings(
                Identifier.fromNamespaceAndPath("minecraft", "widget/scroller"),
                null,
                Identifier.fromNamespaceAndPath("minecraft", "widget/scroller_background"),
                0, 0, 0, false);
    }

    private void onRowToggle(T option) {
        String id = descriptor.id(option);
        if (selectedOrder.contains(id)) {
            selectedOrder.remove(id);
            onDeselect.accept(option);
        } else {
            selectedOrder.add(id);   // append = bottom of selected = lowest priority
            onSelect.accept(option);
        }
        buildList();
    }

    private void onRowMoveUp(T option) {
        String id = descriptor.id(option);
        int index = selectedOrder.indexOf(id);
        if (index <= 0) return;
        selectedOrder.remove(index);
        selectedOrder.add(index - 1, id);
        onMoveUp.accept(option);
        buildList();
    }

    private void onRowMoveDown(T option) {
        String id = descriptor.id(option);
        int index = selectedOrder.indexOf(id);
        if (index < 0 || index >= selectedOrder.size() - 1) return;
        selectedOrder.remove(index);
        selectedOrder.add(index + 1, id);
        onMoveDown.accept(option);
        buildList();
    }

    /**
     * Enables or disables the internal scroll container. Call with {@code false} when a modal overlay
     * is shown over this list, and {@code true} again when the overlay is dismissed.
     */
    public void setScrollActive(boolean active) {
        if (scrollContainer != null) {
            scrollContainer.active = active;
        }
    }

    // ── Header row ──────────────────────────────────────────────────────────────

    private static class HeaderRow extends EmptyComponent {

        private final Component title;
        private final Component hint;
        private final int computedHeight;

        HeaderRow(int x, int y, int width, Component title, Component hint) {
            super(x, y, width, 0);
            this.title = title;
            this.hint = hint;
            int lineHeight = Minecraft.getInstance().font.lineHeight;
            this.computedHeight = lineHeight + (hint != null ? 2 + lineHeight : 0);
            setHeight(computedHeight);
        }

        @Override
        public int getHeight() {
            return computedHeight;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                       float partialTick, int parentWidth, int parentHeight) {
            var font = Minecraft.getInstance().font;
            int x = getTotalX();
            int y = getTotalY();
            graphics.text(font, title, x + ROW_PADDING_X, y, GuiColors.TEXT_SECONDARY, false);
            if (hint != null) {
                graphics.text(font, hint, x + ROW_PADDING_X, y + font.lineHeight + 2, GuiColors.TEXT_HINT, false);
            }
        }
    }

    // ── Selectable / reorderable row ────────────────────────────────────────────

    private static class PackRow<T> extends AbstractContainerWidget implements IWidget {

        private final T option;
        private final boolean isSelected;
        private final boolean canMoveUp;
        private final boolean canMoveDown;
        private final Consumer<T> onToggle;
        private final Consumer<T> onMoveUp;
        private final Consumer<T> onMoveDown;
        private final List<IComponent> childComponents = new ArrayList<>();

        PackRow(int x, int y, int width, int height,
                int textX, int textWidth,
                T option, RowDescriptor<T> descriptor,
                boolean isSelected, int position, boolean canMoveUp, boolean canMoveDown,
                Consumer<T> onToggle, Consumer<T> onMoveUp, Consumer<T> onMoveDown,
                AbstractScrollArea.ScrollbarSettings scrollbarSettings) {
            super(x, y, width, height, Component.empty(), scrollbarSettings);
            this.option = option;
            this.isSelected = isSelected;
            this.canMoveUp = canMoveUp;
            this.canMoveDown = canMoveDown;
            this.onToggle = onToggle;
            this.onMoveUp = onMoveUp;
            this.onMoveDown = onMoveDown;

            int lineHeight = Minecraft.getInstance().font.lineHeight;

            childComponents.add(new ColorComponent(
                    0, 0, width, height,
                    isSelected ? GuiColors.ROW_SELECTED : GuiColors.ROW_BACKGROUND));

            if (isSelected) {
                childComponents.add(new TextComponent(
                        ROW_PADDING_X, ROW_PADDING_Y,
                        Component.literal(position + "."), GuiColors.ACCENT));
            }

            TextComponent nameText = new TextComponent(
                    textX, ROW_PADDING_Y,
                    descriptor.name(option),
                    isSelected ? GuiColors.NAME_SELECTED : GuiColors.NAME_DEFAULT);
            nameText.setDrawShadow(true);
            childComponents.add(nameText);

            Component desc = descriptor.description(option);
            if (desc != null) {
                childComponents.add(new MultiLineTextComponent(
                        textX, ROW_PADDING_Y + lineHeight + 2,
                        textWidth, desc, GuiColors.DESCRIPTION));
            }
        }

        private int checkboxX(int rowLeft, int rowWidth) {
            return rowLeft + rowWidth - CHECKMARK_SIZE - CHECKMARK_RIGHT_MARGIN;
        }

        private int downArrowX(int rowLeft, int rowWidth) {
            return checkboxX(rowLeft, rowWidth) - ARROW_GAP - ARROW_SIZE;
        }

        private int upArrowX(int rowLeft, int rowWidth) {
            return downArrowX(rowLeft, rowWidth) - ARROW_GAP - ARROW_SIZE;
        }

        @Override
        protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int rowLeft = getX();
            int rowTop = getY();
            int rowWidth = getWidth();
            int rowHeight = getHeight();

            int borderColor = isSelected ? GuiColors.BORDER_SELECTED
                    : isHovered() ? GuiColors.BORDER_HOVERED
                      : GuiColors.BORDER_IDLE;
            GuiHelper.drawBorder(graphics, rowLeft, rowTop, rowWidth, rowHeight, borderColor);

            for (IComponent component : childComponents) {
                component.updateParentPosition(rowLeft, rowTop, rowWidth, rowHeight);
                component.extractRenderState(graphics, mouseX, mouseY, partialTick, rowWidth, rowHeight);
            }

            if (isSelected) {
                int arrowY = rowTop + (rowHeight - ARROW_SIZE) / 2;
                drawArrow(graphics, "▲", upArrowX(rowLeft, rowWidth), arrowY, canMoveUp, mouseX, mouseY);
                drawArrow(graphics, "▼", downArrowX(rowLeft, rowWidth), arrowY, canMoveDown, mouseX, mouseY);
            }

            int checkboxY = rowTop + (rowHeight - CHECKMARK_SIZE) / 2;
            drawCheckbox(graphics, checkboxX(rowLeft, rowWidth), checkboxY, isSelected);
        }

        private static void drawArrow(GuiGraphicsExtractor graphics, String glyph, int x, int y,
                                      boolean enabled, int mouseX, int mouseY) {
            int color = !enabled ? GuiColors.BORDER_IDLE
                    : within(mouseX, mouseY, x, y, ARROW_SIZE) ? GuiColors.BORDER_HOVERED
                      : GuiColors.NAME_DEFAULT;
            var font = Minecraft.getInstance().font;
            graphics.centeredText(font, glyph, x + ARROW_SIZE / 2, y + (ARROW_SIZE - font.lineHeight) / 2, color);
        }

        private static void drawCheckbox(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
            GuiHelper.drawBorder(graphics, x, y, CHECKMARK_SIZE, CHECKMARK_SIZE, GuiColors.BORDER_IDLE);
            if (checked) {
                graphics.fill(x + 1, y + 1, x + CHECKMARK_SIZE - 1, y + CHECKMARK_SIZE - 1, GuiColors.CHECKMARK_BOX);
                var font = Minecraft.getInstance().font;
                graphics.centeredText(font, "✓", x + CHECKMARK_SIZE / 2,
                        y + (CHECKMARK_SIZE - font.lineHeight) / 2, GuiColors.CHECKMARK_TICK);
            }
        }

        private static boolean within(double px, double py, int x, int y, int size) {
            return px >= x && px < x + size && py >= y && py < y + size;
        }

        @Override
        public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean bl) {
            if (event.button() != 0 || !isMouseOver(event.x(), event.y())) {
                return false;
            }

            if (isSelected) {
                int rowLeft = getX();
                int rowWidth = getWidth();
                int arrowY = getY() + (getHeight() - ARROW_SIZE) / 2;
                if (within(event.x(), event.y(), upArrowX(rowLeft, rowWidth), arrowY, ARROW_SIZE)) {
                    if (canMoveUp) onMoveUp.accept(option);
                    return true;   // consume even when disabled so it never falls through to a toggle
                }
                if (within(event.x(), event.y(), downArrowX(rowLeft, rowWidth), arrowY, ARROW_SIZE)) {
                    if (canMoveDown) onMoveDown.accept(option);
                    return true;
                }
            }

            onToggle.accept(option);
            return true;
        }

        @Override protected int contentHeight() { return 0; }
        @Override protected double scrollRate() { return 0; }
        @Override protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationOutput) { }

        @Override
        public @NotNull ScreenRectangle getBorderForArrowNavigation(@NotNull ScreenDirection direction) {
            return getRectangle();
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() { return List.of(); }

        @Override
        public @NotNull Collection<? extends NarratableEntry> getNarratables() { return List.of(); }
    }
}
