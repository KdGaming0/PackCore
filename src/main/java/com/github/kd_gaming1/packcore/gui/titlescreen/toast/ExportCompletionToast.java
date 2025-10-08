package com.github.kd_gaming1.packcore.gui.titlescreen.toast;

import io.wispforest.owo.ui.base.BaseOwoToast;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.toast.Toast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Path;

public class ExportCompletionToast extends BaseOwoToast<FlowLayout> {

    private static final int TOAST_WIDTH = 280;
    private static final int TOAST_PADDING = 12;
    private static final long TOAST_DURATION_MS = 10000;

    public ExportCompletionToast(String configName, Path exportPath) {
        super(() -> createToastContent(configName, exportPath),
                createTimeoutPredicate(System.currentTimeMillis()));
    }

    private static VisibilityPredicate<FlowLayout> createTimeoutPredicate(long startTimeMillis) {
        return (toast, time) -> {
            long elapsed = System.currentTimeMillis() - startTimeMillis;
            return elapsed < TOAST_DURATION_MS ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        };
    }

    private static FlowLayout createToastContent(String configName, Path exportPath) {
        // Main container (same size, same layout)
        FlowLayout container = Containers.verticalFlow(Sizing.fixed(TOAST_WIDTH), Sizing.content());

        // Title
        LabelComponent title = Components.label(
                Text.literal("Export Complete! ").formatted(Formatting.WHITE)
                        .append(Text.literal(configName).formatted(Formatting.GREEN, Formatting.BOLD))
        );
        title.horizontalTextAlignment(HorizontalAlignment.LEFT);

        // File path info (optional line)
        LabelComponent pathInfo = Components.label(
                Text.literal("Saved to: ").formatted(Formatting.GRAY)
                        .append(Text.literal(exportPath.getFileName().toString()).formatted(Formatting.YELLOW))
        );
        pathInfo.horizontalTextAlignment(HorizontalAlignment.LEFT);

        // Small instruction or confirmation
        LabelComponent instruction = Components.label(
                Text.literal("You can now share or import this config.").formatted(Formatting.GRAY, Formatting.ITALIC)
        );
        instruction.horizontalTextAlignment(HorizontalAlignment.LEFT);

        // Assemble
        container
                .child(title)
                .child(pathInfo)
                .child(instruction)
                .gap(2)
                .padding(Insets.of(TOAST_PADDING))
                .surface(Surface.flat(0xC0_000000)) // Same dark background
                .horizontalAlignment(HorizontalAlignment.LEFT)
                .verticalAlignment(VerticalAlignment.TOP);

        // Add green border (success accent)
        FlowLayout borderContainer = Containers.verticalFlow(Sizing.content(), Sizing.content());
        borderContainer
                .child(container)
                .surface(Surface.outline(0xFF_55FF55)) // Bright green border
                .padding(Insets.of(1));

        return borderContainer;
    }
}
