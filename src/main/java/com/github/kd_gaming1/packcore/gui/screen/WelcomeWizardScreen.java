package com.github.kd_gaming1.packcore.gui.screen;

import com.daqem.uilib.gui.AbstractScreen;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.github.kd_gaming1.packcore.gui.wizard.*;
import com.github.kd_gaming1.packcore.gui.wizard.page.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class WelcomeWizardScreen extends AbstractScreen {

    private static final int HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 26;
    private static final int PANEL_PADDING = 8;

    private WizardState wizardState;
    private WizardNavigator navigator;

    private WizardHeaderComponent headerComponent;
    private WizardContentPanel contentPanel;
    private WizardButtonBar buttonBar;

    @Nullable
    private final Screen lastScreen;

    public WelcomeWizardScreen(@Nullable Screen lastScreen) {
        super(Component.translatable("gui.packcore.wizard.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        wizardState = new WizardState();
        navigator = new WizardNavigator(wizardState);

        registerPages();
        navigator.initialize();

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
        navigator.addPage(new ConfirmApplyPage(wizardState, navigator, contentWidth, contentHeight));
    }

    private void buildLayout() {
        headerComponent = new WizardHeaderComponent(0, 0, width, HEADER_HEIGHT, navigator);
        addComponent(headerComponent);

        contentPanel = new WizardContentPanel(
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
        buttonBar.setOnFinish(() -> Minecraft.getInstance().setScreen(lastScreen));

        navigator.setOnPageChange(event -> {
            headerComponent.onPageChanged();
            buttonBar.refresh();
        });
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

    public WizardState getWizardState() {
        return wizardState;
    }
}