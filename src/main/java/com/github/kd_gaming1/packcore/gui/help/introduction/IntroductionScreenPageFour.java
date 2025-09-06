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

// Work in progress page for miscellaneous information
public class IntroductionScreenPageFour extends BaseWizardPage {


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

    public IntroductionScreenPageFour() {
        super(
                new WizardPageInfo(
                        Text.literal("Useful information"),
                        4,
                        5 // Total wizard steps
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/test_temp.png")
        );

        this.welcomeMarkdown = MarkdownFileUtil.readMarkdownFile("Welcome.md");
        this.modpackInfo = PackCore.getModpackInfo();
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        contentContainer.margins(Insets.bottom(42));
        // Welcome header
        contentContainer.child(createWelcomeHeader());

        // Markdown content in scrollable area
        contentContainer.child(createMarkdownSection());

        // Quick info section
        contentContainer.child(createQuickInfoSection().positioning(Positioning.relative(0, 100)));
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {

    }

    private FlowLayout createWelcomeHeader() {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6);

        // Create welcome text
        Text welcomeText = TextOps.concat(
                TextOps.withColor("Useful information when playing on  ", TEXT_WHITE),
                Text.literal(modpackInfo.getName()).setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        );

        LabelComponent welcomeTitle = Components.label(welcomeText);

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("The pack have many mods and some features are a bit hidden some tomes, here is a few tips to get you started.")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.of(2, 0, 2, 0)).sizing(Sizing.expand(), Sizing.content());


        header.child(welcomeTitle).child(subtitle);

        return header;
    }

    private ScrollContainer<FlowLayout> createMarkdownSection() {
        // Create a FlowLayout to wrap the markdown content
        FlowLayout markdownWrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        // Get the processed markdown component
        var markdownComponent = COMPONENT_CACHE.computeIfAbsent(
                welcomeMarkdown,
                MARKDOWN_PROCESSOR::process
        );

        // Add the markdown component to our wrapper FlowLayout
        markdownWrapper.child(markdownComponent);

        // Create the ScrollContainer and configure it step by step
        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                markdownWrapper
        );

        // Configure the scroll container
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

        // Discord link as a transparent button
        if (modpackInfo.getDiscord() != null && !modpackInfo.getDiscord().isEmpty()) {
            Text discordText = TextOps.concat(
                    TextOps.withColor("💬 Discord: ", TEXT_WHITE),
                    TextOps.withColor(modpackInfo.getDiscord(), TextOps.color(Formatting.AQUA))
            );

            ButtonComponent discordButton = (ButtonComponent) Components.button(discordText, btn -> {
                        Util.getOperatingSystem().open(modpackInfo.getDiscord());
                    })
                    .renderer(ButtonComponent.Renderer.flat(0x00000000, 0x00000000, 0x00000000)) // fully transparent background
                    .textShadow(false)
                    .sizing(Sizing.content(), Sizing.fixed(16));

            infoSection.child(discordButton);
        }

        // Issue tracker link as a transparent button
        if (modpackInfo.getIssueTracker() != null && !modpackInfo.getIssueTracker().isEmpty()) {
            Text issueText = TextOps.concat(
                    TextOps.withColor("🐛 Issues: ", TEXT_WHITE),
                    TextOps.withColor(modpackInfo.getIssueTracker(), TextOps.color(Formatting.AQUA))
            );

            ButtonComponent issueButton = (ButtonComponent) Components.button(issueText, btn -> {
                        Util.getOperatingSystem().open(modpackInfo.getIssueTracker());
                    })
                    .renderer(ButtonComponent.Renderer.flat(0x00000000, 0x00000000, 0x00000000)) // fully transparent background
                    .textShadow(false)
                    .sizing(Sizing.content(), Sizing.fixed(16));

            infoSection.child(issueButton);
        }

        return infoSection;
    }

    @Override
    protected void onContinuePressed() {
        this.client.setScreen(WizardNavigator.createWizardPage(5));
    }

    @Override
    protected int getContentColumnWidthPercent() {
        return 100;
    }

    @Override
    protected boolean shouldShowStatusInfo() {
        return false; // Show setup status on welcome page
    }

}
