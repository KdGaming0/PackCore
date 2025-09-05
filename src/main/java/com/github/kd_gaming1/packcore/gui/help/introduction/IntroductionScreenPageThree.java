package com.github.kd_gaming1.packcore.gui.help.introduction;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.help.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.help.WizardNavigator;
import com.github.kd_gaming1.packcore.util.MarkdownFileUtil;
import com.github.kd_gaming1.packcore.util.ModpackInfo;
import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.lavendermd.feature.*;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.component.ButtonComponent;
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
import net.minecraft.util.Util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IntroductionScreenPageThree extends BaseWizardPage {
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

    private final Set<String> selectedOptimisationProfiles = new HashSet<>();
    LabelComponent headerTitle;
    private FlowLayout rightPanel;

    private static final int SELECTED_OUTLINE_COLOR = 0xFF00FF00;
    private static final int UNSELECTED_OUTLINE_COLOR = ACCENT_GOLD;

    // Helper POJO for options
    public static class OptionProfile {
        public final String key;
        public final String title;
        public final String description;

        public OptionProfile(String key, String title, String description) {
            this.key = key;
            this.title = title;
            this.description = description;
        }
    }

    // Easily extend this list!
    private final List<OptionProfile> allProfiles = List.of(
            new OptionProfile("Max FPS", "FPS Profile: Max FPS", "This profile optimises for FPS, prioritising frames over visuals. Recommended for low-end laptops."),
            new OptionProfile("Balanced", "Normal Profile: Balanced", "This profile optimises for FPS and visuals, prioritising balance. Recommended for everyone."),
            new OptionProfile("Shaders", "Shaders Profile: Quality", "This profile optimises for visuals, prioritising quality over frames. Recommended for mid-end PCs and those who want shaders.")
            // Add more OptionProfiles here!
    );

    public IntroductionScreenPageThree() {
        super(
                new WizardPageInfo(
                        Text.literal("Miscellaneous"),
                        3,
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
        contentContainer.child(createQuickInfoSection().positioning(Positioning.relative(0, 100)));
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
                TextOps.withColor("Edit a few miscellaneous settings", TEXT_WHITE),
                Text.literal(modpackInfo.getName()).setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        );

        LabelComponent welcomeTitle = Components.label(welcomeText);

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("The pack have many mods and some settings are very personal, here you can edit some of them.")
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
        );

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
        scrollContainer.margins(Insets.bottom(45));

        return scrollContainer;
    }

    private FlowLayout createQuickInfoSection() {
        FlowLayout infoSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x20_FFD700).and(Surface.outline(ACCENT_GOLD)))
                .padding(Insets.of(6));

        LabelComponent infoTitle = Components.label(
                TextOps.withColor("Quick Links", ACCENT_GOLD)
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        );

        infoSection.child(infoTitle);

        if (modpackInfo.getDiscord() != null && !modpackInfo.getDiscord().isEmpty()) {
            Text discordText = TextOps.concat(
                    TextOps.withColor("💬 Discord: ", TEXT_WHITE),
                    TextOps.withColor(modpackInfo.getDiscord(), TextOps.color(Formatting.AQUA))
            );

            ButtonComponent discordButton = (ButtonComponent) Components.button(discordText, btn -> {
                        Util.getOperatingSystem().open(modpackInfo.getDiscord());
                    })
                    .renderer(ButtonComponent.Renderer.flat(0x00000000, 0x00000000, 0x00000000))
                    .textShadow(false)
                    .sizing(Sizing.content(), Sizing.fixed(16));

            infoSection.child(discordButton);
        }

        if (modpackInfo.getIssueTracker() != null && !modpackInfo.getIssueTracker().isEmpty()) {
            Text issueText = TextOps.concat(
                    TextOps.withColor("🐛 Issues: ", TEXT_WHITE),
                    TextOps.withColor(modpackInfo.getIssueTracker(), TextOps.color(Formatting.AQUA))
            );

            ButtonComponent issueButton = (ButtonComponent) Components.button(issueText, btn -> {
                        Util.getOperatingSystem().open(modpackInfo.getIssueTracker());
                    })
                    .renderer(ButtonComponent.Renderer.flat(0x00000000, 0x00000000, 0x00000000))
                    .textShadow(false)
                    .sizing(Sizing.content(), Sizing.fixed(16));

            infoSection.child(issueButton);
        }

        return infoSection;
    }

    private FlowLayout createHeader() {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6);

        String selected = selectedOptimisationProfiles.isEmpty() ? "None" : String.join(", ", selectedOptimisationProfiles);
        headerTitle = Components.label(TextOps.withColor("Selected profiles: " + selected, ACCENT_GOLD)).maxWidth(250);

        header.child(headerTitle);

        return header;
    }

    // Helper method for creating profile boxes
    private FlowLayout createProfileBox(OptionProfile profile) {
        boolean isSelected = selectedOptimisationProfiles.contains(profile.key);
        FlowLayout box = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        box.surface(Surface.flat(0x20_FFD700).and(Surface.outline(isSelected ? SELECTED_OUTLINE_COLOR : UNSELECTED_OUTLINE_COLOR)));
        box.padding(Insets.of(6));

        LabelComponent infoTitle = Components.label(
                TextOps.withColor(profile.title, ACCENT_GOLD).setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        );
        LabelComponent infoText = Components.label(
                TextOps.withColor(profile.description, TEXT_WHITE).setStyle(Style.EMPTY.withItalic(Boolean.TRUE))
        ).maxWidth(220);

        box.child(infoTitle).child(infoText);

        box.mouseDown().subscribe((mouseX, mouseY, button) -> {
            toggleOptimisationProfile(profile.key);
            return true;
        });

        return box;
    }

    // Scrollable container for all profile boxes
    private ScrollContainer<FlowLayout> createProfilesScrollContainer() {
        FlowLayout profilesLayout = Containers.verticalFlow(Sizing.fill(95), Sizing.content()).gap(8);

        for (OptionProfile profile : allProfiles) {
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
        scrollContainer.padding(Insets.of(8));
        scrollContainer.margins(Insets.bottom(45));
        return scrollContainer;
    }

    private void toggleOptimisationProfile(String profileKey) {
        if (selectedOptimisationProfiles.contains(profileKey)) {
            selectedOptimisationProfiles.remove(profileKey);
        } else {
            selectedOptimisationProfiles.add(profileKey);
        }
        redrawRightPanel();
    }

    private void redrawRightPanel() {
        if (headerTitle != null) {
            String selected = selectedOptimisationProfiles.isEmpty() ? "None" : String.join(", ", selectedOptimisationProfiles);
            headerTitle.text(TextOps.withColor("Selected profiles: " + selected, ACCENT_GOLD));
        }
        rightPanel.clearChildren();
        rightPanel.child(createHeader());
        rightPanel.child(createProfilesScrollContainer());
    }

    @Override
    protected void onContinuePressed() {
        this.client.setScreen(WizardNavigator.createWizardPage(4));
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