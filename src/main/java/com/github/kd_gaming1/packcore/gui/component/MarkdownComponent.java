package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.api.component.IComponent;
import com.daqem.uilib.api.widget.IWidget;
import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight Markdown renderer built on UILib's component system.
 *
 * <p>Supported syntax:
 *
 * <pre>
 *   Headings       # through ######
 *   Bullet list    - or *
 *   Bold           **text**
 *   Italic         *text*
 *   Inline code    `code`
 *   Fenced code    ```lang ... ```
 *   Link           [label](url)
 *   Image          ![alt](namespace:path  WxH)
 *   Blockquote     > text
 *   Horizontal     ---
 *   Blank line     (adds vertical spacing)
 * </pre>
 */
public class MarkdownComponent extends AbstractComponent {

    // ── Spacing ──────────────────────────────────────────────────────────────
    private static final int SPACING_HEADING = 4;
    private static final int SPACING_PARAGRAPH = 2;
    private static final int SPACING_EMPTY_LINE = 5;
    private static final int SPACING_IMAGE = 6;
    private static final int SPACING_RULE = 6;
    private static final int SPACING_BLOCKQUOTE = 3;
    private static final int SPACING_CODE_BLOCK = 6;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int BULLET_INDENT = 8;
    private static final int BLOCKQUOTE_INDENT = 10;
    private static final int BLOCKQUOTE_BAR_WIDTH = 2;
    private static final int BLOCKQUOTE_BAR_GAP = 4;
    private static final int RULE_HEIGHT = 1;
    private static final int DEFAULT_IMAGE_HEIGHT = 80;
    private static final int CODE_BLOCK_PAD_X = 6;
    private static final int CODE_BLOCK_PAD_Y = 4;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int COLOR_RULE = 0xFF444444;
    private static final int COLOR_LINK = 0xFF55AAFF;
    private static final int COLOR_BLOCKQUOTE = 0xFFAAAAAA;
    private static final int COLOR_HEADING_1 = 0xFFFFFF55;
    private static final int COLOR_HEADING_2 = 0xFFFFAA55;
    private static final int COLOR_HEADING_3 = 0xFFFF5555;
    private static final int COLOR_CODE_INLINE = 0xFFFF9580;
    private static final int COLOR_CODE_BLOCK_TEXT = 0xFFD4D4D4;
    private static final int COLOR_CODE_BLOCK_BG = 0xCC1A1A1A;
    private static final int COLOR_CODE_BLOCK_BORDER = 0xFF3C3C3C;
    private static final int COLOR_CODE_LANG_LABEL = 0xFF888888;

    // ── Internal records ──────────────────────────────────────────────────────
    private record ImageInfo(
            Identifier location, int renderWidth, int renderHeight, int texWidth, int texHeight) {}

    private record ParsedLink(int startCharIndex, int endCharIndex, String url) {}

    // ── State ─────────────────────────────────────────────────────────────────
    private final String markdown;
    private final int maxWidth;
    private final int defaultColor;

    private final List<int[]> blockquoteBars = new ArrayList<>();
    private final List<int[]> horizontalRules = new ArrayList<>();
    private final List<ImageInfo> images = new ArrayList<>();
    private final List<int[]> imagePositions = new ArrayList<>();
    /** Each entry: {@code [x, y, width, height]} of a code block background. */
    private final List<int[]> codeBlockBgs = new ArrayList<>();

    // ── Construction ──────────────────────────────────────────────────────────

    public MarkdownComponent(int x, int y, int maxWidth, String markdown) {
        this(x, y, maxWidth, markdown, 0xFFFFFFFF);
    }

    public MarkdownComponent(int x, int y, int maxWidth, String markdown, int defaultColor) {
        super(x, y, maxWidth, 0);
        this.markdown = markdown != null ? markdown : "";
        this.maxWidth = maxWidth;
        this.defaultColor = defaultColor;
        rebuild();
    }

    // ── Rebuild ───────────────────────────────────────────────────────────────

    public void rebuild() {
        clearComponents();
        clearOnlyWidgets();
        blockquoteBars.clear();
        horizontalRules.clear();
        images.clear();
        imagePositions.clear();
        codeBlockBgs.clear();

        int currentY = 0;
        String[] rawLines = markdown.split("\n", -1);
        int i = 0;

        while (i < rawLines.length) {
            String line = rawLines[i].trim();

            // ── Fenced code block ──
            if (line.startsWith("```")) {
                String lang = line.substring(3).trim();
                List<String> codeLines = new ArrayList<>();
                i++;
                while (i < rawLines.length && !rawLines[i].trim().equals("```")) {
                    codeLines.add(rawLines[i]);
                    i++;
                }
                currentY += buildCodeBlock(codeLines, lang, currentY);
                i++; // skip closing ```
                continue;
            }

            if (line.isEmpty()) {
                currentY += SPACING_EMPTY_LINE;
                i++;
                continue;
            }

            if (isHorizontalRule(line)) {
                currentY += buildHorizontalRule(currentY);
                i++;
                continue;
            }

            int headingLevel = getHeadingLevel(line);
            if (headingLevel > 0) {
                currentY += buildHeading(line, headingLevel, currentY);
                i++;
                continue;
            }

            if (isBlockquote(line)) {
                currentY += buildBlockquote(line, currentY);
                i++;
                continue;
            }

            if (isBullet(line)) {
                currentY += buildBullet(line, currentY);
                i++;
                continue;
            }

            ImageInfo image = tryParseStandaloneImage(line);
            if (image != null) {
                currentY += buildImage(image, currentY);
                i++;
                continue;
            }

            currentY += buildParagraph(line, currentY);
            i++;
        }

        setHeight(currentY);
    }

    // ── Block builders ────────────────────────────────────────────────────────

    private int buildHeading(String line, int level, int y) {
        String content = line.substring(level + 1).trim();
        MutableComponent text = parseInline(content);
        text.withStyle(s -> s.withBold(true).withColor(getHeadingColor(level)));

        float scale = getHeadingScale(level);
        int unscaledWidth = (int) (maxWidth / scale);
        ScaledTextComponent comp = new ScaledTextComponent(0, y, unscaledWidth, text, scale);
        addComponent(comp);
        return comp.getScaledHeight() + SPACING_HEADING;
    }

    private int buildBullet(String line, int y) {
        String content = line.substring(2).trim();
        List<ParsedLink> links = new ArrayList<>();
        MutableComponent bulletPrefix =
                Component.literal("• ").withStyle(Style.EMPTY.withColor(defaultColor));
        MutableComponent body = parseInlineWithLinks(content, links, bulletPrefix, defaultColor);
        MutableComponent combined = bulletPrefix.copy().append(body);

        int textWidth = maxWidth - BULLET_INDENT;
        MultiLineTextComponent comp =
                new MultiLineTextComponent(BULLET_INDENT, y, textWidth, combined, defaultColor);
        addComponent(comp);
        registerLinkWidgets(links, combined, BULLET_INDENT, y, textWidth);
        return comp.getHeight() + SPACING_PARAGRAPH;
    }

    private int buildParagraph(String line, int y) {
        List<ParsedLink> links = new ArrayList<>();
        MutableComponent text = parseInlineWithLinks(line, links, Component.empty(), defaultColor);
        MultiLineTextComponent comp = new MultiLineTextComponent(0, y, maxWidth, text, defaultColor);
        addComponent(comp);
        registerLinkWidgets(links, text, 0, y, maxWidth);
        return comp.getHeight() + SPACING_PARAGRAPH;
    }

    private int buildBlockquote(String line, int y) {
        String content = line.substring(1).trim();
        int textX = BLOCKQUOTE_BAR_WIDTH + BLOCKQUOTE_BAR_GAP + BLOCKQUOTE_INDENT;
        int textWidth = maxWidth - textX;

        List<ParsedLink> links = new ArrayList<>();
        Style italicBase = Style.EMPTY.withColor(defaultColor).withItalic(true);
        MutableComponent text =
                parseInlineWithLinks(content, links, Component.empty(), defaultColor, italicBase);
        text.withStyle(s -> s.withColor(COLOR_BLOCKQUOTE).withItalic(true));

        MultiLineTextComponent comp =
                new MultiLineTextComponent(textX, y, textWidth, text, COLOR_BLOCKQUOTE);
        addComponent(comp);
        blockquoteBars.add(new int[] {BLOCKQUOTE_INDENT, y, comp.getHeight()});
        registerLinkWidgets(links, text, textX, y, textWidth);
        return comp.getHeight() + SPACING_BLOCKQUOTE;
    }

    private int buildHorizontalRule(int y) {
        horizontalRules.add(new int[] {y});
        return RULE_HEIGHT + SPACING_RULE;
    }

    private int buildImage(ImageInfo info, int y) {
        images.add(info);
        imagePositions.add(new int[] {0, y, info.renderWidth(), info.renderHeight()});
        return info.renderHeight() + SPACING_IMAGE;
    }

    /**
     * Renders a fenced code block: dark background, optional language label, then each line of code.
     */
    private int buildCodeBlock(List<String> codeLines, String lang, int y) {
        Font font = Minecraft.getInstance().font;
        int innerWidth = maxWidth - CODE_BLOCK_PAD_X * 2;
        int contentY = y + CODE_BLOCK_PAD_Y;

        // Optional language label — right-aligned, muted
        if (!lang.isEmpty()) {
            int labelWidth = font.width(lang);
            int labelX = maxWidth - labelWidth - CODE_BLOCK_PAD_X;
            MutableComponent langLabel =
                    Component.literal(lang).withStyle(Style.EMPTY.withColor(COLOR_CODE_LANG_LABEL));
            addComponent(
                    new com.daqem.uilib.gui.component.text.TextComponent(
                            labelX, contentY, langLabel, COLOR_CODE_LANG_LABEL));
            contentY += font.lineHeight + CODE_BLOCK_PAD_Y;
        }

        // Code lines — truncated to avoid overflow
        for (String codeLine : codeLines) {
            MutableComponent lineText =
                    codeLine.isEmpty()
                            ? Component.empty()
                            : Component.literal(codeLine).withStyle(Style.EMPTY.withColor(COLOR_CODE_BLOCK_TEXT));
            addComponent(
                    new com.daqem.uilib.gui.component.text.TruncatedTextComponent(
                            CODE_BLOCK_PAD_X, contentY, innerWidth, lineText, COLOR_CODE_BLOCK_TEXT));
            contentY += font.lineHeight + 1;
        }

        int totalHeight = (contentY - y) + CODE_BLOCK_PAD_Y;
        codeBlockBgs.add(new int[] {0, y, maxWidth, totalHeight});
        return totalHeight + SPACING_CODE_BLOCK;
    }

    // ── Link hit-region registration ──────────────────────────────────────────

    private void registerLinkWidgets(
            List<ParsedLink> links,
            MutableComponent fullText,
            int columnX,
            int blockY,
            int columnWidth) {
        if (links.isEmpty()) return;

        Font font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight;
        String flatText = fullText.getString();

        List<FormattedText> wrappedLines =
                font.getSplitter().splitLines(fullText, columnWidth, Style.EMPTY);
        if (wrappedLines.isEmpty()) return;

        int[] lineStart = new int[wrappedLines.size()];
        int[] lineEnd = new int[wrappedLines.size()];
        int searchFrom = 0;
        for (int li = 0; li < wrappedLines.size(); li++) {
            String lineText = wrappedLines.get(li).getString();
            int found = flatText.indexOf(lineText, searchFrom);
            if (found == -1) found = searchFrom;
            lineStart[li] = found;
            lineEnd[li] = found + lineText.length();
            searchFrom = found + lineText.length();
        }
        lineEnd[wrappedLines.size() - 1] = flatText.length();

        for (ParsedLink link : links) {
            if (link.endCharIndex() <= link.startCharIndex()) continue;

            for (int li = 0; li < wrappedLines.size(); li++) {
                int overlapStart = Math.max(link.startCharIndex(), lineStart[li]);
                int overlapEnd = Math.min(link.endCharIndex(), lineEnd[li]);
                if (overlapEnd <= overlapStart) continue;

                String lineText = wrappedLines.get(li).getString();
                int localStart = overlapStart - lineStart[li];
                int localEnd = Math.min(overlapEnd - lineStart[li], lineText.length());
                if (localEnd <= localStart) continue;

                int startPx = font.width(lineText.substring(0, localStart));
                int endPx = font.width(lineText.substring(0, localEnd));
                int hitWidth = (endPx - startPx) + 1;
                if (hitWidth <= 0) continue;

                addWidget(
                        new LinkWidget(
                                this,
                                columnX + Math.max(0, startPx - 1),
                                blockY + li * lineHeight,
                                hitWidth,
                                lineHeight,
                                link.url()));
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            int parentWidth,
            int parentHeight) {
        int bx = getTotalX();
        int by = getTotalY();

        // Code block backgrounds (drawn first, behind everything)
        for (int[] bg : codeBlockBgs) {
            int x1 = bx + bg[0], y1 = by + bg[1], x2 = bx + bg[0] + bg[2], y2 = by + bg[1] + bg[3];
            graphics.fill(x1, y1, x2, y2, COLOR_CODE_BLOCK_BG);
            graphics.fill(x1, y1, x2, y1 + 1, COLOR_CODE_BLOCK_BORDER); // top
            graphics.fill(x1, y2 - 1, x2, y2, COLOR_CODE_BLOCK_BORDER); // bottom
            graphics.fill(x1, y1, x1 + 1, y2, COLOR_CODE_BLOCK_BORDER); // left
            graphics.fill(x2 - 1, y1, x2, y2, COLOR_CODE_BLOCK_BORDER); // right
        }

        // Blockquote bars
        for (int[] bar : blockquoteBars) {
            graphics.fill(
                    bx + bar[0],
                    by + bar[1],
                    bx + bar[0] + BLOCKQUOTE_BAR_WIDTH,
                    by + bar[1] + bar[2],
                    COLOR_RULE);
        }

        // Horizontal rules
        for (int[] rule : horizontalRules) {
            graphics.fill(bx, by + rule[0], bx + maxWidth, by + rule[0] + RULE_HEIGHT, COLOR_RULE);
        }

        // Images
        for (int i = 0; i < images.size(); i++) {
            ImageInfo info = images.get(i);
            int[] pos = imagePositions.get(i);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    info.location(),
                    bx + pos[0],
                    by + pos[1],
                    0.0f,
                    0.0f,
                    pos[2],
                    pos[3],
                    info.texWidth(),
                    info.texHeight(),
                    info.texWidth(),
                    info.texHeight());
        }

        // Child components
        for (IComponent child : getComponents()) {
            child.render(graphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
        }
    }

    // ── Inline parser ─────────────────────────────────────────────────────────

    private MutableComponent parseInline(String text) {
        return parseInlineWithLinks(text, new ArrayList<>(), Component.empty(), defaultColor);
    }

    private MutableComponent parseInlineWithLinks(
            String text, List<ParsedLink> links, MutableComponent prefix, int color) {
        return parseInlineWithLinks(text, links, prefix, color, Style.EMPTY.withColor(color));
    }

    private MutableComponent parseInlineWithLinks(
            String text,
            List<ParsedLink> links,
            MutableComponent prefix,
            int color,
            Style baseStyle) {
        MutableComponent root = Component.empty();
        StringBuilder buffer = new StringBuilder();
        boolean bold = false;
        boolean italic = false;
        int plainCharIndex = prefix.getString().length();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // ── Inline code: `code` ──
            if (c == '`') {
                MutableComponent flushed = flushBuffer(buffer, bold, italic, baseStyle, color);
                if (flushed != null) {
                    root.append(flushed);
                    plainCharIndex += flushed.getString().length();
                }
                int close = text.indexOf('`', i + 1);
                if (close != -1) {
                    String code = text.substring(i + 1, close);
                    root.append(
                            Component.literal(code).withStyle(Style.EMPTY.withColor(COLOR_CODE_INLINE)));
                    plainCharIndex += code.length();
                    i = close;
                } else {
                    buffer.append(c); // unmatched backtick — treat as literal
                }
                continue;
            }

            // ── Image: ![alt](url) ──
            if (c == '!' && i + 1 < text.length() && text.charAt(i + 1) == '[') {
                int closeBracket = text.indexOf(']', i + 2);
                if (closeBracket != -1
                        && closeBracket + 1 < text.length()
                        && text.charAt(closeBracket + 1) == '(') {
                    int closeParen = text.indexOf(')', closeBracket + 2);
                    if (closeParen != -1) {
                        MutableComponent flushed = flushBuffer(buffer, bold, italic, baseStyle, color);
                        if (flushed != null) {
                            root.append(flushed);
                            plainCharIndex += flushed.getString().length();
                        }
                        String alt = "[" + text.substring(i + 2, closeBracket) + "]";
                        root.append(
                                Component.literal(alt)
                                        .withStyle(Style.EMPTY.withItalic(true).withColor(COLOR_BLOCKQUOTE)));
                        plainCharIndex += alt.length();
                        i = closeParen;
                        continue;
                    }
                }
            }

            // ── Link: [label](url) ──
            if (c == '[') {
                int closeBracket = text.indexOf(']', i + 1);
                if (closeBracket != -1
                        && closeBracket + 1 < text.length()
                        && text.charAt(closeBracket + 1) == '(') {
                    int closeParen = text.indexOf(')', closeBracket + 2);
                    if (closeParen != -1) {
                        MutableComponent flushed = flushBuffer(buffer, bold, italic, baseStyle, color);
                        if (flushed != null) {
                            root.append(flushed);
                            plainCharIndex += flushed.getString().length();
                        }
                        String label = text.substring(i + 1, closeBracket);
                        String url = text.substring(closeBracket + 2, closeParen);
                        int start = plainCharIndex;
                        root.append(buildLinkSpan(label, bold, italic));
                        links.add(new ParsedLink(start, start + label.length(), url));
                        plainCharIndex += label.length();
                        i = closeParen;
                        continue;
                    }
                }
            }

            // ── Bold: **text** ──
            if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                MutableComponent flushed = flushBuffer(buffer, bold, italic, baseStyle, color);
                if (flushed != null) {
                    root.append(flushed);
                    plainCharIndex += flushed.getString().length();
                }
                bold = !bold;
                i++;
                continue;
            }

            // ── Italic: *text* ──
            if (c == '*') {
                MutableComponent flushed = flushBuffer(buffer, bold, italic, baseStyle, color);
                if (flushed != null) {
                    root.append(flushed);
                    plainCharIndex += flushed.getString().length();
                }
                italic = !italic;
                continue;
            }

            buffer.append(c);
        }

        MutableComponent flushed = flushBuffer(buffer, bold, italic, baseStyle, color);
        if (flushed != null) root.append(flushed);

        return root;
    }

    private MutableComponent flushBuffer(
            StringBuilder buffer, boolean bold, boolean italic, Style baseStyle, int color) {
        if (buffer.isEmpty()) return null;
        Style style = baseStyle.withColor(color);
        if (bold) style = style.withBold(true);
        if (italic) style = style.withItalic(true);
        MutableComponent comp = Component.literal(buffer.toString()).setStyle(style);
        buffer.setLength(0);
        return comp;
    }

    private MutableComponent buildLinkSpan(String label, boolean bold, boolean italic) {
        return Component.literal(label)
                .setStyle(
                        Style.EMPTY
                                .withColor(COLOR_LINK)
                                .withUnderlined(true)
                                .withBold(bold)
                                .withItalic(italic));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getHeadingLevel(String line) {
        int level = 0;
        while (level < 6 && level < line.length() && line.charAt(level) == '#') level++;
        return (level > 0 && level < line.length() && line.charAt(level) == ' ') ? level : 0;
    }

    private boolean isBullet(String line) {
        return line.startsWith("- ") || line.startsWith("* ");
    }

    private boolean isBlockquote(String line) {
        return line.startsWith("> ");
    }

    private boolean isHorizontalRule(String line) {
        return line.equals("---") || line.equals("***") || line.equals("___");
    }

    private float getHeadingScale(int level) {
        return switch (level) {
            case 1 -> 1.75f;
            case 2 -> 1.35f;
            case 3 -> 1.15f;
            default -> 1.0f;
        };
    }

    private int getHeadingColor(int level) {
        return switch (level) {
            case 1 -> COLOR_HEADING_1;
            case 2 -> COLOR_HEADING_2;
            case 3 -> COLOR_HEADING_3;
            default -> defaultColor;
        };
    }

    private ImageInfo tryParseStandaloneImage(String line) {
        if (!line.startsWith("![")) return null;
        int closeBracket = line.indexOf(']', 2);
        if (closeBracket == -1 || closeBracket + 1 >= line.length()) return null;
        if (line.charAt(closeBracket + 1) != '(') return null;
        int closeParen = line.indexOf(')', closeBracket + 2);
        if (closeParen != line.length() - 1) return null;

        String inner = line.substring(closeBracket + 2, closeParen).trim();
        String resourcePath = inner;
        int renderWidth = maxWidth, renderHeight = DEFAULT_IMAGE_HEIGHT;
        int texWidth = maxWidth, texHeight = DEFAULT_IMAGE_HEIGHT;

        int dimSep = inner.lastIndexOf(' ');
        if (dimSep != -1) {
            String dims = inner.substring(dimSep + 1);
            int xSep = dims.indexOf('x');
            if (xSep > 0) {
                try {
                    int w = Integer.parseInt(dims.substring(0, xSep));
                    int h = Integer.parseInt(dims.substring(xSep + 1));
                    texWidth = w;
                    texHeight = h;
                    renderWidth = w;
                    renderHeight = h;
                    if (renderWidth > maxWidth) {
                        renderHeight = renderHeight * maxWidth / renderWidth;
                        renderWidth = maxWidth;
                    }
                    resourcePath = inner.substring(0, dimSep).trim();
                } catch (NumberFormatException ignored) {
                }
            }
        }

        try {
            return new ImageInfo(Identifier.parse(resourcePath), renderWidth, renderHeight, texWidth, texHeight);
        } catch (Exception e) {
            return null;
        }
    }

    // ── ScaledTextComponent ───────────────────────────────────────────────────

    /**
     * Wraps a {@link MultiLineTextComponent} and applies a pose-matrix scale for headings. The
     * logical height accounts for the scale so layout is correct.
     */
    private static class ScaledTextComponent extends AbstractComponent {

        private final MultiLineTextComponent inner;
        private final float scale;
        private final int scaledHeight;

        ScaledTextComponent(int x, int y, int unscaledWidth, MutableComponent text, float scale) {
            super(x, y, (int) (unscaledWidth * scale), 0);
            this.scale = scale;
            this.inner = new MultiLineTextComponent(0, 0, unscaledWidth, text);
            this.scaledHeight = (int) (inner.getHeight() * scale);
            setHeight(scaledHeight);
        }

        int getScaledHeight() {
            return scaledHeight;
        }

        @Override
        public void render(
                GuiGraphics graphics, int mouseX, int mouseY, float pt, int parentW, int parentH) {
            int dx = getTotalX(), dy = getTotalY();
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) dx, (float) dy);
            graphics.pose().scale(scale, scale);

            int sw = (int) (parentW / scale), sh = (int) (parentH / scale);
            inner.updateParentPosition(0, 0, sw, sh);
            inner.render(graphics, (int) (mouseX / scale), (int) (mouseY / scale), pt, sw, sh);

            graphics.pose().popMatrix();
        }
    }

    // ── LinkWidget ────────────────────────────────────────────────────────────

    private static class LinkWidget extends AbstractWidget implements IWidget {

        private final MarkdownComponent parent;
        private final int localX, localY, localWidth, localHeight;
        private final String url;

        LinkWidget(
                MarkdownComponent parent, int lx, int ly, int width, int height, String url) {
            super(0, 0, width, height, Component.empty());
            this.parent = parent;
            this.localX = lx;
            this.localY = ly;
            this.localWidth = width;
            this.localHeight = height;
            this.url = url;
        }

        @Override public int getX() { return parent.getTotalX() + localX; }
        @Override public int getY() { return parent.getTotalY() + localY; }
        @Override public int getWidth() { return localWidth; }
        @Override public int getHeight() { return localHeight; }

        @Override
        public @NotNull ScreenRectangle getRectangle() {
            return new ScreenRectangle(getX(), getY(), localWidth, localHeight);
        }

        @Override
        public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean bl) {
            if (event.button() != 0 || !isMouseOver(event.x(), event.y())) return false;
            try {
                Util.getPlatform().openUri(new URI(url));
            } catch (Exception ignored) {}
            return true;
        }

        @Override
        public boolean isMouseOver(double mx, double my) {
            int ax = getX(), ay = getY();
            return mx >= ax && mx < ax + localWidth && my >= ay && my < ay + localHeight;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics g, int mx, int my, float pt) {
            if (isMouseOver(mx, my)) g.requestCursor(CursorTypes.POINTING_HAND);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput o) {}
    }
}