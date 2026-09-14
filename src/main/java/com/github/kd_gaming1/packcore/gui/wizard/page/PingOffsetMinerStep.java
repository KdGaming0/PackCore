package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

/** Uses the optional mod's live config API without adding a required dependency. */
public final class PingOffsetMinerStep implements WizardStep {
    @Override public String id() { return "ping_offset_miner"; }
    @Override public int version() { return 1; }

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("ping-offset-miner");
    }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new PingOffsetMinerPage(state, navigator, width, height);
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        return List.of(SummaryRow.single(id(), "Ping Offset Miner",
                "gui.packcore.wizard.ping_offset_miner.", state.getSelection(PingOffsetMinerPage.STATE_KEY)));
    }

    private static Object feature() throws ReflectiveOperationException {
        return Class.forName("pom.rewrite.features.PingOffsetMiner").getField("instance").get(null);
    }

    static boolean isEnabled() throws ReflectiveOperationException {
        Object feature = feature();
        return (boolean) feature.getClass().getMethod("isEnabled").invoke(feature);
    }

    @Override
    public void apply(WizardState state) {
        String selected = state.getSelection(PingOffsetMinerPage.STATE_KEY);
        if (selected == null) return;
        try {
            Object feature = feature();
            feature.getClass().getMethod("setEnabled", boolean.class).invoke(feature, selected.equals("enabled"));
            Class.forName("pom.rewrite.config.ConfigHandler").getMethod("save").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to apply Ping Offset Miner setting", e);
        }
    }
}
