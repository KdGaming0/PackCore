package com.github.kd_gaming1.packcore.gui.help.introduction;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.help.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.help.WizardDataManager;
import com.github.kd_gaming1.packcore.gui.help.WizardNavigator;
import com.github.kd_gaming1.packcore.util.MarkdownFileUtil;
import com.github.kd_gaming1.packcore.util.ModpackInfo;
import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.lavendermd.feature.*;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IntroductionScreenPageOne extends BaseWizardPage {
    private static final MarkdownProcessor<ParentComponent> MARKDOWN_PROCESSOR =
            new MarkdownProcessor<>(
                    OwoUICompiler::new,
                    new BasicFormattingFeature(),
                    new ColorFeature(),
                    new LinkFeature(),
                    new ListFeature(),
                    new BlockQuoteFeature(),
                    new ImageFeature()
            );

    private static final Map<String, ParentComponent> COMPONENT_CACHE = new ConcurrentHashMap<>();

    private final String welcomeMarkdown;
    private final ModpackInfo modpackInfo;

    private String selectedOptimisationProfile = "";
    LabelComponent headerTitle;
    private FlowLayout rightPanel;

    private static final int SELECTED_OUTLINE_COLOR = 0xFF00FF00;
    private static final int UNSELECTED_OUTLINE_COLOR = ACCENT_GOLD;

    public record OptionProfile(String key, String title, String description) {
    }

    // The selectable options
    private final List<IntroductionScreenPageOne.OptionProfile> allProfiles = List.of(
            new IntroductionScreenPageOne.OptionProfile("Max FPS", "Profile: Max FPS", "Maximizes performance by reducing visual effects and render distances. Uses fast graphics mode, minimal particles, and optimized settings for the highest possible frame rates. Perfect for older hardware."),
            new IntroductionScreenPageOne.OptionProfile("Balanced", "Profile: Balanced", "Provides an optimal balance between performance and visual quality. Maintains good frame rates while preserving important visual features like shadows and fancy graphics. Ideal for most gaming scenarios and hardware configurations."),
            new IntroductionScreenPageOne.OptionProfile("Quality", "Profile: Quality", "Prioritizes visual fidelity with high render distances, fancy graphics mode, and enhanced visual effects. Includes fabulous graphics with improved lighting and shadows. Best suited for high-end systems or content creation."),
            new IntroductionScreenPageOne.OptionProfile("Shaders", "Profile: Shaders", "Ultimate visual experience combining quality settings with shaders enabled. Features high render distances, fabulous graphics, and optimized settings and shaders enabled from the get go. Requires high-end hardware for optimal performance.")
    );

    public IntroductionScreenPageOne() {
        super(
                new WizardPageInfo(
                        Text.literal("FPS OR QUALITY???"),
                        1,
                        5 // Total wizard steps
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        this.welcomeMarkdown = MarkdownFileUtil.readMarkdownFile("Optimisation.md");
        this.modpackInfo = PackCore.getModpackInfo();
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        contentContainer.child(createWelcomeHeader());
        contentContainer.child(createMarkdownSection());
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {
        this.rightPanel = contentContainerRight;
        rightPanel.child(createHeader());
        rightPanel.child(createProfilesScrollContainer());
    }

    private FlowLayout createWelcomeHeader() {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6);

        Text welcomeText = TextOps.concat(
                TextOps.withColor("Choose your prefer optimisation profile for ", TEXT_WHITE),
                Text.literal(modpackInfo.getName()).setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        );

        LabelComponent welcomeTitle = Components.label(welcomeText);

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("Please read the information below carefully before continuing. Need help? Click the discord button at the bottom.")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.of(2, 0, 2, 0)).sizing(Sizing.expand(), Sizing.content());

        header.child(welcomeTitle).child(subtitle);

        return header;
    }

    private ScrollContainer<FlowLayout> createMarkdownSection() {
        FlowLayout markdownWrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        var markdownComponent = COMPONENT_CACHE.computeIfAbsent(
                welcomeMarkdown,
                MARKDOWN_PROCESSOR::process
        ).horizontalSizing(Sizing.fill(98));

        markdownWrapper.child(markdownComponent);

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                markdownWrapper
        );

        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollbarThiccness(6);
        scrollContainer.surface(Surface.flat(0x40_000000).and(Surface.outline(0x30_FFFFFF)));
        scrollContainer.padding(Insets.of(8));

        return scrollContainer;
    }

    private FlowLayout createHeader() {
        FlowLayout header = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .margins(Insets.top(4));

        if (selectedOptimisationProfile.isEmpty()) {
            headerTitle = (LabelComponent) Components.label(TextOps.withColor("Select your optimisation profile by clicking one of the boxes below", ACCENT_GOLD)).horizontalSizing(Sizing.fill(100));
        } else {
            headerTitle = (LabelComponent) Components.label(TextOps.withColor("Your selected profile is: " + selectedOptimisationProfile, ACCENT_GOLD)).horizontalSizing(Sizing.fill(100));
        }

        header.child(headerTitle);

        return header;
    }

    private FlowLayout createProfileBox(IntroductionScreenPageOne.OptionProfile profile) {
        boolean isSelected = profile.key.equals(selectedOptimisationProfile);
        FlowLayout box = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        box.surface(Surface.flat(0x20_FFD700).and(Surface.outline(isSelected ? SELECTED_OUTLINE_COLOR : UNSELECTED_OUTLINE_COLOR)));
        box.padding(Insets.of(2));

        LabelComponent infoTitle = (LabelComponent) Components.label(
                TextOps.withColor(profile.title, ACCENT_GOLD).setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        ).margins(Insets.of(2, 2, 2, 2));
        LabelComponent infoText = (LabelComponent) Components.label(
                TextOps.withColor(profile.description, TEXT_WHITE).setStyle(Style.EMPTY.withItalic(Boolean.TRUE))
        ).horizontalSizing(Sizing.fill(100))
                .margins(Insets.of(2, 2, 2, 2));

        box.child(infoTitle).child(infoText);

        box.mouseDown().subscribe((mouseX, mouseY, button) -> {
            selectedOptimisationProfile(profile.key);
            return true;
        });

        return box;
    }

    // Scrollable container for all profile boxes
    private ScrollContainer<FlowLayout> createProfilesScrollContainer() {
        FlowLayout profilesLayout = Containers.verticalFlow(Sizing.fill(96), Sizing.content()).gap(6);

        for (IntroductionScreenPageOne.OptionProfile profile : allProfiles) {
            profilesLayout.child(createProfileBox(profile));
        }

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

    private void selectedOptimisationProfile(String profileKey) {
        selectedOptimisationProfile = profileKey;

        // Store in data manager
        WizardDataManager.getInstance().setOptimizationProfile(profileKey);

        if (headerTitle != null) {
            headerTitle.text(
                    TextOps.withColor("Your selected profile is: " + selectedOptimisationProfile, ACCENT_GOLD)
            );
        }

        // Instead of clearing and rebuilding, just update the existing profile boxes
        updateProfileBoxes();
    }

    private void updateProfileBoxes() {
        rightPanel.children().stream()
                .filter(child -> child instanceof ScrollContainer)
                .findFirst()
                .ifPresent(scrollContainer -> {
                    FlowLayout profilesLayout = (FlowLayout) ((ScrollContainer<?>) scrollContainer).child();

                    // Update existing profile boxes instead of rebuilding
                    for (int i = 0; i < profilesLayout.children().size() && i < allProfiles.size(); i++) {
                        Component child = profilesLayout.children().get(i);
                        if (child instanceof FlowLayout existingBox) {
                            OptionProfile profile = allProfiles.get(i);
                            boolean isSelected = profile.key.equals(selectedOptimisationProfile);

                            // Update the surface to reflect the selection state
                            existingBox.surface(Surface.flat(0x20_FFD700).and(
                                    Surface.outline(isSelected ? SELECTED_OUTLINE_COLOR : UNSELECTED_OUTLINE_COLOR)
                            ));
                        }
                    }
                });
    }


    @Override
    protected void onContinuePressed() {
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