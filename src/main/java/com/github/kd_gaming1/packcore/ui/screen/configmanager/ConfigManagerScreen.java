package com.github.kd_gaming1.packcore.ui.screen.configmanager;

import com.github.kd_gaming1.packcore.config.apply.ConfigApplyService;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.github.kd_gaming1.packcore.config.model.ConfigMetadata;
import com.github.kd_gaming1.packcore.ui.screen.base.BasePackCoreScreen;
import com.github.kd_gaming1.packcore.ui.screen.components.ScreenUIComponents;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
//? if >= 1.21.10 {
/*import net.minecraft.text.StyleSpriteSource;
*///?}
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Main configuration menu screen - refactored for improved maintainability.
 * Uses ScreenUIComponents for common patterns and cleaner code structure.
 */
public class ConfigManagerScreen extends BasePackCoreScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ConfigFileRepository.ConfigFile selectedConfig = null;
    private FlowLayout infoPanel;
    private final Map<ConfigFileRepository.ConfigFile, FlowLayout> entryComponents = new HashMap<>();

    public ConfigManagerScreen() {
        super(null); // No parent - closes to main menu
    }

    @Override
    protected Component createTitleLabel() {
        return Components.label(
                Text.literal("Configuration Manager")
                        //? if <= 1.21.8 {
                        .styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))
                         //?} else
                        //.styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte"))))
        ).color(color(TEXT_PRIMARY));
    }

    @Override
    protected FlowLayout createHeaderActions() {
        FlowLayout actions = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(8);

        // Current config display
        ConfigMetadata currentMeta = ConfigFileRepository.getCurrentConfig();
        FlowLayout currentConfigInfo = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(2)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .margins(Insets.right(8));

        currentConfigInfo.child(Components.label(
                Text.literal("Active: " + currentMeta.getName()).setStyle(Style.EMPTY.withBold(true))
        ).color(color(ACCENT_SECONDARY)));

        currentConfigInfo.child(Components.label(
                Text.literal("v" + currentMeta.getVersion() + " | " + currentMeta.getTargetResolution())
        ).color(color(TEXT_SECONDARY)));

        actions.child(currentConfigInfo);

        // Close button
        actions.child(ScreenUIComponents.createButton("Close",
                btn -> navigateBack(), 90, 19));

        return actions;
    }

    @Override
    protected FlowLayout createMainContent() {
        FlowLayout mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand())
                .gap(8);

        mainContent.child(createSidebar());
        mainContent.child(createInfoPanel());

        return mainContent;
    }

    // ===== Sidebar =====

    private FlowLayout createSidebar() {
        FlowLayout sidebar = ScreenUIComponents.createSidebar(35);

        // Info text
        sidebar.child(createInfoText());

        // Config sections
        sidebar.child(createConfigSection("Official Configs",
                ConfigFileRepository.getOfficialConfigs(), true));
        sidebar.child(createConfigSection("Custom Configs",
                ConfigFileRepository.getCustomConfigs(), false));

        // Action buttons
        sidebar.child(createSidebarButtons());

        return sidebar;
    }

    private FlowLayout createInfoText() {
        int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
        int padding = guiScale <= 2 ? 16 : 8;

        FlowLayout infoContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .padding(Insets.of(padding, 0, padding, 0));

        LabelComponent infoLabel = (LabelComponent) Components.label(
                        Text.literal("Manage your modpack configurations. Select a config to view details or apply it.")
                ).color(color(TEXT_PRIMARY))
                .sizing(Sizing.fill(95), Sizing.content());

        infoContainer.child(infoLabel);
        return infoContainer;
    }

    private FlowLayout createConfigSection(String title, List<ConfigFileRepository.ConfigFile> configs,
                                           boolean isOfficial) {
        FlowLayout section = ScreenUIComponents.createSection(title, isOfficial ? 45 : 50);

        // List content
        FlowLayout listContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2);

        if (configs.isEmpty()) {
            listContent.child(Components.label(Text.literal("No configs found"))
                    .color(color(TEXT_SECONDARY)));
        } else {
            for (ConfigFileRepository.ConfigFile config : configs) {
                listContent.child(createConfigEntry(config));
            }
        }

        // Wrap in scroll container
        section.child(ScreenUIComponents.createScrollContainer(listContent));

        return section;
    }

    private FlowLayout createConfigEntry(ConfigFileRepository.ConfigFile config) {
        FlowLayout entry = ScreenUIComponents.createListEntry();

        // Extract display name
        String version = "v" + config.metadata().getVersion();
        String configName = config.getDisplayName().endsWith(version)
                ? config.getDisplayName().replaceAll(version, "")
                : config.getDisplayName();

        // Name
        entry.child(Components.label(Text.literal(configName))
                .color(color(TEXT_PRIMARY)));

        // Badges
        FlowLayout badges = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        badges.child(Components.label(Text.literal(config.official() ? "Official" : "Custom"))
                .color(color(config.official() ? SUCCESS_BORDER : WARNING_BORDER)));

        badges.child(Components.label(Text.literal(version))
                .color(color(TEXT_SECONDARY)));

        entry.child(badges);

        // Store reference
        entryComponents.put(config, entry);

        // Apply hover and selection
        ScreenUIComponents.applyHoverEffects(entry, () -> selectConfig(config));

        return entry;
    }

    private FlowLayout createSidebarButtons() {
        FlowLayout buttonRow = (FlowLayout) Containers.ltrTextFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        buttonRow.child(ScreenUIComponents.createButton("Import",
                        btn -> MinecraftClient.getInstance().setScreen(new ImportConfigScreen()), 90, 19)
                .margins(Insets.bottom(4)));

        buttonRow.child(ScreenUIComponents.createButton("Export",
                        btn -> MinecraftClient.getInstance().setScreen(new ExportConfigScreen()), 90, 19)
                .margins(Insets.bottom(4)));

        buttonRow.child(ScreenUIComponents.createButton("Backup",
                        btn -> MinecraftClient.getInstance().setScreen(new BackupManagementScreen()), 90, 19)
                .margins(Insets.bottom(4)));

        return buttonRow;
    }

    // ===== Info Panel =====

    private FlowLayout createInfoPanel() {
        infoPanel = ScreenUIComponents.createInfoPanel(65);
        showEmptyState();
        return infoPanel;
    }

    private void showEmptyState() {
        infoPanel.clearChildren();
        infoPanel.child(ScreenUIComponents.createEmptyState(
                "Select a configuration to view details"));
    }

    private void selectConfig(ConfigFileRepository.ConfigFile config) {
        // Reset previous selection
        if (selectedConfig != null && entryComponents.containsKey(selectedConfig)) {
            ScreenUIComponents.applySelectedState(entryComponents.get(selectedConfig), false);
        }

        // Set new selection
        selectedConfig = config;
        if (entryComponents.containsKey(config)) {
            ScreenUIComponents.applySelectedState(entryComponents.get(config), true);
        }

        showConfigDetails();
    }

    private void showConfigDetails() {
        if (selectedConfig == null) return;

        infoPanel.clearChildren();
        infoPanel.horizontalAlignment(HorizontalAlignment.LEFT);
        infoPanel.verticalAlignment(VerticalAlignment.TOP);

        ConfigMetadata meta = selectedConfig.metadata();
        int padding = MinecraftClient.getInstance().options.getGuiScale().getValue() <= 2 ? 6 : 0;

        // Create scrollable content container
        FlowLayout scrollableContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8);

        // Header
        scrollableContent.child(Components.label(Text.literal(meta.getName())
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(color(ACCENT_SECONDARY))
                .margins(Insets.of(padding, 0, 0, 0)));

        // Info box
        FlowLayout infoBox = ScreenUIComponents.createInfoBox();
        infoBox.child(ScreenUIComponents.createInfoRow("Version:", meta.getVersion()));
        infoBox.child(ScreenUIComponents.createInfoRow("Author:", meta.getAuthor()));
        infoBox.child(ScreenUIComponents.createInfoRow("Resolution:", meta.getTargetResolution()));
        infoBox.child(ScreenUIComponents.createInfoRow("Source:", meta.getSource()));

        if (meta.getCreatedDate() != null && !meta.getCreatedDate().isEmpty()) {
            infoBox.child(ScreenUIComponents.createInfoRow("Created:",
                    ScreenUIComponents.formatTimestamp(meta.getCreatedDate())));
        }

        scrollableContent.child(infoBox);

        // Description
        if (meta.getDescription() != null && !meta.getDescription().isEmpty()) {
            scrollableContent.child(Components.label(Text.literal("Description:")
                            .setStyle(Style.EMPTY.withBold(true)))
                    .color(color(ACCENT_SECONDARY)));

            scrollableContent.child(Components.label(Text.literal(meta.getDescription()))
                    .color(color(TEXT_PRIMARY))
                    .sizing(Sizing.fill(94), Sizing.content()));
        }

        // Mods list
        scrollableContent.child(createModsList(meta));

        // Add scrollable content to panel
        infoPanel.child(ScreenUIComponents.createScrollContainer(scrollableContent)
                .sizing(Sizing.fill(96), Sizing.expand()));

        // Action buttons (outside scroll, always visible at bottom)
        infoPanel.child(createActionButtons());
    }

    private Component createModsList(ConfigMetadata meta) {
        if (meta.getMods() == null || meta.getMods().isEmpty()) {
            return Containers.verticalFlow(Sizing.content(), Sizing.content());
        }

        FlowLayout container = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        container.child(Components.label(Text.literal("Included Mods:")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(color(ACCENT_SECONDARY)));

        FlowLayout modsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2);

        int displayCount = Math.min(15, meta.getMods().size());
        for (int i = 0; i < displayCount; i++) {
            modsContainer.child(Components.label(Text.literal("• " + meta.getMods().get(i)))
                    .color(color(TEXT_PRIMARY)));
        }

        if (meta.getMods().size() > displayCount) {
            modsContainer.child(Components.label(
                    Text.literal("... and " + (meta.getMods().size() - displayCount) + " more")
            ).color(color(TEXT_SECONDARY)));
        }

        container.child(ScreenUIComponents.createScrollContainer(modsContainer)
                .sizing(Sizing.fill(100), Sizing.fixed(150)));

        return container;
    }

    private FlowLayout createActionButtons() {
        FlowLayout buttonPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.top(12));

        // Full config
        buttonPanel.child(ScreenUIComponents.createButton("Apply Full Config",
                btn -> showConfirmationDialog(), 120, 20));

        // Selective config (NEW)
        buttonPanel.child(ScreenUIComponents.createButton("Apply Specific Files",
                btn -> MinecraftClient.getInstance().setScreen(
                        new SelectiveFileApplicationScreen(selectedConfig, this)), 120, 20));

        // Delete (if custom)
        if (!selectedConfig.official()) {
            buttonPanel.child(ScreenUIComponents.createButton("Delete",
                    btn -> deleteConfig(), 90, 20));
        }

        return buttonPanel;
    }

    // ===== Actions =====

    private void showConfirmationDialog() {
        if (selectedConfig == null) return;

        FlowLayout dialog = ScreenUIComponents.createDialog(
                "Apply Configuration?",
                "This will close the game, and when the game is open again, apply:\"\n" + selectedConfig.getDisplayName() +
                        "\n\n⚠ Current configurations will be backed up automatically.",
                400
        );

        FlowLayout buttons = ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Apply", btn -> applyConfig(), 90, 20),
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay(), 90, 20)
        );

        dialog.child(buttons);
        showOverlay(dialog, false);
    }

    private void applyConfig() {
        if (selectedConfig == null) return;

        closeTopOverlay();

        try {
            ConfigApplyService.scheduleConfigApplication(selectedConfig);

            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Applying: " + selectedConfig.getDisplayName() + " - Restarting..."),
                        false
                );
            }
        } catch (Exception e) {
            LOGGER.error("Failed to apply config", e);
        }
    }

    private void deleteConfig() {
        if (selectedConfig == null || selectedConfig.official()) return;

        if (ConfigFileRepository.deleteConfig(selectedConfig)) {
            selectedConfig = null;
            // Rebuild the screen
            build(this.uiAdapter.rootComponent);
        }
    }
}