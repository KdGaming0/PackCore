package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.integration.ResourcePackManager;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Step: resource packs — enables the user's chosen packs. Runs last so the Caxton font pack folded
 * into the selection by {@link CaxtonFontStep} is applied in the same pass.
 */
public final class ResourcePackStep implements WizardStep {

    @Override public String id() { return "resource_packs"; }
    @Override public int version() { return 2; }

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
        List<String> packs = state.getResourcePackOrder();
        List<SummaryRow> rows = new ArrayList<>();
        rows.add(SummaryRow.of(id(), "Resource Packs",
                packs.isEmpty() ? Component.literal("None selected") : Component.literal(packs.size() + " selected"),
                packs.isEmpty()));
        // Numbered in priority order (1 = highest priority / top of the list).
        for (int i = 0; i < packs.size(); i++) {
            rows.add(SummaryRow.sub(id(), Component.literal((i + 1) + ". " + packs.get(i))));
        }
        return rows;
    }

    @Override
    public void apply(WizardState state) {
        // Strip every managed pack from the current selection so the append order below is fully
        // authoritative: unchecked user-selectable packs stay gone (disabled), checked ones are
        // re-added in the chosen order. Non-user-selectable packs (core mod/config packs) are never
        // excluded, so they are preserved.
        Set<String> excludes = new HashSet<>(ResourcePackManager.availableUserSelectablePackIds());

        // Caxton font packs are removed too; the Caxton step re-adds the chosen one in the same pass.
        CaxtonFontPage.FontOption.all().stream()
                .map(CaxtonFontPage.FontOption::packId)
                .filter(Objects::nonNull)
                .forEach(excludes::add);

        // apply() appends in order and the last entry wins, so reverse the priority-ordered selection
        // (top = highest priority) to put the top pack last.
        List<String> priorityOrder = state.getResourcePackOrder();
        List<String> appendOrder = new ArrayList<>(priorityOrder);
        Collections.reverse(appendOrder);

        ResourcePackManager.apply(appendOrder, excludes);
    }
}
