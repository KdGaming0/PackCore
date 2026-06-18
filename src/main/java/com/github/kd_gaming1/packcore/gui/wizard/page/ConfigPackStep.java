package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.github.kd_gaming1.packcore.PackCorePreLaunch;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.update.UpdateChecker;

public class ConfigPackStep implements WizardStep {

    @Override
    public String id() {
        return "config_packs";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public BaseWizardPage createPage(WizardState state, WizardNavigator navigator, int width, int height) {
        if (isPre5Upgrade()) {
            return new ConfigPackReapplyPage(state, navigator, width, height);
        }
        return new ConfigPackPage(state, navigator, width, height);
    }

    private static boolean isPre5Upgrade() {
        String previous = PackCorePreLaunch.getPreviousModpackVersion();
        if (previous == null || previous.isBlank() || "Unknown".equals(previous)) {
            return false;
        }
        if (!UpdateChecker.isNewerVersion("5.0.0", previous)) {
            return false;
        }
        return PackCoreConfig.lastAppliedPackFile != null && !PackCoreConfig.lastAppliedPackFile.isBlank();
    }
}
