package com.github.kd_gaming1.packcore.gui.configscreen;

import com.github.kd_gaming1.packcore.util.ConfigFileUtils;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class ConfigConfirmationPopup {

    // Theme constants
    private static final int PANEL_BACKGROUND = 0xF0_1A1A1A;
    private static final int ACCENT_GOLD = 0xFF_FFD700;
    private static final int TEXT_WHITE = 0xFFFFFF;
    private static final int TEXT_SECONDARY = 0xB9BBBE;
    private static final int STATUS_SUCCESS_BG = 0xC0_2D5016;
    private static final int STATUS_SUCCESS_BORDER = 0xFF_52C41A;
    private static final int STATUS_ERROR_BG = 0xC0_5C1717;
    private static final int STATUS_ERROR_BORDER = 0xFF_FF4D4F;

    public static OverlayContainer<FlowLayout> createConfirmationPopup(
            ConfigFileUtils.ConfigFile config,
            Runnable onConfirm,
            Runnable onCancel) {

        FlowLayout popupContent = Containers.verticalFlow(Sizing.fixed(400), Sizing.content());
        popupContent.gap(8);
        popupContent.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.panelWithInset(2).and(Surface.outline(ACCENT_GOLD))));
        popupContent.padding(Insets.of(16));

        // Title
        LabelComponent title = Components.label(Text.literal("Confirm Configuration Change")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(ACCENT_GOLD));
        popupContent.child(title);

        // Selected config info
        FlowLayout configInfo = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        configInfo.gap(4);
        configInfo.surface(Surface.flat(0xC0_2A2A2A).and(Surface.outline(0xFF_555555)));
        configInfo.padding(Insets.of(8));

        configInfo.child(Components.label(Text.literal("Selected Configuration:")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(TEXT_WHITE)));
        configInfo.child(Components.label(Text.literal(config.getDisplayName()))
                .color(Color.ofRgb(ACCENT_GOLD)));
        configInfo.child(Components.label(Text.literal("Version: " + config.getMetadata().getVersion()))
                .color(Color.ofRgb(TEXT_SECONDARY)));
        configInfo.child(Components.label(Text.literal("Author: " + config.getMetadata().getAuthor()))
                .color(Color.ofRgb(TEXT_SECONDARY)));

        popupContent.child(configInfo);

        // Warning/Info section
        FlowLayout warningSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        warningSection.gap(4);
        warningSection.surface(Surface.flat(0xC0_3A2A1A).and(Surface.outline(0xFF_FFA500)));
        warningSection.padding(Insets.of(8));

        warningSection.child(Components.label(Text.literal("What will happen:")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(0xFF_FFA500)));
        warningSection.child(Components.label(Text.literal("• Current configuration will be backed up"))
                .color(Color.ofRgb(TEXT_WHITE)));
        warningSection.child(Components.label(Text.literal("• Game will close automatically"))
                .color(Color.ofRgb(TEXT_WHITE)));
        warningSection.child(Components.label(Text.literal("• Config will be applied when you restart the game"))
                .color(Color.ofRgb(TEXT_WHITE)));
        warningSection.child(Components.label(Text.literal("• This action cannot be easily undone"))
                .color(Color.ofRgb(0xFF_FF6B6B)));

        popupContent.child(warningSection);

        // Buttons
        FlowLayout buttonLayout = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonLayout.gap(8);
        buttonLayout.horizontalAlignment(HorizontalAlignment.CENTER);

        ButtonComponent confirmButton = (ButtonComponent) Components.button(Text.literal("Apply Configuration"),
                        button -> onConfirm.run())
                .renderer(ButtonComponent.Renderer.flat(STATUS_SUCCESS_BG, STATUS_SUCCESS_BORDER, 0xFF_333333))
                .sizing(Sizing.fixed(150), Sizing.fixed(25));

        ButtonComponent cancelButton = (ButtonComponent) Components.button(Text.literal("Cancel"),
                        button -> onCancel.run())
                .renderer(ButtonComponent.Renderer.flat(STATUS_ERROR_BG, STATUS_ERROR_BORDER, 0xFF_333333))
                .sizing(Sizing.fixed(80), Sizing.fixed(25));

        buttonLayout.child(confirmButton);
        buttonLayout.child(cancelButton);
        popupContent.child(buttonLayout);

        // Create overlay container
        OverlayContainer<FlowLayout> overlay = Containers.overlay(popupContent);
        overlay.closeOnClick(false); // Don't close when clicking outside

        return overlay;
    }
}