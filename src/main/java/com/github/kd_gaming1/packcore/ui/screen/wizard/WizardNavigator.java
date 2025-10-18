package com.github.kd_gaming1.packcore.ui.screen.wizard;

import com.github.kd_gaming1.packcore.ui.screen.wizard.pages.*;
import net.minecraft.client.gui.screen.Screen;

/**
 * Manages wizard navigation and page creation
 */
public class WizardNavigator {

    // Update the switch statement in createWizardPage method:
    public static Screen createWizardPage(int pageNumber) {
        return switch (pageNumber) {
            case 1 -> new OptimizationWizardPage();
            case 2 -> new TabDesignWizardPage();
            case 3 -> new ResourcePacksWizardPage();
            case 4 -> new UsefulInfoWizardPage();
            case 5 -> new ApplyConfigurationWizard();
            default -> new WelcomeWizardPage();
        };
    }

    public static void startWizard(net.minecraft.client.MinecraftClient client) {
        client.setScreen(createWizardPage(0));
    }
}