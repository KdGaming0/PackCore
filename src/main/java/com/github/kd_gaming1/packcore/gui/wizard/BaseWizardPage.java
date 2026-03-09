package com.github.kd_gaming1.packcore.gui.wizard;

import com.daqem.uilib.gui.component.EmptyComponent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for all wizard pages.
 * Each step extends this and implements the abstract methods below.
 */
public abstract class BaseWizardPage extends EmptyComponent {

    protected final WizardState state;
    protected final WizardNavigator navigator;

    private final List<Component> validationErrors = new ArrayList<>();
    private boolean visited = false;

    public BaseWizardPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(0, 0, width, height);
        this.state = state;
        this.navigator = navigator;
    }

    // --- Abstract methods ---

    /** The title shown at the top of this page. */
    public abstract Component getTitle();

    /** Optional subtitle shown below the title. Returns null by default. */
    public Component getSubtitle() {
        return null;
    }

    /**
     * Returns true if the user can proceed from this page.
     * Call {@link #addValidationError} to report errors, then return false.
     * Use {@link #runValidation()} instead of calling this directly.
     */
    public abstract boolean validate();

    /**
     * Called when this page becomes active. Build your UI components here.
     * May be called multiple times if the user navigates back and forth.
     */
    public abstract void onEnter();

    /** Called when the user navigates away. Use for cleanup or saving state. */
    public abstract void onExit();

    // --- Validation ---

    public List<Component> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }

    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    protected void clearValidationErrors() {
        validationErrors.clear();
    }

    protected void addValidationError(Component error) {
        validationErrors.add(error);
    }

    /** Clears previous errors, then runs validation. Always use this instead of calling validate() directly. */
    public final boolean runValidation() {
        clearValidationErrors();
        return validate();
    }

    // --- Visited state ---

    public boolean hasBeenVisited() {
        return visited;
    }

    public void markAsVisited() {
        this.visited = true;
    }

    public void resetVisited() {
        this.visited = false;
    }

    // --- Navigation flags ---

    /** Returns true if this page can be skipped. Override to make a page optional. */
    public boolean canSkip() {
        return false;
    }

    /** Returns true if the user can navigate back from this page. */
    public boolean canGoBack() {
        return true;
    }
}