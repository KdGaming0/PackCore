package com.github.kd_gaming1.packcore.wizard.ui.theme;

import java.awt.*;

public class WizardTheme {
    // Dark theme colors
    public static final Color BACKGROUND_DARK = new Color(32, 34, 37);
    public static final Color BACKGROUND_MEDIUM = new Color(47, 49, 54);
    public static final Color BACKGROUND_LIGHT = new Color(54, 57, 63);

    // Gold/Orange accent colors
    public static final Color ACCENT_GOLD = new Color(255, 193, 7);
    public static final Color ACCENT_ORANGE = new Color(255, 152, 0);
    public static final Color ACCENT_HOVER = new Color(255, 213, 79);

    // Text colors
    public static final Color TEXT_PRIMARY = new Color(220, 221, 222);
    public static final Color TEXT_SECONDARY = new Color(185, 187, 190);
    public static final Color TEXT_MUTED = new Color(142, 146, 151);

    // Status colors
    public static final Color SUCCESS = new Color(67, 181, 129);
    public static final Color WARNING = new Color(250, 166, 26);
    public static final Color ERROR = new Color(237, 66, 69);
    public static final Color INFO = new Color(114, 137, 218);
    public static final Color DISCORD = new Color(88, 101, 242);

    // Borders and separators
    public static final Color BORDER = new Color(72, 75, 81);
    public static final Color SEPARATOR = new Color(64, 68, 75);

    public static Font getFont(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static Font getTitleFont() {
        return getFont(Font.BOLD, 24);
    }

    public static Font getHeaderFont() {
        return getFont(Font.BOLD, 16);
    }

    public static Font getBodyFont() {
        return getFont(Font.PLAIN, 13);
    }

    public static Font getSmallFont() {
        return getFont(Font.PLAIN, 11);
    }
}