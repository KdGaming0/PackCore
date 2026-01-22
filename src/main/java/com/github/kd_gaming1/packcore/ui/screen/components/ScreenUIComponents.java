package com.github.kd_gaming1.packcore.ui.screen.components;

import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Reusable UI components for PackCore screens.
 * Provides factory methods for common UI patterns used across different screens.
 */
public class ScreenUIComponents {

    // ===== Panel Creation =====

    /**
     * Create a standard sidebar panel
     */
    public static FlowLayout createSidebar(int widthPercent) {
        return (FlowLayout) Containers.verticalFlow(Sizing.fill(widthPercent), Sizing.expand())
                .gap(8)
                .surface(TextureSurfaces.stretched(
                        Identifier.of(MOD_ID, "textures/gui/menu/notif_box.png"), 607, 755))
                .padding(Insets.of(12));
    }

    /**
     * Create a standard info panel
     */
    public static FlowLayout createInfoPanel(int widthPercent) {
        return (FlowLayout) Containers.verticalFlow(Sizing.fill(widthPercent), Sizing.expand())
                .gap(8)
                .surface(TextureSurfaces.stretched(
                        Identifier.of(MOD_ID, "textures/gui/menu/info_box.png"), 1142, 934))
                .padding(Insets.of(14));
    }

    /**
     * Create a section with title and content
     */
    public static FlowLayout createSection(String title, int expandPercent) {
        Sizing verticalSizing = expandPercent > 0 ? Sizing.expand(expandPercent) : Sizing.content();

        FlowLayout section = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), verticalSizing)
                .gap(4)
                .surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_SECONDARY)))
                .padding(Insets.of(8));

        if (title != null) {
            section.child(Components.label(Text.literal(title)
                            .setStyle(Style.EMPTY.withBold(true)))
                    .color(color(TEXT_PRIMARY)));
        }

        return section;
    }

    // ===== Info Display =====

    /**
     * Create an info row with label and value
     */
    public static FlowLayout createInfoRow(String label, String value) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8);

        row.child(Components.label(Text.literal(label))
                .color(color(TEXT_SECONDARY))
                .sizing(Sizing.fixed(80), Sizing.content()));

        row.child(Components.label(Text.literal(value))
                .color(color(TEXT_PRIMARY))
                .horizontalSizing(Sizing.expand()));

        return row;
    }

    /**
     * Create an info box with multiple rows
     */
    public static FlowLayout createInfoBox() {
        return (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ENTRY_BORDER)))
                .padding(Insets.of(8));
    }

    // ===== Status Cards =====

    /**
     * Create a status card with icon and message
     */
    public static FlowLayout createStatusCard(String icon, String title, String message,
                                              int bgColor, int borderColor) {
        FlowLayout card = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .surface(Surface.flat(bgColor).and(Surface.outline(borderColor)))
                .padding(Insets.of(12));

        // Header with icon
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .verticalAlignment(VerticalAlignment.CENTER);

        if (icon != null) {
            header.child(Components.label(Text.literal(icon))
                    .color(color(borderColor)));
        }

        header.child(Components.label(Text.literal(title).setStyle(Style.EMPTY.withBold(true)))
                .color(color(TEXT_PRIMARY)));

        card.child(header);

        // Message
        if (message != null) {
            LabelComponent msg = (LabelComponent) Components.label(Text.literal(message))
                    .color(color(TEXT_SECONDARY))
                    .horizontalSizing(Sizing.fill(95));
            card.child(msg);
        }

        return card;
    }

    /**
     * Create a success status card
     */
    public static FlowLayout createSuccessCard(String title, String message) {
        return createStatusCard("✓", title, message, SUCCESS_BG, SUCCESS_BORDER);
    }

    /**
     * Create a warning status card
     */
    public static FlowLayout createWarningCard(String title, String message) {
        return createStatusCard("⚠", title, message, WARNING_BG, WARNING_BORDER);
    }

    /**
     * Create an error status card
     */
    public static FlowLayout createErrorCard(String title, String message) {
        return createStatusCard("✗", title, message, ERROR_BG, ERROR_BORDER);
    }

    // ===== Buttons =====

    /**
     * Create a standard action button
     */
    public static ButtonComponent createButton(String text, ButtonComponent.PressAction action) {
        return (ButtonComponent) Components.button(Text.literal(text), action::onPress)
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20));
    }

    /**
     * Create a button with custom size
     */
    public static ButtonComponent createButton(String text, ButtonComponent.PressAction action,
                                               int width, int height) {
        return (ButtonComponent) Components.button(Text.literal(text), action::onPress)
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, width, height * 3))
                .sizing(Sizing.fixed(width), Sizing.fixed(height));
    }

    /**
     * Create a horizontal button row
     */
    public static FlowLayout createButtonRow(ButtonComponent... buttons) {
        FlowLayout row = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        for (ButtonComponent button : buttons) {
            row.child(button);
        }

        return row;
    }

    // ===== Lists & Entries =====

    /**
     * Create a selectable list entry
     */
    public static FlowLayout createListEntry() {
        return (FlowLayout) Containers.verticalFlow(Sizing.fill(95), Sizing.content())
                .gap(2)
                .surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ENTRY_BORDER)))
                .padding(Insets.of(6));
    }

    /**
     * Apply hover effects to a list entry
     */
    public static void applyHoverEffects(FlowLayout entry, Runnable onSelect) {
        entry.mouseEnter().subscribe(() ->
                entry.surface(Surface.flat(ENTRY_HOVER).and(Surface.outline(ACCENT_SECONDARY))));

        entry.mouseLeave().subscribe(() ->
                entry.surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ENTRY_BORDER))));

        if (onSelect != null) {
            //? if <= 1.21.8 {
            entry.mouseDown().subscribe((x, y, button) -> {
                if (button == 0) {
                    onSelect.run();
                    return true;
                }
                return false;
            });
            //?} else {
            /*entry.mouseDown().subscribe((click, doubled) -> {
                if (click.button() == 0) {
                    onSelect.run();
                    return true;
                }
                return false;
            });
            *///?}
            entry.cursorStyle(CursorStyle.HAND);
        }
    }

    /**
     * Apply selected state to entry
     */
    public static void applySelectedState(FlowLayout entry, boolean selected) {
        if (selected) {
            entry.surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ACCENT_SECONDARY)));
        } else {
            entry.surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
        }
    }

    // ===== Dialogs =====

    /**
     * Create a confirmation dialog
     */
    public static FlowLayout createDialog(String title, String message, int width) {
        FlowLayout dialog = (FlowLayout) Containers.verticalFlow(Sizing.fixed(width), Sizing.content())
                .gap(12)
                .surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_SECONDARY)))
                .padding(Insets.of(20))
                .positioning(Positioning.relative(50, 40));

        // Title
        dialog.child(Components.label(Text.literal(title).setStyle(Style.EMPTY.withBold(true)))
                .color(color(ACCENT_SECONDARY)));

        // Message
        if (message != null) {
            LabelComponent msg = (LabelComponent) Components.label(Text.literal(message))
                    .color(color(TEXT_PRIMARY))
                    .horizontalSizing(Sizing.fill(100));
            dialog.child(msg);
        }

        return dialog;
    }

    /**
     * Create a warning dialog
     */
    public static FlowLayout createWarningDialog(String title, String message, int width) {
        FlowLayout dialog = (FlowLayout) Containers.verticalFlow(Sizing.fixed(width), Sizing.content())
                .gap(12)
                .surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(WARNING_BORDER)))
                .padding(Insets.of(20))
                .positioning(Positioning.relative(50, 40));

        // Header with icon
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        header.child(Components.label(Text.literal("⚠"))
                .color(color(WARNING_BORDER))
                .sizing(Sizing.fixed(24), Sizing.content()));

        header.child(Components.label(Text.literal(title).setStyle(Style.EMPTY.withBold(true)))
                .color(color(WARNING_BORDER)));

        dialog.child(header);

        // Message
        if (message != null) {
            LabelComponent msg = (LabelComponent) Components.label(Text.literal(message))
                    .color(color(TEXT_PRIMARY))
                    .horizontalSizing(Sizing.fill(100));
            dialog.child(msg);
        }

        return dialog;
    }

    // ===== Scrollable Content =====

    /**
     * Create a scrollable content container
     */
    public static ScrollContainer<FlowLayout> createScrollContainer(FlowLayout content) {
        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                content
        );
        scroll.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scroll.scrollbarThiccness(6);
        return scroll;
    }

    /**
     * Create a scrollable content container horizontal
     */
    public static ScrollContainer<FlowLayout> createScrollBoxHorizontal(FlowLayout content) {
        ScrollContainer<FlowLayout> scroll = Containers.horizontalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                content
        );
        scroll.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scroll.scrollbarThiccness(6);
        return scroll;
    }

    // ===== Empty States =====

    /**
     * Create an empty state message
     */
    public static FlowLayout createEmptyState(String message) {
        FlowLayout empty = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand())
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        empty.child(Components.label(Text.literal(message))
                .color(color(TEXT_SECONDARY)));

        return empty;
    }

    // ===== Utility Functions =====

    /**
     * Format file size in human-readable format
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Format timestamp for display
     */
    public static String formatTimestamp(String isoTimestamp) {
        try {
            return isoTimestamp.replace('T', ' ').substring(0, Math.min(isoTimestamp.length(), 19));
        } catch (Exception e) {
            return isoTimestamp;
        }
    }

    /**
     * Strip markdown formatting from text
     */
    public static String stripMarkdown(String text) {
        if (text == null) return "";

        text = text.replaceAll("\\{[^}]*}", "");           // Color codes
        text = text.replaceAll("\\*\\*", "");                // Bold
        text = text.replaceAll("^#+\\s*", "");              // Headers
        text = text.replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1"); // Links
        text = text.replaceAll("_", "");                    // Italics
        text = text.replaceAll("`", "");                    // Code

        return text.trim();
    }
}