package com.github.kd_gaming1.packcore.ui.help.guide;

import com.github.kd_gaming1.packcore.ui.screen.components.WizardUIComponents;
import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import com.github.kd_gaming1.packcore.util.help.guide.GuideInfo;
import com.github.kd_gaming1.packcore.ui.theme.UITheme;
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

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Screen that displays the detailed content of a selected guide.
 */
public class GuideDetailScreen extends BaseOwoScreen<FlowLayout> {

    private final GuideInfo guide;
    private final Screen parentScreen;
    private final Identifier backgroundTexture = Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png");

    public GuideDetailScreen(GuideInfo guide, Screen parentScreen) {
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
                .surface(TextureSurfaces.stretched(backgroundTexture, 1920, 1082))
                .padding(Insets.of(8));

        rootComponent.child(createHeader());
        rootComponent.child(createMainContent());
    }

    private FlowLayout createHeader() {
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(42))
                .padding(Insets.of(2))
                .verticalAlignment(VerticalAlignment.CENTER);

        // Logo
        header.child(Components.texture(
                Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png"),
                0, 0, 40, 40, 40, 40));

        // Title - strip markdown formatting
        String cleanTitle = stripMarkdownFormatting(guide.getTitle());
        header.child(Components.label(
                        Text.literal(cleanTitle)
                                .styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))
                ).color(Color.ofRgb(UITheme.ACCENT_SECONDARY))
                .shadow(true)
                .margins(Insets.of(0, 0, 4, 4)));

        // Spacer
        header.child(Containers.horizontalFlow(Sizing.expand(), Sizing.expand()));

        // Back button
        header.child(Components.button(
                        Text.literal("Back"),
                        btn -> MinecraftClient.getInstance().setScreen(parentScreen)
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/previous.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19)));

        return header;
    }

    private ScrollContainer<FlowLayout> createMainContent() {
        String content = guide.getFullContent();
        if (content == null || content.isEmpty()) {
            content = "# Error\n\nFailed to load guide content.";
        }

        // Use the reusable markdown scroll component
        return WizardUIComponents.createMarkdownScroll(content);
    }

    /**
     * Strip markdown formatting from text
     */
    private String stripMarkdownFormatting(String text) {
        if (text == null) return "";

        // Remove {gold} and other color codes
        text = text.replaceAll("\\{[^}]*\\}", "");

        // Remove ** for bold
        text = text.replaceAll("\\*\\*", "");

        // Remove other common markdown
        text = text.replaceAll("^#+\\s*", ""); // Headers
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1"); // Links
        text = text.replaceAll("_", ""); // Italics
        text = text.replaceAll("`", ""); // Code

        return text.trim();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(parentScreen);
    }
}