package com.github.kd_gaming1.packcore.gui.help.guide;

import com.github.kd_gaming1.packcore.gui.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.configscreen.ModpackConfigMenuScreen;
import com.github.kd_gaming1.packcore.gui.help.guide.util.GuideInfo;
import com.github.kd_gaming1.packcore.gui.ui.UITheme;
import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.lavendermd.feature.*;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class GuideViewerScreen extends BaseOwoScreen<FlowLayout> {

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

    private final GuideInfo guide;
    private final Screen parentScreen;
    private final Identifier backgroundTexture = Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png");

    public GuideViewerScreen(GuideInfo guide, Screen parentScreen) {
        this.guide = guide;
        this.parentScreen = parentScreen;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(UiSurfaces.stretched(backgroundTexture, 1920, 1082))
                .padding(Insets.of(8));

        rootComponent.child(createHeader());
        rootComponent.child(createMainContent());
    }

    private FlowLayout createHeader() {
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(42))
                .padding(Insets.of(2))
                .verticalAlignment(VerticalAlignment.CENTER);


        // Logo and title
        header.child(Components.texture(
                Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png"),
                0, 0, 40, 40, 40, 40));

        header.child(Components.label(Text.literal(guide.getTitle()).styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))))
                .color(Color.ofRgb(UITheme.ACCENT_GOLD))
                .shadow(true)
                .margins(Insets.of(0, 0, 4, 4)));

        //Spacer
        header.child(Containers.horizontalFlow(Sizing.expand(), Sizing.expand()));

        header.child(Components.button(Text.literal("Back"),
                        btn -> MinecraftClient.getInstance().setScreen(new BaseGuidePage()))
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/previous.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19)));

        return header;
    }

    private ScrollContainer<FlowLayout> createMainContent() {
        // Create a FlowLayout to wrap the markdown content
        FlowLayout markdownWrapper = Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(4);

        // Get the processed markdown component
        String content = guide.getFullContent();
        if (content == null || content.isEmpty()) {
            content = "# Error\n\nFailed to load guide content.";
        }

        var markdownComponent = COMPONENT_CACHE.computeIfAbsent(
                content,
                MARKDOWN_PROCESSOR::process
        );

        // Add the markdown component to our wrapper FlowLayout
        markdownWrapper.child(markdownComponent);

        // Create the ScrollContainer
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
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        this.client.setScreen(parentScreen);
    }
}