package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Step: Caxton font — the chosen font pack is folded into the resource-pack selection so the
 * {@link ResourcePackStep} (which runs after this one) applies it together with any other packs.
 */
public final class CaxtonFontStep implements WizardStep {

    @Override public String id() { return "caxton_font"; }
    @Override public int version() { return 1; }

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("caxton");
    }

    // The chosen font pack is applied by the resource-pack step, so it must run too.
    @Override
    public List<String> requires() {
        return List.of("resource_packs");
    }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new CaxtonFontPage(state, navigator, width, height);
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        String caxtonId = state.getSelection(CaxtonFontPage.STATE_KEY);
        if (caxtonId == null) {
            caxtonId = CaxtonFontPage.FontOption.NONE_ID;
            state.setSelection(CaxtonFontPage.STATE_KEY, caxtonId);
        }

        boolean none = CaxtonFontPage.FontOption.NONE_ID.equals(caxtonId);
        Component value = none
                ? Component.translatable("gui.packcore.wizard.caxton_font.none.name")
                : Component.translatable("gui.packcore.wizard.caxton_font." + caxtonId + ".name");
        return List.of(SummaryRow.of(id(), "Font", value, none));
    }

    @Override
    public void apply(WizardState state) {
        // Always clear any existing Caxton packs from the selection first.
        CaxtonFontPage.FontOption.all().forEach(opt -> {
            if (opt.packId() != null) state.removeResourcePack(opt.packId());
        });

        String selectedId = state.getSelection(CaxtonFontPage.STATE_KEY);
        if (selectedId == null || CaxtonFontPage.FontOption.NONE_ID.equals(selectedId)) return;

        CaxtonFontPage.FontOption.all().stream()
                .filter(opt -> opt.id().equals(selectedId))
                .findFirst()
                .ifPresent(opt -> {
                    if (opt.packId() != null) state.addResourcePack(opt.packId());
                });
    }
}
