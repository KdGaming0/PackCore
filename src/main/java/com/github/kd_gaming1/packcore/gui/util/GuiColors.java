package com.github.kd_gaming1.packcore.gui.util;

/**
 * Centralized color palette for all GUI components.
 * Edit values here to restyle MultiSelectList, OptionSelectList, and OptionCardGrid globally.
 */
public final class GuiColors {

    private GuiColors() {}

    // --- Row / Card backgrounds ---
    public static final int ROW_BACKGROUND = 0x22FFFFFF;
    public static final int ROW_SELECTED = 0x33FFFFFF;

    // --- Borders ---
    public static final int BORDER_SELECTED = 0xFF2196F3;
    public static final int BORDER_HOVERED = 0xFFFFAA00;
    public static final int BORDER_IDLE = 0xFF333333;

    // --- Selection indicator (left bar in list rows) ---
    public static final int INDICATOR_SELECTED = 0xFF2196F3;

    // --- Text: name / label ---
    public static final int NAME_SELECTED = 0xFF2196F3;
    public static final int NAME_DEFAULT = 0xFFCCCCCC;

    // --- Text: description ---
    public static final int DESCRIPTION = 0xFFAAAAAA;

    // --- Checkbox (MultiSelectList) ---
    public static final int CHECKMARK_BOX = 0xFF2196F3;
    public static final int CHECKMARK_TICK = 0xFFFFFFFF;
}