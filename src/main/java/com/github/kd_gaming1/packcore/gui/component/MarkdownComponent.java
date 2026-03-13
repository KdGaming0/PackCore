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
 * <p>
 * Supported syntax:
 *   Headings     # through ######
 *   Bullet list  - or *
 *   Bold         **text**
 *   Italic       *text*
 *   Link         [label](url)
 *   Image        ![alt](namespace:path/to/texture.png  WxH)
 *   Blockquote   > text
 *   Horizontal   ---
 *   Blank line   (adds vertical spacing)
 */
public class MarkdownComponent extends AbstractComponent {

    // Spacing constants
    private static final int SPACING_HEADING = 4;
    private static final int SPACING_PARAGRAPH = 2;
    private static final int SPACING_EMPTY_LINE = 5;
    private static final int SPACING_IMAGE = 6;
    private static final int SPACING_RULE = 6;
    private static final int SPACING_BLOCKQUOTE = 3;

    // Layout constants
    private static final int BULLET_INDENT = 8;
    private static final int BLOCKQUOTE_INDENT = 10;
    private static final int BLOCKQUOTE_BAR_WIDTH = 2;
    private static final int BLOCKQUOTE_BAR_GAP = 4;
    private static final int RULE_HEIGHT = 1;
    private static final int DEFAULT_IMAGE_HEIGHT = 80;

    // Colors
    private static final int COLOR_RULE = 0xFF444444;
    private static final int COLOR_LINK = 0xFF55AAFF;
    private static final int COLOR_BLOCKQUOTE = 0xFFAAAAAA;
    private static final int COLOR_HEADING_1 = 0xFFFFFF55;
    private static final int COLOR_HEADING_2 = 0xFFFFAA55;
    private static final int COLOR_HEADING_3 = 0xFFFF5555;

    private record ImageInfo(Identifier location, int renderWidth, int renderHeight, int texWidth, int texHeight) {}
    private record ParsedLink(MutableComponent textBefore, MutableComponent textThrough, String label, String url) {}

    private final String markdown;
    private final int maxWidth;
    private final int defaultColor;

    private final List<int[]> blockquoteBars = new ArrayList<>();
    private final List<int[]> horizontalRules = new ArrayList<>();
    private final List<ImageInfo> images = new ArrayList<>();
    private final List<int[]> imagePositions = new ArrayList<>();

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

    public void rebuild() {
        clearComponents();
        clearOnlyWidgets();
        blockquoteBars.clear();
        horizontalRules.clear();
        images.clear();
        imagePositions.clear();

        int currentY = 0;

        for (String rawLine : markdown.split("\n", -1)) {
            String line = rawLine.trim();

            if (line.isEmpty()) {
                currentY += SPACING_EMPTY_LINE;
                continue;
            }

            if (isHorizontalRule(line)) {
                currentY += buildHorizontalRule(currentY);
                continue;
            }

            int headingLevel = getHeadingLevel(line);
            if (headingLevel > 0) {
                currentY += buildHeading(line, headingLevel, currentY);
                continue;
            }

            if (isBlockquote(line)) {
                currentY += buildBlockquote(line, currentY);
                continue;
            }

            if (isBullet(line)) {
                currentY += buildBullet(line, currentY);
                continue;
            }

            ImageInfo image = tryParseStandaloneImage(line);
            if (image != null) {
                currentY += buildImage(image, currentY);
                continue;
            }

            currentY += buildParagraph(line, currentY);
        }

        setHeight(currentY);
    }

    // -------------------------------------------------------------------------
    // Block builders
    // -------------------------------------------------------------------------

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

        MutableComponent bullet = Component.literal("• ").withStyle(Style.EMPTY.withColor(defaultColor));
        MutableComponent body = parseInlineWithLinks(content, links, bullet, defaultColor);
        MutableComponent combined = bullet.copy().append(body);

        int textWidth = maxWidth - BULLET_INDENT;
        MultiLineTextComponent comp = new MultiLineTextComponent(BULLET_INDENT, y, textWidth, combined, defaultColor);
        addComponent(comp);

        registerLinkWidgets(links, BULLET_INDENT, y, textWidth);
        return comp.getHeight() + SPACING_PARAGRAPH;
    }

    private int buildParagraph(String line, int y) {
        List<ParsedLink> links = new ArrayList<>();
        MutableComponent text = parseInlineWithLinks(line, links, Component.empty(), defaultColor);

        MultiLineTextComponent comp = new MultiLineTextComponent(0, y, maxWidth, text, defaultColor);
        addComponent(comp);

        registerLinkWidgets(links, 0, y, maxWidth);
        return comp.getHeight() + SPACING_PARAGRAPH;
    }

    private int buildBlockquote(String line, int y) {
        String content = line.substring(1).trim();
        int textX = BLOCKQUOTE_BAR_WIDTH + BLOCKQUOTE_BAR_GAP + BLOCKQUOTE_INDENT;
        int textWidth = maxWidth - textX;

        List<ParsedLink> links = new ArrayList<>();
        Style italicStyle = Style.EMPTY.withColor(defaultColor).withItalic(true);
        MutableComponent text = parseInlineWithLinks(content, links, Component.empty(), defaultColor, italicStyle);
        text.withStyle(s -> s.withColor(COLOR_BLOCKQUOTE).withItalic(true));

        MultiLineTextComponent comp = new MultiLineTextComponent(textX, y, textWidth, text, COLOR_BLOCKQUOTE);
        addComponent(comp);
        blockquoteBars.add(new int[]{BLOCKQUOTE_INDENT, y, comp.getHeight()});

        registerLinkWidgets(links, textX, y, textWidth);
        return comp.getHeight() + SPACING_BLOCKQUOTE;
    }

    private int buildHorizontalRule(int y) {
        horizontalRules.add(new int[]{y});
        return RULE_HEIGHT + SPACING_RULE;
    }

    private int buildImage(ImageInfo info, int y) {
        images.add(info);
        imagePositions.add(new int[]{0, y, info.renderWidth(), info.renderHeight()});
        return info.renderHeight() + SPACING_IMAGE;
    }

    // -------------------------------------------------------------------------
    // Link hit-region registration
    // -------------------------------------------------------------------------

    private void registerLinkWidgets(List<ParsedLink> links, int columnX, int blockY, int columnWidth) {
        if (links.isEmpty()) return;

        Font font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight;

        for (ParsedLink link : links) {
            List<FormattedText> wrappedBefore = font.getSplitter().splitLines(link.textBefore(), columnWidth, Style.EMPTY);
            List<FormattedText> wrappedThrough = font.getSplitter().splitLines(link.textThrough(), columnWidth, Style.EMPTY);

            if (wrappedThrough.isEmpty()) continue;

            int linkStartLine = wrappedBefore.isEmpty() ? 0 : wrappedBefore.size() - 1;
            int linkStartX = wrappedBefore.isEmpty() ? columnX : columnX + font.width(wrappedBefore.get(linkStartLine));

            for (int lineIdx = linkStartLine; lineIdx < wrappedThrough.size(); lineIdx++) {
                int hitX;
                int hitWidth;

                if (lineIdx == linkStartLine) {
                    hitX = linkStartX;
                    hitWidth = font.width(wrappedThrough.get(lineIdx)) - (linkStartX - columnX);
                } else {
                    hitX = columnX;
                    hitWidth = font.width(wrappedThrough.get(lineIdx));
                }

                if (hitWidth <= 0) continue;

                addWidget(new LinkWidget(this, hitX, blockY + lineIdx * lineHeight, hitWidth, lineHeight, link.url()));
            }
        }
    }

    // -------------------------------------------------------------------------
    // ScaledTextComponent
    // -------------------------------------------------------------------------

    /**
     * Wraps a MultiLineTextComponent and applies a pose-matrix scale when rendering,
     * so heading text appears visually larger. The logical height accounts for the scale, so the layout is correct.
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
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                           float partialTick, int parentWidth, int parentHeight) {
            int drawX = getTotalX();
            int drawY = getTotalY();

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) drawX, (float) drawY);
            graphics.pose().scale(scale, scale);

            int scaledParentWidth = (int) (parentWidth / scale);
            int scaledParentHeight = (int) (parentHeight / scale);

            inner.updateParentPosition(0, 0, scaledParentWidth, scaledParentHeight);
            inner.render(graphics, (int) (mouseX / scale), (int) (mouseY / scale),
                    partialTick, scaledParentWidth, scaledParentHeight);

            graphics.pose().popMatrix();
        }
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick, int parentWidth, int parentHeight) {
        int baseX = getTotalX();
        int baseY = getTotalY();

        for (int[] bar : blockquoteBars) {
            graphics.fill(
                    baseX + bar[0], baseY + bar[1],
                    baseX + bar[0] + BLOCKQUOTE_BAR_WIDTH, baseY + bar[1] + bar[2],
                    COLOR_RULE
            );
        }

        for (int[] rule : horizontalRules) {
            graphics.fill(baseX, baseY + rule[0], baseX + maxWidth, baseY + rule[0] + RULE_HEIGHT, COLOR_RULE);
        }

        for (int i = 0; i < images.size(); i++) {
            ImageInfo info = images.get(i);
            int[] pos = imagePositions.get(i);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    info.location(),
                    baseX + pos[0], baseY + pos[1],
                    0.0f, 0.0f,
                    pos[2], pos[3],
                    info.texWidth(), info.texHeight(),
                    info.texWidth(), info.texHeight()
            );
        }

        for (IComponent child : getComponents()) {
            child.render(graphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
        }
    }

    // -------------------------------------------------------------------------
    // LinkWidget
    // -------------------------------------------------------------------------

    private static class LinkWidget extends AbstractWidget implements IWidget {

        private final MarkdownComponent parent;
        private final int localX;
        private final int localY;
        private final int localWidth;
        private final int localHeight;
        private final String url;

        LinkWidget(MarkdownComponent parent, int localX, int localY, int width, int height, String url) {
            super(0, 0, width, height, Component.empty());
            this.parent = parent;
            this.localX = localX;
            this.localY = localY;
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
            openUrl(url);
            return true;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            int absX = getX();
            int absY = getY();
            return mouseX >= absX && mouseX < absX + localWidth
                    && mouseY >= absY && mouseY < absY + localHeight;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (isMouseOver(mouseX, mouseY)) {
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}

        private static void openUrl(String url) {
            try {
                Util.getPlatform().openUri(new URI(url));
            } catch (Exception ignored) {}
        }
    }

    // -------------------------------------------------------------------------
    // Inline parser
    // -------------------------------------------------------------------------

    private MutableComponent parseInlineWithLinks(String text, List<ParsedLink> links,
                                                  MutableComponent prefix, int color) {
        return parseInlineWithLinks(text, links, prefix, color, Style.EMPTY.withColor(color));
    }

    private MutableComponent parseInlineWithLinks(String text, List<ParsedLink> links,
                                                  MutableComponent prefix, int color, Style baseStyle) {
        MutableComponent root = Component.empty();
        MutableComponent accumulator = prefix.copy();
        StringBuilder buffer = new StringBuilder();
        boolean bold = false;
        boolean italic = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Inline image — render as italic alt text
            if (c == '!' && i + 1 < text.length() && text.charAt(i + 1) == '[') {
                int closeBracket = text.indexOf(']', i + 2);
                if (closeBracket != -1 && closeBracket + 1 < text.length() && text.charAt(closeBracket + 1) == '(') {
                    int closeParen = text.indexOf(')', closeBracket + 2);
                    if (closeParen != -1) {
                        appendFlushed(root, accumulator, flushBuffer(buffer, bold, italic, baseStyle, color));
                        MutableComponent altText = Component.literal("[" + text.substring(i + 2, closeBracket) + "]")
                                .withStyle(Style.EMPTY.withItalic(true).withColor(COLOR_BLOCKQUOTE));
                        root.append(altText);
                        accumulator.append(altText.copy());
                        i = closeParen;
                        continue;
                    }
                }
            }

            // Link
            if (c == '[') {
                int closeBracket = text.indexOf(']', i + 1);
                if (closeBracket != -1 && closeBracket + 1 < text.length() && text.charAt(closeBracket + 1) == '(') {
                    int closeParen = text.indexOf(')', closeBracket + 2);
                    if (closeParen != -1) {
                        appendFlushed(root, accumulator, flushBuffer(buffer, bold, italic, baseStyle, color));

                        String label = text.substring(i + 1, closeBracket);
                        String url = text.substring(closeBracket + 2, closeParen);

                        MutableComponent textBefore = accumulator.copy();
                        MutableComponent linkSpan = buildLinkSpan(label, bold, italic);
                        root.append(linkSpan);
                        accumulator.append(linkSpan.copy());

                        links.add(new ParsedLink(textBefore, textBefore.copy().append(linkSpan.copy()), label, url));
                        i = closeParen;
                        continue;
                    }
                }
            }

            // Bold
            if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                appendFlushed(root, accumulator, flushBuffer(buffer, bold, italic, baseStyle, color));
                bold = !bold;
                i++;
                continue;
            }

            // Italic
            if (c == '*') {
                appendFlushed(root, accumulator, flushBuffer(buffer, bold, italic, baseStyle, color));
                italic = !italic;
                continue;
            }

            buffer.append(c);
        }

        appendFlushed(root, accumulator, flushBuffer(buffer, bold, italic, baseStyle, color));
        return root;
    }

    private void appendFlushed(MutableComponent root, MutableComponent accumulator, MutableComponent flushed) {
        if (flushed == null) return;
        root.append(flushed);
        accumulator.append(flushed.copy());
    }

    private MutableComponent parseInline(String text) {
        return parseInlineWithLinks(text, new ArrayList<>(), Component.empty(), defaultColor);
    }

    private MutableComponent buildLinkSpan(String label, boolean bold, boolean italic) {
        return Component.literal(label).setStyle(Style.EMPTY
                .withColor(COLOR_LINK)
                .withUnderlined(true)
                .withBold(bold)
                .withItalic(italic));
    }

    private MutableComponent flushBuffer(StringBuilder buffer, boolean bold, boolean italic,
                                         Style baseStyle, int color) {
        if (buffer.isEmpty()) return null;
        Style style = baseStyle.withColor(color);
        if (bold) style = style.withBold(true);
        if (italic) style = style.withItalic(true);
        MutableComponent comp = Component.literal(buffer.toString()).setStyle(style);
        buffer.setLength(0);
        return comp;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    private ImageInfo tryParseStandaloneImage(String line) {
        if (!line.startsWith("![")) return null;
        int closeBracket = line.indexOf(']', 2);
        if (closeBracket == -1 || closeBracket + 1 >= line.length()) return null;
        if (line.charAt(closeBracket + 1) != '(') return null;
        int closeParen = line.indexOf(')', closeBracket + 2);
        if (closeParen != line.length() - 1) return null;

        String inner = line.substring(closeBracket + 2, closeParen).trim();
        String resourcePath = inner;
        int renderWidth = maxWidth;
        int renderHeight = DEFAULT_IMAGE_HEIGHT;
        int texWidth = maxWidth;
        int texHeight = DEFAULT_IMAGE_HEIGHT;

        int dimSepIdx = inner.lastIndexOf(' ');
        if (dimSepIdx != -1) {
            String maybeDims = inner.substring(dimSepIdx + 1);
            int xSepIdx = maybeDims.indexOf('x');
            if (xSepIdx > 0) {
                try {
                    int w = Integer.parseInt(maybeDims.substring(0, xSepIdx));
                    int h = Integer.parseInt(maybeDims.substring(xSepIdx + 1));
                    texWidth = w;
                    texHeight = h;
                    renderWidth = w;
                    renderHeight = h;
                    if (renderWidth > maxWidth) {
                        renderHeight = renderHeight * maxWidth / renderWidth;
                        renderWidth = maxWidth;
                    }
                    resourcePath = inner.substring(0, dimSepIdx).trim();
                } catch (NumberFormatException ignored) {}
            }
        }

        try {
            return new ImageInfo(Identifier.parse(resourcePath), renderWidth, renderHeight, texWidth, texHeight); // ← add texWidth, texHeight
        } catch (Exception e) {
            return null;
        }
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
}
