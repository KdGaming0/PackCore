package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.integration.StorageDesignManager;

import java.util.List;

/** Step: storage design — applies a {@link StorageDesignManager.StorageDesign}. */
public final class StorageDesignStep implements WizardStep {

    @Override public String id() { return "storage_design"; }
    @Override public int version() { return 1; }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new StorageDesignPage(state, navigator, width, height);
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        return List.of(SummaryRow.single(id(), "Storage Design",
                "gui.packcore.wizard.storage_design.", state.getSelection(StorageDesignPage.STATE_KEY)));
    }

    @Override
    public void apply(WizardState state) {
        String selectedId = state.getSelection(StorageDesignPage.STATE_KEY);
        if (selectedId == null) return;
        StorageDesignManager.StorageDesign design = switch (selectedId) {
            case "overlay" -> StorageDesignManager.StorageDesign.OVERLAY;
            case "vanilla" -> StorageDesignManager.StorageDesign.VANILLA;
            default -> throw new RuntimeException("Unknown storage design ID: " + selectedId);
        };
        if (!StorageDesignManager.apply(design)) {
            throw new RuntimeException("Failed to apply storage design: " + selectedId);
        }
    }
}
