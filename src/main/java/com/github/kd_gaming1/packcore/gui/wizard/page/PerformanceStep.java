package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.integration.PerformanceProfileService;

import java.util.List;

/** Step: performance profile — applies a {@link PerformanceProfileService.PerformanceProfile}. */
public final class PerformanceStep implements WizardStep {

    @Override public String id() { return "performance"; }
    @Override public int version() { return 1; }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new PerformancePage(state, navigator, width, height);
    }

    @Override
    public List<SummaryRow> summaryRows(WizardState state) {
        return List.of(SummaryRow.single(id(), "Performance Profile",
                "gui.packcore.wizard.performance.", state.getSelection(PerformancePage.STATE_KEY)));
    }

    @Override
    public void apply(WizardState state) {
        String selectedId = state.getSelection(PerformancePage.STATE_KEY);
        if (selectedId == null) return;
        PerformanceProfileService.PerformanceProfile profile = switch (selectedId) {
            case "maxfps" -> PerformanceProfileService.PerformanceProfile.PERFORMANCE;
            case "balanced" -> PerformanceProfileService.PerformanceProfile.BALANCED;
            case "quality" -> PerformanceProfileService.PerformanceProfile.QUALITY;
            case "quality_performance_shaders" -> PerformanceProfileService.PerformanceProfile.SHADERS_PERFORMANCE;
            case "quality_quality_shaders" -> PerformanceProfileService.PerformanceProfile.SHADERS_QUALITY;
            default -> throw new RuntimeException("Unknown profile ID: " + selectedId);
        };
        if (!PerformanceProfileService.applyAll(profile)) {
            throw new RuntimeException("One or more integrations failed for profile: " + profile.getDisplayName());
        }
    }
}
