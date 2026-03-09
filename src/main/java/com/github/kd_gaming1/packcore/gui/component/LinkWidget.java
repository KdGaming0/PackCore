package com.github.kd_gaming1.packcore.gui.component;

import com.daqem.uilib.api.widget.IWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

import static com.github.kd_gaming1.packcore.PackCore.LOGGER;

public class LinkWidget extends AbstractWidget implements IWidget {

    private final String url;

    public LinkWidget(int x, int y, int width, int height, String url) {
        super(x, y, width, height, Component.empty());
        this.url = url;
        this.setAlpha(0f); // fully invisible
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() == 0 && isHovered()) {
            tryOpenUrl();
            return true;
        }
        return false;
    }

    private void tryOpenUrl() {
        if (url == null || url.isBlank()) return;
        try {
            Util.getPlatform().openUri(url);
        } catch (Exception e) {
            LOGGER.warn("Couldn't open uri '{}'", url, e);
        }
    }

    @Override
    protected void renderWidget(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // invisible — nothing to render
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.@NonNull NarrationElementOutput output) {
    }
}