package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.integration.ItemBackgroundManager;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

/** Step: item background — applies an {@link ItemBackgroundManager.ItemBackground}. */
public final class ItemBackgroundStep implements WizardStep {

    @Override public String id() { return "item_background"; }
    @Override public int version() { return 1; }

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("skyblocker");
    }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new ItemBackgroundPage(state, navigator, width, height);
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        return List.of(SummaryRow.single(id(), "Item Background",
                "gui.packcore.wizard.item_background.", state.getSelection(ItemBackgroundPage.STATE_KEY)));
    }

    @Override
    public void apply(WizardState state) {
        String selectedId = state.getSelection(ItemBackgroundPage.STATE_KEY);
        if (selectedId == null) return;
        ItemBackgroundManager.ItemBackground background = switch (selectedId) {
            case "none" -> ItemBackgroundManager.ItemBackground.NONE;
            case "circle" -> ItemBackgroundManager.ItemBackground.CIRCLE;
            case "square" -> ItemBackgroundManager.ItemBackground.SQUARE;
            default -> throw new RuntimeException("Unknown item background ID: " + selectedId);
        };
        if (!ItemBackgroundManager.apply(background)) {
            throw new RuntimeException("Failed to apply item background: " + selectedId);
        }
    }
}
