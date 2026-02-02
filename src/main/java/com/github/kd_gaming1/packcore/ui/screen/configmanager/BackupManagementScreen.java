package com.github.kd_gaming1.packcore.ui.screen.configmanager;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.apply.ConfigApplyService;
import com.github.kd_gaming1.packcore.config.backup.BackupManager;
import com.github.kd_gaming1.packcore.config.model.ConfigMetadata;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.github.kd_gaming1.packcore.notification.BackupNotifications;
import com.github.kd_gaming1.packcore.ui.screen.base.BasePackCoreScreen;
import com.github.kd_gaming1.packcore.ui.screen.components.ScreenUIComponents;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Backup management screen - refactored for improved code clarity.
 * Handles backup creation, restoration, and deletion with async operations.
 */
public class BackupManagementScreen extends BasePackCoreScreen {

    private BackupManager.BackupInfo selectedBackup = null;
    private FlowLayout infoPanel;
    private FlowLayout sidebarContent;
    private final Map<BackupManager.BackupInfo, FlowLayout> entryComponents = new HashMap<>();

    // Progress tracking
    private FlowLayout progressDialog = null;
    private LabelComponent progressLabel = null;
    private volatile boolean operationInBackground = false;
    private volatile String currentOperationName = "";
    private volatile boolean isRestoreOperation = false;

    public BackupManagementScreen() {
        super(new ConfigManagerScreen());
    }

    @Override
    protected Component createTitleLabel() {
        return Components.label(
                Text.literal("Backup Manager - " + getModpackInfo().getName())
                        .styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte"))))
        ).color(color(TEXT_PRIMARY));
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

        // Backup sections container
        sidebarContent = Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(8);

        sidebar.child(ScreenUIComponents.createScrollContainer(sidebarContent));

        // Action buttons
        sidebar.child(createSidebarButtons());

        // Load backups
        rebuildSidebarContent();

        return sidebar;
    }

    private Component createInfoText() {
        int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
        int padding = guiScale <= 2 ? 16 : 8;

        FlowLayout container = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .padding(Insets.of(padding, 0, padding, 0));

        LabelComponent infoLabel = (LabelComponent) Components.label(
                        Text.literal("Manage your configuration backups. Auto backups are created before applying new configs.")
                ).color(color(TEXT_PRIMARY))
                .sizing(Sizing.fill(95), Sizing.content());

        container.child(infoLabel);
        return container;
    }

    private void rebuildSidebarContent() {
        sidebarContent.clearChildren();
        entryComponents.clear();

        // Show loading
        sidebarContent.child(ScreenUIComponents.createEmptyState("Loading backups..."));

        // Load backups asynchronously
        BackupManager.getBackupsAsync().thenAccept(allBackups ->
                MinecraftClient.getInstance().execute(() -> {
                    sidebarContent.clearChildren();

                    List<BackupManager.BackupInfo> manualBackups = allBackups.stream()
                            .filter(b -> b.type() == BackupManager.BackupType.MANUAL)
                            .toList();

                    List<BackupManager.BackupInfo> autoBackups = allBackups.stream()
                            .filter(b -> b.type() == BackupManager.BackupType.AUTO)
                            .toList();

                    sidebarContent.child(createBackupSection("Manual Backups", manualBackups, true));
                    sidebarContent.child(createBackupSection("Auto Backups", autoBackups, false));
                })
        );
    }

    private FlowLayout createBackupSection(String title, List<BackupManager.BackupInfo> backups,
                                           boolean isManual) {
        FlowLayout section = ScreenUIComponents.createSection(title, isManual ? 45 : 50);
        section.horizontalSizing(Sizing.fill(98));

        FlowLayout listContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2);

        if (backups.isEmpty()) {
            listContent.child(Components.label(Text.literal("No backups found"))
                    .color(color(TEXT_SECONDARY)));
        } else {
            for (BackupManager.BackupInfo backup : backups) {
                listContent.child(createBackupEntry(backup));
            }
        }

        section.child(ScreenUIComponents.createScrollContainer(listContent));
        return section;
    }

    private FlowLayout createBackupEntry(BackupManager.BackupInfo backup) {
        FlowLayout entry = ScreenUIComponents.createListEntry();

        // Display title
        String displayTitle = backup.title() != null && !backup.title().isEmpty()
                ? backup.title()
                : backup.configName();

        entry.child(Components.label(Text.literal(displayTitle))
                .color(color(TEXT_PRIMARY)));

        // Badges
        FlowLayout badges = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        badges.child(Components.label(Text.literal(backup.type().getDisplayName()))
                .color(color(backup.type() == BackupManager.BackupType.MANUAL
                        ? SUCCESS_BORDER
                        : WARNING_BORDER)));

        badges.child(Components.label(Text.literal("v" + backup.configVersion()))
                .color(color(TEXT_SECONDARY)));

        entry.child(badges);

        // Store reference
        entryComponents.put(backup, entry);

        // Apply hover and selection
        ScreenUIComponents.applyHoverEffects(entry, () -> selectBackup(backup));

        return entry;
    }

    private FlowLayout createSidebarButtons() {
        FlowLayout buttonRow = (FlowLayout) Containers.ltrTextFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        buttonRow.child(ScreenUIComponents.createButton("Create Backup",
                        btn -> showCreateBackupDialog(), 90, 19)
                .margins(Insets.bottom(4)));

        buttonRow.child(ScreenUIComponents.createButton("Open Folder",
                        btn -> BackupManager.openBackupsFolder(), 90, 19)
                .margins(Insets.bottom(4)));

        buttonRow.child(ScreenUIComponents.createButton("Refresh",
                        btn -> refreshBackupsList(), 90, 19)
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
                "Select a backup to view details"));
    }

    private void selectBackup(BackupManager.BackupInfo backup) {
        // Reset previous selection
        if (selectedBackup != null && entryComponents.containsKey(selectedBackup)) {
            ScreenUIComponents.applySelectedState(entryComponents.get(selectedBackup), false);
        }

        // Set new selection
        selectedBackup = backup;
        if (entryComponents.containsKey(backup)) {
            ScreenUIComponents.applySelectedState(entryComponents.get(backup), true);
        }

        showBackupDetails();
    }

    private void showBackupDetails() {
        if (selectedBackup == null) return;

        infoPanel.clearChildren();
        infoPanel.horizontalAlignment(HorizontalAlignment.LEFT);
        infoPanel.verticalAlignment(VerticalAlignment.TOP);

        int padding = MinecraftClient.getInstance().options.getGuiScale().getValue() <= 2 ? 6 : 0;

        // Header
        String headerText = selectedBackup.title() != null && !selectedBackup.title().isEmpty()
                ? selectedBackup.title()
                : selectedBackup.configName();

        infoPanel.child(Components.label(Text.literal(headerText)
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(color(ACCENT_SECONDARY))
                .margins(Insets.of(padding, 0, 0, 0)));

        // Info box
        FlowLayout infoBox = ScreenUIComponents.createInfoBox();
        infoBox.child(ScreenUIComponents.createInfoRow("Type:", selectedBackup.type().getDisplayName()));
        infoBox.child(ScreenUIComponents.createInfoRow("Config:", selectedBackup.configName()));
        infoBox.child(ScreenUIComponents.createInfoRow("Version:", selectedBackup.configVersion()));
        infoBox.child(ScreenUIComponents.createInfoRow("Created:",
                ScreenUIComponents.formatTimestamp(selectedBackup.timestamp())));
        infoBox.child(ScreenUIComponents.createInfoRow("Size:",
                ScreenUIComponents.formatSize(selectedBackup.sizeBytes())));
        infoBox.child(ScreenUIComponents.createInfoRow("Backup ID:", selectedBackup.backupId()));

        infoPanel.child(infoBox);

        // Description
        if (selectedBackup.description() != null && !selectedBackup.description().trim().isEmpty()) {
            infoPanel.child(Components.label(Text.literal("Description:")
                            .setStyle(Style.EMPTY.withBold(true)))
                    .color(color(ACCENT_SECONDARY)));

            infoPanel.child(Components.label(Text.literal(selectedBackup.description()))
                    .color(color(TEXT_PRIMARY))
                    .sizing(Sizing.fill(95), Sizing.content()));
        }

        // Warning box
        infoPanel.child(ScreenUIComponents.createWarningCard(
                "Restore Information",
                "Restoring will replace current files. An auto-backup will be created first."
        ));

        // Action buttons
        infoPanel.child(createActionButtons());
    }

    private FlowLayout createActionButtons() {
        FlowLayout buttonPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        // Full restore
        buttonPanel.child(ScreenUIComponents.createButton("Restore Full Backup",
                btn -> showRestoreConfirmation(), 120, 20));

        // Selective restore (NEW)
        buttonPanel.child(ScreenUIComponents.createButton("Restore Specific Files",
                btn -> MinecraftClient.getInstance().setScreen(
                        new SelectiveFileApplicationScreen(selectedBackup, this)), 120, 20));

        // Delete (if manual)
        if (selectedBackup.type() == BackupManager.BackupType.MANUAL) {
            buttonPanel.child(ScreenUIComponents.createButton("Delete",
                    btn -> showDeleteConfirmation(), 90, 20));
        }

        return buttonPanel;
    }

    // ===== Backup Operations =====

    private void showCreateBackupDialog() {
        FlowLayout dialog = ScreenUIComponents.createDialog(
                "Create Manual Backup",
                null,
                450
        );

        dialog.child(Components.label(Text.literal("Title:*"))
                .color(color(TEXT_PRIMARY)));

        TextBoxComponent titleField = Components.textBox(Sizing.fill(95), "");
        titleField.setPlaceholder(Text.literal("Enter backup title"));
        dialog.child(titleField);

        dialog.child(Components.label(Text.literal("Description (optional):"))
                .color(color(TEXT_PRIMARY)));

        TextBoxComponent descriptionField = Components.textBox(Sizing.fill(95), "");
        descriptionField.setPlaceholder(Text.literal("Additional details about this backup"));
        dialog.child(descriptionField);

        dialog.child(ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Create", btn -> {
                    String title = titleField.getText().trim();
                    String description = descriptionField.getText().trim();
                    closeTopOverlay();
                    performCreateBackup(
                            title.isEmpty() ? null : title,
                            description.isEmpty() ? null : description
                    );
                }),
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay())
        ));

        showOverlay(dialog, false);
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
        FlowLayout dialog = ScreenUIComponents.createWarningDialog(
                "Backup Notice",
                null,
                400
        );

        FlowLayout warningText = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        warningText.child(Components.label(Text.literal("⚠ Important Notice:"))
                .color(color(TEXT_PRIMARY))
                .margins(Insets.bottom(4)));

        warningText.child(Components.label(Text.literal("• The backup will run in the background"))
                .color(color(TEXT_PRIMARY)));

        warningText.child(Components.label(Text.literal("• A progress indicator will show the status"))
                .color(color(TEXT_PRIMARY)));

        warningText.child(Components.label(Text.literal("• You can continue using the interface"))
                .color(color(TEXT_PRIMARY))
                .margins(Insets.bottom(8)));

        dialog.child(warningText);

        dialog.child(ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay(), 80, 20),
                ScreenUIComponents.createButton("Continue", btn -> {
                    closeTopOverlay();
                    executeBackupCreation(title, description);
                }, 120, 20)
        ));

        showOverlay(dialog, false);
    }

    private void executeBackupCreation(String title, String description) {
        operationInBackground = false;
        showProgressDialog("Creating Backup", "Preparing backup...");

        BackupManager.createManualBackupAsync(title, description, this::updateProgress)
                .thenAccept(backupPath -> MinecraftClient.getInstance().execute(() -> {
                    closeProgressDialog();
                    refreshBackupsList();

                    BackupNotifications.notifyBackupComplete(
                            currentOperationName, backupPath, false);

                    // Auto-open folder if still on screen
                    if (MinecraftClient.getInstance().currentScreen == this) {
                        try {
                            Util.getOperatingSystem().open(backupPath.getParent().toFile());
                        } catch (Exception e) {
                            PackCore.LOGGER.warn("Failed to auto-open backup folder", e);
                        }
                    }
                }))
                .exceptionally(throwable -> {
                    MinecraftClient.getInstance().execute(() -> {
                        closeProgressDialog();
                        PackCore.LOGGER.error("Failed to create backup", throwable);
                        showErrorDialog("Backup failed: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void showRestoreConfirmation() {
        if (selectedBackup == null) return;

        FlowLayout dialog = ScreenUIComponents.createWarningDialog(
                "Restore Backup?",
                null,
                500
        );

        dialog.child(Components.label(Text.literal("Backup: " + selectedBackup.getDisplayName())
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(color(TEXT_PRIMARY)));

        FlowLayout warningBox = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(WARNING_BORDER)))
                .padding(Insets.of(12));

        warningBox.child(Components.label(Text.literal("This will:"))
                .color(color(TEXT_PRIMARY)));

        warningBox.child(Components.label(Text.literal("• Replace your current configuration files"))
                .color(color(TEXT_SECONDARY)));

        warningBox.child(Components.label(Text.literal("• Create an auto-backup of your current state"))
                .color(color(TEXT_SECONDARY)));

        warningBox.child(Components.label(Text.literal("• Overwrite mod configs and settings"))
                .color(color(TEXT_SECONDARY)));

        dialog.child(warningBox);

        dialog.child(ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Restore", btn -> {
                    closeTopOverlay();
                    showRestoreWarningDialog();
                }),
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay())
        ));

        showOverlay(dialog, false);
    }

    private void showRestoreWarningDialog() {
        FlowLayout dialog = ScreenUIComponents.createWarningDialog(
                "Restore Notice",
                null,
                400
        );

        FlowLayout warningText = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        warningText.child(Components.label(Text.literal("⚠ Game Will Close"))
                .color(color(TEXT_PRIMARY))
                .margins(Insets.bottom(4)));

        warningText.child(Components.label(Text.literal("• The game will close and restart"))
                .color(color(TEXT_PRIMARY)));

        warningText.child(Components.label(Text.literal("• The backup will be applied on startup"))
                .color(color(TEXT_PRIMARY)));

        warningText.child(Components.label(Text.literal("• An auto-backup will be created first"))
                .color(color(TEXT_PRIMARY))
                .margins(Insets.bottom(8)));

        dialog.child(warningText);

        dialog.child(ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay(), 80, 20),
                ScreenUIComponents. createButton("Restore & Close Game", btn -> {
                    closeTopOverlay();
                    performRestore();
                }, 150, 20)
        ));

        showOverlay(dialog, false);
    }

    private void performRestore() {
        if (selectedBackup == null) return;

        closeTopOverlay();

        try {
            // Get the backup file path
            Path gameDir = MinecraftClient.getInstance().runDirectory. toPath();
            Path backupsDir = gameDir.resolve("packcore/backups");
            Path backupZipPath = backupsDir.resolve(selectedBackup.backupId() + ".zip");

            // Create a temporary ConfigFile wrapper for the backup
            ConfigMetadata metadata = ConfigMetadata.builder()
                    .name(selectedBackup.configName() != null ? selectedBackup.configName() : "Restored Backup")
                    .version(selectedBackup.configVersion() != null ? selectedBackup.configVersion() : "1.0.0")
                    .description(selectedBackup.description() != null ? selectedBackup.description() : "Backup restoration")
                    .build();

            ConfigFileRepository.ConfigFile backupAsConfig = new ConfigFileRepository.ConfigFile(
                    backupZipPath.getFileName().toString(),
                    backupZipPath,
                    false,
                    metadata
            );

            // Use ConfigApplyService to schedule restoration
            ConfigApplyService.scheduleConfigApplication(backupAsConfig);

            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Restoring:  " + selectedBackup.getDisplayName() + " - Restarting..."),
                        false
                );
            }

        } catch (Exception e) {
            PackCore.LOGGER.error("Failed to schedule backup restoration", e);
            showErrorDialog("Failed to schedule restore:  " + e.getMessage());
        }
    }

    private void showDeleteConfirmation() {
        if (selectedBackup == null) return;

        FlowLayout dialog = ScreenUIComponents.createDialog(
                "Delete Backup?",
                selectedBackup.getDisplayName() + "\n\nThis action cannot be undone.",
                400
        );

        dialog.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ERROR_BORDER)));

        dialog.child(ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Delete", btn -> {
                    closeTopOverlay();
                    performDelete();
                }),
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay())
        ));

        showOverlay(dialog, false);
    }

    private void performDelete() {
        if (selectedBackup == null) return;

        if (BackupManager.deleteBackup(selectedBackup)) {
            PackCore.LOGGER.info("Deleted backup: {}", selectedBackup.getDisplayName());
            selectedBackup = null;
            refreshBackupsList();
        } else {
            showErrorDialog("Failed to delete backup");
        }
    }

    // ===== Progress & Dialogs =====

    private void showProgressDialog(String title, String message) {
        progressDialog = ScreenUIComponents.createDialog(title, null, 350);
        progressDialog.positioning(Positioning.absolute(
                (this.width - 350) / 2,
                (this.height - 150) / 2
        ));
        progressLabel = (LabelComponent) Components.label(Text.literal(message))
                .color(color(TEXT_PRIMARY))
                .margins(Insets.bottom(12));
        progressDialog.child(progressLabel);

        FlowLayout buttonRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER);

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
        FlowLayout dialog = ScreenUIComponents.createDialog("Error", message, 350);
        dialog.surface(Surface.flat(DARK_PANEL_BACKGROUND).and(Surface.outline(ERROR_BORDER)));
        dialog.positioning(Positioning.absolute(
                (this.width - 350) / 2,
                (this.height - 120) / 2
        ));
        dialog.child(ScreenUIComponents.createButton("OK",
                        btn -> rootComponent.removeChild(dialog), 80, 20)
                .horizontalSizing(Sizing.content()));

        rootComponent.child(dialog);
    }

    private void refreshBackupsList() {
        selectedBackup = null;
        showEmptyState();
        rebuildSidebarContent();
    }

    @Override
    public void close() {
        super.close();
    }
}