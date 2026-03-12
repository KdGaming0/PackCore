package com.github.kd_gaming1.packcore.gui.wizard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Navigation state machine for the setup wizard.
 *
 * <p>Pages are registered with {@link #addPage(BaseWizardPage)} in order, then
 * {@link #initialize()} activates the first page. {@link #nextPage()} and
 * {@link #previousPage()} guard every transition: they check
 * {@link BaseWizardPage#validate()}, {@link BaseWizardPage#canGoBack()}, and bounds
 * before calling {@link BaseWizardPage#onExit()} / {@link BaseWizardPage#onEnter()}
 * on the outgoing and incoming pages respectively. A {@link PageChangeEvent} is
 * fired after every successful navigation so the host screen can update its
 * header and button bar.
 */
public class WizardNavigator {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/WizardNavigator");

    private final List<BaseWizardPage> pages = new ArrayList<>();
    private final WizardState state;

    private int currentPageIndex = 0;
    private Consumer<PageChangeEvent> onPageChange;

    public WizardNavigator(WizardState state) {
        this.state = state;
    }

    public void addPage(BaseWizardPage page) {
        pages.add(page);
        LOGGER.debug("Added wizard page: {} (total pages: {})", page.getClass().getSimpleName(), pages.size());
    }

    public List<BaseWizardPage> getPages() {
        return Collections.unmodifiableList(pages);
    }

    public int getPageCount() {
        return pages.size();
    }

    public BaseWizardPage getCurrentPage() {
        validateIndex(currentPageIndex);
        return pages.get(currentPageIndex);
    }

    public int getCurrentIndex() {
        return currentPageIndex;
    }

    public int getCurrentPageNumber() {
        return currentPageIndex + 1;
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= pages.size()) {
            throw new IllegalStateException("Invalid page index: " + index);
        }
    }

    public void nextPage() {
        if (!canProceed()) {
            LOGGER.warn("Cannot proceed from page {}", getCurrentPageNumber());
            return;
        }
        navigateTo(currentPageIndex + 1, NavigationDirection.FORWARD);
    }

    public void previousPage() {
        if (!hasPrevious()) {
            LOGGER.warn("Already on first page");
            return;
        }
        if (!getCurrentPage().canGoBack()) {
            LOGGER.warn("Back navigation disabled on page {}", getCurrentPageNumber());
            return;
        }
        navigateTo(currentPageIndex - 1, NavigationDirection.BACKWARD);
    }

    private void navigateTo(int newIndex, NavigationDirection direction) {
        validateIndex(newIndex);

        int previousIndex = currentPageIndex;
        getCurrentPage().onExit();
        currentPageIndex = newIndex;

        BaseWizardPage newPage = getCurrentPage();
        newPage.onEnter();
        newPage.markAsVisited();

        firePageChangeEvent(previousIndex, newIndex, direction);
        LOGGER.debug("Navigated to page {}/{}", getCurrentPageNumber(), getPageCount());
    }

    public boolean hasPrevious() { return currentPageIndex > 0; }
    public boolean hasNext() { return currentPageIndex < pages.size() - 1; }
    public boolean isOnLastPage() { return currentPageIndex == pages.size() - 1; }
    public boolean canProceed() { return hasNext() && getCurrentPage().validate(); }
    public WizardState getState() { return state; }

    public void initialize() {
        if (pages.isEmpty()) throw new IllegalStateException("Cannot initialize wizard with no pages");

        currentPageIndex = 0;
        BaseWizardPage firstPage = getCurrentPage();
        firstPage.onEnter();
        firstPage.markAsVisited();
        LOGGER.info("Wizard initialized with {} pages", pages.size());
    }

    public void setOnPageChange(Consumer<PageChangeEvent> callback) {
        this.onPageChange = callback;
    }

    private void firePageChangeEvent(int from, int to, NavigationDirection direction) {
        if (onPageChange != null) {
            onPageChange.accept(new PageChangeEvent(from, to, direction));
        }
    }

    public record PageChangeEvent(int fromIndex, int toIndex, NavigationDirection direction) { }

    public enum NavigationDirection {
        FORWARD,
        BACKWARD
    }
}