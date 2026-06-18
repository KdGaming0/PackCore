package com.github.kd_gaming1.packcore.gui.screen;

import com.daqem.uilib.gui.AbstractScreen;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardButtonBar;
import com.github.kd_gaming1.packcore.gui.wizard.WizardContentPanel;
import com.github.kd_gaming1.packcore.gui.wizard.WizardHeaderComponent;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.gui.wizard.WizardSteps;
import com.github.kd_gaming1.packcore.gui.wizard.WizardVersionStore;
import com.github.kd_gaming1.packcore.gui.wizard.page.ConfirmApplyPage;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private final List<WizardStep> runSteps;

    private WelcomeWizardScreen(Screen lastScreen, List<WizardStep> runSteps) {
        super(Component.translatable("gui.packcore.wizard.title", ModpackMetadata.getInstance().getModpackName()));
        this.lastScreen = lastScreen;
        this.runSteps = runSteps;
    }

    /** The full wizard for new users: every available step + Confirm &amp; Apply. */
    public static WelcomeWizardScreen full(Screen lastScreen) {
        return new WelcomeWizardScreen(lastScreen, WizardSteps.available());
    }

    /**
     * A focused wizard showing only the given steps (plus Confirm &amp; Apply). Used by the
     * post-update "new pages" flow and the {@code /packcore wizard <id>} command. Unknown or
     * unavailable ids are dropped.
     */
    public static WelcomeWizardScreen forSteps(Screen lastScreen, List<String> stepIds) {
        // Pull in coupled steps (declared via WizardStep#requires) so cross-step state — e.g. the
        // Caxton font folded into the resource-pack selection — is always applied as a unit.
        Set<String> wanted = new LinkedHashSet<>(stepIds);
        Deque<String> pending = new ArrayDeque<>(stepIds);
        while (!pending.isEmpty()) {
            WizardStep step = WizardSteps.byId(pending.poll());
            if (step == null) continue;
            for (String required : step.requires()) {
                if (wanted.add(required)) pending.add(required);
            }
        }

        // Keep registry order (= apply order) and drop unavailable steps.
        List<WizardStep> resolved = WizardSteps.available().stream()
                .filter(step -> wanted.contains(step.id()))
                .toList();
        return new WelcomeWizardScreen(lastScreen, resolved);
    }

    @Override
    protected void init() {
        boolean firstOpen = wizardState == null;

        if (firstOpen) {
            wizardState = new WizardState();
            wizardState.migratedFromV3 = PackCore.migratedFromV3;
            navigator = new WizardNavigator(wizardState);
            registerPages();
            navigator.initialize();
            navigator.getPages().forEach(BaseWizardPage::preloadAssets);
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

        for (WizardStep step : runSteps) {
            navigator.addPage(step.createPage(wizardState, navigator, contentWidth, contentHeight));
        }

        confirmApplyPage = new ConfirmApplyPage(wizardState, navigator, contentWidth, contentHeight, runSteps);
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
        // Re-apply persisted confirm state after screen init/reload.
        buttonBar.setFinishEnabled(confirmApplyPage.isApplyCompleted());

        // Finish — settings have been applied; mark complete and close
        buttonBar.setOnFinish(() -> {
            markWizardComplete();
            boolean openedFromTitle = lastScreen instanceof TitleScreen;
            if (openedFromTitle) {
                Minecraft.getInstance().setScreen(resolvePostWizardScreen());
            } else {
                Minecraft.getInstance().setScreen(lastScreen);
            }
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

    /** Records every shown step as applied at its current version, on Finish or Skip. */
    private void markWizardComplete() {
        WizardVersionStore.load().markApplied(runSteps);
    }

    private Screen resolvePostWizardScreen() {
        return switch (PackCoreConfig.menuStyle) {
            case MODERN -> new SBETitleScreen();
            case MODERN_MINIMAL -> new SBETitleScreen(false);
            case MINIMAL -> new PackCoreTitleScreen();
        };
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Block screen closure when there are previous pages — keyPressed handles the actual navigation.
        return !navigator.hasPrevious();
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE && navigator.hasPrevious()) {
            navigator.previousPage();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }
}
