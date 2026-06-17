package com.github.kd_gaming1.packcore.gui.wizard;

import net.minecraft.network.chat.Component;

/**
 * One row on the wizard's Confirm &amp; Apply summary page.
 *
 * @param stepId owning step id; maps the row to its step's apply success/error status
 * @param label  left-hand label (empty for sub-rows that only show a value)
 * @param value  right-hand value text
 * @param skipped whether the user left this selection empty (renders dimmed)
 * @param subRow  whether to render indented beneath a header row (e.g. an individual resource pack)
 */
public record SummaryRow(String stepId, String label, Component value, boolean skipped, boolean subRow) {

    private static final Component SKIPPED = Component.literal("Skipped");

    /** A primary row with a label and value. */
    public static SummaryRow of(String stepId, String label, Component value, boolean skipped) {
        return new SummaryRow(stepId, label, value, skipped, false);
    }

    /**
     * A primary row for a single-selection step: shows "Skipped" when nothing is chosen, otherwise
     * the translated option name at {@code translationPrefix + selectedId + ".name"}.
     */
    public static SummaryRow single(String stepId, String label, String translationPrefix, String selectedId) {
        boolean skipped = selectedId == null;
        Component value = skipped ? SKIPPED : Component.translatable(translationPrefix + selectedId + ".name");
        return of(stepId, label, value, skipped);
    }

    /** An indented value-only row beneath a header (no label). */
    public static SummaryRow sub(String stepId, Component value) {
        return new SummaryRow(stepId, "", value, false, true);
    }
}
