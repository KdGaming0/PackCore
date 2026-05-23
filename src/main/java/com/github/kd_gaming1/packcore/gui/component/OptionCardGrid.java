package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.api.component.IComponent;
import com.daqem.uilib.api.widget.IWidget;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.color.ColorComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
//? if >=26.1 {
import net.minecraft.client.gui.components.AbstractScrollArea;
//?} else {

//?}
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class OptionCardGrid<T> extends EmptyComponent {

    private static final int SCROLL_BAR_WIDTH  = 8;
    private static final int PREVIEW_ASPECT_W  = 16;
    private static final int PREVIEW_ASPECT_H  = 9;
    private static final int BORDER_THICKNESS  = 1;
    private static final int LABEL_PADDING     = 6;
    private static final int BADGE_SIZE        = 10;

    private final int columns;
    private final int cardGap;
    private final List<T> options;
    private final CardDescriptor<T> descriptor;
    private final Consumer<T> onSelect;
    private String selectedId;

    public OptionCardGrid(int x, int y, int width, int height, int columns, int cardGap, List<T> options, CardDescriptor<T> descriptor, String selectedId, Consumer<T> onSelect) {
        super(x, y, width, height);
        this.columns = columns;
        this.cardGap = cardGap;
        this.options = options;
        this.descriptor = descriptor;
        this.selectedId = selectedId;
        this.onSelect = onSelect;

        buildGrid();
    }

    private void buildGrid() {
        this.clearComponents();

        int gridWidth = getWidth();
        int gridHeight = getHeight();

        int cardWidth = (gridWidth - SCROLL_BAR_WIDTH - (cardGap * (columns - 1))) / columns;
        int texW = descriptor.previewTextureWidth(options.getFirst());
        int texH = descriptor.previewTextureHeight(options.getFirst());
        int previewHeight = cardWidth * texH / texW;
        int textWidth = cardWidth - (LABEL_PADDING * 2);
        int lineHeight = Minecraft.getInstance().font.lineHeight;

        int maxDescriptionHeight = calculateMaxDescriptionHeight(textWidth);
        int cardLabelHeight = LABEL_PADDING + lineHeight + 2 + maxDescriptionHeight + LABEL_PADDING;
        int cardHeight = previewHeight + cardLabelHeight;

        EmptyComponent cardGrid = new EmptyComponent(0, 0, gridWidth - SCROLL_BAR_WIDTH, 0);

        for (int i = 0; i < options.size(); i++) {
            T option = options.get(i);
            int col = i % columns;
            int row = i / columns;

            int cardX = col * (cardWidth + cardGap);
            int cardY = row * (cardHeight + cardGap);
            boolean isSelected = descriptor.id(option).equals(selectedId);

            //? if >=26.1 {
            OptionCard<T> card = new OptionCard<>(
                    cardX, cardY, cardWidth, cardHeight, previewHeight,
                    option, descriptor, isSelected,
                    clicked -> {
                        String clickedId = descriptor.id(clicked);
                        if (clickedId.equals(this.selectedId)) {
                            this.selectedId = null;
                            this.onSelect.accept(null);
                        } else {
                            this.selectedId = clickedId;
                            this.onSelect.accept(clicked);
                        }
                        this.buildGrid();
                    },
                    new AbstractScrollArea.ScrollbarSettings(
                            Identifier.fromNamespaceAndPath("minecraft", "widget/scroller"),
                            null,
                            Identifier.fromNamespaceAndPath("minecraft", "widget/scroller_background"),
                            0, 0, 0, false
                    )
            );
            //?} else {
            /*OptionCard<T> card = new OptionCard<>(
                    cardX, cardY, cardWidth, cardHeight, previewHeight,
                    option, descriptor, isSelected,
                    clicked -> {
                        String clickedId = descriptor.id(clicked);
                        if (clickedId.equals(this.selectedId)) {
                            this.selectedId = null;
                            this.onSelect.accept(null);
                        } else {
                            this.selectedId = clickedId;
                            this.onSelect.accept(clicked);
                        }
                        this.buildGrid();
                    }
            );
            *///?}
            cardGrid.addWidget(card);
        }

        int totalRows = (int) Math.ceil((double) options.size() / columns);
        cardGrid.setHeight(totalRows * (cardHeight + cardGap));

        this.addComponent(GuiHelper.scrollWrapped(0, 0, gridWidth, gridHeight,
                scroll -> scroll.addComponent(cardGrid)));

        this.updateParentPosition(getParentX(), getParentY(), gridWidth, gridHeight);
    }

    private int calculateMaxDescriptionHeight(int textWidth) {
        int maxHeight = 0;
        for (T option : options) {
            MultiLineTextComponent probe = new MultiLineTextComponent(
                    0, 0, textWidth, descriptor.description(option), GuiColors.DESCRIPTION
            );
            maxHeight = Math.max(maxHeight, probe.getHeight());
        }
        return maxHeight;
    }

    public interface CardDescriptor<T> {
        String id(T option);
        Component name(T option);
        Component description(T option);
        Identifier previewTexture(T option);
        int previewTextureWidth(T option);
        int previewTextureHeight(T option);

        static <T> CardDescriptor<T> of(
                Function<T, String> id,
                Function<T, Component> name,
                Function<T, Component> description,
                Function<T, Identifier> previewTexture,
                ToIntFunction<T> previewTexWidth,
                ToIntFunction<T> previewTexHeight
        ) {
            return new CardDescriptor<>() {
                @Override public String id(T o) { return id.apply(o); }
                @Override public Component name(T o) { return name.apply(o); }
                @Override public Component description(T o) { return description.apply(o); }
                @Override public Identifier previewTexture(T o) { return previewTexture.apply(o); }
                @Override public int previewTextureWidth(T o) { return previewTexWidth.applyAsInt(o); }
                @Override public int previewTextureHeight(T o) { return previewTexHeight.applyAsInt(o); }
            };
        }
    }

    private static class OptionCard<T> extends AbstractContainerWidget implements IWidget {
        private final T option;
        private final CardDescriptor<T> descriptor;
        private final boolean isSelected;
        private final int previewHeight;
        private final Consumer<T> onClick;
        private final List<IComponent> childComponents = new ArrayList<>();

        //? if >=26.1 {
        OptionCard(int x, int y, int width, int height, int previewHeight, T option, CardDescriptor<T> descriptor, boolean isSelected, Consumer<T> onClick, AbstractScrollArea.ScrollbarSettings scrollbarSettings) {
            super(x, y, width, height, Component.empty(), scrollbarSettings);
        //?} else {
        /*OptionCard(int x, int y, int width, int height, int previewHeight, T option, CardDescriptor<T> descriptor, boolean isSelected, Consumer<T> onClick) {
            super(x, y, width, height, Component.empty());
         *///?}
            this.previewHeight = previewHeight;
            this.option = option;
            this.descriptor = descriptor;
            this.isSelected = isSelected;
            this.onClick = onClick;

            setupChildComponents(width, height);
        }

        private void setupChildComponents(int width, int height) {
            int labelAreaY = BORDER_THICKNESS + previewHeight;
            int labelAreaHeight = height - labelAreaY - BORDER_THICKNESS;
            childComponents.add(new ColorComponent(
                    BORDER_THICKNESS,
                    labelAreaY,
                    width - BORDER_THICKNESS * 2,
                    labelAreaHeight,
                    GuiColors.ROW_BACKGROUND
            ));

            int textWidth = width - (LABEL_PADDING * 2);
            int labelsStartY = BORDER_THICKNESS + previewHeight + LABEL_PADDING;
            int lineHeight = Minecraft.getInstance().font.lineHeight;

            TextComponent nameText = new TextComponent(
                    LABEL_PADDING, labelsStartY,
                    descriptor.name(option),
                    isSelected ? GuiColors.NAME_SELECTED : GuiColors.NAME_DEFAULT
            );
            nameText.setDrawShadow(true);
            childComponents.add(nameText);

            childComponents.add(new MultiLineTextComponent(
                    LABEL_PADDING, labelsStartY + lineHeight + 2,
                    textWidth, descriptor.description(option), GuiColors.DESCRIPTION
            ));
        }


        //? if >=26.1 {
        @Override
        protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        //?} else {
        /*@Override
        protected void renderWidget(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
         *///?}
            int cardLeft = getX();
            int cardTop = getY();
            int cardWidth = getWidth();
            int cardHeight = getHeight();

            int borderColor = isSelected ? GuiColors.BORDER_SELECTED
                    : isHovered() ? GuiColors.BORDER_HOVERED
                    : GuiColors.BORDER_IDLE;
            drawBorder(graphics, cardLeft, cardTop, cardWidth, cardHeight, borderColor);

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    descriptor.previewTexture(option),
                    cardLeft + BORDER_THICKNESS, cardTop + BORDER_THICKNESS,
                    0f, 0f,
                    cardWidth - BORDER_THICKNESS * 2, previewHeight,
                    descriptor.previewTextureWidth(option), descriptor.previewTextureHeight(option),
                    descriptor.previewTextureWidth(option), descriptor.previewTextureHeight(option)
            );

            if (isSelected) {
                drawCheckmarkBadge(graphics, cardLeft + cardWidth - BADGE_SIZE - 4, cardTop + 4);
            }

            for (IComponent component : childComponents) {
                component.updateParentPosition(cardLeft, cardTop, cardWidth, cardHeight);
                //? if >=26.1 {
                component.extractRenderState(graphics, mouseX, mouseY, partialTick, cardWidth, cardHeight);
                //?} else {
                /*component.extractRenderState(graphics, mouseX, mouseY, partialTick, cardWidth, cardHeight);
                 *///?}
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

        private void drawCheckmarkBadge(GuiGraphicsExtractor graphics, int badgeX, int badgeY) {
            graphics.fill(badgeX, badgeY, badgeX + BADGE_SIZE, badgeY + BADGE_SIZE, GuiColors.BORDER_SELECTED);
            graphics.centeredText(
                    Minecraft.getInstance().font,
                    "✓",
                    badgeX + BADGE_SIZE / 2,
                    badgeY + 1,
                    GuiColors.CHECKMARK_TICK
            );
        }

        private static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
            GuiHelper.drawBorder(graphics, x, y, width, height, color);
        }

        @Override protected int contentHeight() { return 0; }

        @Override protected double scrollRate() { return 0; }

        @Override protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationOutput) { }

        @Override public @NotNull ScreenRectangle getBorderForArrowNavigation(@NotNull ScreenDirection direction) { return getRectangle(); }

        @Override public @NotNull List<? extends GuiEventListener> children() { return List.of(); }

        @Override public @NotNull Collection<? extends NarratableEntry> getNarratables() { return List.of(); }
    }
}