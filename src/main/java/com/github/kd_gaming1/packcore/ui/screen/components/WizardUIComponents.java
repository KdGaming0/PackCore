package com.github.kd_gaming1.packcore.ui.screen.components;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.lavendermd.CustomLavenderCompiler;
import com.github.kd_gaming1.packcore.modpack.ModpackInfo;
import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.lavendermd.feature.*;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Reusable UI components for wizard pages.
 * Provides factory methods for common UI patterns.
 */
public class WizardUIComponents {

    // Shared markdown processor
    private static final MarkdownProcessor<ParentComponent> MARKDOWN_PROCESSOR =
            new MarkdownProcessor<>(
                    CustomLavenderCompiler::new,
                    new BasicFormattingFeature(),
                    new ColorFeature(),
                    new LinkFeature(),
                    new ListFeature(),
                    new BlockQuoteFeature(),
                    new ImageFeature()
            );

    private static final Map<String, ParentComponent> MARKDOWN_CACHE = new ConcurrentHashMap<>();
    private static final ModpackInfo MODPACK_INFO = PackCore.getModpackInfo();

    /**
     * Create a standard wizard page header with title and subtitle
     */
    public static FlowLayout createHeader(String titlePrefix, String subtitle) {
        return createHeader(titlePrefix, subtitle, true);
    }

    /**
     * Create a wizard page header with optional modpack name highlight
     */
    public static FlowLayout createHeader(String titlePrefix, String subtitle, boolean includeModpackName) {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6);

        Text titleText;
        if (includeModpackName) {
            titleText = TextOps.concat(
                    TextOps.withColor(titlePrefix + " ", TEXT_PRIMARY),
                    Text.literal(MODPACK_INFO.getName())
                            .setStyle(Style.EMPTY.withColor(ACCENT_SECONDARY).withBold(true))
            );
        } else {
            titleText = TextOps.withColor(titlePrefix, TEXT_PRIMARY);
        }

        header.child(Components.label(titleText).shadow(true));

        if (subtitle != null && !subtitle.isEmpty()) {
            LabelComponent subtitleLabel = (LabelComponent) Components.label(
                            Text.literal(subtitle)
                                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true))
                    ).color(Color.ofRgb(TEXT_SECONDARY))
                    .margins(Insets.of(2, 0, 2, 0))
                    .sizing(Sizing.expand(), Sizing.content());

            header.child(subtitleLabel);
        }

        return header;
    }

    /**
     * Create a selection card for options (used in multiple wizard pages)
     */
    public static FlowLayout createSelectionCard(boolean isSelected, Consumer<FlowLayout> onClick) {
        int borderColor = isSelected ? SUCCESS_BORDER : 0x40_FFFFFF;

        FlowLayout card = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .surface(Surface.outline(borderColor))
                .padding(Insets.of(12))
                .cursorStyle(CursorStyle.HAND);

        if (onClick != null) {
            card.mouseDown().subscribe((x, y, button) -> {
                onClick.accept(card);
                return true;
            });
        }

        return card;
    }

    /**
     * Create a scrollable markdown section
     */
    public static ScrollContainer<FlowLayout> createMarkdownScroll(String content) {
        FlowLayout wrapper = Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(4);

        var markdownComponent = MARKDOWN_CACHE.computeIfAbsent(
                content,
                MARKDOWN_PROCESSOR::process
        ).horizontalSizing(Sizing.fill(98));

        wrapper.child(markdownComponent);

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                wrapper
        );

        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollbarThiccness(6);
        scrollContainer.surface(Surface.flat(0x40_000000).and(Surface.outline(0x30_FFFFFF)));
        scrollContainer.padding(Insets.of(8));

        return scrollContainer;
    }

    /**
     * Create a status label with icon and color
     */
    public static LabelComponent createStatusLabel(String text, String icon, int color) {
        String displayText = icon != null ? icon + " " + text : text;
        return (LabelComponent) Components.label(
                        Text.literal(displayText).setStyle(Style.EMPTY.withBold(true))
                ).color(Color.ofRgb(color))
                .horizontalSizing(Sizing.fill(100));
    }

    /**
     * Create an info card with title and description
     */
    public static FlowLayout createInfoCard(String title, String description, int bgColor, int borderColor) {
        FlowLayout card = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .surface(Surface.flat(bgColor).and(Surface.outline(borderColor)))
                .padding(Insets.of(8))
                .margins(Insets.of(6,0,6,6));

        if (title != null) {
            card.child(Components.label(
                    Text.literal(title).setStyle(Style.EMPTY.withBold(true))
            ).color(Color.ofRgb(borderColor)));
        }

        if (description != null) {
            LabelComponent desc = (LabelComponent) Components.label(Text.literal(description))
                    .color(Color.ofRgb(TEXT_SECONDARY))
                    .horizontalSizing(Sizing.fill(95));
            card.child(desc);
        }

        return card;
    }

    /**
     * Create an option box with icon, title, and description
     */
    public static FlowLayout createOptionBox(String icon, String title, String description,
                                             boolean isSelected, Consumer<FlowLayout> onClick) {
        FlowLayout card = createSelectionCard(isSelected, onClick);

        // Header with icon and title
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        if (icon != null) {
            header.child(Components.label(Text.literal(icon))
                    .color(Color.ofRgb(ACCENT_SECONDARY)));
        }

        header.child(Components.label(
                        Text.literal(title).setStyle(Style.EMPTY.withBold(true))
                ).color(Color.ofRgb(TEXT_PRIMARY))
                .horizontalSizing(Sizing.expand()));

        card.child(header);

        if (description != null) {
            LabelComponent desc = (LabelComponent) Components.label(Text.literal(description))
                    .color(Color.ofRgb(TEXT_SECONDARY))
                    .horizontalSizing(Sizing.fill(100));
            card.child(desc);
        }

        return card;
    }

    /**
     * Create a progress step label with status icon
     */
    public static LabelComponent createProgressStepLabel(String stepName, ProgressStatus status) {
        String icon = switch (status) {
            case SUCCESS -> "✅";
            case ERROR -> "❌";
            case RUNNING -> "⏳";
            case PENDING -> "⏸";
        };

        Formatting color = switch (status) {
            case SUCCESS -> Formatting.GREEN;
            case ERROR -> Formatting.RED;
            case RUNNING -> Formatting.YELLOW;
            case PENDING -> Formatting.GRAY;
        };

        return (LabelComponent) Components.label(
                Text.literal(icon + " " + stepName)
                        .setStyle(Style.EMPTY.withColor(color))
        ).margins(Insets.left(8));
    }

    /**
     * Create a summary item for configuration display
     */
    public static FlowLayout createSummaryItem(String label, String value) {
        FlowLayout item = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .margins(Insets.vertical(2));

        item.child(Components.label(Text.literal(label))
                .color(Color.ofRgb(TEXT_PRIMARY)));

        if (value != null) {
            LabelComponent valueLabel = (LabelComponent) Components.label(Text.literal(value))
                    .color(Color.ofRgb(TEXT_SECONDARY))
                    .horizontalSizing(Sizing.expand());

            item.child(valueLabel);
        }

        return item;
    }

    /**
     * Create an image selection box with click handling
     */
    public static FlowLayout createImageOption(String title, String texturePath, int textureWidth,
                                               int textureHeight, boolean isSelected, Runnable onClick) {
        FlowLayout wrapper = (FlowLayout) Containers.verticalFlow(Sizing.fill(32), Sizing.expand())
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.of(8))
                .cursorStyle(CursorStyle.HAND);

        // Title
        wrapper.child(Components.label(
                TextOps.withColor(title, TEXT_PRIMARY).setStyle(Style.EMPTY.withBold(true))
        ).margins(Insets.of(4, 4, 2, 4)));

        // Image container
        Identifier textureId = Identifier.of(MOD_ID, texturePath);
        Surface imageSurface = isSelected
                ? Surface.outline(SUCCESS_BORDER).and(Surface.tiled(textureId, textureWidth, textureHeight))
                : Surface.tiled(textureId, textureWidth, textureHeight);

        FlowLayout imageContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand())
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .surface(imageSurface)
                .margins(Insets.of(4))
                .cursorStyle(CursorStyle.HAND);

        if (onClick != null) {
            imageContainer.mouseDown().subscribe((x, y, button) -> {
                onClick.run();
                return true;
            });
        }

        wrapper.child(imageContainer);
        return wrapper;
    }

    /**
     * Progress status enum for step indicators
     */
    public enum ProgressStatus {
        SUCCESS,
        ERROR,
        RUNNING,
        PENDING
    }
}