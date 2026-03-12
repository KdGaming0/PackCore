package com.github.kd_gaming1.packcore.gui.screen;

import com.daqem.uilib.gui.AbstractScreen;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.github.kd_gaming1.packcore.gui.wizard.*;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.wizard.page.*;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import eu.midnightdust.lib.config.MidnightConfig;
import com.github.kd_gaming1.packcore.gui.wizard.page.ConfirmApplyPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class WelcomeWizardScreen extends AbstractScreen {

    private static final int HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 26;
    private static final int PANEL_PADDING = 8;

    private WizardState wizardState;
    private WizardNavigator navigator;

    private WizardHeaderComponent headerComponent;
    private WizardButtonBar buttonBar;
    private ConfirmApplyPage confirmApplyPage;

    private final Screen lastScreen;

    public WelcomeWizardScreen(Screen lastScreen) {
        super(Component.translatable("gui.packcore.wizard.title", ModpackMetadata.getInstance().getModpackName()));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        boolean firstOpen = wizardState == null;

        if (firstOpen) {
            wizardState = new WizardState();
            navigator = new WizardNavigator(wizardState);
            registerPages();
            navigator.initialize();
        } else {
            resizePages();
        }

        buildLayout();
        wireEvents();

        super.init();
    }

    private void registerPages() {
        int contentWidth = width - PANEL_PADDING * 2;
        int contentHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;

        navigator.addPage(new WelcomePage(wizardState, navigator, contentWidth, contentHeight));
        navigator.addPage(new MainMenuDesignPage(wizardState, navigator, contentWidth, contentHeight));
        navigator.addPage(new PerformancePage(wizardState, navigator, contentWidth, contentHeight));
        navigator.addPage(new TabDesignPage(wizardState, navigator, contentWidth, contentHeight));
        navigator.addPage(new ItemBackgroundPage(wizardState, navigator, contentWidth, contentHeight));
        navigator.addPage(new StorageDesignPage(wizardState, navigator, contentWidth, contentHeight));
        navigator.addPage(new ResourcePackPage(wizardState, navigator, contentWidth, contentHeight));

        confirmApplyPage = new ConfirmApplyPage(wizardState, navigator, contentWidth, contentHeight);
        navigator.addPage(confirmApplyPage);
    }

    /** Updates the size of each existing page after a window resize, then re-enters the current page. */
    private void resizePages() {
        int contentWidth = width - PANEL_PADDING * 2;
        int contentHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;

        for (BaseWizardPage page : navigator.getPages()) {
            page.setWidth(contentWidth);
            page.setHeight(contentHeight);
        }

        // Re-enter the current page so it rebuilds its child components at the new size
        navigator.getCurrentPage().onEnter();
    }

    private void buildLayout() {
        headerComponent = new WizardHeaderComponent(0, 0, width, HEADER_HEIGHT, navigator);
        addComponent(headerComponent);

        WizardContentPanel contentPanel = new WizardContentPanel(
                PANEL_PADDING, HEADER_HEIGHT,
                width - PANEL_PADDING * 2,
                height - HEADER_HEIGHT - FOOTER_HEIGHT,
                navigator
        );
        addComponent(contentPanel);

        buttonBar = new WizardButtonBar(navigator, width, FOOTER_HEIGHT);

        EmptyComponent footerWrapper = new EmptyComponent(0, height - FOOTER_HEIGHT, width, FOOTER_HEIGHT);
        footerWrapper.addComponent(buttonBar);
        addComponent(footerWrapper);
    }

    private void wireEvents() {
        // When apply succeeds, unlock the Finish button
        confirmApplyPage.setOnApplySucceeded(() -> buttonBar.setFinishEnabled(true));

        // Finish — settings have been applied; mark complete and close
        buttonBar.setOnFinish(() -> {
            markWizardComplete();
            Minecraft.getInstance().setScreen(lastScreen);
        });

        // Skip on the last page — close without applying; still marks complete
        buttonBar.setOnSkipFinish(() -> {
            markWizardComplete();
            Minecraft.getInstance().setScreen(lastScreen);
        });

        navigator.setOnPageChange(event -> {
            headerComponent.onPageChanged();
            buttonBar.refresh();
        });
    }

    /** Writes the wizard-complete flag to the config and saves it. */
    private void markWizardComplete() {
        PackCoreConfig.successfulWelcomeWizard = true;
        MidnightConfig.write(MOD_ID);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (navigator.hasPrevious()) {
            navigator.previousPage();
            return false;
        }
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }
}