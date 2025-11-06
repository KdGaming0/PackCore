package com.github.kd_gaming1.packcore.ui.toast;

import io.wispforest.owo.ui.base.BaseOwoToast;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
/**
 * Improved toast system using builder pattern for easy toast creation
 */
public class PackCoreToast extends BaseOwoToast<FlowLayout> {

    // Constants
    private static final int DEFAULT_WIDTH = 280;
    private static final int DEFAULT_PADDING = 12;
    private static final long DEFAULT_DURATION_MS = 10000;

    /**
     * Toast type enum for common toast styles
     */
    public enum ToastType {
        SUCCESS(0xFF_55FF55, "✓"),
        WARNING(0xFF_FFD700, "⚠"),
        ERROR(0xFF_FF5555, "✗"),
        INFO(0xFF_5555FF, "ℹ"),
        UPDATE(0xFF_FFD700, "↑");

        private final int borderColor;
        private final String icon;

        ToastType(int borderColor, String icon) {
            this.borderColor = borderColor;
            this.icon = icon;
        }
    }

    private PackCoreToast(Builder builder) {
        super(
                () -> createContent(builder),
                createTimeoutPredicate(System.currentTimeMillis(), builder.duration)
        );
    }

    /**
     * Create the timeout predicate
     */
    private static VisibilityPredicate<FlowLayout> createTimeoutPredicate(long startTime, long duration) {
        return (toast, time) -> {
            long elapsed = System.currentTimeMillis() - startTime;
            return elapsed < duration ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
        };
    }

    /**
     * Create the toast content from builder
     */
    private static FlowLayout createContent(Builder builder) {
        FlowLayout container = Containers.verticalFlow(
                Sizing.fixed(builder.width),
                Sizing.content()
        );

        // Add icon if present
        if (builder.icon != null) {
            FlowLayout iconRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                    .gap(6)
                    .verticalAlignment(VerticalAlignment.CENTER);

            // Icon texture or text
            if (builder.iconTexture != null) {
                iconRow.child(Components.texture(
                        builder.iconTexture,
                        0, 0, builder.iconSize, builder.iconSize,
                        builder.iconSize, builder.iconSize
                ));
            } else if (builder.icon != null) {
                iconRow.child(Components.label(
                        Text.literal(builder.icon).formatted(Formatting.BOLD)
                ).color(Color.ofRgb(builder.borderColor)));
            }

            // Title next to icon
            if (builder.title != null) {
                iconRow.child(builder.title);
            }

            container.child(iconRow);
        } else if (builder.title != null) {
            // Title without icon
            container.child(builder.title);
        }

        // Add all lines
        for (LabelComponent line : builder.lines) {
            container.child(line);
        }

        // Apply styling
        container
                .gap(builder.gap)
                .padding(Insets.of(builder.padding))
                .surface(Surface.flat(builder.backgroundColor))
                .horizontalAlignment(builder.horizontalAlignment)
                .verticalAlignment(VerticalAlignment.TOP);

        // Add border if specified
        if (builder.borderColor != 0) {
            FlowLayout borderContainer = Containers.verticalFlow(
                    Sizing.content(),
                    Sizing.content()
            );
            borderContainer
                    .child(container)
                    .surface(Surface.outline(builder.borderColor))
                    .padding(Insets.of(1));
            return borderContainer;
        }

        return container;
    }

    /**
     * Show this toast
     */
    public void show() {
        ToastManager toastManager = MinecraftClient.getInstance().getToastManager();
        toastManager.add(this);
    }

    /**
     * Builder class for creating toasts
     */
    public static class Builder {
        // Required
        private LabelComponent title;

        // Optional with defaults
        private final List<LabelComponent> lines = new ArrayList<>();
        private ToastType type = ToastType.INFO;
        private int borderColor = ToastType.INFO.borderColor;
        private String icon = null;
        private Identifier iconTexture = null;
        private int iconSize = 16;
        private int width = DEFAULT_WIDTH;
        private int padding = DEFAULT_PADDING;
        private int gap = 2;
        private long duration = DEFAULT_DURATION_MS;
        private int backgroundColor = 0xC0_000000;
        private HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;

        /**
         * Set the toast type (applies default styling)
         */
        public Builder type(ToastType type) {
            this.type = type;
            this.borderColor = type.borderColor;
            this.icon = type.icon;
            return this;
        }

        /**
         * Set the title text
         */
        public Builder title(String text) {
            this.title = Components.label(Text.literal(text).formatted(Formatting.WHITE, Formatting.BOLD));
            this.title.horizontalTextAlignment(HorizontalAlignment.LEFT);
            return this;
        }

        /**
         * Set the title text with formatting
         */
        public Builder title(Text text) {
            this.title = Components.label(text);
            this.title.horizontalTextAlignment(HorizontalAlignment.LEFT);
            return this;
        }

        /**
         * Add a line of text
         */
        public Builder line(String text) {
            return line(text, Formatting.GRAY);
        }

        /**
         * Add a line of text with formatting
         */
        public Builder line(String text, Formatting... formatting) {
            LabelComponent line = Components.label(Text.literal(text).formatted(formatting));
            line.horizontalTextAlignment(HorizontalAlignment.LEFT);
            this.lines.add(line);
            return this;
        }

        /**
         * Add a Text component as a line
         */
        public Builder line(Text text) {
            LabelComponent line = Components.label(text);
            line.horizontalTextAlignment(HorizontalAlignment.LEFT);
            this.lines.add(line);
            return this;
        }

        /**
         * Set custom icon
         */
        public Builder icon(String icon) {
            this.icon = icon;
            this.iconTexture = null;
            return this;
        }

        /**
         * Set icon texture
         */
        public Builder iconTexture(Identifier texture, int size) {
            this.iconTexture = texture;
            this.iconSize = size;
            this.icon = null;
            return this;
        }

        /**
         * Set border color
         */
        public Builder borderColor(int color) {
            this.borderColor = color;
            return this;
        }

        /**
         * Set background color
         */
        public Builder backgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        /**
         * Set duration in milliseconds
         */
        public Builder duration(long durationMs) {
            this.duration = durationMs;
            return this;
        }

        /**
         * Set width
         */
        public Builder width(int width) {
            this.width = width;
            return this;
        }

        /**
         * Set padding
         */
        public Builder padding(int padding) {
            this.padding = padding;
            return this;
        }

        /**
         * Set gap between elements
         */
        public Builder gap(int gap) {
            this.gap = gap;
            return this;
        }

        /**
         * Set horizontal alignment
         */
        public Builder alignment(HorizontalAlignment alignment) {
            this.horizontalAlignment = alignment;
            return this;
        }

        /**
         * Build the toast
         */
        public PackCoreToast build() {
            if (title == null) {
                throw new IllegalStateException("Toast must have a title");
            }
            return new PackCoreToast(this);
        }

        /**
         * Build and show the toast immediately
         */
        public void show() {
            build().show();
        }
    }

    // ===== Static factory methods for common toasts =====

    /**
     * Create an update available toast
     */
    public static void showUpdateAvailable(String currentVersion, String newVersion, String modpackName) {
        new Builder()
                .type(ToastType.UPDATE)
                .title(Text.literal("Update Available for ")
                        .append(Text.literal(modpackName).formatted(Formatting.GOLD, Formatting.BOLD))
                        .append(Text.literal("!")))
                .line(Text.literal(currentVersion).formatted(Formatting.GRAY)
                        .append(Text.literal(" → ").formatted(Formatting.DARK_AQUA))
                        .append(Text.literal(newVersion).formatted(Formatting.GOLD)))
                .line("Update the pack in your launcher", Formatting.GRAY, Formatting.ITALIC)
                .duration(12000)
                .show();
    }

    /**
     * Create an export completion toast
     */
    public static void showExportComplete(String configName, String fileName) {
        new Builder()
                .type(ToastType.SUCCESS)
                .title("Export Complete!")
                .line(Text.literal("Config: ").formatted(Formatting.GRAY)
                        .append(Text.literal(configName).formatted(Formatting.GREEN, Formatting.BOLD)))
                .line(Text.literal("Saved as: ").formatted(Formatting.GRAY)
                        .append(Text.literal(fileName).formatted(Formatting.YELLOW)))
                .line("You can now share or import this config", Formatting.GRAY, Formatting.ITALIC)
                .show();
    }

    /**
     * Create a backup completion toast
     */
    public static void showBackupComplete(String backupName, String fileName, boolean isRestore) {
        String title = isRestore ? "Restore Complete!" : "Backup Complete!";
        String message = isRestore ? "Configuration restored successfully." : "Backup saved successfully.";

        new Builder()
                .type(ToastType.SUCCESS)
                .borderColor(isRestore ? 0xFF_5555FF : 0xFF_55FF55)
                .title(title)
                .line(Text.literal("Name: ").formatted(Formatting.GRAY)
                        .append(Text.literal(backupName).formatted(Formatting.GREEN, Formatting.BOLD)))
                .line(Text.literal("File: ").formatted(Formatting.GRAY)
                        .append(Text.literal(fileName).formatted(Formatting.YELLOW)))
                .line(message, Formatting.GRAY, Formatting.ITALIC)
                .show();
    }

    /**
     * Create an error toast
     */
    public static void showError(String title, String message) {
        new Builder()
                .type(ToastType.ERROR)
                .title(title)
                .line(message, Formatting.RED)
                .duration(8000)
                .show();
    }

    /**
     * Create a warning toast
     */
    public static void showWarning(String title, String message) {
        new Builder()
                .type(ToastType.WARNING)
                .title(title)
                .line(message, Formatting.YELLOW)
                .show();
    }

    /**
     * Create an info toast
     */
    public static void showInfo(String title, String message) {
        new Builder()
                .type(ToastType.INFO)
                .title(title)
                .line(message, Formatting.AQUA)
                .show();
    }

    /**
     * Create a simple success toast
     */
    public static void showSuccess(String message) {
        new Builder()
                .type(ToastType.SUCCESS)
                .title("Success!")
                .line(message, Formatting.GREEN)
                .duration(5000)
                .show();
    }
}