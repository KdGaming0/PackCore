package com.github.kd_gaming1.packcore.gui.ui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.Color;

public final class UITheme {

    private UITheme() {}

    // Colors
    public static final int PANEL_BACKGROUND = 0xC0_1A1A1A;
    public static final int ACCENT_GOLD = 0xFF_FFD700;
    public static final int TEXT_WHITE = 0xFFFFFF;
    public static final int TEXT_SECONDARY = 0xB9BBBE;

    public static final int STATUS_SUCCESS_BG = 0xC0_2D5016;
    public static final int STATUS_SUCCESS_BORDER = 0xFF_52C41A;
    public static final int STATUS_WARNING_BG = 0xC0_5C3317;
    public static final int STATUS_WARNING_BORDER = 0xFF_FAAD14;
    public static final int STATUS_ERROR_BG = 0xC0_5C1717;
    public static final int STATUS_ERROR_BORDER = 0xFF_FF4D4F;

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
        return ButtonComponent.Renderer.flat(STATUS_SUCCESS_BG, STATUS_SUCCESS_BORDER, ENTRY_BORDER);
    }

    public static ButtonComponent.Renderer warningRenderer() {
        return ButtonComponent.Renderer.flat(STATUS_WARNING_BG, STATUS_WARNING_BORDER, ENTRY_BORDER);
    }

    public static ButtonComponent.Renderer errorRenderer() {
        return ButtonComponent.Renderer.flat(STATUS_ERROR_BG, STATUS_ERROR_BORDER, ENTRY_BORDER);
    }
}