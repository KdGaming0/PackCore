package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/** Step: main menu design — applies the chosen {@link PackCoreConfig.MenuStyle}. */
public final class MainMenuDesignStep implements WizardStep {

    @Override public String id() { return "main_menu_design"; }
    @Override public int version() { return 1; }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new MainMenuDesignPage(state, navigator, width, height);
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        return List.of(SummaryRow.single(id(), "Main Menu Design",
                "gui.packcore.wizard.menu_design.", state.getSelection(MainMenuDesignPage.STATE_KEY)));
    }

    @Override
    public void apply(WizardState state) {
        String selectedId = state.getSelection(MainMenuDesignPage.STATE_KEY);
        if (selectedId == null) return;
        PackCoreConfig.menuStyle = switch (selectedId) {
            case "modern" -> PackCoreConfig.MenuStyle.MODERN;
            case "modern_minimal" -> PackCoreConfig.MenuStyle.MODERN_MINIMAL;
            case "minimal" -> PackCoreConfig.MenuStyle.MINIMAL;
            default -> throw new RuntimeException("Unknown menu design ID: " + selectedId);
        };
        MidnightConfig.write(MOD_ID);
    }
}
