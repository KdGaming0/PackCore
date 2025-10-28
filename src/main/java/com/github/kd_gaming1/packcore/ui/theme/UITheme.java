package com.github.kd_gaming1.packcore.ui.theme;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.Color;

public final class UITheme {

    private UITheme() {}

    // Panel colors
    public static final int PANEL_BACKGROUND = 0xC0_1A1A1A;
    public static final int DARK_PANEL_BACKGROUND = 0xFF_2A2A2A;

    // NEW: Primary accent colors
    public static final int ACCENT_PRIMARY = 0xFF_4A90E2;      // Blue
    public static final int ACCENT_SECONDARY = 0xFF_FFB84D;    // Warm yellow

    // OLD: Kept for backwards compatibility
    @Deprecated public static final int ACCENT_GOLD = ACCENT_SECONDARY;

    // NEW: Text colors
    public static final int TEXT_PRIMARY = 0xFF_FFFFFF;        // White
    public static final int TEXT_SECONDARY = 0xFF_B0B8C8;      // Light gray
    public static final int TEXT_HINT = 0xFF_6B7280;           // Darker gray

    // OLD: Kept for backwards compatibility
    @Deprecated public static final int TEXT_WHITE = TEXT_PRIMARY;

    // NEW: Status colors with proper naming
    public static final int SUCCESS_BG = 0xE0_1A4D2E;
    public static final int SUCCESS_BORDER = 0xFF_10B981;      // Green
    public static final int WARNING_BG = 0xE0_78350F;
    public static final int WARNING_BORDER = 0xFF_F59E0B;      // Orange
    public static final int ERROR_BG = 0xE0_7F1D1D;
    public static final int ERROR_BORDER = 0xFF_EF4444;        // Red

    // OLD: Kept for backwards compatibility
    @Deprecated public static final int STATUS_SUCCESS_BG = SUCCESS_BG;
    @Deprecated public static final int STATUS_SUCCESS_BORDER = SUCCESS_BORDER;
    @Deprecated public static final int STATUS_WARNING_BG = WARNING_BG;
    @Deprecated public static final int STATUS_WARNING_BORDER = WARNING_BORDER;
    @Deprecated public static final int STATUS_ERROR_BG = ERROR_BG;
    @Deprecated public static final int STATUS_ERROR_BORDER = ERROR_BORDER;

    // Entry/List item colors
    public static final int ENTRY_BACKGROUND = 0xC0_2A2A2A;
    public static final int ENTRY_HOVER = 0xC0_3A3A3A;
    public static final int ENTRY_SELECTED = 0xC0_4A4A4A;
    public static final int ENTRY_BORDER = 0xFF_555555;

    // Convenience helpers
    public static Color color(int rgb) { return Color.ofRgb(rgb); }

    public static ButtonComponent.Renderer defaultEntryRenderer() {
        return ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ENTRY_BORDER, ENTRY_BORDER);
    }

    public static ButtonComponent.Renderer successRenderer() {
        return ButtonComponent.Renderer.flat(SUCCESS_BG, SUCCESS_BORDER, ENTRY_BORDER);
    }

    public static ButtonComponent.Renderer warningRenderer() {
        return ButtonComponent.Renderer.flat(WARNING_BG, WARNING_BORDER, ENTRY_BORDER);
    }

    public static ButtonComponent.Renderer errorRenderer() {
        return ButtonComponent.Renderer.flat(ERROR_BG, ERROR_BORDER, ENTRY_BORDER);
    }
}
