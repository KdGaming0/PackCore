package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.AbstractScreen;
import com.github.kd_gaming1.packcore.gui.util.ImageBackground;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class ConfigScreen extends AbstractScreen {

    private static final int HEADER_HEIGHT = 40;
    private static final int NAV_HEIGHT = 24;
    private static final int PANEL_PADDING = 8;
    private static final int CONTENT_MARGIN = 4;

    private ConfigTab activeTab = ConfigTab.CONFIGURATION;

    private ConfigurationPage configPage;
    private ExportPage exportPage;
    private BackupsPage backupsPage;
    private ImportPage importPage;

    private ConfigContentPanel contentPanel;
    private TabNavBar navBar;

    public ConfigScreen() {
        super(Component.translatable("gui.packcore.config.title"));
        setBackground(new ImageBackground(
                Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/welcome_bg.png"),
                1920, 1080,
                ImageBackground.BackgroundMode.STRETCH
        ));
    }

    @Override
    protected void init() {
        int contentWidth = width;
        int contentHeight = height - HEADER_HEIGHT - NAV_HEIGHT - CONTENT_MARGIN * 2;

        boolean firstInit = configPage == null;

        if (firstInit) {
            configPage = new ConfigurationPage(contentWidth, contentHeight);
            exportPage = new ExportPage(contentWidth, contentHeight);
            backupsPage = new BackupsPage(contentWidth, contentHeight);
            importPage = new ImportPage(contentWidth, contentHeight);
            getCurrentPage().onEnter();
        } else {
            resizePages(contentWidth, contentHeight);
        }

        addComponent(new ConfigScreenHeader(0, 0, width, HEADER_HEIGHT, () -> Minecraft.getInstance().setScreen(null)));

        contentPanel = new ConfigContentPanel(
                PANEL_PADDING, HEADER_HEIGHT,
                width - PANEL_PADDING * 2,
                height - HEADER_HEIGHT - NAV_HEIGHT - CONTENT_MARGIN
        );
        contentPanel.setPage(getCurrentPage());
        addComponent(contentPanel);

        navBar = new TabNavBar(PANEL_PADDING, height - NAV_HEIGHT, width - PANEL_PADDING * 2, NAV_HEIGHT, activeTab, this::switchTab);
        addComponent(navBar);

        super.init();
    }

    private void resizePages(int width, int height) {
        for (BaseConfigPage page : allPages()) {
            page.setWidth(width);
            page.setHeight(height);
        }
        getCurrentPage().onEnter();
    }

    private void switchTab(ConfigTab tab) {
        if (tab == activeTab) return;
        getCurrentPage().onExit();
        activeTab = tab;
        navBar.setActiveTab(tab);
        getCurrentPage().onEnter();
        contentPanel.setPage(getCurrentPage());
    }

    private BaseConfigPage getCurrentPage() {
        return switch (activeTab) {
            case CONFIGURATION -> configPage;
            case EXPORT -> exportPage;
            case BACKUPS -> backupsPage;
            case IMPORT -> importPage;
        };
    }

    private BaseConfigPage[] allPages() {
        return new BaseConfigPage[]{ configPage, exportPage, backupsPage, importPage };
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return activeTab != ConfigTab.BACKUPS || !backupsPage.handleEsc();
    }
}