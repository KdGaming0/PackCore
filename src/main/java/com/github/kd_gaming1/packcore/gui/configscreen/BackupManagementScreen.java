package com.github.kd_gaming1.packcore.gui.configscreen;

import com.github.kd_gaming1.packcore.gui.util.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.ui.UITheme;
import com.github.kd_gaming1.packcore.util.BackupManager;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;

/**
 * Backup management screen - matches the style of other config screens
 */
public class BackupManagementScreen extends BaseOwoScreen<FlowLayout> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private BackupManager.BackupInfo selectedBackup = null;
    private FlowLayout infoPanel;
    private FlowLayout rootComponent;
    private FlowLayout sidebarContent; // Store reference to rebuild
    private OverlayContainer<FlowLayout> currentOverlay = null;

    // Store entry components to update their surfaces
    private Map<BackupManager.BackupInfo, FlowLayout> entryComponents = new HashMap<>();

    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.rootComponent = rootComponent;

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
                        Text.literal("Backup Manager - " + getModpackInfo().getName())
                                .styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        // Back button
        var backContainer = Containers.horizontalFlow(Sizing.expand(), Sizing.content());
        backContainer.horizontalAlignment(HorizontalAlignment.RIGHT);
        backContainer.child(Components.button(Text.literal("Back"),
                        btn -> MinecraftClient.getInstance().setScreen(new ModpackConfigMenuScreen()))
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/previous.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19)));
        header.child(backContainer);

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

        var infoLabel = Components.label(Text.literal(
                        "Manage your configuration backups. Auto backups are created before applying new configs, and you can create manual backups anytime."))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content());

        var infoContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        infoContainer.padding(Insets.of(padding, 0, padding, 0));
        infoContainer.child(infoLabel);

        sidebar.child(infoContainer);

        // Create scrollable content container
        sidebarContent = Containers.verticalFlow(Sizing.fill(98), Sizing.content());
        sidebarContent.gap(8);

        rebuildSidebarContent();

        var scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), sidebarContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        sidebar.child(scrollContainer);

        // Action buttons
        var buttonContainer = Containers.ltrTextFlow(Sizing.fill(100), Sizing.content());
        buttonContainer.gap(4);
        buttonContainer.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonContainer.child(Components.button(Text.literal("Create Backup"),
                        btn -> showCreateBackupDialog())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19))
                .margins(Insets.bottom(4)));

        buttonContainer.child(Components.button(Text.literal("Open Folder"),
                        btn -> BackupManager.openBackupsFolder())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19))
                .margins(Insets.bottom(4)));

        buttonContainer.child(Components.button(Text.literal("Refresh"),
                        btn -> refreshBackupsList())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19))
                .margins(Insets.bottom(4)));

        sidebar.child(buttonContainer);

        return sidebar;
    }

    // FIX: New method to rebuild sidebar content when refreshing
    private void rebuildSidebarContent() {
        sidebarContent.clearChildren();
        entryComponents.clear();

        // Backups sections
        List<BackupManager.BackupInfo> allBackups = BackupManager.getBackups();

        List<BackupManager.BackupInfo> manualBackups = allBackups.stream()
                .filter(b -> b.type == BackupManager.BackupType.MANUAL)
                .toList();

        List<BackupManager.BackupInfo> autoBackups = allBackups.stream()
                .filter(b -> b.type == BackupManager.BackupType.AUTO)
                .toList();

        sidebarContent.child(createBackupSection("Manual Backups", manualBackups, true));
        sidebarContent.child(createBackupSection("Auto Backups", autoBackups, false));
    }

    private FlowLayout createBackupSection(String title, List<BackupManager.BackupInfo> backups, boolean isManual) {
        var section = Containers.verticalFlow(Sizing.fill(100), Sizing.expand(isManual ? 45 : 50));
        section.gap(4);
        section.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        section.padding(Insets.of(8));

        section.child(Components.label(Text.literal(title)
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        var listContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        listContent.gap(2);

        if (backups.isEmpty()) {
            listContent.child(Components.label(Text.literal("No backups found"))
                    .color(UITheme.color(UITheme.TEXT_SECONDARY)));
        } else {
            for (BackupManager.BackupInfo backup : backups) {
                listContent.child(createBackupEntry(backup));
            }
        }

        var scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), listContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());

        section.child(scrollContainer);
        return section;
    }

    private FlowLayout createBackupEntry(BackupManager.BackupInfo backup) {
        var entry = Containers.verticalFlow(Sizing.fill(95), Sizing.content());
        entry.gap(2);
        entry.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        entry.padding(Insets.of(6));

        // Backup title (updated to use title field)
        String displayTitle = backup.title != null && !backup.title.isEmpty() ? backup.title : backup.configName;
        entry.child(Components.label(Text.literal(displayTitle))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        // Metadata badges
        var badges = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        badges.gap(4);

        badges.child(Components.label(Text.literal(backup.type.getDisplayName()))
                .color(UITheme.color(backup.type == BackupManager.BackupType.MANUAL ?
                        UITheme.STATUS_SUCCESS_BORDER : UITheme.STATUS_WARNING_BORDER)));

        badges.child(Components.label(Text.literal("v" + backup.configVersion))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        entry.child(badges);

        // Store the entry component
        entryComponents.put(backup, entry);

        // Selection handling
        entry.mouseDown().subscribe((mouseX, mouseY, button) -> {
            selectBackup(backup);
            return true;
        });

        // Hover effects
        entry.mouseEnter().subscribe(() -> {
            if (selectedBackup != backup) {
                entry.surface(Surface.flat(UITheme.ENTRY_HOVER).and(Surface.outline(UITheme.ACCENT_GOLD)));
            }
        });

        entry.mouseLeave().subscribe(() -> {
            if (selectedBackup != backup) {
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
        infoPanel.child(Components.label(Text.literal("Select a backup to view details"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));
    }

    private void selectBackup(BackupManager.BackupInfo backup) {
        // Reset previous selection's surface
        if (selectedBackup != null && entryComponents.containsKey(selectedBackup)) {
            FlowLayout previousEntry = entryComponents.get(selectedBackup);
            previousEntry.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        }

        // Set new selection
        selectedBackup = backup;

        // Update new selection's surface
        if (entryComponents.containsKey(backup)) {
            FlowLayout currentEntry = entryComponents.get(backup);
            currentEntry.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        }

        showBackupDetails();
    }

    private void showBackupDetails() {
        if (selectedBackup == null) return;

        infoPanel.clearChildren();
        infoPanel.horizontalAlignment(HorizontalAlignment.LEFT);
        infoPanel.verticalAlignment(VerticalAlignment.TOP);

        // Header - use title if available, otherwise configName
        int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
        int padding = guiScale <= 2 ? 6 : 0;

        String headerText = selectedBackup.title != null && !selectedBackup.title.isEmpty()
                ? selectedBackup.title : selectedBackup.configName;

        infoPanel.child(Components.label(Text.literal(headerText)
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD))
                .margins(Insets.of(padding, 0, 0, 0)));

        // Info box
        var infoBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        infoBox.gap(4);
        infoBox.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        infoBox.padding(Insets.of(8));

        infoBox.child(createInfoRow("Type:", selectedBackup.type.getDisplayName()));
        infoBox.child(createInfoRow("Config:", selectedBackup.configName));
        infoBox.child(createInfoRow("Version:", selectedBackup.configVersion));
        infoBox.child(createInfoRow("Created:", formatTimestamp(selectedBackup.timestamp)));
        infoBox.child(createInfoRow("Size:", formatSize(selectedBackup.sizeBytes)));
        infoBox.child(createInfoRow("Backup ID:", selectedBackup.backupId));

        infoPanel.child(infoBox);

        // Description - only show if not null/empty
        if (selectedBackup.description != null && !selectedBackup.description.trim().isEmpty()) {
            infoPanel.child(Components.label(Text.literal("Description:")
                            .setStyle(Style.EMPTY.withBold(true)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));

            infoPanel.child(Components.label(Text.literal(selectedBackup.description))
                    .color(UITheme.color(UITheme.TEXT_WHITE))
                    .sizing(Sizing.fill(95), Sizing.content()));
        }

        // Warning message
        var warningBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        warningBox.gap(4);
        warningBox.surface(Surface.flat(UITheme.STATUS_WARNING_BG)
                .and(Surface.outline(UITheme.STATUS_WARNING_BORDER)));
        warningBox.padding(Insets.of(12));

        warningBox.child(Components.label(Text.literal("⚠ Restore Information")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.STATUS_WARNING_BORDER)));

        warningBox.child(Components.label(Text.literal(
                        "Restoring a backup will replace your current configuration files. " +
                                "An auto-backup will be created before restoring."))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content()));

        infoPanel.child(warningBox);

        // Action buttons - FIX: Use vertical layout for proper wrapping
        var buttonPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        buttonPanel.gap(8);
        buttonPanel.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonPanel.child(Components.button(Text.literal("Restore Backup"),
                        btn -> showRestoreConfirmation())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        // Delete button only for manual backups
        if (selectedBackup.type == BackupManager.BackupType.MANUAL) {
            buttonPanel.child(Components.button(Text.literal("Delete"),
                            btn -> showDeleteConfirmation())
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

    private String formatTimestamp(String isoTimestamp) {
        try {
            return isoTimestamp.replace('T', ' ').substring(0, Math.min(isoTimestamp.length(), 19));
        } catch (Exception e) {
            return isoTimestamp;
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void showCreateBackupDialog() {
        var popup = Containers.verticalFlow(Sizing.fixed(450), Sizing.content());
        popup.gap(12);
        popup.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        popup.padding(Insets.of(20));

        popup.child(Components.label(Text.literal("Create Manual Backup")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));

        popup.child(Components.label(Text.literal("Title:*"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        var titleField = Components.textBox(Sizing.fill(95), "");
        titleField.setPlaceholder(Text.literal("Enter backup title"));
        popup.child(titleField);

        popup.child(Components.label(Text.literal("Description (optional):"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        var descriptionField = Components.textBox(Sizing.fill(95), "");
        descriptionField.setPlaceholder(Text.literal("Additional details about this backup"));
        popup.child(descriptionField);

        var buttons = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttons.gap(12);
        buttons.horizontalAlignment(HorizontalAlignment.CENTER);

        buttons.child(Components.button(Text.literal("Create"), btn -> {
                    showCreateBackupWarningDialog(titleField.getText().trim(), descriptionField.getText().trim());
                }).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        buttons.child(Components.button(Text.literal("Cancel"), btn -> closeOverlay())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        popup.child(buttons);

        showOverlay(popup);
    }

    private void showCreateBackupWarningDialog(String title, String description) {
        if (title.isEmpty()) {
            title = "Manual backup - " + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
        }

        final String finalTitle = title;
        final String finalDescription = description.isEmpty() ? null : description;

        closeOverlay(); // Close the input dialog

        var popup = Containers.verticalFlow(Sizing.fixed(450), Sizing.content());
        popup.gap(12);
        popup.surface(Surface.flat(UITheme.DARK_PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        popup.padding(Insets.of(20));

        // Title
        popup.child(Components.label(Text.literal("Backup Warning"))
                .color(UITheme.color(UITheme.ACCENT_GOLD))
                .margins(Insets.bottom(8)));

        // Warning message (copied from export screen)
        var warningText = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        warningText.gap(4);

        warningText.child(Components.label(Text.literal("⚠ Important Notice:"))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .margins(Insets.bottom(4)));

        warningText.child(Components.label(Text.literal("• The backup process might cause the game to appear unresponsive"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        warningText.child(Components.label(Text.literal("• Your system may ask you to close the process"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        warningText.child(Components.label(Text.literal("• Please wait - the backup is still running in the background"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        warningText.child(Components.label(Text.literal("• Do not force close the application during backup"))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .margins(Insets.bottom(8)));

        popup.child(warningText);

        // Buttons
        var buttons = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttons.gap(12);
        buttons.horizontalAlignment(HorizontalAlignment.CENTER);

        buttons.child(Components.button(Text.literal("Cancel"), btn -> {
            closeOverlay();
        }).sizing(Sizing.fixed(80), Sizing.fixed(20)));

        buttons.child(Components.button(Text.literal("Continue Backup"), btn -> {
            closeOverlay();
            performCreateBackup(finalTitle, finalDescription);
        }).sizing(Sizing.fixed(120), Sizing.fixed(20)));

        popup.child(buttons);

        showOverlay(popup);
    }

    private void performCreateBackup(String title, String description) {
        BackupManager.createManualBackup(title, description);
        refreshBackupsList();
    }

    private void showRestoreConfirmation() {
        if (selectedBackup == null) return;

        var popup = Containers.verticalFlow(Sizing.fixed(500), Sizing.content());
        popup.gap(12);
        popup.surface(Surface.flat(UITheme.PANEL_BACKGROUND)
                .and(Surface.outline(UITheme.STATUS_WARNING_BORDER)));
        popup.padding(Insets.of(20));

        var headerRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        headerRow.gap(8);
        headerRow.verticalAlignment(VerticalAlignment.CENTER);

        headerRow.child(Components.label(Text.literal("⚠"))
                .color(UITheme.color(UITheme.STATUS_WARNING_BORDER))
                .sizing(Sizing.fixed(24), Sizing.content()));

        headerRow.child(Components.label(Text.literal("Restore Backup?")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.STATUS_WARNING_BORDER)));

        popup.child(headerRow);

        popup.child(Components.label(Text.literal("Backup: " + selectedBackup.getDisplayName())
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        var warningBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        warningBox.gap(4);
        warningBox.surface(Surface.flat(UITheme.ENTRY_BACKGROUND)
                .and(Surface.outline(UITheme.STATUS_WARNING_BORDER)));
        warningBox.padding(Insets.of(12));

        warningBox.child(Components.label(Text.literal("This will:"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        warningBox.child(Components.label(Text.literal("• Replace your current configuration files"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        warningBox.child(Components.label(Text.literal("• Create an auto-backup of your current state"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        warningBox.child(Components.label(Text.literal("• Overwrite mod configs and settings"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        popup.child(warningBox);

        var buttons = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttons.gap(12);
        buttons.horizontalAlignment(HorizontalAlignment.CENTER);

        buttons.child(Components.button(Text.literal("Restore"), btn -> {
                    closeOverlay();
                    performRestore();
                }).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        buttons.child(Components.button(Text.literal("Cancel"), btn -> closeOverlay())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        popup.child(buttons);

        showOverlay(popup);
    }

    private void showDeleteConfirmation() {
        if (selectedBackup == null) return;

        var popup = Containers.verticalFlow(Sizing.fixed(400), Sizing.content());
        popup.gap(12);
        popup.surface(Surface.flat(UITheme.PANEL_BACKGROUND)
                .and(Surface.outline(UITheme.STATUS_ERROR_BORDER)));
        popup.padding(Insets.of(20));

        popup.child(Components.label(Text.literal("Delete Backup?")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.STATUS_ERROR_BORDER)));

        popup.child(Components.label(Text.literal(selectedBackup.getDisplayName()))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        popup.child(Components.label(Text.literal("This action cannot be undone."))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        var buttons = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttons.gap(12);
        buttons.horizontalAlignment(HorizontalAlignment.CENTER);

        buttons.child(Components.button(Text.literal("Delete"), btn -> {
                    closeOverlay();
                    performDelete();
                }).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        buttons.child(Components.button(Text.literal("Cancel"), btn -> closeOverlay())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        popup.child(buttons);

        showOverlay(popup);
    }

    private void performRestore() {
        if (selectedBackup == null) return;

        if (BackupManager.restoreBackup(selectedBackup)) {
            LOGGER.info("Successfully restored backup: {}", selectedBackup.getDisplayName());

            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Backup restored: " +
                                (selectedBackup.title != null ? selectedBackup.title : selectedBackup.configName)), false);
            }

            refreshBackupsList();
        } else {
            LOGGER.error("Failed to restore backup: {}", selectedBackup.getDisplayName());

            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Failed to restore backup!"), false);
            }
        }
    }

    private void performDelete() {
        if (selectedBackup == null) return;

        if (BackupManager.deleteBackup(selectedBackup)) {
            LOGGER.info("Deleted backup: {}", selectedBackup.getDisplayName());
            selectedBackup = null;
            refreshBackupsList();
        } else {
            LOGGER.error("Failed to delete backup");
        }
    }

    private void showOverlay(FlowLayout popup) {
        currentOverlay = Containers.overlay(popup);
        currentOverlay.positioning(Positioning.relative(50, 40));
        currentOverlay.zIndex(15);
        rootComponent.child(currentOverlay);
    }

    private void closeOverlay() {
        if (currentOverlay != null) {
            rootComponent.removeChild(currentOverlay);
            currentOverlay = null;
        }
    }

    // FIX: Simplified refresh method that only rebuilds the sidebar content
    private void refreshBackupsList() {
        selectedBackup = null;
        showEmptyState();
        rebuildSidebarContent();
    }
}