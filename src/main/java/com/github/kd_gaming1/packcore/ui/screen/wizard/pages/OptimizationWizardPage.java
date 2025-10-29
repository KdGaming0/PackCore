package com.github.kd_gaming1.packcore.ui.screen.wizard.pages;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.ui.screen.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.util.wizard.WizardDataStore;
import com.github.kd_gaming1.packcore.ui.screen.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.ui.screen.components.WizardUIComponents;
import com.github.kd_gaming1.packcore.util.markdown.MarkdownService;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Example of a simplified wizard page using the new component system.
 * This shows the optimization profile selection page.
 */
public class OptimizationWizardPage extends BaseWizardPage {

    private static final String FALLBACK_CONTENT = """
            # Optimization Profile
            
            Choose your preferred performance settings.
            Find and edit this content in `rundir/packcore/wizard_markdown_content/optimisation.md`
            """;

    /**
     * Configuration for optimization profiles
     */
    private record ProfileOption(
            String key,
            String icon,
            String title,
            String description
    ) {}

    private static final List<ProfileOption> PROFILES = List.of(
            new ProfileOption("Max FPS", "🚀", "Maximum FPS",
                    "Highest frame rates. Reduces visual quality for smooth gameplay."),
            new ProfileOption("Balanced", "⚖", "Balanced",
                    "Best of both. Good FPS while keeping important visuals."),
            new ProfileOption("Quality", "💎", "Visual Quality",
                    "Beautiful graphics. Requires good hardware."),
            new ProfileOption("Shaders", "✨", "Shaders Enabled",
                    "Ultimate visuals. Needs high-end system.")
    );

    private final String markdownContent;
    private final WizardDataStore dataStore;

    private String selectedProfile;
    private LabelComponent headerTitle;
    private FlowLayout rightPanel;

    public OptimizationWizardPage() {
        super(
                new WizardPageInfo(
                        Text.literal("FPS OR QUALITY???"),
                        1,
                        6
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        // Load markdown content
        MarkdownService markdownService = new MarkdownService();
        this.markdownContent = markdownService.getOrDefault("optimisation.md", FALLBACK_CONTENT);

        // Initialize data store
        this.dataStore = WizardDataStore.getInstance();
        this.selectedProfile = dataStore.getOptimizationProfile();
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        // Create header using component factory
        contentContainer.child(
                WizardUIComponents.createHeader(
                        "Choose your preferred optimisation profile for",
                        "Please read the information below carefully before continuing."
                )
        );

        // Create markdown section using component factory
        contentContainer.child(
                WizardUIComponents.createMarkdownScroll(markdownContent)
        );
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {
        this.rightPanel = contentContainerRight;

        // Create selection header
        rightPanel.child(createSelectionHeader());

        // Create profile options
        rightPanel.child(createProfileOptions());
    }

    /**
     * Create the selection status header
     */
    private FlowLayout createSelectionHeader() {
        FlowLayout header = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .margins(Insets.of(8, 0, 8, 8));

        // Create status label based on selection
        if (selectedProfile.isEmpty()) {
            headerTitle = WizardUIComponents.createStatusLabel(
                    "Select your performance profile below",
                    "👇",
                    ACCENT_SECONDARY
            );
        } else {
            ProfileOption selected = findProfile(selectedProfile);
            String icon = selected != null ? selected.icon() + " " : "";
            headerTitle = WizardUIComponents.createStatusLabel(
                    "Selected: " + icon + selectedProfile,
                    "✓",
                    SUCCESS_BORDER
            );
        }

        header.child(headerTitle);
        return header;
    }

    /**
     * Create scrollable profile options
     */
    private ScrollContainer<FlowLayout> createProfileOptions() {
        FlowLayout profilesLayout = Containers.verticalFlow(Sizing.fill(96), Sizing.content())
                .gap(8);

        // Create option boxes using component factory
        for (ProfileOption profile : PROFILES) {
            boolean isSelected = profile.key().equals(selectedProfile);

            FlowLayout optionBox = WizardUIComponents.createOptionBox(
                    profile.icon(),
                    profile.title(),
                    profile.description(),
                    isSelected,
                    (box) -> selectProfile(profile.key())
            );

            profilesLayout.child(optionBox);
        }

        // Create scroll container
        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                profilesLayout
        );

        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollbarThiccness(6);
        scrollContainer.surface(Surface.flat(0x40_000000).and(Surface.outline(0x30_FFFFFF)));
        scrollContainer.padding(Insets.of(6));
        scrollContainer.margins(Insets.bottom(4));

        return scrollContainer;
    }

    /**
     * Handle profile selection
     */
    private void selectProfile(String profileKey) {
        selectedProfile = profileKey;
        dataStore.setOptimizationProfile(profileKey);

        // Update header with selection
        if (headerTitle != null) {
            ProfileOption selected = findProfile(profileKey);
            String icon = selected != null ? selected.icon() + " " : "";

            headerTitle.text(
                    Text.literal("✓ Selected: " + icon + profileKey)
                            .setStyle(Style.EMPTY.withBold(true))
            ).color(Color.ofRgb(SUCCESS_BORDER));
        }

        // Refresh profile boxes to update selection state
        rebuildRightPanel();
    }

    /**
     * Rebuild the right panel to update selection states
     */
    private void rebuildRightPanel() {
        rightPanel.clearChildren();
        rightPanel.child(createSelectionHeader());
        rightPanel.child(createProfileOptions());
    }

    /**
     * Find a profile by key
     */
    private ProfileOption findProfile(String key) {
        return PROFILES.stream()
                .filter(p -> p.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void onContinuePressed() {
        assert this.client != null;
        this.client.setScreen(WizardNavigator.createWizardPage(2));
    }

    @Override
    protected int getContentColumnWidthPercent() {
        return 55;
    }

    @Override
    protected boolean shouldShowStatusInfo() {
        return false;
    }

    @Override
    protected boolean shouldShowRightPanel() {
        return true;
    }
}