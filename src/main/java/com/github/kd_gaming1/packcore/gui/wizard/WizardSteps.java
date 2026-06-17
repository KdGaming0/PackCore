package com.github.kd_gaming1.packcore.gui.wizard;

import com.github.kd_gaming1.packcore.gui.wizard.page.CaxtonFontStep;
import com.github.kd_gaming1.packcore.gui.wizard.page.ItemBackgroundStep;
import com.github.kd_gaming1.packcore.gui.wizard.page.MainMenuDesignStep;
import com.github.kd_gaming1.packcore.gui.wizard.page.PerformanceStep;
import com.github.kd_gaming1.packcore.gui.wizard.page.ResourcePackStep;
import com.github.kd_gaming1.packcore.gui.wizard.page.ScamScreenerStep;
import com.github.kd_gaming1.packcore.gui.wizard.page.StorageDesignStep;
import com.github.kd_gaming1.packcore.gui.wizard.page.SwordBlockStep;
import com.github.kd_gaming1.packcore.gui.wizard.page.TabDesignStep;

import java.util.List;

/**
 * The single registration point for configurable wizard steps.
 *
 * <p>List order is both the page order shown to the user and the order {@link WizardStep#apply}
 * runs in. The intro {@code WelcomePage} and the {@code ConfirmApplyPage} are added by the screen
 * and are not listed here.
 *
 * <p>Add a page → add its {@code XxxStep} to {@link #ALL}. Remove a page → delete that line and the
 * step's two files.
 */
public final class WizardSteps {

    private WizardSteps() {}

    private static final List<WizardStep> ALL = List.of(
            new MainMenuDesignStep(),
            new PerformanceStep(),
            new TabDesignStep(),
            new ItemBackgroundStep(),
            new StorageDesignStep(),
            new SwordBlockStep(),
            new ScamScreenerStep(),
            new CaxtonFontStep(),
            // ResourcePackStep applies last: the Caxton step folds its chosen font pack into the
            // resource-pack selection, which this step then applies in one pass.
            new ResourcePackStep()
    );

    public static List<WizardStep> all() {
        return ALL;
    }

    /** Steps whose required mods/environment are present. */
    public static List<WizardStep> available() {
        return ALL.stream().filter(WizardStep::isAvailable).toList();
    }

    /** The available step with the given id, or {@code null} if none matches. */
    public static WizardStep byId(String id) {
        return ALL.stream()
                .filter(step -> step.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    /** Available steps the user has not yet applied at their current version. */
    public static List<WizardStep> pending(WizardVersionStore store) {
        return available().stream().filter(store::isPending).toList();
    }

    /** Convenience overload that loads the store from disk. */
    public static List<WizardStep> pending() {
        return pending(WizardVersionStore.load());
    }
}
