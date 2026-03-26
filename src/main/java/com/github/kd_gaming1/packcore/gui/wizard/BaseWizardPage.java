package com.github.kd_gaming1.packcore.gui.wizard;

import com.daqem.uilib.gui.component.EmptyComponent;
import net.minecraft.network.chat.Component;

/**
 * Abstract base class for all wizard pages.
 * Each step extends this and implements the abstract methods below.
 */
public abstract class BaseWizardPage extends EmptyComponent {

    protected final WizardState state;
    protected final WizardNavigator navigator;

    public BaseWizardPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(0, 0, width, height);
        this.state = state;
        this.navigator = navigator;
    }

    /** The title shown at the top of this page. */
    public abstract Component getTitle();

    /** Returns true if the user can proceed from this page. */
    public abstract boolean validate();

    /** Called when this page becomes active. Build your UI components here. */
    public abstract void onEnter();

    /** Called when the user navigates away. Use for cleanup or saving state. */
    public abstract void onExit();

    /**
     * Called when the user clicks the Continue/Next button, before navigation occurs.
     *
     * <p>Return {@code false} to block navigation — for example, to show a warning overlay.
     * The page is responsible for any UI changes (showing a dialog) before returning false.
     * Default: always allow.
     */
    public boolean onContinueAttempted() { return true; }

    // Navigation flags
    public boolean canGoBack() { return true; }

    // Visited state
    public void markAsVisited() {}

    /** Called once at wizard init to pre-load any GPU resources. Default no-op. */
    public void preloadAssets() {}
}