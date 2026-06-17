package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.integration.ScamScreenerConfigurator;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Step: ScamScreener — minimum alert level plus optional ping toggles. */
public final class ScamScreenerStep implements WizardStep {

    @Override public String id() { return "scam_screener"; }
    @Override public int version() { return 1; }

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("scamscreener");
    }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new ScamScreenerPage(state, navigator, width, height);
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        List<SummaryRow> rows = new ArrayList<>();

        String alertLevel = state.getSelection(ScamScreenerPage.ALERT_LEVEL_KEY);
        boolean alertSkipped = alertLevel == null;
        Component alertValue = alertSkipped
                ? Component.literal("Skipped")
                : ScamScreenerPage.labelForAlertLevel(alertLevel);
        rows.add(SummaryRow.of(id(), "ScamScreener Alerts", alertValue, alertSkipped));

        Set<String> pings = state.getMultiSelection(ScamScreenerPage.PING_OPTIONS_KEY);
        rows.add(SummaryRow.of(id(), "ScamScreener Pings",
                pings.isEmpty() ? Component.literal("None selected") : Component.literal(pings.size() + " selected"),
                pings.isEmpty()));
        for (String optionId : pings.stream().sorted(Comparator.naturalOrder()).toList()) {
            rows.add(SummaryRow.sub(id(), ScamScreenerPage.labelForPingOption(optionId)));
        }

        return rows;
    }

    @Override
    public void apply(WizardState state) {
        String alertLevel = state.getSelection(ScamScreenerPage.ALERT_LEVEL_KEY);
        Set<String> pings = state.getMultiSelection(ScamScreenerPage.PING_OPTIONS_KEY);
        if (alertLevel == null && pings.isEmpty()) return;

        String riskLevel = alertLevel != null
                ? alertLevel
                : ScamScreenerConfigurator.defaultSettings().minimumRiskLevel();
        if (!ScamScreenerConfigurator.apply(riskLevel,
                pings.contains("risk_warning"),
                pings.contains("blacklist_warning"))) {
            throw new RuntimeException("Failed to update ScamScreener settings");
        }
    }
}
