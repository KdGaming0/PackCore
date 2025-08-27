package com.github.kd_gaming1.packcore.gui.toast;

import io.wispforest.owo.ui.base.BaseOwoToast;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.toast.Toast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class UpdateNotificationToast extends BaseOwoToast<FlowLayout> {

    private static final int TOAST_WIDTH = 280;
    private static final int TOAST_PADDING = 12;
    private static final long TOAST_DURATION_MS = 12000;

    public UpdateNotificationToast(String currentVersion, String newVersion, String modpackName) {
        super(() -> createToastContent(currentVersion, newVersion, modpackName),
                createCustomTimeoutPredicate(System.currentTimeMillis()));
    }

    private static VisibilityPredicate<FlowLayout> createCustomTimeoutPredicate(long startTimeMillis) {
        return (toast, startTime) -> {
            long elapsedTime = System.currentTimeMillis() - startTimeMillis;

            if (elapsedTime < TOAST_DURATION_MS) {
                return Toast.Visibility.SHOW;
            } else {
                return Toast.Visibility.HIDE;
            }
        };
    }

    private static FlowLayout createToastContent(String currentVersion, String newVersion, String modpackName) {
        // Main container
        FlowLayout container = Containers.verticalFlow(Sizing.fixed(TOAST_WIDTH), Sizing.content());

        // Title section
        LabelComponent title = Components.label(
                Text.literal("Update Available for ").formatted(Formatting.WHITE)
                        .append(Text.literal(modpackName).formatted(Formatting.GOLD, Formatting.BOLD))
                        .append(Text.literal("!").formatted(Formatting.WHITE))
        );
        title.horizontalTextAlignment(HorizontalAlignment.LEFT);

        // Version comparison
        LabelComponent versionInfo = Components.label(
                Text.literal(currentVersion).formatted(Formatting.GRAY)
                        .append(Text.literal(" → ").formatted(Formatting.DARK_AQUA))
                        .append(Text.literal(newVersion).formatted(Formatting.GOLD))
        );
        versionInfo.horizontalTextAlignment(HorizontalAlignment.LEFT);

        LabelComponent instructionText = Components.label(
                Text.literal("Click the update button for details").formatted(Formatting.GRAY, Formatting.ITALIC)
        );
        instructionText.horizontalTextAlignment(HorizontalAlignment.LEFT);

        // Assemble
        container
                .child(title)
                .child(versionInfo)
                .child(instructionText)
                .gap(2)
                .padding(Insets.of(TOAST_PADDING))
                .surface(Surface.flat(0xC0_000000))
                .horizontalAlignment(HorizontalAlignment.LEFT)
                .verticalAlignment(VerticalAlignment.TOP);

        // Add golden border
        FlowLayout borderContainer = Containers.verticalFlow(Sizing.content(), Sizing.content());
        borderContainer
                .child(container)
                .surface(Surface.outline(0xFF_FFD700))
                .padding(Insets.of(1));

        return borderContainer;
    }
}