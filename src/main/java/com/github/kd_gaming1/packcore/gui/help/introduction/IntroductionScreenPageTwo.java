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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public class IntroductionScreenPageTwo extends BaseWizardPage{

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

    // The selectable options
    private final List<OptionProfile> allProfiles = List.of(
            new OptionProfile("Max FPS", "FPS Profile: Max FPS", "This profile optimises for FPS, prioritising frames over visuals. It is recommended for low-end laptops."),
            new OptionProfile("Balanced", "Normal Profile: Balanced", "This profile optimises for FPS and visuals, prioritising balance between frames over visuals. It is recommended for everyone."),
            new OptionProfile("Shaders", "Shaders Profile: Quality", "This profile optimises for visuals, prioritising quality over frames. It is recommended for mid-end PCs and people that want shaders.")
    );

    public IntroductionScreenPageTwo() {
        super(
                new WizardPageInfo(
                        Text.literal("FPS OR FPS+QUALITY???"),
                        2,
                        5 // Total wizard steps
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/test_temp.png")
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
        for (OptionProfile profile : allProfiles) {
            rightPanel.child(createProfileBox(profile));
        }
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
                Text.literal("The pack offers three options for a modpack optimisation profile: one that prioritises FPS over visuals (recommended for low-end laptops); a normal profile, optimised without sacrificing visual quality (recommended for everyone); and finally, a profile optimised with shaders (recommended for mid-end PCs).")
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

        if (selectedOptimisationProfile.isEmpty()) {
            headerTitle = Components.label(TextOps.withColor("Select your optimisation profile by clicking one of the boxes below", ACCENT_GOLD)).maxWidth(250);
        } else {
            headerTitle = Components.label(TextOps.withColor("Your selected profile is: " + selectedOptimisationProfile, ACCENT_GOLD)).maxWidth(250);
        }

        header.child(headerTitle);

        return header;
    }

    private FlowLayout createProfileBox(OptionProfile profile) {
        boolean isSelected = profile.key.equals(selectedOptimisationProfile);
        FlowLayout box = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        box.surface(Surface.flat(0x20_FFD700).and(Surface.outline(isSelected ? SELECTED_OUTLINE_COLOR : UNSELECTED_OUTLINE_COLOR)));
        box.padding(Insets.of(6));

        LabelComponent infoTitle = Components.label(
                TextOps.withColor(profile.title, ACCENT_GOLD).setStyle(Style.EMPTY.withBold(Boolean.TRUE))
        );
        LabelComponent infoText = Components.label(
                TextOps.withColor(profile.description, TEXT_WHITE).setStyle(Style.EMPTY.withItalic(Boolean.TRUE))
        ).maxWidth(250);

        box.child(infoTitle).child(infoText);

        box.mouseDown().subscribe((mouseX, mouseY, button) -> {
            selectedOptimisationProfile(profile.key);
            return true;
        });

        return box;
    }

    private void selectedOptimisationProfile(String profileKey) {
        selectedOptimisationProfile = profileKey;

        if (headerTitle != null) {
            headerTitle.text(
                    TextOps.withColor("Your selected profile is: " + selectedOptimisationProfile, ACCENT_GOLD)
            );
        }

        rightPanel.clearChildren();
        rightPanel.child(createHeader());
        for (OptionProfile profile : allProfiles) {
            rightPanel.child(createProfileBox(profile));
        }
    }

    @Override
    protected void onContinuePressed() {
        this.client.setScreen(WizardNavigator.createWizardPage(3));
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