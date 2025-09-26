package com.github.kd_gaming1.packcore.gui.configscreen;

import com.github.kd_gaming1.packcore.gui.util.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.ui.UITheme;
import com.github.kd_gaming1.packcore.util.ConfigApplicationManager;
import com.github.kd_gaming1.packcore.util.ConfigFileUtils;
import com.github.kd_gaming1.packcore.util.ConfigMetadata;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
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

/**
 * Main configuration menu screen - simplified version
 */
public class ModpackConfigMenuScreen extends BaseOwoScreen<FlowLayout> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ConfigFileUtils.ConfigFile selectedConfig = null;
    private FlowLayout infoPanel;
    private OverlayContainer<FlowLayout> confirmationPopup = null;

    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(UiSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png"), 1920, 1082));
        rootComponent.padding(Insets.of(8));

        rootComponent.child(createHeader());
        rootComponent.child(createMainContent());
    }

    private FlowLayout createHeader() {
        var header = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(50));
        header.gap(8);
        header.verticalAlignment(VerticalAlignment.CENTER);

        // Logo
        header.child(Components.texture(
                Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png"),
                0, 0, 40, 40, 40, 40));

        // Title
        header.child(Components.label(
                        Text.literal("Configuration Manager")
                                .styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        // Current config display
        ConfigMetadata currentMeta = ConfigFileUtils.getCurrentConfig();
        var currentConfigInfo = Containers.verticalFlow(Sizing.expand(), Sizing.content());
        currentConfigInfo.gap(2);
        currentConfigInfo.horizontalAlignment(HorizontalAlignment.RIGHT);

        currentConfigInfo.child(Components.label(
                        Text.literal("Active: " + currentMeta.getName())
                                .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));

        currentConfigInfo.child(Components.label(
                        Text.literal("v" + currentMeta.getVersion() + " | " + currentMeta.getTargetResolution()))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        header.child(currentConfigInfo);

        // Back button
        header.child(Components.button(Text.literal("Close"),
                        btn -> MinecraftClient.getInstance().setScreen(null))
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/previous.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19)));

        return header;
    }

    private FlowLayout createMainContent() {
        var mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        mainContent.gap(8);
        mainContent.child(createSidebar());
        mainContent.child(createInfoPanel());
        return mainContent;
    }

    private FlowLayout createSidebar() {
        var sidebar = Containers.verticalFlow(Sizing.fill(35), Sizing.expand());
        sidebar.gap(8);
        sidebar.surface(UiSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/menu/notif_box.png"), 607, 755));
        sidebar.padding(Insets.of(12));

        // Info text
        int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
        int padding = guiScale <= 2 ? 16 : 8;

        var infoLabel = Components.label(Text.literal("Manage your modpack configurations. Select a config to view details or apply it.")).color(UITheme.color(UITheme.TEXT_WHITE)).sizing(Sizing.fill(95), Sizing.content());

        var infoContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        infoContainer.padding(Insets.of(padding, 0, padding, 0));
        infoContainer.child(infoLabel);

        sidebar.child(infoContainer);

        sidebar.child(createConfigSection("Official Configs", ConfigFileUtils.getOfficialConfigs(), true));
        sidebar.child(createConfigSection("Custom Configs", ConfigFileUtils.getCustomConfigs(), false));

        var buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonRow.gap(4);
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonRow.child(Components.button(Text.literal("Import"),
                        btn -> MinecraftClient.getInstance().setScreen(new ConfigImportScreen()))
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19)));

        buttonRow.child(Components.button(Text.literal("Export"),
                        btn -> MinecraftClient.getInstance().setScreen(new ConfigExportScreen()))
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19)));

        sidebar.child(buttonRow);

        return sidebar;
    }

    private FlowLayout createConfigSection(String title, List<ConfigFileUtils.ConfigFile> configs, boolean official) {
        var section = Containers.verticalFlow(Sizing.fill(100), Sizing.expand(official ? 45 : 50));
        section.gap(4);
        section.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        section.padding(Insets.of(8));

        section.child(Components.label(Text.literal(title)
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        var listContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        listContent.gap(2);

        if (configs.isEmpty()) {
            listContent.child(Components.label(Text.literal("No configs found"))
                    .color(UITheme.color(UITheme.TEXT_SECONDARY)));
        } else {
            for (ConfigFileUtils.ConfigFile config : configs) {
                listContent.child(createConfigEntry(config));
            }
        }

        var scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), listContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());

        section.child(scrollContainer);
        return section;
    }

    private FlowLayout createConfigEntry(ConfigFileUtils.ConfigFile config) {
        var entry = Containers.verticalFlow(Sizing.fill(95), Sizing.content());
        entry.gap(2);
        entry.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        entry.padding(Insets.of(6));

        String version = "v" + config.getMetadata().getVersion();
        String configName = config.getDisplayName().endsWith(version)
                ? config.getDisplayName().replaceAll(version, "")
                : config.getDisplayName();

        entry.child(Components.label(Text.literal(configName))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        // Metadata badges
        var badges = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        badges.gap(4);

        badges.child(Components.label(Text.literal(config.isOfficial() ? "Official" : "Custom"))
                .color(UITheme.color(config.isOfficial() ?
                        UITheme.STATUS_SUCCESS_BORDER : UITheme.STATUS_WARNING_BORDER)));

        badges.child(Components.label(Text.literal(version))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        entry.child(badges);

        // Selection handling
        entry.mouseDown().subscribe((mouseX, mouseY, button) -> {
            selectConfig(config);
            return true;
        });

        // Hover effects
        entry.mouseEnter().subscribe(() -> {
            if (selectedConfig != config) {
                entry.surface(Surface.flat(UITheme.ENTRY_HOVER).and(Surface.outline(UITheme.ACCENT_GOLD)));
            }
        });

        entry.mouseLeave().subscribe(() -> {
            if (selectedConfig != config) {
                entry.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
            }
        });

        return entry;
    }

    private FlowLayout createInfoPanel() {
        infoPanel = Containers.verticalFlow(Sizing.fill(65), Sizing.expand());
        infoPanel.gap(8);
        infoPanel.surface(UiSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/menu/info_box.png"), 1142, 934));
        infoPanel.padding(Insets.of(14));

        showEmptyState();
        return infoPanel;
    }

    private void showEmptyState() {
        infoPanel.clearChildren();
        infoPanel.horizontalAlignment(HorizontalAlignment.CENTER);
        infoPanel.verticalAlignment(VerticalAlignment.CENTER);
        infoPanel.child(Components.label(Text.literal("Select a configuration to view details"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));
    }

    private void selectConfig(ConfigFileUtils.ConfigFile config) {
        selectedConfig = config;
        showConfigDetails();
    }

    private void showConfigDetails() {
        if (selectedConfig == null) return;

        infoPanel.clearChildren();
        infoPanel.horizontalAlignment(HorizontalAlignment.LEFT);
        infoPanel.verticalAlignment(VerticalAlignment.TOP);

        ConfigMetadata meta = selectedConfig.getMetadata();

        // Header
        int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
        int padding = guiScale <= 2 ? 6 : 0;

        infoPanel.child(Components.label(Text.literal(meta.getName())
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD))
                .margins(Insets.of(padding, 0, 0, 0)));

        // Info box
        var infoBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        infoBox.gap(4);
        infoBox.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        infoBox.padding(Insets.of(8));

        infoBox.child(createInfoRow("Version:", meta.getVersion()));
        infoBox.child(createInfoRow("Author:", meta.getAuthor()));
        infoBox.child(createInfoRow("Resolution:", meta.getTargetResolution()));
        infoBox.child(createInfoRow("Source:", meta.getSource()));

        if (meta.getCreatedDate() != null && !meta.getCreatedDate().isEmpty()) {
            infoBox.child(createInfoRow("Created:", formatDate(meta.getCreatedDate())));
        }

        infoPanel.child(infoBox);

        // Description
        if (meta.getDescription() != null && !meta.getDescription().isEmpty()) {
            infoPanel.child(Components.label(Text.literal("Description:")
                            .setStyle(Style.EMPTY.withBold(true)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));

            infoPanel.child(Components.label(Text.literal(meta.getDescription()))
                    .color(UITheme.color(UITheme.TEXT_WHITE))
                    .sizing(Sizing.fill(95), Sizing.content()));
        }

        // Mods list
        if (meta.getMods() != null && !meta.getMods().isEmpty()) {
            infoPanel.child(Components.label(Text.literal("Included Mods:")
                            .setStyle(Style.EMPTY.withBold(true)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));

            var modsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            modsContainer.gap(2);

            int displayCount = Math.min(15, meta.getMods().size());
            for (int i = 0; i < displayCount; i++) {
                modsContainer.child(Components.label(Text.literal("• " + meta.getMods().get(i)))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }

            if (meta.getMods().size() > displayCount) {
                modsContainer.child(Components.label(
                                Text.literal("... and " + (meta.getMods().size() - displayCount) + " more"))
                        .color(UITheme.color(UITheme.TEXT_SECONDARY)));
            }

            var scrollableMods = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), modsContainer);
            scrollableMods.scrollbar(ScrollContainer.Scrollbar.vanilla());
            infoPanel.child(scrollableMods);
        }

        // Action buttons
        var buttonPanel = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonPanel.gap(8);
        buttonPanel.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonPanel.child(Components.button(Text.literal("Apply Config"),
                        btn -> showConfirmationPopup())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        // Delete button for custom configs only
        if (!selectedConfig.isOfficial()) {
            buttonPanel.child(Components.button(Text.literal("Delete"),
                            btn -> deleteConfig())
                    .renderer(ButtonComponent.Renderer.texture(
                            Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                    .sizing(Sizing.fixed(100), Sizing.fixed(20)));
        }

        infoPanel.child(buttonPanel);
    }

    private FlowLayout createInfoRow(String label, String value) {
        var row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(8);
        row.child(Components.label(Text.literal(label))
                .color(UITheme.color(UITheme.TEXT_SECONDARY))
                .sizing(Sizing.fixed(80), Sizing.content()));
        row.child(Components.label(Text.literal(value))
                .color(UITheme.color(UITheme.TEXT_WHITE)));
        return row;
    }

    private String formatDate(String isoDate) {
        try {
            return isoDate.replace('T', ' ').substring(0, Math.min(isoDate.length(), 19));
        } catch (Exception e) {
            return isoDate;
        }
    }

    private void showConfirmationPopup() {
        if (selectedConfig == null) return;

        var popup = Containers.verticalFlow(Sizing.fixed(400), Sizing.content());
        popup.gap(8);
        popup.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        popup.padding(Insets.of(16));

        popup.child(Components.label(Text.literal("Apply Configuration?")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));

        popup.child(Components.label(Text.literal(
                        "This will restart the game and apply:\n" +
                                selectedConfig.getDisplayName()))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        popup.child(Components.label(Text.literal(
                        "⚠ Current configurations will be backed up automatically."))
                .color(UITheme.color(UITheme.STATUS_WARNING_BORDER)));

        var buttons = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttons.gap(8);
        buttons.horizontalAlignment(HorizontalAlignment.CENTER);

        buttons.child(Components.button(Text.literal("Apply"), btn -> applyConfig())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 60))
                .sizing(Sizing.fixed(90), Sizing.fixed(20)));

        buttons.child(Components.button(Text.literal("Cancel"), btn -> closePopup())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 60))
                .sizing(Sizing.fixed(90), Sizing.fixed(20)));

        popup.child(buttons);

        confirmationPopup = Containers.overlay(popup);
        confirmationPopup.positioning(Positioning.relative(50, 40));
        confirmationPopup.zIndex(10);

        this.uiAdapter.rootComponent.child(confirmationPopup);
    }

    private void applyConfig() {
        if (selectedConfig == null) return;

        closePopup();

        try {
            ConfigApplicationManager.scheduleConfigApplication(selectedConfig);

            // Show notification
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Applying: " + selectedConfig.getDisplayName() + " - Restarting..."),
                        false);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to apply config", e);
        }
    }

    private void deleteConfig() {
        if (selectedConfig == null || selectedConfig.isOfficial()) return;

        if (ConfigFileUtils.deleteConfig(selectedConfig)) {
            selectedConfig = null;

            // Refresh screen
            this.build(this.uiAdapter.rootComponent);
        }
    }

    private void closePopup() {
        if (confirmationPopup != null) {
            this.uiAdapter.rootComponent.removeChild(confirmationPopup);
            confirmationPopup = null;
        }
    }
}