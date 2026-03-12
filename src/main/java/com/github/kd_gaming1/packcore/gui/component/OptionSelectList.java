package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.api.component.IComponent;
import com.daqem.uilib.api.widget.IWidget;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.color.ColorComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A reusable scrollable vertical list of selectable text rows.
 * Each row shows a name and an optional description.
 *
 * @param <T> The option type (record, class, enum, etc.)
 */
public class OptionSelectList<T> extends EmptyComponent {

    private static final int ROW_GAP         = 4;
    private static final int ROW_PADDING_X   = 10;
    private static final int ROW_PADDING_Y   = 8;
    private static final int SCROLL_BAR_WIDTH = 8;
    private static final int INDICATOR_WIDTH  = 3;
    private static final int INDICATOR_GAP    = 8;

    private final List<T> options;
    private final RowDescriptor<T> descriptor;
    private final Consumer<T> onSelect;
    private String selectedId;
    private ScrollContainerWidget scrollContainer;

    public OptionSelectList(int x, int y, int width, int height, List<T> options, RowDescriptor<T> descriptor, String selectedId, Consumer<T> onSelect) {
        super(x, y, width, height);
        this.options = options;
        this.descriptor = descriptor;
        this.selectedId = selectedId;
        this.onSelect = onSelect;

        buildList();
    }

    private void buildList() {
        double savedScroll = scrollContainer != null ? scrollContainer.scrollAmount() : 0;
        this.clearComponents();

        int listWidth = getWidth();
        int listHeight = getHeight();
        int rowWidth = listWidth - SCROLL_BAR_WIDTH;
        int textWidth = rowWidth - ROW_PADDING_X * 2 - INDICATOR_WIDTH - INDICATOR_GAP;
        int lineHeight = Minecraft.getInstance().font.lineHeight;

        scrollContainer = new ScrollContainerWidget(listWidth, listHeight);
        EmptyComponent rowContainer = new EmptyComponent(0, 0, rowWidth, 0);

        int currentY = 0;

        for (T option : options) {
            boolean isSelected = descriptor.id(option).equals(selectedId);

            int descHeight = 0;
            Component desc = descriptor.description(option);
            if (desc != null) {
                MultiLineTextComponent probe = new MultiLineTextComponent(0, 0, textWidth, desc, GuiColors.DESCRIPTION);
                descHeight = probe.getHeight();
            }

            int rowHeight = ROW_PADDING_Y + lineHeight + (descHeight > 0 ? 2 + descHeight : 0) + ROW_PADDING_Y;

            SelectRow<T> row = new SelectRow<>(
                    0, currentY,
                    rowWidth, rowHeight,
                    option,
                    descriptor,
                    isSelected,
                    clicked -> {
                        String clickedId = descriptor.id(clicked);
                        selectedId = clickedId.equals(selectedId) ? null : clickedId;
                        onSelect.accept(clicked);
                        buildList();
                    }
            );
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
     * Tells the list how to read each field from your option type.
     * Build one with {@link #of} using method references.
     * Description may return null if you don't need one.
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

        SelectRow(
                int x, int y,
                int width, int height,
                T option,
                RowDescriptor<T> descriptor,
                boolean isSelected,
                Consumer<T> onClick
        ) {
            super(x, y, width, height, Component.empty());
            this.option = option;
            this.isSelected = isSelected;
            this.onClick = onClick;

            int lineHeight = Minecraft.getInstance().font.lineHeight;
            int textX = ROW_PADDING_X + INDICATOR_WIDTH + INDICATOR_GAP;
            int textWidth = width - textX - ROW_PADDING_X;

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

        @Override
        protected void renderWidget(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int rowLeft = getX();
            int rowTop = getY();
            int rowWidth = getWidth();
            int rowHeight = getHeight();

            int borderColor = isSelected ? GuiColors.BORDER_SELECTED : isHovered() ? GuiColors.BORDER_HOVERED : GuiColors.BORDER_IDLE;
            drawBorder(graphics, rowLeft, rowTop, rowWidth, rowHeight, borderColor);

            for (IComponent component : childComponents) {
                component.updateParentPosition(rowLeft, rowTop, rowWidth, rowHeight);
                component.renderBase(graphics, mouseX, mouseY, partialTick, rowWidth, rowHeight);
            }
        }

        @Override
        public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean bl) {
            if (event.button() == 0 && isMouseOver(event.x(), event.y())) {
                onClick.accept(option);
                return true;
            }
            return false;
        }

        private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
            GuiHelper.drawBorder(graphics, x, y, width, height, color);
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