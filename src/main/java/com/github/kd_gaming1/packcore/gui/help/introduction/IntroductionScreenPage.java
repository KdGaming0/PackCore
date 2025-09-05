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

public class IntroductionScreenPage extends BaseWizardPage {

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

    public IntroductionScreenPage() {
        super(
                new WizardPageInfo(
                        Text.literal("Welcome"),
                        0,
                        5 // Total wizard steps
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        this.welcomeMarkdown = MarkdownFileUtil.readMarkdownFile("Welcome.md");
        this.modpackInfo = PackCore.getModpackInfo();
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        // Welcome header
        contentContainer.child(createWelcomeHeader());

        // Markdown content in scrollable area
        contentContainer.child(createMarkdownSection());
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {

    }

    private FlowLayout createWelcomeHeader() {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6);

        // Create welcome text
        Text welcomeText = TextOps.concat(
                TextOps.withColor("Welcome to ", TEXT_WHITE),
                Text.literal(modpackInfo.getName()).setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        );

        LabelComponent welcomeTitle = Components.label(welcomeText);

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("Let's get you set up for the best experience! Take 30 seconds and read through the welcome please.")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.of(2, 0, 2, 0)).sizing(Sizing.expand(), Sizing.content());


        header.child(welcomeTitle).child(subtitle);

        return header;
    }

    private ScrollContainer<FlowLayout> createMarkdownSection() {
        // Create a FlowLayout to wrap the markdown content
        FlowLayout markdownWrapper = Containers.verticalFlow(Sizing.fill(98), Sizing.content())
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

        return scrollContainer;
    }

    @Override
    protected void onContinuePressed() {
        this.client.setScreen(WizardNavigator.createWizardPage(1));
    }

    @Override
    protected boolean shouldShowStatusInfo() {
        return true; // Show setup status on welcome page
    }
}