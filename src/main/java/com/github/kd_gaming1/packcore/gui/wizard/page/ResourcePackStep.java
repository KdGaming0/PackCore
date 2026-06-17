package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.integration.ResourcePackManager;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Step: resource packs — enables the user's chosen packs. Runs last so the Caxton font pack folded
 * into the selection by {@link CaxtonFontStep} is applied in the same pass.
 */
public final class ResourcePackStep implements WizardStep {

    @Override public String id() { return "resource_packs"; }
    @Override public int version() { return 1; }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new ResourcePackPage(state, navigator, width, height);
    }

    // Applying packs clears Caxton font packs (the Caxton step re-adds the chosen one), so when
    // Caxton is present that step must run too — otherwise an active font would be silently removed.
    @Override
    public List<String> requires() {
        return List.of("caxton_font");
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        Set<String> packs = state.getSelectedResourcePacks();
        List<SummaryRow> rows = new ArrayList<>();
        rows.add(SummaryRow.of(id(), "Resource Packs",
                packs.isEmpty() ? Component.literal("None selected") : Component.literal(packs.size() + " selected"),
                packs.isEmpty()));
        for (String packId : packs) {
            rows.add(SummaryRow.sub(id(), Component.literal(packId)));
        }
        return rows;
    }

    @Override
    public void apply(WizardState state) {
        Set<String> caxtonPackIds = CaxtonFontPage.FontOption.all().stream()
                .map(CaxtonFontPage.FontOption::packId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        ResourcePackManager.apply(state.getSelectedResourcePacks(), caxtonPackIds);
    }
}
