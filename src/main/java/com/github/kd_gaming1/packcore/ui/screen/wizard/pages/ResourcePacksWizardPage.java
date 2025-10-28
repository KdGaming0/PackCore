package com.github.kd_gaming1.packcore.ui.screen.wizard.pages;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.ui.screen.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.util.wizard.WizardDataStore;
import com.github.kd_gaming1.packcore.ui.screen.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.ui.screen.wizard.components.WizardUIComponents;
import com.github.kd_gaming1.packcore.util.markdown.MarkdownService;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Resource Packs selection page - allows choosing multiple resource packs
 */
public class ResourcePacksWizardPage extends BaseWizardPage {

    private static final String FALLBACK_CONTENT = """
            # Resource Packs
            
            Choose your Resource Pack! This is the default Resource Pack content.
            
            Find and edit this content in `rundir/packcore/wizard_markdown_content/resource_packs.md`
            """;

    /**
     * Resource pack configuration
     */
    private record PackOption(
            String key,
            String icon,
            String title,
            String description,
            boolean requiresJVM
    ) {}

    private static final List<PackOption> AVAILABLE_PACKS = List.of(
            new PackOption(
                    "HypixelPlus",
                    "⭐",
                    "Hypixel Plus",
                    "Clean vanilla-style pack for Hypixel. Updates items and icons for clarity.",
                    true
            ),
            new PackOption(
                    "FurfSkyOverlay",
                    "🎭",
                    "FurfSky Overlay",
                    "Popular choice. Retextures items only, keeping vanilla GUI.",
                    false
            ),
            new PackOption(
                    "FurfSkyFull",
                    "🌟",
                    "FurfSky Full",
                    "Complete makeover. Items + GUI + menus in unique style.",
                    false
            ),
            new PackOption(
                    "SkyBlockDarkUI",
                    "🌙",
                    "SkyBlock Dark UI",
                    "Sleek dark theme for menus and mod interfaces. Modern aesthetic.",
                    false
            ),
            new PackOption(
                    "SkyBlockDarkmode",
                    "🌑",
                    "SkyBlock Dark mode",
                    "Sleek dark theme for The End island and The Mist",
                    false
            ),
            new PackOption(
                    "Defrosted",
                    "❄",
                    "Defrosted",
                    "Frosty blue 16x pack. Minimalist look with cool aesthetic.",
                    false
            ),
            new PackOption(
                    "Looshy",
                    "✨",
                    "Looshy",
                    "Smooth vanilla-like 16x. Refined textures, fresh and polished.",
                    false
            )
    );

    private final String markdownContent;
    private final WizardDataStore dataStore;
    private final Set<String> selectedResourcePacks = new LinkedHashSet<>();

    private FlowLayout rightPanel;

    public ResourcePacksWizardPage() {
        super(
                new WizardPageInfo(
                        Text.literal("Resource Packs"),
                        4,
                        6
                ),
                Identifier.of(PackCore.MOD_ID, "textures/gui/wizard/welcome_bg.png")
        );

        MarkdownService markdownService = new MarkdownService();
        this.markdownContent = markdownService.getOrDefault("resource_packs.md", FALLBACK_CONTENT);

        this.dataStore = WizardDataStore.getInstance();
        this.selectedResourcePacks.addAll(dataStore.getResourcePacksOrdered());
    }

    @Override
    protected void buildContent(FlowLayout contentContainer) {
        // Header
        contentContainer.child(
                WizardUIComponents.createHeader(
                        "Choose resource packs for",
                        null // No subtitle for this page
                )
        );

        // Markdown content
        contentContainer.child(WizardUIComponents.createMarkdownScroll(markdownContent));
    }

    @Override
    protected void buildContentRight(FlowLayout contentContainerRight) {
        this.rightPanel = contentContainerRight;
        rightPanel.child(createSelectionHeader());
        rightPanel.child(createPackOptions());
    }

    /**
     * Create the selection header with count and loading order
     */
    private FlowLayout createSelectionHeader() {
        FlowLayout header = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .margins(Insets.of(8, 0, 8, 8));

        // Title
        header.child(Components.label(
                Text.literal("🎨 Resource Packs").setStyle(Style.EMPTY.withBold(true))
        ).color(Color.ofRgb(TEXT_PRIMARY)));

        // Selection status
        LabelComponent headerTitle;
        if (selectedResourcePacks.isEmpty()) {
            headerTitle = (LabelComponent) Components.label(
                            Text.literal("Select packs below (optional)")
                    ).color(Color.ofRgb(ACCENT_SECONDARY))
                    .horizontalSizing(Sizing.fill(100));
        } else {
            int count = selectedResourcePacks.size();
            headerTitle = (LabelComponent) Components.label(
                            Text.literal("✓ " + count + " pack" + (count == 1 ? "" : "s") + " selected")
                                    .setStyle(Style.EMPTY.withBold(true))
                    ).color(Color.ofRgb(SUCCESS_BORDER))
                    .horizontalSizing(Sizing.fill(100));
        }

        header.child(headerTitle);

        // Show loading order if any selected
        if (!selectedResourcePacks.isEmpty()) {
            header.child(createLoadingOrderPreview());
        }

        return header;
    }

    /**
     * Create loading order preview
     */
    private FlowLayout createLoadingOrderPreview() {
        FlowLayout orderSection = WizardUIComponents.createInfoCard(
                "📋 Loading Order (Top = Priority)",
                null,
                0x20_4A90E2,
                ACCENT_PRIMARY
        );

        int index = 1;
        for (String packKey : selectedResourcePacks) {
            FlowLayout orderItem = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                    .gap(6)
                    .verticalAlignment(VerticalAlignment.CENTER);

            // Number badge
            FlowLayout badge = (FlowLayout) Containers.horizontalFlow(Sizing.fixed(20), Sizing.fixed(20))
                    .surface(Surface.flat(ACCENT_PRIMARY))
                    .horizontalAlignment(HorizontalAlignment.CENTER)
                    .verticalAlignment(VerticalAlignment.CENTER);

            badge.child(Components.label(Text.literal(String.valueOf(index)))
                    .color(Color.ofRgb(TEXT_PRIMARY)));

            orderItem.child(badge);

            // Pack name
            orderItem.child(Components.label(Text.literal(packKey))
                    .color(Color.ofRgb(TEXT_SECONDARY))
                    .horizontalSizing(Sizing.expand()));

            orderSection.child(orderItem);
            index++;
        }

        return orderSection;
    }

    /**
     * Create scrollable pack options
     */
    private ScrollContainer<FlowLayout> createPackOptions() {
        FlowLayout packsLayout = Containers.verticalFlow(Sizing.fill(96), Sizing.content()).gap(6);

        for (PackOption pack : AVAILABLE_PACKS) {
            packsLayout.child(createPackOption(pack));
        }

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                packsLayout
        );

        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollbarThiccness(6);
        scrollContainer.surface(Surface.flat(0x40_000000).and(Surface.outline(0x30_FFFFFF)));
        scrollContainer.padding(Insets.of(6));
        scrollContainer.margins(Insets.bottom(4));

        return scrollContainer;
    }

    /**
     * Create a single pack option
     */
    private FlowLayout createPackOption(PackOption pack) {
        boolean isSelected = selectedResourcePacks.contains(pack.key);

        FlowLayout card = WizardUIComponents.createSelectionCard(isSelected, (c) -> togglePack(pack.key));
        card.cursorStyle(CursorStyle.HAND);

        // Header with icon, title, and JVM badge if needed
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        // Icon
        header.child(Components.label(Text.literal(pack.icon))
                .color(Color.ofRgb(ACCENT_SECONDARY)));

        // Title
        header.child(Components.label(
                        Text.literal(pack.title).setStyle(Style.EMPTY.withBold(true))
                ).color(Color.ofRgb(TEXT_PRIMARY))
                .horizontalSizing(Sizing.expand()));

        // JVM warning badge
        if (pack.requiresJVM) {
            FlowLayout warningBadge = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .surface(Surface.flat(WARNING_BG).and(Surface.outline(WARNING_BORDER)))
                    .padding(Insets.of(2));

            warningBadge.child(Components.label(Text.literal("⚠ JVM"))
                    .color(Color.ofRgb(WARNING_BORDER)));

            header.child(warningBadge);
        }

        card.child(header);

        // Description
        LabelComponent desc = (LabelComponent) Components.label(Text.literal(pack.description))
                .color(Color.ofRgb(TEXT_SECONDARY))
                .horizontalSizing(Sizing.fill(100));
        card.child(desc);

        // JVM warning details if selected and requires JVM
        if (pack.requiresJVM && isSelected) {
            FlowLayout jvmWarning = WizardUIComponents.createInfoCard(
                    "⚠ Requires JVM Argument",
                    "Add -Xss4M to launcher settings. Exit wizard, add argument, restart game.",
                    0x20_F59E0B,
                    WARNING_BORDER
            );
            jvmWarning.margins(Insets.top(6));
            card.child(jvmWarning);
        }

        return card;
    }

    /**
     * Toggle pack selection
     */
    private void togglePack(String packKey) {
        if (selectedResourcePacks.contains(packKey)) {
            selectedResourcePacks.remove(packKey);
        } else {
            selectedResourcePacks.add(packKey);
        }

        // Update data store
        dataStore.setResourcePacksOrdered(new ArrayList<>(selectedResourcePacks));

        // Rebuild right panel to update UI
        rebuildRightPanel();
    }

    /**
     * Rebuild the right panel
     */
    private void rebuildRightPanel() {
        rightPanel.clearChildren();
        rightPanel.child(createSelectionHeader());
        rightPanel.child(createPackOptions());
    }

    @Override
    protected void onContinuePressed() {
        assert this.client != null;
        this.client.setScreen(WizardNavigator.createWizardPage(5));
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