package com.github.kd_gaming1.packcore.gui.wizard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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

    public boolean nextPage() {
        if (!canProceed()) {
            LOGGER.warn("Cannot proceed from page {}", getCurrentPageNumber());
            return false;
        }
        return navigateTo(currentPageIndex + 1, NavigationDirection.FORWARD);
    }

    public boolean previousPage() {
        if (!hasPrevious()) {
            LOGGER.warn("Already on first page");
            return false;
        }
        if (!getCurrentPage().canGoBack()) {
            LOGGER.warn("Back navigation disabled on page {}", getCurrentPageNumber());
            return false;
        }
        return navigateTo(currentPageIndex - 1, NavigationDirection.BACKWARD);
    }

    public boolean jumpToPage(int index) {
        if (index < 0 || index >= pages.size()) {
            LOGGER.warn("Jump target {} out of bounds", index);
            return false;
        }
        if (index == currentPageIndex) return true;

        NavigationDirection direction = index > currentPageIndex
                ? NavigationDirection.FORWARD
                : NavigationDirection.BACKWARD;

        return navigateTo(index, direction);
    }

    private boolean navigateTo(int newIndex, NavigationDirection direction) {
        validateIndex(newIndex);

        int previousIndex = currentPageIndex;
        getCurrentPage().onExit();
        currentPageIndex = newIndex;

        BaseWizardPage newPage = getCurrentPage();
        newPage.onEnter();
        newPage.markAsVisited();

        firePageChangeEvent(previousIndex, newIndex, direction);
        LOGGER.debug("Navigated to page {}/{}", getCurrentPageNumber(), getPageCount());
        return true;
    }

    public boolean hasPrevious() { return currentPageIndex > 0; }
    public boolean hasNext() { return currentPageIndex < pages.size() - 1; }
    public boolean isOnFirstPage() { return currentPageIndex == 0; }
    public boolean isOnLastPage() { return currentPageIndex == pages.size() - 1; }
    public boolean canProceed() { return hasNext() && getCurrentPage().validate(); }
    public boolean canSkip() { return hasNext() && getCurrentPage().canSkip(); }
    public int getLastPageIndex() { return pages.size() - 1; }
    public WizardState getState() { return state; }

    public void initialize() {
        if (pages.isEmpty()) throw new IllegalStateException("Cannot initialize wizard with no pages");

        currentPageIndex = 0;
        BaseWizardPage firstPage = getCurrentPage();
        firstPage.onEnter();
        firstPage.markAsVisited();
        LOGGER.info("Wizard initialized with {} pages", pages.size());
    }

    public void reset() {
        if (!pages.isEmpty()) getCurrentPage().onExit();

        currentPageIndex = 0;
        state.reset();
        for (BaseWizardPage page : pages) page.resetVisited();

        if (!pages.isEmpty()) {
            BaseWizardPage firstPage = getCurrentPage();
            firstPage.onEnter();
            firstPage.markAsVisited();
        }

        LOGGER.info("Wizard reset");
    }

    public void setOnPageChange(Consumer<PageChangeEvent> callback) {
        this.onPageChange = callback;
    }

    private void firePageChangeEvent(int from, int to, NavigationDirection direction) {
        if (onPageChange != null) {
            onPageChange.accept(new PageChangeEvent(from, to, direction));
        }
    }

    public record PageChangeEvent(int fromIndex, int toIndex, NavigationDirection direction) {
        public int getFromPageNumber() { return fromIndex + 1; }
        public int getToPageNumber() { return toIndex + 1; }
    }

    public enum NavigationDirection {
        FORWARD,
        BACKWARD
    }
}