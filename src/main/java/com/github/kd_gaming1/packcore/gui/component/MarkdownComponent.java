package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.api.component.IComponent;
import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * A lightweight Markdown renderer built on top of UILib's component system.
 * Renders a string of Markdown as a vertical stack of {@link MultiLineTextComponent}s.
 * Supported syntax: headings (# levels 1–6), bullet lists (- or *), **bold**, *italic*, blank lines.
 */
public class MarkdownComponent extends AbstractComponent {

    private static final int HEADER_SPACING = 4;
    private static final int PARAGRAPH_SPACING = 2;
    private static final int EMPTY_LINE_SPACING = 5;
    private static final int BULLET_INDENT = 8;

    private String markdown;
    private int maxWidth;
    private final int defaultColor;

    public MarkdownComponent(int x, int y, int maxWidth, String markdown) {
        this(x, y, maxWidth, markdown, 0xFFFFFFFF);
    }

    public MarkdownComponent(int x, int y, int maxWidth, String markdown, int defaultColor) {
        super(x, y, maxWidth, 0);
        this.markdown = markdown;
        this.maxWidth = maxWidth;
        this.defaultColor = defaultColor;
        rebuild();
    }

    /** Replaces the current Markdown content and rebuilds the layout. */
    public void setMarkdown(String markdown) {
        this.markdown = markdown;
        rebuild();
    }

    /** Updates the maximum render width and rebuilds the layout. */
    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        rebuild();
    }

    /** Clears all child components and rebuilds them from the current Markdown string. */
    public void rebuild() {
        this.clearComponents();

        int currentY = 0;
        String[] lines = markdown.split("\n", -1);

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty()) {
                currentY += EMPTY_LINE_SPACING;
                continue;
            }

            int headerLevel = getHeaderLevel(line);
            if (headerLevel > 0) {
                currentY += buildHeader(line, headerLevel, currentY);
                continue;
            }

            if (isBullet(line)) {
                currentY += buildBullet(line, currentY);
                continue;
            }

            currentY += buildParagraph(line, currentY);
        }

        this.setHeight(currentY);
    }

    private int buildHeader(String line, int headerLevel, int currentY) {
        String content = line.substring(headerLevel + 1).trim();

        MutableComponent text = parseInline(content);
        text.withStyle(style -> style
                .withBold(true)
                .withColor(getHeaderColor(headerLevel)));

        MultiLineTextComponent comp = new MultiLineTextComponent(0, currentY, maxWidth, text);
        addComponent(comp);
        return comp.getHeight() + HEADER_SPACING;
    }

    private int buildBullet(String line, int currentY) {
        String content = line.substring(2).trim();

        MutableComponent bullet = Component.literal("• ")
                .withStyle(Style.EMPTY.withColor(defaultColor))
                .append(parseInline(content));

        MultiLineTextComponent comp = new MultiLineTextComponent(
                BULLET_INDENT, currentY,
                maxWidth - BULLET_INDENT,
                bullet, defaultColor
        );
        addComponent(comp);
        return comp.getHeight() + PARAGRAPH_SPACING;
    }

    private int buildParagraph(String line, int currentY) {
        MultiLineTextComponent comp = new MultiLineTextComponent(
                0, currentY, maxWidth, parseInline(line), defaultColor
        );
        addComponent(comp);
        return comp.getHeight() + PARAGRAPH_SPACING;
    }

    /**
     * Single-pass parser for inline styles (**bold** and *italic*).
     * Supports nesting — e.g. ***bold italic***.
     */
    private MutableComponent parseInline(String text) {
        MutableComponent root = Component.empty();
        StringBuilder buffer = new StringBuilder(text.length());
        boolean bold = false;
        boolean italic = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                flushBuffer(root, buffer, bold, italic);
                bold = !bold;
                i++;
                continue;
            }

            if (c == '*') {
                flushBuffer(root, buffer, bold, italic);
                italic = !italic;
                continue;
            }

            buffer.append(c);
        }

        flushBuffer(root, buffer, bold, italic);
        return root;
    }

    private void flushBuffer(MutableComponent root, StringBuilder buffer, boolean bold, boolean italic) {
        if (buffer.isEmpty()) return;

        Style style = Style.EMPTY;
        if (bold) style = style.withBold(true);
        if (italic) style = style.withItalic(true);

        root.append(Component.literal(buffer.toString()).setStyle(style));
        buffer.setLength(0);
    }

    private int getHeaderLevel(String line) {
        int level = 0;
        while (level < 6 && level < line.length() && line.charAt(level) == '#') level++;
        return (level > 0 && line.length() > level && line.charAt(level) == ' ') ? level : 0;
    }

    private boolean isBullet(String line) {
        return line.startsWith("- ") || line.startsWith("* ");
    }

    private int getHeaderColor(int level) {
        return switch (level) {
            case 1 -> 0xFFFFFF55; // yellow
            case 2 -> 0xFFFFAA55; // orange
            case 3 -> 0xFFFF5555; // red
            default -> defaultColor;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick, int parentWidth, int parentHeight) {
        for (IComponent child : this.getComponents()) {
            child.render(graphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
        }
    }
}