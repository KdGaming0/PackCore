package com.github.kd_gaming1.packcore.ui.screen.configmanager;

import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import com.github.kd_gaming1.packcore.ui.theme.UITheme;
import com.github.kd_gaming1.packcore.notification.BackupNotifications;
import com.github.kd_gaming1.packcore.config.backup.BackupManager;
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
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;
import static com.github.kd_gaming1.packcore.ui.screen.configmanager.ConfigManagerScreen.getHorizontalFlowLayout;

/**
 * Backup management screen with async operations and progress reporting
 */
public class BackupManagementScreen extends BaseOwoScreen<FlowLayout> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private BackupManager.BackupInfo selectedBackup = null;
    private FlowLayout infoPanel;
    private FlowLayout rootComponent;
    private FlowLayout sidebarContent;
    private OverlayContainer<FlowLayout> currentOverlay = null;
    private FlowLayout progressDialog = null;
    private LabelComponent progressLabel = null;

    private final Map<BackupManager.BackupInfo, FlowLayout> entryComponents = new HashMap<>();

    // Background operation tracking
    private volatile boolean operationInBackground = false;
    private volatile String currentOperationName = "";
    private volatile boolean isRestoreOperation = false;

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.rootComponent = rootComponent;

        rootComponent.surface(TextureSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png"), 1920, 1082));
        rootComponent.padding(Insets.of(8));

        rootComponent.child(createHeader());
        rootComponent.child(createMainContent());
    }

    private FlowLayout createHeader() {
        var header = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(50));
        header.gap(8);
        header.verticalAlignment(VerticalAlignment.CENTER);

        header.child(Components.texture(
                Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png"),
                0, 0, 40, 40, 40, 40));

        header.child(Components.label(
                        Text.literal("Backup Manager - " + getModpackInfo().getName())
                                .styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        return getFlowLayout(header);
    }

    @NotNull
    static FlowLayout getFlowLayout(FlowLayout header) {
        var backContainer = Containers.horizontalFlow(Sizing.expand(), Sizing.content());
        backContainer.horizontalAlignment(HorizontalAlignment.RIGHT);
        backContainer.child(Components.button(Text.literal("Back"),
                        btn -> MinecraftClient.getInstance().setScreen(new ConfigManagerScreen()))
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
        sidebar.surface(TextureSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/menu/notif_box.png"), 607, 755));
        sidebar.padding(Insets.of(12));

        int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
        int padding = guiScale <= 2 ? 16 : 8;

        var infoLabel = Components.label(
                        Text.literal("Manage your configuration backups. Auto backups are created before applying new configs."))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content());

        var infoContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        infoContainer.padding(Insets.of(padding, 0, padding, 0));
        infoContainer.child(infoLabel);

        sidebar.child(infoContainer);

        sidebarContent = Containers.verticalFlow(Sizing.fill(98), Sizing.content());
        sidebarContent.gap(8);

        rebuildSidebarContent();

        var scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), sidebarContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        sidebar.child(scrollContainer);

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

    private void rebuildSidebarContent() {
        sidebarContent.clearChildren();
        entryComponents.clear();

        showLoadingInSidebar();

        BackupManager.getBackupsAsync().thenAccept(allBackups -> MinecraftClient.getInstance().execute(() -> {
            sidebarContent.clearChildren();

            List<BackupManager.BackupInfo> manualBackups = allBackups.stream()
                    .filter(b -> b.type() == BackupManager.BackupType.MANUAL)
                    .toList();

            List<BackupManager.BackupInfo> autoBackups = allBackups.stream()
                    .filter(b -> b.type() == BackupManager.BackupType.AUTO)
                    .toList();

            sidebarContent.child(createBackupSection("Manual Backups", manualBackups, true));
            sidebarContent.child(createBackupSection("Auto Backups", autoBackups, false));
        }));
    }

    private void showLoadingInSidebar() {
        sidebarContent.clearChildren();
        var loading = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        loading.child(Components.label(Text.literal("Loading backups..."))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));
        sidebarContent.child(loading);
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

        String displayTitle = backup.title() != null && !backup.title().isEmpty() ? backup.title() : backup.configName();
        entry.child(Components.label(Text.literal(displayTitle))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        var badges = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        badges.gap(4);

        badges.child(Components.label(Text.literal(backup.type().getDisplayName()))
                .color(UITheme.color(backup.type() == BackupManager.BackupType.MANUAL ?
                        UITheme.STATUS_SUCCESS_BORDER : UITheme.STATUS_WARNING_BORDER)));

        badges.child(Components.label(Text.literal("v" + backup.configVersion()))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        entry.child(badges);

        entryComponents.put(backup, entry);

        entry.mouseDown().subscribe((mouseX, mouseY, button) -> {
            selectBackup(backup);
            return true;
        });

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
        infoPanel.surface(TextureSurfaces.stretched(
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
        if (selectedBackup != null && entryComponents.containsKey(selectedBackup)) {
            FlowLayout previousEntry = entryComponents.get(selectedBackup);
            previousEntry.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        }

        selectedBackup = backup;

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

        int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
        int padding = guiScale <= 2 ? 6 : 0;

        String headerText = selectedBackup.title() != null && !selectedBackup.title().isEmpty()
                ? selectedBackup.title() : selectedBackup.configName();

        infoPanel.child(Components.label(Text.literal(headerText)
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD))
                .margins(Insets.of(padding, 0, 0, 0)));

        var infoBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        infoBox.gap(4);
        infoBox.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        infoBox.padding(Insets.of(8));

        infoBox.child(createInfoRow("Type:", selectedBackup.type().getDisplayName()));
        infoBox.child(createInfoRow("Config:", selectedBackup.configName()));
        infoBox.child(createInfoRow("Version:", selectedBackup.configVersion()));
        infoBox.child(createInfoRow("Created:", formatTimestamp(selectedBackup.timestamp())));
        infoBox.child(createInfoRow("Size:", formatSize(selectedBackup.sizeBytes())));
        infoBox.child(createInfoRow("Backup ID:", selectedBackup.backupId()));

        infoPanel.child(infoBox);

        if (selectedBackup.description() != null && !selectedBackup.description().trim().isEmpty()) {
            infoPanel.child(Components.label(Text.literal("Description:")
                            .setStyle(Style.EMPTY.withBold(true)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));

            infoPanel.child(Components.label(Text.literal(selectedBackup.description()))
                    .color(UITheme.color(UITheme.TEXT_WHITE))
                    .sizing(Sizing.fill(95), Sizing.content()));
        }

        var warningBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        warningBox.gap(4);
        warningBox.surface(Surface.flat(UITheme.STATUS_WARNING_BG)
                .and(Surface.outline(UITheme.STATUS_WARNING_BORDER)));
        warningBox.padding(Insets.of(12));

        warningBox.child(Components.label(Text.literal("⚠ Restore Information")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.STATUS_WARNING_BORDER)));

        warningBox.child(Components.label(Text.literal(
                        "Restoring will replace current files. An auto-backup will be created first."))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content()));

        infoPanel.child(warningBox);

        var buttonPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        buttonPanel.gap(8);
        buttonPanel.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonPanel.child(Components.button(Text.literal("Restore Backup"),
                        btn -> showRestoreConfirmation())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        if (selectedBackup.type() == BackupManager.BackupType.MANUAL) {
            buttonPanel.child(Components.button(Text.literal("Delete"),
                            btn -> showDeleteConfirmation())
                    .renderer(ButtonComponent.Renderer.texture(
                            Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                    .sizing(Sizing.fixed(100), Sizing.fixed(20)));
        }

        infoPanel.child(buttonPanel);
    }

    private FlowLayout createInfoRow(String label, String value) {
        return getHorizontalFlowLayout(label, value);
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
                    String title = titleField.getText().trim();
                    String description = descriptionField.getText().trim();
                    closeOverlay();
                    performCreateBackup(title.isEmpty() ? null : title, description.isEmpty() ? null : description);
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

    private void performCreateBackup(String title, String description) {
        String finalTitle = title != null ? title : "Manual backup - " +
                java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));

        currentOperationName = finalTitle;
        operationInBackground = false;
        isRestoreOperation = false;

        showBackupWarningDialog(finalTitle, description);
    }

    private void showBackupWarningDialog(String title, String description) {
        var dialogContainer = Containers.verticalFlow(Sizing.fixed(400), Sizing.content());
        dialogContainer.surface(Surface.flat(UITheme.DARK_PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        dialogContainer.padding(Insets.of(16));
        dialogContainer.positioning(Positioning.absolute(
                (this.width - 400) / 2,
                (this.height - 200) / 2
        )).zIndex(10);

        dialogContainer.child(Components.label(Text.literal("Backup Notice"))
                .color(UITheme.color(UITheme.ACCENT_GOLD))
                .margins(Insets.bottom(8)));

        var warningText = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        warningText.gap(4);

        warningText.child(Components.label(Text.literal("⚠ Important Notice:"))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .margins(Insets.bottom(4)));

        warningText.child(Components.label(Text.literal("• The backup will run in the background"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        warningText.child(Components.label(Text.literal("• A progress indicator will show the status"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        warningText.child(Components.label(Text.literal("• You can continue using the interface"))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .margins(Insets.bottom(8)));

        dialogContainer.child(warningText);

        var buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonRow.gap(8);
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonRow.child(Components.button(Text.literal("Cancel"),
                btn -> rootComponent.removeChild(dialogContainer)).sizing(Sizing.fixed(80), Sizing.fixed(20)));

        buttonRow.child(Components.button(Text.literal("Continue"), btn -> {
            rootComponent.removeChild(dialogContainer);
            executeBackupCreation(title, description);
        }).sizing(Sizing.fixed(120), Sizing.fixed(20)));

        dialogContainer.child(buttonRow);
        rootComponent.child(dialogContainer);
    }

    private void executeBackupCreation(String title, String description) {
        operationInBackground = false;
        showProgressDialog("Creating Backup", "Preparing backup...");

        BackupManager.createManualBackupAsync(title, description, this::updateProgress)
                .thenAccept(backupPath -> MinecraftClient.getInstance().execute(() -> {
                    closeProgressDialog();
                    refreshBackupsList();

                    // Notify user of completion
                    BackupNotifications.notifyBackupComplete(
                            currentOperationName, backupPath, false);

                    // Auto-open folder if still on screen
                    if (MinecraftClient.getInstance().currentScreen == this) {
                        try {
                            Util.getOperatingSystem().open(backupPath.getParent().toFile());
                        } catch (Exception e) {
                            LOGGER.warn("Failed to auto-open backup folder", e);
                        }
                    }
                }))
                .exceptionally(throwable -> {
                    MinecraftClient.getInstance().execute(() -> {
                        closeProgressDialog();
                        LOGGER.error("Failed to create backup", throwable);
                        showErrorDialog("Backup failed: " + throwable.getMessage());
                    });
                    return null;
                });
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
                    showRestoreWarningDialog();
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

    private void showRestoreWarningDialog() {
        var dialogContainer = Containers.verticalFlow(Sizing.fixed(400), Sizing.content());
        dialogContainer.surface(Surface.flat(UITheme.DARK_PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        dialogContainer.padding(Insets.of(16));
        dialogContainer.positioning(Positioning.absolute(
                (this.width - 400) / 2,
                (this.height - 200) / 2
        )).zIndex(10);

        dialogContainer.child(Components.label(Text.literal("Restore Notice"))
                .color(UITheme.color(UITheme.ACCENT_GOLD))
                .margins(Insets.bottom(8)));

        var warningText = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        warningText.gap(4);

        warningText.child(Components.label(Text.literal("⚠ Important Notice:"))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .margins(Insets.bottom(4)));

        warningText.child(Components.label(Text.literal("• The restore will run in the background"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        warningText.child(Components.label(Text.literal("• A progress indicator will show the status"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        warningText.child(Components.label(Text.literal("• You can continue using the interface"))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .margins(Insets.bottom(8)));

        dialogContainer.child(warningText);

        var buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonRow.gap(8);
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonRow.child(Components.button(Text.literal("Cancel"), btn -> rootComponent.removeChild(dialogContainer)).sizing(Sizing.fixed(80), Sizing.fixed(20)));

        buttonRow.child(Components.button(Text.literal("Continue Restore"), btn -> {
            rootComponent.removeChild(dialogContainer);
            performRestore();
        }).sizing(Sizing.fixed(120), Sizing.fixed(20)));

        dialogContainer.child(buttonRow);
        rootComponent.child(dialogContainer);
    }

    private void performRestore() {
        if (selectedBackup == null) return;

        currentOperationName = selectedBackup.getDisplayName();
        operationInBackground = false;
        isRestoreOperation = true;

        showProgressDialog("Restoring Backup", "Preparing restore...");

        BackupManager.restoreBackupAsync(selectedBackup, this::updateProgress)
                .thenAccept(success -> MinecraftClient.getInstance().execute(() -> {
                    closeProgressDialog();

                    if (success) {
                        refreshBackupsList();

                        // Get backup path for notification
                        Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
                        Path backupsDir = gameDir.resolve("packcore/backups");
                        Path backupPath = backupsDir.resolve(selectedBackup.backupId() + ".zip");

                        // Notify user of completion
                        BackupNotifications.notifyBackupComplete(
                                currentOperationName, backupPath, true);
                    } else {
                        showErrorDialog("Failed to restore backup!");
                    }
                }))
                .exceptionally(throwable -> {
                    MinecraftClient.getInstance().execute(() -> {
                        closeProgressDialog();
                        LOGGER.error("Failed to restore backup", throwable);
                        showErrorDialog("Restore failed: " + throwable.getMessage());
                    });
                    return null;
                });
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

    private void performDelete() {
        if (selectedBackup == null) return;

        if (BackupManager.deleteBackup(selectedBackup)) {
            LOGGER.info("Deleted backup: {}", selectedBackup.getDisplayName());
            selectedBackup = null;
            refreshBackupsList();
        } else {
            showErrorDialog("Failed to delete backup");
        }
    }

    private void showProgressDialog(String title, String message) {
        progressDialog = Containers.verticalFlow(Sizing.fixed(350), Sizing.content());
        progressDialog.surface(Surface.flat(UITheme.DARK_PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        progressDialog.padding(Insets.of(16));
        progressDialog.positioning(Positioning.absolute(
                (this.width - 350) / 2,
                (this.height - 150) / 2
        ));
        progressDialog.zIndex(20);

        progressDialog.child(Components.label(Text.literal(title)
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD))
                .margins(Insets.bottom(8)));

        progressLabel = (LabelComponent) Components.label(Text.literal(message))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .margins(Insets.bottom(12));
        progressDialog.child(progressLabel);

        // Button row
        FlowLayout buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonRow.gap(8);
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        // Continue in background button
        ButtonComponent backgroundButton = (ButtonComponent) Components.button(
                Text.literal("Continue in Background"),
                btn -> {
                    operationInBackground = true;
                    closeProgressDialog();
                }
        ).horizontalSizing(Sizing.content());

        buttonRow.child(backgroundButton);
        progressDialog.child(buttonRow);

        rootComponent.child(progressDialog);
    }

    private void updateProgress(String message) {
        MinecraftClient.getInstance().execute(() -> {
            if (progressLabel != null && !operationInBackground) {
                progressLabel.text(Text.literal(message));
            }
        });
    }

    private void closeProgressDialog() {
        if (progressDialog != null) {
            rootComponent.removeChild(progressDialog);
            progressDialog = null;
            progressLabel = null;
        }
    }

    private void showErrorDialog(String message) {
        var errorDialog = Containers.verticalFlow(Sizing.fixed(350), Sizing.content());
        errorDialog.surface(Surface.flat(UITheme.DARK_PANEL_BACKGROUND)
                .and(Surface.outline(UITheme.STATUS_ERROR_BORDER)));
        errorDialog.padding(Insets.of(16));
        errorDialog.positioning(Positioning.absolute(
                (this.width - 350) / 2,
                (this.height - 120) / 2
        ));
        errorDialog.zIndex(20);

        errorDialog.child(Components.label(Text.literal("Error")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.STATUS_ERROR_BORDER))
                .margins(Insets.bottom(8)));

        errorDialog.child(Components.label(Text.literal(message))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .margins(Insets.bottom(12)));

        errorDialog.child(Components.button(Text.literal("OK"), btn -> rootComponent.removeChild(errorDialog)).sizing(Sizing.fixed(80), Sizing.fixed(20))
                .horizontalSizing(Sizing.content()));

        rootComponent.child(errorDialog);
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

    private void refreshBackupsList() {
        selectedBackup = null;
        showEmptyState();
        rebuildSidebarContent();
    }

    @Override
    public void close() {
        BackupManager.shutdown();
        super.close();
    }
}