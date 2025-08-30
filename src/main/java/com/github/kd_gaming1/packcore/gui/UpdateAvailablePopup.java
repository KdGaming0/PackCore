package com.github.kd_gaming1.packcore.gui;

import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.lavendermd.feature.*;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class UpdateAvailablePopup {

    // Popup dimensions and styling constants
    private static final int POPUP_WIDTH = 350;
    private static final int POPUP_PADDING = 12;
    private static final int CHANGELOG_HEIGHT = 140;

    // Color constants matching your toast design
    private static final int SECTION_BACKGROUND = 0x80_1A1A1A; // Darker section backgrounds
    private static final int TEXT_WHITE = 0xFFFFFF;
    private static final int TEXT_GRAY = 0xAAAAAA;

    // Cached Markdown processor
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

    // Cache for processed Markdown components to avoid reparsing the same content
    private static final Map<String, ParentComponent> COMPONENT_CACHE = new ConcurrentHashMap<>();

    /**
     * Creates an overlay popup for update notifications with improved Minecraft-vanilla styling
     */
    public static OverlayContainer<FlowLayout> createUpdatePopup(
            String currentVersion,
            String newVersion,
            String changelogMarkdown,
            String modrinthUrl,
            Runnable onClose) {

        // Create the main popup container
        FlowLayout popupContent = createPopupContent(
                currentVersion,
                newVersion,
                changelogMarkdown,
                modrinthUrl,
                onClose
        );

        return Containers.overlay(popupContent)
                .closeOnClick(false);
    }

    private static FlowLayout createPopupContent(
            String currentVersion,
            String newVersion,
            String changelogMarkdown,
            String modrinthUrl,
            Runnable onClose) {

        // Main popup container with a gold border
        FlowLayout mainContainer = Containers.verticalFlow(Sizing.content(), Sizing.content());

        FlowLayout popup = (FlowLayout) Containers.verticalFlow(Sizing.fixed(POPUP_WIDTH), Sizing.content())
                .surface(Surface.VANILLA_TRANSLUCENT.and(Surface.outline(0xFF_FFD700).and(Surface.panelWithInset(3))))
                .padding(Insets.of(10, 10, 10, 10))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.top(48));

        // Add sections to the popup
        popup.child(createHeader(currentVersion, newVersion));
        popup.child(createContentSection(changelogMarkdown));
        popup.child(createButtonSection(modrinthUrl, onClose));

        mainContainer.child(popup);

        return mainContainer;
    }

    private static FlowLayout createHeader(String currentVersion, String newVersion) {
        FlowLayout header = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .surface(Surface.flat(0xC0101010).and(Surface.outline(0x40FFFFFF)).and(Surface.panelWithInset(1)))
                .padding(Insets.of(POPUP_PADDING));

        // Title
        LabelComponent titleLabel = Components.label(
                Text.literal("Update Available").formatted(Formatting.BOLD)
                        .append(Text.literal(" • ").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal("Modpack").formatted(Formatting.GOLD))
        );
        titleLabel.color(Color.ofRgb(TEXT_WHITE));

        // Version comparison
        LabelComponent versionLabel = Components.label(
                Text.literal("v").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal(currentVersion).formatted(Formatting.GRAY))
                        .append(Text.literal(" ➜ ").formatted(Formatting.DARK_AQUA))
                        .append(Text.literal("v").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal(newVersion).formatted(Formatting.GOLD, Formatting.BOLD))
        );
        versionLabel.color(Color.ofRgb(TEXT_GRAY));

        // Add subtle update indicator
        LabelComponent updateIndicator = Components.label(
                Text.literal("● ").formatted(Formatting.GREEN)
                        .append(Text.literal("New version ready").formatted(Formatting.GRAY, Formatting.ITALIC))
        );
        updateIndicator.color(Color.ofRgb(TEXT_GRAY));

        header.child(titleLabel).child(versionLabel).child(updateIndicator);
        return header;
    }


    private static FlowLayout createContentSection(String changelogMarkdown) {
        FlowLayout contentSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .surface(Surface.flat(SECTION_BACKGROUND))
                .padding(Insets.of(POPUP_PADDING));

        // Description text
        LabelComponent description = Components.label(
                Text.literal("What's new in this update:").formatted(Formatting.WHITE)
        );
        description.color(Color.ofRgb(TEXT_WHITE));

        // Use cached processor and cache the processed components
        var markdownComponent = COMPONENT_CACHE.computeIfAbsent(
                changelogMarkdown,
                MARKDOWN_PROCESSOR::process
        );

        // Scrollable changelog
        var changelogScroll = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.fixed(CHANGELOG_HEIGHT),
                markdownComponent
        );

        changelogScroll
                .scrollbar(ScrollContainer.Scrollbar.vanilla())
                .scrollbarThiccness(4)
                .surface(Surface.flat(0x80_101010).and(Surface.outline(0x60FFFFFF)).and(Surface.panelWithInset(2)))
                .padding(Insets.of(8));

        changelogScroll.padding(Insets.of(6, 10, 6, 10));
        markdownComponent.margins(Insets.of(2, 0, 2, 0));

        contentSection
                .child(description)
                .child(changelogScroll);

        return contentSection;
    }

    private static FlowLayout createButtonSection(String modrinthUrl, Runnable onClose) {
        FlowLayout buttonSection = (FlowLayout) Containers.ltrTextFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .padding(Insets.of(8))
                .surface(Surface.flat(0xC0101010) // translucent dark base
                        .and(Surface.outline(0x40FFFFFF)) // soft white outline
                        .and(Surface.panelWithInset(0)))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        // Instruction text
        LabelComponent instructionText = Components.label(
                Text.literal("Need Help Updating? Click: 'Help me Update'").formatted(Formatting.GRAY, Formatting.ITALIC)
        );
        instructionText
                .color(Color.ofRgb(TEXT_GRAY))
                .horizontalSizing(Sizing.content())
                .margins(Insets.of(0,4,4,0));

        // Create a button row
        FlowLayout buttonsRow = createButtonsRow(modrinthUrl, onClose);

        buttonSection
                .child(instructionText)
                .child(buttonsRow);

        return buttonSection;
    }

    private static FlowLayout createButtonsRow(String modrinthUrl, Runnable onClose) {
        FlowLayout buttonsRow = (FlowLayout) Containers.ltrTextFlow(Sizing.fill(100), Sizing.content())
                .horizontalAlignment(HorizontalAlignment.LEFT);

        // Help me Update button
        ButtonComponent helpButton = createStyledButton("Help me Update", 90, () -> {
            //TODO: Open help screen
        });

        // Show the Changelog on Modrinth
        ButtonComponent modrinthButton = createStyledButton("View Changelog on Modrinth", 160, () -> {
            Util.getOperatingSystem().open(modrinthUrl);
            onClose.run();
        });

        // Later button
        ButtonComponent laterButton = createStyledButton("Later", 50, onClose);

        buttonsRow
                .child(helpButton)
                .child(modrinthButton)
                .child(laterButton);

        return buttonsRow;
    }

    private static ButtonComponent createStyledButton(String text, int width, Runnable onClick) {
        return (ButtonComponent) Components.button(Text.literal(text), button -> onClick.run())
                .sizing(Sizing.fixed(width), Sizing.fixed(20))
                .margins(Insets.of(2, 2, 2, 2));
    }
}