package com.github.kd_gaming1.packcore.ui.screen.wizard.pages;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import com.github.kd_gaming1.packcore.ui.screen.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.ui.screen.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.lavendermd.CustomLavenderCompiler;
import com.github.kd_gaming1.packcore.util.markdown.MarkdownService;
import com.github.kd_gaming1.packcore.modpack.ModpackInfo;
import io.wispforest.lavendermd.MarkdownProcessor;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class UsefulInfoWizardPage extends BaseWizardPage {

    private static final String FALLBACK_USEFUL_INFO_DESCRIPTION = """
            # Useful Information

            Useful Information! This is the default Useful Information content.
            
            Find and edit this content in `rundir/packcore/wizard_markdown_content/useful_information.md`
            """;

    private final MarkdownService markdownService = new MarkdownService();

    private final String markdownContent;
    private final ModpackInfo modpackInfo;

    private static final MarkdownProcessor<ParentComponent> MARKDOWN_PROCESSOR =
            new MarkdownProcessor<>(
                    CustomLavenderCompiler::new,
                    new BasicFormattingFeature(),
                    new ColorFeature(),
                    new LinkFeature(),
                    new ListFeature(),
                    new BlockQuoteFeature(),
                    new ImageFeature()
            );

    private static final Map<String, ParentComponent> COMPONENT_CACHE = new ConcurrentHashMap<>();

    public UsefulInfoWizardPage() {
        super(
                new WizardPageInfo(
                        Text.literal("Useful information"),
                        4,
                        5 // Total wizard steps
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        this.markdownContent = markdownService.getOrDefault("useful_information.md", FALLBACK_USEFUL_INFO_DESCRIPTION);
        this.modpackInfo = PackCore.getModpackInfo();
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        contentContainer.surface(TextureSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/wizard/frame.png"), 1920, 1080));
        contentContainer.padding(Insets.of(24, 36, 24, 24));

        // Welcome header
        contentContainer.child(createWelcomeHeader());

        // Markdown content in scrollable area
        contentContainer.child(createMarkdownSection());
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {

    }

    private FlowLayout createWelcomeHeader() {
        FlowLayout header = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .margins(Insets.of(0, 0, 36, 36));

        // Create welcome text
        Text welcomeText = TextOps.concat(
                TextOps.withColor("Useful information when playing on  ", TEXT_WHITE),
                Text.literal(modpackInfo.getName()).setStyle(Style.EMPTY.withColor(ACCENT_GOLD).withBold(Boolean.TRUE))
        );

        LabelComponent welcomeTitle = Components.label(welcomeText);

        LabelComponent subtitle = (LabelComponent) Components.label(
                Text.literal("The pack have many mods and some features, here is a few tips to get you started.")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(Boolean.TRUE))
        ).color(Color.ofRgb(TEXT_SECONDARY)).margins(Insets.of(2, 0, 2, 0)).sizing(Sizing.expand(), Sizing.content());


        header.child(welcomeTitle).child(subtitle);

        return header;
    }

    private ScrollContainer<FlowLayout> createMarkdownSection() {
        // Create a FlowLayout to wrap the markdown content
        FlowLayout markdownWrapper = Containers.verticalFlow(Sizing.fill(96), Sizing.content())
                .gap(4);

        // Get the processed markdown component
        var markdownComponent = COMPONENT_CACHE.computeIfAbsent(
                markdownContent,
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
        scrollContainer.margins(Insets.bottom(10));

        return scrollContainer;
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
        return false;
    }

}
