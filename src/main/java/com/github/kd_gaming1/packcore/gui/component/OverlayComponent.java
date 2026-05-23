package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * A toggleable overlay panel with a title, scrollable Markdown body, and a close button.
 */
public class OverlayComponent extends AbstractComponent {

    private static final int PADDING = 8;
    private static final int CLOSE_BTN_SIZE = 16;
    private static final int HEADER_BOTTOM_GAP = 20;

    private static final int COLOR_DIM = 0x99000000;
    private static final int COLOR_TITLE = 0xFFFFFFFF;

    private static final WidgetSprites X_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/x"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/x"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/xhover")
    );

    private boolean shown = false;

    private final ScrollContainerWidget scrollContainer;
    private final ButtonWidget closeButton;

    private Runnable onClose;

    public OverlayComponent(int x, int y, int width, int height, Component title, String markdown) {
        super(x, y, width, height);

        int fontHeight = Minecraft.getInstance().font.lineHeight;
        int headerHeight = fontHeight + HEADER_BOTTOM_GAP;
        int innerWidth = width - PADDING * 2;
        int scrollHeight = height - headerHeight;

        TextComponent titleComp = new TextComponent(PADDING, PADDING, title, COLOR_TITLE);
        titleComp.setDrawShadow(true);
        this.addComponent(titleComp);

        MarkdownComponent markdownComponent = new MarkdownComponent(0, 0, innerWidth - 8, markdown);
        scrollContainer = new ScrollContainerWidget(innerWidth, scrollHeight);
        scrollContainer.addComponent(markdownComponent);

        EmptyComponent scrollWrapper = new EmptyComponent(PADDING, headerHeight, innerWidth, scrollHeight);
        scrollWrapper.addWidget(scrollContainer);
        this.addComponent(scrollWrapper);

        closeButton = new CustomButtonWidget(
                width - CLOSE_BTN_SIZE - PADDING, PADDING,
                CLOSE_BTN_SIZE, CLOSE_BTN_SIZE,
                Component.literal(""),
                X_BUTTON_SPRITES,
                btn -> setShown(false)
        );
        this.addWidget(closeButton);

        setShown(false);
    }

    public void setShown(boolean shown) {
        this.shown = shown;
        scrollContainer.visible = shown;
        scrollContainer.active = shown;
        closeButton.visible = shown;
        closeButton.active = shown;
        if (!shown && onClose != null) {
            onClose.run();
        }
    }

    public void toggle() {
        setShown(!shown);
    }

    public boolean isShown() {
        return shown;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    //? if >=26.1 {
    @Override
    public void extractRenderStateBase(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
    //?} else {
    /*@Override
    public void renderBase(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
    *///?}
        if (!shown) return;
        super.extractRenderStateBase(graphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
    //? if >=26.1 {
        }
    //?} else {
    /*}
    *///?}

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
    //?} else {
    /*@Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
    *///?}
        graphics.fill(0, 0, parentWidth, parentHeight, COLOR_DIM);
    //? if >=26.1 {
        }
    //?} else {
    /*}
    *///?}
}