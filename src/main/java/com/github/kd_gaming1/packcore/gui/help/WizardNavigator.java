package com.github.kd_gaming1.packcore.gui.help;

import com.github.kd_gaming1.packcore.gui.help.introduction.*;
import net.minecraft.client.gui.screen.Screen;

/**
 * Manages wizard navigation and page creation
 */
public class WizardNavigator {

    public static Screen createWizardPage(int pageNumber) {
        return switch (pageNumber) {
            case 1 -> new IntroductionScreenPageOne();
            case 2 -> new IntroductionScreenPageTwo();
            case 3 -> new IntroductionScreenPageThree();
            case 4 -> new IntroductionScreenPageFour();
            case 5 -> new IntroductionScreenPageFive();
            default -> new IntroductionScreenPage();
        };
    }

    public static void startWizard(net.minecraft.client.MinecraftClient client) {
        client.setScreen(createWizardPage(0));
    }
}