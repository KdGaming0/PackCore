package com.github.kd_gaming1.packcore.ui.screen.base;

import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Base class for all PackCore screens providing common layout patterns and utilities.
 * Simplifies screen creation with standardized header, content areas, and navigation.
 */
public abstract class BasePackCoreScreen extends BaseOwoScreen<FlowLayout> {

    protected static final Identifier DEFAULT_BACKGROUND =
            Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png");

    protected final Screen parentScreen;
    protected final Identifier backgroundTexture;
    protected FlowLayout rootComponent;

    /**
     * Create a screen with default background
     */
    protected BasePackCoreScreen(@Nullable Screen parentScreen) {
        this(parentScreen, DEFAULT_BACKGROUND);
    }

    /**
     * Create a screen with custom background
     */
    protected BasePackCoreScreen(@Nullable Screen parentScreen, Identifier backgroundTexture) {
        this.parentScreen = parentScreen;
        this.backgroundTexture = backgroundTexture;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected final void build(FlowLayout rootComponent) {
        this.rootComponent = rootComponent;

        // Apply background
        rootComponent.surface(TextureSurfaces.stretched(backgroundTexture, 1920, 1082));
        rootComponent.padding(Insets.of(8));

        // Build header
        rootComponent.child(createHeader());

        // Build main content
        rootComponent.child(createMainContent());
    }

    /**
     * Create the screen header with title and navigation
     */
    protected FlowLayout createHeader() {
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(50))
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        // Logo
        header.child(createLogo());

        // Title
        header.child(createTitleLabel());

        // Spacer
        header.child(Containers.horizontalFlow(Sizing.expand(), Sizing.content()));

        // Navigation buttons
        header.child(createHeaderActions());

        return header;
    }

    /**
     * Create the logo component
     */
    protected Component createLogo() {
        return Components.texture(
                Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png"),
                0, 0, 40, 40, 40, 40
        );
    }

    /**
     * Create the title label - override to customize
     */
    protected abstract Component createTitleLabel();

    /**
     * Create header action buttons (e.g., Back, Close)
     */
    protected FlowLayout createHeaderActions() {
        FlowLayout actions = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(8);

        // Add back/close button
        String buttonText = parentScreen != null ? "Back" : "Close";
        actions.child(Components.button(
                Text.literal(buttonText),
                btn -> navigateBack()
        ).renderer(ButtonComponent.Renderer.texture(
                Identifier.of(MOD_ID, "textures/gui/wizard/previous.png"), 0, 0, 90, 57
        )).sizing(Sizing.fixed(90), Sizing.fixed(19)));

        return actions;
    }

    /**
     * Create the main content area - implement in subclasses
     */
    protected abstract FlowLayout createMainContent();

    /**
     * Navigate back to parent screen or close
     */
    protected void navigateBack() {
        MinecraftClient.getInstance().setScreen(parentScreen);
    }

    /**
     * Show an overlay dialog
     */
    protected void showOverlay(FlowLayout content, boolean closeOnClick) {
        var overlay = Containers.overlay(content);
        overlay.closeOnClick(closeOnClick);
        overlay.surface(Surface.flat(0x80_000000));
        rootComponent.child(overlay);
    }

    /**
     * Close the topmost overlay
     */
    protected void closeTopOverlay() {
        if (!rootComponent.children().isEmpty()) {
            Component lastChild = rootComponent.children().getLast();
            if (lastChild instanceof io.wispforest.owo.ui.container.OverlayContainer) {
                rootComponent.removeChild(lastChild);
            }
        }
    }

    @Override
    public void close() {
        navigateBack();
    }
}