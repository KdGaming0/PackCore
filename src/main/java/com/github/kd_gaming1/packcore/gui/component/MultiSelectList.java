package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.api.component.IComponent;
import com.daqem.uilib.api.widget.IWidget;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.color.ColorComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
//? if >=26.1 {
import net.minecraft.client.gui.components.AbstractScrollArea;
//?} else {

 //?}
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A reusable scrollable vertical list that supports selecting multiple rows simultaneously.
 * Each row shows a name and an optional description.
 *
 * <p>Clicking a selected row deselects it; clicking an unselected row selects it.
 * The scroll position is preserved across rebuilds.
 *
 * @param <T> The option type (record, class, enum, etc.)
 */
public class MultiSelectList<T> extends EmptyComponent {

    private static final int ROW_GAP             = 4;
    private static final int ROW_PADDING_X        = 10;
    private static final int ROW_PADDING_Y        = 8;
    private static final int SCROLL_BAR_WIDTH     = 8;
    private static final int INDICATOR_WIDTH      = 3;
    private static final int INDICATOR_GAP        = 8;
    private static final int CHECKMARK_RIGHT_MARGIN = 8;
    private static final int CHECKMARK_SIZE       = 9;

    private final List<T> options;
    private final RowDescriptor<T> descriptor;
    private final Consumer<T> onSelect;
    private final Consumer<T> onDeselect;
    private final Set<String> selectedIds;
    private ScrollContainerWidget scrollContainer;

    public MultiSelectList(
            int x, int y,
            int width, int height,
            List<T> options,
            RowDescriptor<T> descriptor,
            Set<String> selectedIds,
            Consumer<T> onSelect,
            Consumer<T> onDeselect
    ) {
        super(x, y, width, height);
        this.options = options;
        this.descriptor = descriptor;
        this.selectedIds = new HashSet<>(selectedIds);
        this.onSelect = onSelect;
        this.onDeselect = onDeselect;

        buildList();
    }

    private void buildList() {
        double savedScroll = scrollContainer != null ? scrollContainer.scrollAmount() : 0;
        this.clearComponents();

        int listWidth = getWidth();
        int listHeight = getHeight();
        int rowWidth = listWidth - SCROLL_BAR_WIDTH;
        int textWidth = rowWidth - ROW_PADDING_X * 2 - INDICATOR_WIDTH - INDICATOR_GAP - CHECKMARK_SIZE - CHECKMARK_RIGHT_MARGIN;
        int lineHeight = Minecraft.getInstance().font.lineHeight;

        scrollContainer = new ScrollContainerWidget(listWidth, listHeight);
        EmptyComponent rowContainer = new EmptyComponent(0, 0, rowWidth, 0);

        int currentY = 0;

        for (T option : options) {
            boolean isSelected = selectedIds.contains(descriptor.id(option));

            int descHeight = 0;
            Component desc = descriptor.description(option);
            if (desc != null) {
                MultiLineTextComponent probe = new MultiLineTextComponent(0, 0, textWidth, desc, GuiColors.DESCRIPTION);
                descHeight = probe.getHeight();
            }

            int rowHeight = ROW_PADDING_Y + lineHeight + (descHeight > 0 ? 2 + descHeight : 0) + ROW_PADDING_Y;

            //? if >=26.1 {
            SelectRow<T> row = new SelectRow<>(
                    0, currentY,
                    rowWidth, rowHeight,
                    option,
                    descriptor,
                    isSelected,
                    clicked -> {
                        String clickedId = descriptor.id(clicked);
                        if (selectedIds.contains(clickedId)) {
                            selectedIds.remove(clickedId);
                            onDeselect.accept(clicked);
                        } else {
                            selectedIds.add(clickedId);
                            onSelect.accept(clicked);
                        }
                        buildList();
                    },
                    new AbstractScrollArea.ScrollbarSettings(
                            Identifier.fromNamespaceAndPath("minecraft", "widget/scroller"),
                            null,
                            Identifier.fromNamespaceAndPath("minecraft", "widget/scroller_background"),
                            0, 0, 0, false
                    )
            );
            //?} else {
            /*SelectRow<T> row = new SelectRow<>(
                    0, currentY,
                    rowWidth, rowHeight,
                    option,
                    descriptor,
                    isSelected,
                    clicked -> {
                        String clickedId = descriptor.id(clicked);
                        if (selectedIds.contains(clickedId)) {
                            selectedIds.remove(clickedId);
                            onDeselect.accept(clicked);
                        } else {
                            selectedIds.add(clickedId);
                            onSelect.accept(clicked);
                        }
                        buildList();
                    }
            );
            *///?}
            rowContainer.addWidget(row);
            currentY += rowHeight + ROW_GAP;
        }

        rowContainer.setHeight(currentY);
        scrollContainer.addComponent(rowContainer);
        scrollContainer.setScrollAmount(savedScroll);

        EmptyComponent scrollWrapper = new EmptyComponent(0, 0, listWidth, listHeight);
        scrollWrapper.addWidget(scrollContainer);
        this.addComponent(scrollWrapper);

        this.updateParentPosition(getParentX(), getParentY(), listWidth, listHeight);
    }

    /**
     * Enables or disables the internal scroll container.
     * Call with {@code false} when a modal overlay is shown over this list,
     * and {@code true} again when the overlay is dismissed.
     */
    public void setScrollActive(boolean active) {
        if (scrollContainer != null) {
            scrollContainer.active = active;
        }
    }

    /**
     * Tells the list how to read each field from your option type.
     * Build one with {@link #of} using method references.
     * Description may return null for name-only rows.
     */
    public interface RowDescriptor<T> {
        String id(T option);
        Component name(T option);
        Component description(T option);

        static <T> RowDescriptor<T> of(
                Function<T, String> id,
                Function<T, Component> name,
                Function<T, Component> description
        ) {
            return new RowDescriptor<>() {
                @Override public String id(T o) { return id.apply(o); }
                @Override public Component name(T o) { return name.apply(o); }
                @Override public Component description(T o) { return description.apply(o); }
            };
        }
    }

    private static class SelectRow<T> extends AbstractContainerWidget implements IWidget {

        private final T option;
        private final boolean isSelected;
        private final Consumer<T> onClick;
        private final List<IComponent> childComponents = new ArrayList<>();

        //? if >=26.1 {
        SelectRow(
                int x, int y,
                int width, int height,
                T option,
                RowDescriptor<T> descriptor,
                boolean isSelected,
                Consumer<T> onClick,
                AbstractScrollArea.ScrollbarSettings scrollbarSettings
        ) {
            super(x, y, width, height, Component.empty(), scrollbarSettings);
            //?} else {
        /*SelectRow(
                int x, int y,
                int width, int height,
                T option,
                RowDescriptor<T> descriptor,
                boolean isSelected,
                Consumer<T> onClick
        ) {
            super(x, y, width, height, Component.empty());
        *///?}
            this.option = option;
            this.isSelected = isSelected;
            this.onClick = onClick;

            int lineHeight = Minecraft.getInstance().font.lineHeight;
            int textX = ROW_PADDING_X + INDICATOR_WIDTH + INDICATOR_GAP;
            int textWidth = width - textX - ROW_PADDING_X - CHECKMARK_SIZE - CHECKMARK_RIGHT_MARGIN;

            childComponents.add(new ColorComponent(
                    0, 0, width, height,
                    isSelected ? GuiColors.ROW_SELECTED : GuiColors.ROW_BACKGROUND
            ));

            if (isSelected) {
                childComponents.add(new ColorComponent(0, 0, INDICATOR_WIDTH, height, GuiColors.INDICATOR_SELECTED));
            }

            TextComponent nameText = new TextComponent(
                    textX, ROW_PADDING_Y,
                    descriptor.name(option),
                    isSelected ? GuiColors.NAME_SELECTED : GuiColors.NAME_DEFAULT
            );
            nameText.setDrawShadow(true);
            childComponents.add(nameText);

            Component desc = descriptor.description(option);
            if (desc != null) {
                childComponents.add(new MultiLineTextComponent(
                        textX, ROW_PADDING_Y + lineHeight + 2,
                        textWidth, desc, GuiColors.DESCRIPTION
                ));
            }
        }

        //? if >=26.1 {
        @Override
        protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            //?} else {
          /*@Override
            protected void renderWidget(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        *///?}
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
                //? if >=26.1 {
                component.extractRenderState(graphics, mouseX, mouseY, partialTick, rowWidth, rowHeight);
                //?} else {
                /*component.extractRenderState(graphics, mouseX, mouseY, partialTick, rowWidth, rowHeight);
                 *///?}
            }

            int checkboxX = rowLeft + rowWidth - CHECKMARK_SIZE - CHECKMARK_RIGHT_MARGIN;
            int checkboxY = rowTop + (rowHeight - CHECKMARK_SIZE) / 2;
            drawCheckbox(graphics, checkboxX, checkboxY, isSelected);
        }

        @Override
        public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean bl) {
            if (event.button() == 0 && isMouseOver(event.x(), event.y())) {
                onClick.accept(option);
                return true;
            }
            return false;
        }

        private static void drawCheckbox(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
            GuiHelper.drawBorder(graphics, x, y, CHECKMARK_SIZE, CHECKMARK_SIZE, GuiColors.BORDER_IDLE);

            if (checked) {
                graphics.fill(x + 1, y + 1, x + CHECKMARK_SIZE - 1, y + CHECKMARK_SIZE - 1, GuiColors.CHECKMARK_BOX);
                graphics.centeredText(
                        Minecraft.getInstance().font,
                        "✓",
                        x + CHECKMARK_SIZE / 2,
                        y + (CHECKMARK_SIZE - Minecraft.getInstance().font.lineHeight) / 2,
                        GuiColors.CHECKMARK_TICK
                );
            }
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
        public @NotNull Collection<? extends net.minecraft.client.gui.narration.NarratableEntry> getNarratables() {
            return List.of();
        }
    }
}