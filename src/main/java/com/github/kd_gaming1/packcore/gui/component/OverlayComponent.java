package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * A toggleable overlay panel with a title, scrollable markdown body, and a footer button row.
 *
 * <p>Typical usage:
 * <pre>{@code
 * OverlayComponent overlay = new OverlayComponent(x, y, 500, 350, title, markdownString);
 * overlay.addActionButton(Component.literal("Close"), btn -> overlay.setShown(false));
 * screen.addComponent(overlay); // add last so it renders on top
 *
 * ButtonWidget trigger = new ButtonWidget(..., btn -> overlay.toggle());
 * screen.addWidget(trigger);
 * }</pre>
 */
public class OverlayComponent extends AbstractComponent {

    private static final int PADDING = 8;
    private static final int CLOSE_BTN_SIZE = 16;
    private static final int HEADER_BOTTOM_GAP = 20;

    private static final int COLOR_DIM = 0x99000000;
    private static final int COLOR_TITLE = 0xFFFFFFFF;

    private boolean shown = false;

    private final MarkdownComponent markdownComponent;
    private final ScrollContainerWidget scrollContainer;
    private final ButtonWidget closeButton;

    private Runnable onClose;

    public OverlayComponent(int x, int y, int width, int height, Component title, String markdown) {
        super(x, y, width, height);

        int fontH = Minecraft.getInstance().font.lineHeight;
        int headerH = fontH + HEADER_BOTTOM_GAP;
        int innerW = width - PADDING * 2;
        int scrollH = height - headerH;

        // Title
        TextComponent titleComp = new TextComponent(PADDING, PADDING, title, COLOR_TITLE);
        titleComp.setDrawShadow(true);
        this.addComponent(titleComp);

        // Scrollable markdown area
        markdownComponent = new MarkdownComponent(0, 0, innerW - 8, markdown);
        scrollContainer = new ScrollContainerWidget(innerW, scrollH);
        scrollContainer.addComponent(markdownComponent);

        // Wrapper offsets the scroll container within the panel
        EmptyComponent scrollWrapper = new EmptyComponent(PADDING, headerH, innerW, scrollH);
        scrollWrapper.addWidget(scrollContainer);
        this.addComponent(scrollWrapper);

        // Close button
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

    /** Replace markdown content at runtime (e.g. after a network fetch). */
    public void setMarkdown(String markdown) {
        markdownComponent.setMarkdown(markdown);
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

    private static final WidgetSprites X_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/x"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/x"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/xhover")
    );

    // Skip all rendering and input when hidden.
    @Override
    public void renderBase(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        if (!shown) return;
        super.renderBase(guiGraphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
    }

    // Draws the screen dim, panel background, border, and title divider.
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        // Dim the screen behind the panel
        guiGraphics.fill(0, 0, parentWidth, parentHeight, COLOR_DIM);
    }
}