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

/**
 * Toast notification for backup/restore completion
 */
public class BackupCompletionToast extends BaseOwoToast<FlowLayout> {

    private static final int TOAST_WIDTH = 280;
    private static final int TOAST_PADDING = 12;
    private static final long TOAST_DURATION_MS = 10000;

    public BackupCompletionToast(String backupName, Path backupPath, boolean isRestore) {
        super(() -> createToastContent(backupName, backupPath, isRestore),
                createTimeoutPredicate(System.currentTimeMillis()));
    }

    private static VisibilityPredicate<FlowLayout> createTimeoutPredicate(long startTimeMillis) {
        return (toast, time) -> {
            long elapsed = System.currentTimeMillis() - startTimeMillis;
            return elapsed < TOAST_DURATION_MS ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        };
    }

    private static FlowLayout createToastContent(String backupName, Path backupPath, boolean isRestore) {
        // Main container
        FlowLayout container = Containers.verticalFlow(Sizing.fixed(TOAST_WIDTH), Sizing.content());

        // Title - different text based on operation type
        String titleText = isRestore ? "Restore Complete! " : "Backup Complete! ";
        LabelComponent title = Components.label(
                Text.literal(titleText).formatted(Formatting.WHITE)
                        .append(Text.literal(backupName).formatted(Formatting.GREEN, Formatting.BOLD))
        );
        title.horizontalTextAlignment(HorizontalAlignment.LEFT);

        // File path info
        LabelComponent pathInfo = Components.label(
                Text.literal("Saved to: ").formatted(Formatting.GRAY)
                        .append(Text.literal(backupPath.getFileName().toString()).formatted(Formatting.YELLOW))
        );
        pathInfo.horizontalTextAlignment(HorizontalAlignment.LEFT);

        // Instruction - different text based on operation type
        String instructionText = isRestore
                ? "Configuration restored successfully."
                : "Backup saved successfully.";
        LabelComponent instruction = Components.label(
                Text.literal(instructionText).formatted(Formatting.GRAY, Formatting.ITALIC)
        );
        instruction.horizontalTextAlignment(HorizontalAlignment.LEFT);

        // Assemble container
        container
                .child(title)
                .child(pathInfo)
                .child(instruction)
                .gap(2)
                .padding(Insets.of(TOAST_PADDING))
                .surface(Surface.flat(0xC0_000000)) // Dark background
                .horizontalAlignment(HorizontalAlignment.LEFT)
                .verticalAlignment(VerticalAlignment.TOP);

        // Border color based on operation type
        // Blue for restore, green for backup
        int borderColor = isRestore ? 0xFF_5555FF : 0xFF_55FF55;

        FlowLayout borderContainer = Containers.verticalFlow(Sizing.content(), Sizing.content());
        borderContainer
                .child(container)
                .surface(Surface.outline(borderColor))
                .padding(Insets.of(1));

        return borderContainer;
    }
}