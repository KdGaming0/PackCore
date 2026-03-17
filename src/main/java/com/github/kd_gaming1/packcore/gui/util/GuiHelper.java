package com.github.kd_gaming1.packcore.gui.util;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public final class GuiHelper {

    private GuiHelper() {}

    public static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static final WidgetSprites BLANK_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_blank_gray_button")
    );

    public static EmptyComponent scrollWrapped(int x, int y, int width, int height, Consumer<ScrollContainerWidget> configure) {
        ScrollContainerWidget scroll = new ScrollContainerWidget(width, height);
        configure.accept(scroll);
        EmptyComponent wrapper = new EmptyComponent(x, y, width, height);
        wrapper.addWidget(scroll);
        return wrapper;
    }

    public static String loadMarkdown(Path path, String fallback, Logger logger) {
        if (!Files.exists(path)) {
            logger.warn("Markdown file not found: {}", path);
            return fallback;
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            logger.error("Failed to read markdown file {}: {}", path.getFileName(), e.getMessage());
            return fallback;
        }
    }
}