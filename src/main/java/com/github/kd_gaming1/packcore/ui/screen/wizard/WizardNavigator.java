package com.github.kd_gaming1.packcore.ui.screen.wizard;

import com.github.kd_gaming1.packcore.ui.screen.wizard.pages.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry and navigation system for wizard pages.
 * Manages page creation, ordering, and metadata.
 */
public class WizardNavigator {

    /**
     * Metadata for a wizard page including its factory and display information
     */
    public record PageInfo(
            String id,
            Text displayName,
            PageFactory factory,
            boolean optional
    ) {}

    /**
     * Functional interface for creating wizard page instances
     */
    @FunctionalInterface
    public interface PageFactory {
        Screen create();
    }

    // Registry of all wizard pages in order
    private static final Map<Integer, PageInfo> PAGES = new LinkedHashMap<>();

    static {
        // Register all wizard pages with metadata
        registerPage(0, "welcome", Text.literal("Welcome"),
                WelcomeWizardPage::new, false);

        registerPage(1, "optimization", Text.literal("Performance Settings"),
                OptimizationWizardPage::new, false);

        registerPage(2, "tab_design", Text.literal("Tab Design"),
                TabDesignWizardPage::new, true);

        registerPage(3, "item_background", Text.literal("Item Background"),
                ItemBackgroundWizardPage::new, true);

        registerPage(4, "resource_packs", Text.literal("Resource Packs"),
                ResourcePacksWizardPage::new, true);

        registerPage(5, "useful_info", Text.literal("Useful Information"),
                UsefulInfoWizardPage::new, false);

        registerPage(6, "apply", Text.literal("Apply Configuration"),
                ApplyConfigurationWizard::new, false);
    }

    /**
     * Register a wizard page with the navigator
     */
    private static void registerPage(int index, String id, Text displayName,
                                     PageFactory factory, boolean optional) {
        PAGES.put(index, new PageInfo(id, displayName, factory, optional));
    }

    /**
     * Create a wizard page by its index
     * @param pageNumber The page number (0-based)
     * @return The created screen, or WelcomeWizardPage if index is invalid
     */
    public static Screen createWizardPage(int pageNumber) {
        PageInfo info = PAGES.get(pageNumber);
        return info != null ? info.factory().create() : new WelcomeWizardPage();
    }

    /**
     * Get metadata for a specific page
     */
    public static PageInfo getPageInfo(int pageNumber) {
        return PAGES.get(pageNumber);
    }

    /**
     * Get the total number of wizard pages
     */
    public static int getTotalPages() {
        return PAGES.size();
    }

    /**
     * Check if a page is optional (can be skipped)
     */
    public static boolean isPageOptional(int pageNumber) {
        PageInfo info = PAGES.get(pageNumber);
        return info != null && info.optional();
    }

    /**
     * Get the next non-optional page index after the given page
     * @return Next required page index, or -1 if at the end
     */
    public static int getNextRequiredPage(int currentPage) {
        for (int i = currentPage + 1; i < PAGES.size(); i++) {
            PageInfo info = PAGES.get(i);
            if (info != null && !info.optional()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get all registered pages for iteration
     */
    public static Map<Integer, PageInfo> getAllPages() {
        return new LinkedHashMap<>(PAGES);
    }
}