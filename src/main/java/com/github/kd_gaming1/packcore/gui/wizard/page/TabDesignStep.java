package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.integration.TabDesignManager;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

/** Step: tab design — applies a {@link TabDesignManager.TabDesign}. */
public final class TabDesignStep implements WizardStep {

    @Override public String id() { return "tab_design"; }
    @Override public int version() { return 1; }

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("skyblocker")
                || FabricLoader.getInstance().isModLoaded("skyhanni");
    }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new TabDesignPage(state, navigator, width, height);
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        return List.of(SummaryRow.single(id(), "Tab Design",
                "gui.packcore.wizard.tab_design.", state.getSelection(TabDesignPage.STATE_KEY)));
    }

    @Override
    public void apply(WizardState state) {
        String selectedId = state.getSelection(TabDesignPage.STATE_KEY);
        if (selectedId == null) return;
        TabDesignManager.TabDesign design = switch (selectedId) {
            case "compact" -> TabDesignManager.TabDesign.COMPACT;
            case "fancy" -> TabDesignManager.TabDesign.FANCY;
            default -> throw new RuntimeException("Unknown tab design ID: " + selectedId);
        };
        if (!TabDesignManager.apply(design)) throw new RuntimeException("Failed to apply tab design: " + selectedId);
    }
}
