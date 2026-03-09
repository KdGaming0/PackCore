package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.api.component.IComponent;
import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.LOGGER;

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

    private static final int SPACING_AFTER_HEADING = 4;
    private static final int SPACING_AFTER_PARAGRAPH = 2;
    private static final int SPACING_EMPTY_LINE = 5;
    private static final int SPACING_AFTER_IMAGE = 6;
    private static final int SPACING_AFTER_RULE = 6;
    private static final int SPACING_AFTER_BLOCKQUOTE = 3;

    private static final int BULLET_INDENT = 8;
    private static final int BLOCKQUOTE_INDENT = 10;
    private static final int BLOCKQUOTE_BAR_WIDTH = 2;
    private static final int BLOCKQUOTE_BAR_GAP = 4;
    private static final int RULE_HEIGHT = 1;
    private static final int DEFAULT_IMAGE_HEIGHT = 80;

    private static final int COLOR_RULE = 0xFF444444;
    private static final int COLOR_LINK = 0xFF55AAFF;
    private static final int COLOR_BLOCKQUOTE = 0xFFAAAAAA;
    private static final int COLOR_HEADING_1 = 0xFFFFFF55;
    private static final int COLOR_HEADING_2 = 0xFFFFAA55;
    private static final int COLOR_HEADING_3 = 0xFFFF5555;

    /**
     * Image syntax: ![alt](namespace:path  WxH)
     * The WxH suffix is optional — if omitted, the image renders at DEFAULT_IMAGE_HEIGHT tall.
     * Example: ![Preview](packcore:textures/gui/packs/foo.png  320x180)
     */
    private record ImageInfo(Identifier location, int width, int height) {}

    private final String markdown;
    private final int maxWidth;
    private final int defaultColor;

    // Drawn in render(); coordinates are LOCAL (relative to this component's origin)
    private final List<int[]> blockquoteBars = new ArrayList<>();
    private final List<int[]> horizontalRules = new ArrayList<>();
    private final List<ImageInfo> images = new ArrayList<>();
    private final List<int[]> imagePositions = new ArrayList<>(); // {x, y, w, h}

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

    private int buildHeading(String line, int level, int y) {
        String content = line.substring(level + 1).trim();
        MutableComponent text = parseInline(content);
        text.withStyle(s -> s.withBold(true).withColor(getHeadingColor(level)));

        float scale = getHeadingScale(level);
        int scaledWidth = (int) (maxWidth / scale);

        MultiLineTextComponent comp = new MultiLineTextComponent(0, y, scaledWidth, text);
        addComponent(comp);

        int scaledHeight = (int) (comp.getHeight() * scale);
        return scaledHeight + SPACING_AFTER_HEADING;
    }

    private int buildBullet(String line, int y) {
        String content = line.substring(2).trim();
        MutableComponent bullet = Component.literal("• ")
                .withStyle(Style.EMPTY.withColor(defaultColor))
                .append(parseInline(content));

        MultiLineTextComponent comp = new MultiLineTextComponent(BULLET_INDENT, y, maxWidth - BULLET_INDENT, bullet, defaultColor);
        addComponent(comp);
        return comp.getHeight() + SPACING_AFTER_PARAGRAPH;
    }

    private int buildParagraph(String line, int y) {
        List<PendingLink> pendingLinks = new ArrayList<>();
        MutableComponent text = parseInlineWithLinks(line, pendingLinks);

        MultiLineTextComponent comp = new MultiLineTextComponent(0, y, maxWidth, text, defaultColor);
        addComponent(comp);

        // Register each link as an invisible clickable widget
        for (PendingLink link : pendingLinks) {
            int labelWidth = Minecraft.getInstance().font.width(link.label());
            int linkH = Minecraft.getInstance().font.lineHeight;
            addWidget(new LinkWidget(0, y, Math.min(labelWidth, maxWidth), linkH, link.url()));
        }

        return comp.getHeight() + SPACING_AFTER_PARAGRAPH;
    }

    private int buildBlockquote(String line, int y) {
        String content = line.substring(1).trim();
        int textX = BLOCKQUOTE_BAR_WIDTH + BLOCKQUOTE_BAR_GAP + BLOCKQUOTE_INDENT;

        List<PendingLink> pendingLinks = new ArrayList<>();
        MutableComponent text = parseInlineWithLinks(content, pendingLinks);
        text.withStyle(s -> s.withColor(COLOR_BLOCKQUOTE).withItalic(true));

        MultiLineTextComponent comp = new MultiLineTextComponent(textX, y, maxWidth - textX, text, COLOR_BLOCKQUOTE);
        addComponent(comp);
        blockquoteBars.add(new int[]{BLOCKQUOTE_INDENT, y, comp.getHeight()});

        for (PendingLink link : pendingLinks) {
            int labelWidth = Minecraft.getInstance().font.width(link.label());
            addWidget(new LinkWidget(textX, y, Math.min(labelWidth, maxWidth - textX),
                    Minecraft.getInstance().font.lineHeight, link.url()));
        }

        return comp.getHeight() + SPACING_AFTER_BLOCKQUOTE;
    }

    private int buildHorizontalRule(int y) {
        horizontalRules.add(new int[]{y});
        return RULE_HEIGHT + SPACING_AFTER_RULE;
    }

    private int buildImage(ImageInfo info, int y) {
        images.add(info);
        imagePositions.add(new int[]{0, y, info.width(), info.height()});
        return info.height() + SPACING_AFTER_IMAGE;
    }

    // Inline parser that also collects link labels/urls for hit-testing
    private record PendingLink(String label, String url) {}

    private MutableComponent parseInlineWithLinks(String text, List<PendingLink> pendingLinks) {
        MutableComponent root = Component.empty();
        StringBuilder buffer = new StringBuilder();
        boolean bold = false;
        boolean italic = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Inline image fallback (alt text only)
            if (c == '!' && i + 1 < text.length() && text.charAt(i + 1) == '[') {
                int closeBracket = text.indexOf(']', i + 2);
                if (closeBracket != -1 && closeBracket + 1 < text.length() && text.charAt(closeBracket + 1) == '(') {
                    int closeParen = text.indexOf(')', closeBracket + 2);
                    if (closeParen != -1) {
                        flushBuffer(root, buffer, bold, italic);
                        String alt = text.substring(i + 2, closeBracket);
                        root.append(Component.literal("[" + alt + "]")
                                .withStyle(Style.EMPTY.withItalic(true).withColor(COLOR_BLOCKQUOTE)));
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
                        flushBuffer(root, buffer, bold, italic);
                        String label = text.substring(i + 1, closeBracket);
                        String url = text.substring(closeBracket + 2, closeParen);
                        root.append(buildLinkComponent(label, bold, italic));
                        pendingLinks.add(new PendingLink(label, url));
                        i = closeParen;
                        continue;
                    }
                }
            }

            // Bold
            if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                flushBuffer(root, buffer, bold, italic);
                bold = !bold;
                i++;
                continue;
            }

            // Italic
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

    private MutableComponent parseInline(String text) {
        return parseInlineWithLinks(text, new ArrayList<>());
    }

    private MutableComponent buildLinkComponent(String label, boolean bold, boolean italic) {
        Style style = Style.EMPTY
                .withColor(COLOR_LINK)
                .withUnderlined(true)
                .withBold(bold)
                .withItalic(italic);
        return Component.literal(label).setStyle(style);
    }

    private void flushBuffer(MutableComponent root, StringBuilder buffer, boolean bold, boolean italic) {
        if (buffer.isEmpty()) return;
        Style style = Style.EMPTY.withColor(defaultColor);
        if (bold) style = style.withBold(true);
        if (italic) style = style.withItalic(true);
        root.append(Component.literal(buffer.toString()).setStyle(style));
        buffer.setLength(0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick, int parentWidth, int parentHeight) {
        int baseX = getTotalX();
        int baseY = getTotalY();

        // Blockquote accent bars
        for (int[] bar : blockquoteBars) {
            int barX = baseX + bar[0];
            int barY = baseY + bar[1];
            graphics.fill(barX, barY, barX + BLOCKQUOTE_BAR_WIDTH, barY + bar[2], COLOR_RULE);
        }

        // Horizontal rules
        for (int[] rule : horizontalRules) {
            int ruleY = baseY + rule[0];
            graphics.fill(baseX, ruleY, baseX + maxWidth, ruleY + RULE_HEIGHT, COLOR_RULE);
        }

        // Images
        for (int idx = 0; idx < images.size(); idx++) {
            ImageInfo info = images.get(idx);
            int[] pos = imagePositions.get(idx);
            int imgX = baseX + pos[0];
            int imgY = baseY + pos[1];
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    info.location(),
                    imgX, imgY,
                    0.0f, 0.0f,
                    pos[2], pos[3],
                    info.width(), info.height(),
                    info.width(), info.height()
            );
        }

        // Child components
        for (IComponent child : getComponents()) {
            child.render(graphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
        }
    }

    private int getHeadingLevel(String line) {
        int level = 0;
        while (level < 6 && level < line.length() && line.charAt(level) == '#') level++;
        boolean hasSpace = level > 0 && level < line.length() && line.charAt(level) == ' ';
        return hasSpace ? level : 0;
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

    /**
     * Parses a standalone image line.
     * Format: ![alt](namespace:path/texture.png) or ![alt](namespace:path/texture.png  WxH)
     */
    private ImageInfo tryParseStandaloneImage(String line) {
        if (!line.startsWith("![")) return null;
        int closeBracket = line.indexOf(']', 2);
        if (closeBracket == -1 || closeBracket + 1 >= line.length()) return null;
        if (line.charAt(closeBracket + 1) != '(') return null;
        int closeParen = line.indexOf(')', closeBracket + 2);
        if (closeParen != line.length() - 1) return null;

        String inner = line.substring(closeBracket + 2, closeParen).trim();
        String resourcePath;
        int renderWidth = maxWidth;
        int renderHeight = DEFAULT_IMAGE_HEIGHT;

        int dimensionSep = inner.lastIndexOf(' ');
        if (dimensionSep != -1) {
            String maybeDims = inner.substring(dimensionSep + 1);
            int xSep = maybeDims.indexOf('x');
            if (xSep > 0) {
                try {
                    int w = Integer.parseInt(maybeDims.substring(0, xSep));
                    int h = Integer.parseInt(maybeDims.substring(xSep + 1));
                    if (w > maxWidth) {
                        h = h * maxWidth / w;
                        w = maxWidth;
                    }
                    renderWidth = w;
                    renderHeight = h;
                    resourcePath = inner.substring(0, dimensionSep).trim();
                } catch (NumberFormatException e) {
                    resourcePath = inner;
                }
            } else {
                resourcePath = inner;
            }
        } else {
            resourcePath = inner;
        }

        try {
            Identifier location = Identifier.parse(resourcePath);
            return new ImageInfo(location, renderWidth, renderHeight);
        } catch (Exception e) {
            return null;
        }
    }

    private float getHeadingScale(int level) {
        return switch (level) {
            case 1 -> 2.0f;
            case 2 -> 1.5f;
            case 3 -> 1.25f;
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