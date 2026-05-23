package com.github.kd_gaming1.packcore.gui.wizard;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.sprite.SpriteComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class WizardHeaderComponent extends AbstractComponent {

    private final WizardNavigator navigator;

    private static final Identifier SPRITE_ACTIVE = Identifier.fromNamespaceAndPath("packcore", "wizard/node_active");
    private static final Identifier SPRITE_VISITED = Identifier.fromNamespaceAndPath("packcore", "wizard/node_visited");
    private static final Identifier SPRITE_LOCKED = Identifier.fromNamespaceAndPath("packcore", "wizard/node_locked");

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_TRACK_DONE = 0xFF4A90D9;
    private static final int COLOR_TRACK_AHEAD = 0x55FFFFFF;

    private static final int ACTIVE_NODE_SIZE = 14;
    private static final int SMALL_NODE_SIZE = 10;
    private static final int TRACK_HALF_HEIGHT = 1;
    private static final int PADDING = 14;

    public WizardHeaderComponent(int x, int y, int width, int height, WizardNavigator navigator) {
        super(x, y, width, height);
        this.navigator = navigator;
    }

    /** Called by the navigator when the active page changes. */
    public void onPageChanged() { }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        //?} else {
     /*@Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
    *///?}
        var font = Minecraft.getInstance().font;
        int pageCount = navigator.getPages().size();
        int activeIndex = navigator.getCurrentIndex();

        int originX = getTotalX();
        int originY = getTotalY();

        Component title = navigator.getPages().get(activeIndex).getTitle();
        graphics.centeredText(font, title, originX + getWidth() / 2, originY + 4, COLOR_TITLE);

        int nodeRowY = originY + getHeight() - 10;
        int available = getWidth() - PADDING * 2;
        int spacing = available / (pageCount - 1);
        int startX = originX + PADDING + (available - spacing * (pageCount - 1)) / 2;

        for (int i = 0; i < pageCount; i++) {
            int nodeCenterX = startX + i * spacing;
            boolean isActive = (i == activeIndex);
            boolean isVisited = (i < activeIndex);

            int nodeSize = isActive ? ACTIVE_NODE_SIZE : SMALL_NODE_SIZE;
            int halfNode = nodeSize / 2;

            // Draw the connecting track segment before this node
            if (i > 0) {
                int prevNodeSize = (i - 1 == activeIndex) ? ACTIVE_NODE_SIZE : SMALL_NODE_SIZE;
                int prevNodeCenterX = startX + (i - 1) * spacing;
                int segmentX1 = prevNodeCenterX + prevNodeSize / 2;
                int segmentX2 = nodeCenterX - halfNode;
                int trackColor = (i <= activeIndex) ? COLOR_TRACK_DONE : COLOR_TRACK_AHEAD;
                graphics.fill(segmentX1, nodeRowY - TRACK_HALF_HEIGHT, segmentX2, nodeRowY + TRACK_HALF_HEIGHT + 1, trackColor);
            }

            Identifier sprite = isActive ? SPRITE_ACTIVE : isVisited ? SPRITE_VISITED : SPRITE_LOCKED;

            SpriteComponent node = new SpriteComponent(nodeCenterX - halfNode, nodeRowY - halfNode, nodeSize, nodeSize, sprite);
            node.updateParentPosition(originX, originY, getWidth(), getHeight());
            node.extractRenderState(graphics, mouseX, mouseY, partialTick, getWidth(), getHeight());

            if (isActive) {
                graphics.centeredText(
                        font,
                        Component.literal(String.valueOf(i + 1)),
                        nodeCenterX,
                        nodeRowY - font.lineHeight / 2,
                        COLOR_TITLE
                );
            }
        }
    }
}