package com.github.kd_gaming1.packcore.gui.configscreen;

import com.github.kd_gaming1.packcore.util.ConfigApplicationManager;
import com.github.kd_gaming1.packcore.util.ConfigFileUtils;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;

public class ModpackConfigMenuScreen extends BaseOwoScreen<FlowLayout> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Theme constants
    protected static final int OVERLAY_DARK = 0x80_000000;
    protected static final int PANEL_BACKGROUND = 0xC0_1A1A1A;
    protected static final int ACCENT_GOLD = 0xFF_FFD700;
    protected static final int TEXT_WHITE = 0xFFFFFF;
    protected static final int TEXT_SECONDARY = 0xB9BBBE;

    // Status panel colors
    protected static final int STATUS_SUCCESS_BG = 0xC0_2D5016;
    protected static final int STATUS_SUCCESS_BORDER = 0xFF_52C41A;
    protected static final int STATUS_WARNING_BG = 0xC0_5C3317;
    protected static final int STATUS_WARNING_BORDER = 0xFF_FAAD14;
    protected static final int STATUS_ERROR_BG = 0xC0_5C1717;
    protected static final int STATUS_ERROR_BORDER = 0xFF_FF4D4F;

    // List entry colors
    protected static final int ENTRY_BACKGROUND = 0xC0_2A2A2A;
    protected static final int ENTRY_HOVER = 0xC0_3A3A3A;
    protected static final int ENTRY_SELECTED = 0xC0_4A4A4A;
    protected static final int ENTRY_BORDER = 0xFF_555555;

    private Identifier backgroundTexture;
    private ConfigFileUtils.ConfigFile selectedConfig = null;
    private FlowLayout selectedEntry = null;
    private FlowLayout infoPanel;
    private OverlayContainer<FlowLayout> currentPopup = null;

    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        int backgroundWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int backgroundHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        backgroundTexture = Identifier.of(MOD_ID, "textures/gui/wizard/test_temp.png");
        // Set background
        rootComponent.surface(Surface.tiled(backgroundTexture, backgroundWidth, backgroundHeight));

        rootComponent.padding(Insets.of(16));

        // Create main layout
        rootComponent.child(createHeader());

        // Create horizontal layout for sidebar and info panel
        FlowLayout contentArea = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        contentArea.gap(8);

        contentArea.child(createSidebar());

        // Create info panel
        infoPanel = createInfoPanel();
        contentArea.child(infoPanel);

        rootComponent.child(contentArea);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);

        int backgroundWidth = client.getWindow().getScaledWidth();
        int backgroundHeight = client.getWindow().getScaledHeight();

        // Update the background surface directly
        this.uiAdapter.rootComponent.surface(Surface.tiled(backgroundTexture, backgroundWidth, backgroundHeight));
    }

    private FlowLayout createHeader() {
        FlowLayout header = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        header.padding(Insets.of(8));
        header.verticalAlignment(VerticalAlignment.CENTER);

        Identifier logoId = Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png");
        TextureComponent logo = Components.texture(logoId, 0, 0, 48, 48, 48 ,48);

        Text titleText = Text.literal(getModpackInfo().getName()).setStyle(Style.EMPTY.withBold(Boolean.TRUE));
        LabelComponent titleLabel = Components.label(titleText).color(Color.ofRgb(TEXT_WHITE));
        titleLabel.margins(Insets.of(8, 0, 0, 0));

        // Current config info section
        FlowLayout currentConfigSection = Containers.horizontalFlow(Sizing.expand(), Sizing.content());
        currentConfigSection.gap(8);
        currentConfigSection.horizontalAlignment(HorizontalAlignment.RIGHT);
        currentConfigSection.verticalAlignment(VerticalAlignment.CENTER);

        // Get current config
        ConfigFileUtils.ConfigMetadata currentConfig = ConfigFileUtils.getCurrentConfig();

        FlowLayout configInfo = Containers.verticalFlow(Sizing.content(), Sizing.content());
        configInfo.gap(2);
        configInfo.horizontalAlignment(HorizontalAlignment.RIGHT);

        LabelComponent currentLabel = Components.label(Text.literal("Current: " + currentConfig.getName())
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(ACCENT_GOLD));

        FlowLayout detailsRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        detailsRow.gap(8);

        LabelComponent versionLabel = Components.label(Text.literal("v" + currentConfig.getVersion()))
                .color(Color.ofRgb(TEXT_SECONDARY));

        LabelComponent resolutionLabel = Components.label(Text.literal("(" + currentConfig.getRecommendedResolution() + ")"))
                .color(Color.ofRgb(TEXT_SECONDARY));

        detailsRow.child(versionLabel);
        detailsRow.child(resolutionLabel);

        configInfo.child(currentLabel);
        configInfo.child(detailsRow);
        currentConfigSection.child(configInfo);

        header.child(logo);
        header.child(titleLabel);
        header.child(currentConfigSection);
        header.margins(Insets.bottom(8));

        return header;
    }

    private FlowLayout createSidebar() {
        FlowLayout sidebar = Containers.verticalFlow(Sizing.fill(30), Sizing.expand());
        sidebar.gap(6);
        sidebar.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        sidebar.padding(Insets.of(8));
        sidebar.horizontalAlignment(HorizontalAlignment.CENTER);

        // Info section at the top
        FlowLayout infoSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        infoSection.gap(4);
        infoSection.surface(Surface.flat(0xC0_2A3A2A).and(Surface.outline(0xFF_4A7C59)));
        infoSection.padding(Insets.of(8));

        LabelComponent infoLabel = (LabelComponent) Components.label(Text.literal("From this screen you can see available config options and apply them to use instead of the current one applied. You can also create/import new configs from this menu."))
                .color(Color.ofRgb(TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content());

        infoSection.child(infoLabel);
        sidebar.child(infoSection);

        // Top section (Shows a list of official config options available)
        FlowLayout topSection = createOfficialConfigSection();

        // Bottom section (Shows a list of custom/community config options available)
        FlowLayout bottomSection = createCustomConfigSection();

        sidebar.child(topSection);
        sidebar.child(bottomSection);
        return sidebar;
    }

    private FlowLayout createOfficialConfigSection() {
        FlowLayout topSection = Containers.verticalFlow(Sizing.fill(100), Sizing.expand(45));
        topSection.gap(4);
        topSection.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        topSection.horizontalAlignment(HorizontalAlignment.CENTER);
        topSection.padding(Insets.of(8));

        LabelComponent header = Components.label(Text.literal("Official Config Options")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(TEXT_WHITE));
        header.margins(Insets.of(0, 0, 4, 0));

        topSection.child(header);

        // Create scrollable list for official configs
        List<ConfigFileUtils.ConfigFile> officialConfigs = ConfigFileUtils.getOfficialConfigs();
        ScrollContainer<FlowLayout> officialScrollContainer = createConfigList(officialConfigs, true);

        topSection.child(officialScrollContainer);

        return topSection;
    }

    private FlowLayout createCustomConfigSection() {
        FlowLayout bottomSection = Containers.verticalFlow(Sizing.fill(100), Sizing.expand(50));
        bottomSection.gap(4);
        bottomSection.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        bottomSection.horizontalAlignment(HorizontalAlignment.CENTER);
        bottomSection.padding(Insets.of(8));

        LabelComponent header2 = Components.label(Text.literal("Custom/Community Config Options")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(TEXT_WHITE));
        header2.margins(Insets.of(0, 0, 4, 0));

        bottomSection.child(header2);

        // Create scrollable list for custom configs
        List<ConfigFileUtils.ConfigFile> customConfigs = ConfigFileUtils.getCustomConfigs();
        ScrollContainer<FlowLayout> customScrollContainer = createConfigList(customConfigs, false);

        bottomSection.child(customScrollContainer);

        return bottomSection;
    }

    private ScrollContainer<FlowLayout> createConfigList(List<ConfigFileUtils.ConfigFile> configs, boolean isOfficial) {
        FlowLayout listContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        listContent.gap(2);

        if (configs.isEmpty()) {
            // Show empty state
            LabelComponent emptyLabel = Components.label(Text.literal("No configs found"))
                    .color(Color.ofRgb(TEXT_SECONDARY));
            listContent.child(emptyLabel);
        } else {
            // Add each config as a clickable entry
            for (ConfigFileUtils.ConfigFile config : configs) {
                FlowLayout entry = createConfigEntry(config);
                listContent.child(entry);
            }
        }

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), listContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollStep(10);

        return scrollContainer;
    }

    private FlowLayout createConfigEntry(ConfigFileUtils.ConfigFile config) {
        FlowLayout entry = Containers.verticalFlow(Sizing.fill(95), Sizing.content());
        entry.gap(2);
        entry.surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
        entry.padding(Insets.of(6));
        entry.margins(Insets.of(1));

        // Config name label
        LabelComponent nameLabel = Components.label(Text.literal(config.getDisplayName()))
                .color(Color.ofRgb(TEXT_WHITE));

        // Type indicator (Official/Custom)
        String typeText = config.isOfficial() ? "Official" : "Custom";
        int typeColor = config.isOfficial() ? STATUS_SUCCESS_BORDER : STATUS_WARNING_BORDER;
        LabelComponent typeLabel = Components.label(Text.literal(typeText))
                .color(Color.ofRgb(typeColor));

        entry.child(nameLabel);
        entry.child(typeLabel);

        // Add click handling
        entry.mouseDown().subscribe((mouseX, mouseY, button) -> {
            selectConfig(config, entry);
            return true;
        });

        // Add hover effect
        entry.mouseEnter().subscribe(() -> {
            if (selectedEntry != entry) {
                entry.surface(Surface.flat(ENTRY_HOVER).and(Surface.outline(ACCENT_GOLD)));
            }
        });

        entry.mouseLeave().subscribe(() -> {
            if (selectedEntry != entry) {
                entry.surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
            }
        });

        return entry;
    }

    private void selectConfig(ConfigFileUtils.ConfigFile config, FlowLayout entry) {
        // Deselect previous entry
        if (selectedEntry != null) {
            selectedEntry.surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
        }

        // Select new entry
        selectedConfig = config;
        selectedEntry = entry;
        entry.surface(Surface.flat(ENTRY_SELECTED).and(Surface.outline(ACCENT_GOLD)));

        // Update info panel
        updateInfoPanel();
    }

    private void updateInfoPanel() {
        if (selectedConfig == null) return;

        // Clear existing content
        infoPanel.clearChildren();

        ConfigFileUtils.ConfigMetadata metadata = selectedConfig.getMetadata();

        // Header with file name
        FlowLayout header = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        header.padding(Insets.of(8));
        header.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent headerLabel = Components.label(Text.literal(selectedConfig.getDisplayName())
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(TEXT_WHITE));
        header.child(headerLabel);
        infoPanel.child(header);

        // Source information
        FlowLayout extractInfo = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        extractInfo.gap(2);
        extractInfo.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
        extractInfo.padding(Insets.of(8));

        extractInfo.child(Components.label(Text.literal("Source: " + metadata.getSource()))
                .color(Color.ofRgb(TEXT_WHITE)));
        extractInfo.child(Components.label(Text.literal("Author: " + metadata.getAuthor()))
                .color(Color.ofRgb(TEXT_SECONDARY)));
        extractInfo.child(Components.label(Text.literal("Version: " + metadata.getVersion()))
                .color(Color.ofRgb(TEXT_SECONDARY)));

        infoPanel.child(extractInfo);

        // Content information - scrollable
        FlowLayout contentWrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.expand());
        contentWrapper.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
        contentWrapper.padding(Insets.of(2)); // Reduced padding for wrapper

        FlowLayout scrollableContent = Containers.verticalFlow(Sizing.fill(98), Sizing.content());
        scrollableContent.gap(4);
        scrollableContent.padding(Insets.of(6));

        // Description
        scrollableContent.child(Components.label(Text.literal("Description:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(ACCENT_GOLD)));
        scrollableContent.child(Components.label(Text.literal(metadata.getDescription()))
                .color(Color.ofRgb(TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content()));

        // Technical details
        scrollableContent.child(Components.label(Text.literal("Technical Details:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(ACCENT_GOLD)));
        scrollableContent.child(Components.label(Text.literal("Resolution: " + metadata.getRecommendedResolution()))
                .color(Color.ofRgb(TEXT_SECONDARY)));
        scrollableContent.child(Components.label(Text.literal("Difficulty: " + metadata.getDifficulty()))
                .color(Color.ofRgb(TEXT_SECONDARY)));
        scrollableContent.child(Components.label(Text.literal("Category: " + metadata.getCategory()))
                .color(Color.ofRgb(TEXT_SECONDARY)));

        // Features
        if (!metadata.getFeatures().isEmpty()) {
            scrollableContent.child(Components.label(Text.literal("Features:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                    .color(Color.ofRgb(ACCENT_GOLD)));
            for (String feature : metadata.getFeatures()) {
                scrollableContent.child(Components.label(Text.literal("• " + feature))
                        .color(Color.ofRgb(TEXT_WHITE)));
            }
        }

        // Create scroll container
        ScrollContainer<FlowLayout> contentScrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), scrollableContent);
        contentScrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        contentScrollContainer.scrollStep(15);

        contentWrapper.child(contentScrollContainer);
        infoPanel.child(contentWrapper);

        // Action button at the bottom
        FlowLayout buttonPanel = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonPanel.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
        buttonPanel.padding(Insets.of(8));
        buttonPanel.horizontalAlignment(HorizontalAlignment.CENTER);

        ButtonComponent applyButton = (ButtonComponent) Components.button(Text.literal("Apply Config"), button -> {
                    showConfirmationPopup();
                })
                .renderer(ButtonComponent.Renderer.flat(STATUS_SUCCESS_BG, STATUS_SUCCESS_BORDER, ENTRY_BORDER))
                .sizing(Sizing.fixed(120), Sizing.fixed(25));

        buttonPanel.child(applyButton);
        infoPanel.child(buttonPanel);
    }

    private void showConfirmationPopup() {
        if (selectedConfig == null) return;

        currentPopup = ConfigConfirmationPopup.createConfirmationPopup(
                selectedConfig,
                this::onConfigApply,
                this::closeConfirmationPopup
        );

        currentPopup.zIndex(10);

        this.uiAdapter.rootComponent.child(currentPopup);
    }

    private void closeConfirmationPopup() {
        if (currentPopup != null) {
            this.uiAdapter.rootComponent.removeChild(currentPopup);
            currentPopup = null;
        }
    }

    private void onConfigApply() {
        if (selectedConfig == null) return;

        closeConfirmationPopup();

        try {
            // Use the new config application manager
            ConfigApplicationManager.applyConfigOnRestart(selectedConfig);

            // Show a final message before the game closes
            // This will be very brief since the game will shut down quickly
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Applying config: " + selectedConfig.getDisplayName() + " - Game will restart..."),
                        false
                );
            }

        } catch (Exception e) {
            LOGGER.error("Failed to apply config", e);

            // Show error message
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Failed to apply config: " + e.getMessage()).styled(style ->
                                style.withColor(net.minecraft.util.Formatting.RED)),
                        false
                );
            }
        }
    }

    // Info panel for config details (name, description, source, etc.)
    private FlowLayout createInfoPanel() {
        FlowLayout infoPanel = Containers.verticalFlow(Sizing.expand(70), Sizing.expand());
        infoPanel.gap(4);
        infoPanel.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        infoPanel.padding(Insets.of(8));
        infoPanel.horizontalAlignment(HorizontalAlignment.CENTER);

        // Default empty state
        LabelComponent emptyLabel = (LabelComponent) Components.label(Text.literal("Select a config to view details"))
                .color(Color.ofRgb(TEXT_SECONDARY));
        infoPanel.child(emptyLabel);

        return infoPanel;
    }
}