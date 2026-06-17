package com.github.kd_gaming1.packcore.gui.wizard;

import java.util.List;

/**
 * Descriptor for a single configurable wizard step.
 *
 * <p>A step pairs a render {@link BaseWizardPage} (created by {@link #createPage}) with the
 * configuration it edits ({@link #apply}) and how it appears on the Confirm &amp; Apply summary
 * ({@link #summaryRows}). Steps are registered in order in {@link WizardSteps}; that order is both
 * the page order shown to the user and the order in which {@link #apply} runs.
 *
 * <p>To add a page: create its {@code XxxPage} (render) and {@code XxxStep} (this), then add one
 * line to {@link WizardSteps}. To remove it: delete the two files and that line.
 */
public interface WizardStep {

    /** Stable, unique id. Used by the {@code /packcore wizard <id>} command and version tracking. */
    String id();

    /**
     * Content version of this step. Bump it when the page changes in a way that existing users
     * should see again; the next launch re-opens the wizard showing only the bumped pages.
     */
    int version();

    /** Whether this step applies in the current environment (e.g. a required mod is loaded). */
    default boolean isAvailable() { return true; }

    /**
     * Ids of other steps that must run alongside this one in a partial run (single-page command or
     * the post-update "new pages" flow). Use only for genuine cross-step state coupling — e.g. the
     * Caxton font step folds its pack into the resource-pack selection that the resource-pack step
     * then applies, so neither is correct without the other. Order still comes from {@link WizardSteps}.
     */
    default List<String> requires() { return List.of(); }

    /** Builds the render page for this step. */
    BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height);

    /** Rows describing this step's selection on the Confirm &amp; Apply page. Empty = no summary. */
    default List<SummaryRow> summaryRows(WizardState state) { return List.of(); }

    /** Applies this step's configuration. Throws to mark the step as failed on the summary page. */
    default void apply(WizardState state) throws Exception {}
}
