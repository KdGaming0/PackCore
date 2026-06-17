package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.integration.DungeonRoutesManager;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

/**
 * Step: dungeon routes — choose between Skyblocker waypoints and Secret Routes Mod.
 * Only relevant when both providers are present, so the user has a real choice.
 */
public final class DungeonRoutesStep implements WizardStep {

    @Override public String id() { return "dungeon_routes"; }
    @Override public int version() { return 1; }

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("skyblocker")
                && FabricLoader.getInstance().isModLoaded("secretroutesmod");
    }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new DungeonRoutesPage(state, navigator, width, height);
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        return List.of(SummaryRow.single(id(), "Dungeon Routes",
                "gui.packcore.wizard.dungeon_routes.", state.getSelection(DungeonRoutesPage.STATE_KEY)));
    }

    @Override
    public void apply(WizardState state) {
        String selectedId = state.getSelection(DungeonRoutesPage.STATE_KEY);
        if (selectedId == null) return;
        DungeonRoutesManager.DungeonRoutesMode mode = switch (selectedId) {
            case "skyblocker_waypoints" -> DungeonRoutesManager.DungeonRoutesMode.SKYBLOCKER_WAYPOINTS;
            case "secret_routes_mod" -> DungeonRoutesManager.DungeonRoutesMode.SECRET_ROUTES_MOD;
            default -> throw new RuntimeException("Unknown dungeon routes ID: " + selectedId);
        };
        if (!DungeonRoutesManager.apply(mode)) {
            throw new RuntimeException("Failed to apply dungeon routes mode: " + selectedId);
        }
    }
}
