package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;

public class SupportWelcomeStep implements WizardStep {

    @Override
    public String id() {
        return "support_welcome";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        return new SupportWelcomePage(state, navigator, width, height);
    }
}
