package com.github.kd_gaming1.packcore.ui.screen.configmanager;

import com.github.kd_gaming1.packcore.config.imports.ConfigImportService;
import com.github.kd_gaming1.packcore.config.imports.ConfigImportService.ImportableFile;
import com.github.kd_gaming1.packcore.config.model.ConfigMetadata;
import com.github.kd_gaming1.packcore.ui.screen.base.BasePackCoreScreen;
import com.github.kd_gaming1.packcore.ui.screen.components.ScreenUIComponents;
import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import com.github.kd_gaming1.packcore.util.task.ProgressListener;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Configuration import screen - folder-based approach.
 * Users place files in the imports folder, then select them from this UI.
 */
public class ImportConfigScreen extends BasePackCoreScreen {

    private ImportableFile selectedFile = null;
    private List<ImportableFile> availableFiles = List.of();
    private final Map<ImportableFile, FlowLayout> entryComponents = new HashMap<>();

    // UI Components
    private LabelComponent statusLabel;
    private FlowLayout fileListContainer;
    private FlowLayout previewContainer;
    private CheckboxComponent applyImmediatelyCheckbox;
    private CheckboxComponent deleteAfterImportCheckbox;
    private ButtonComponent importButton;
    private FlowLayout progressPanel;

    public ImportConfigScreen() {
        super(new ConfigManagerScreen());
    }

    @Override
    protected Component createTitleLabel() {
        return Components.label(
                Text.literal("Import Configuration - " + getModpackInfo().getName())
                        .styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))
        ).color(color(TEXT_PRIMARY));
    }

    @Override
    protected FlowLayout createMainContent() {
        FlowLayout mainContent = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.expand())
                .gap(8);

        mainContent.child(createSidebar());
        mainContent.child(createPreviewPanel());

        // Load files on screen open
        refreshFileList();

        return mainContent;
    }

    // ===== Sidebar =====

    private FlowLayout createSidebar() {
        // Create the base sidebar with styling
        FlowLayout sidebar = (FlowLayout) ScreenUIComponents.createSidebar(40)
                .padding(Insets.of(22, 18, 14, 14));

        // Create scrollable content container
        FlowLayout sidebarContent = Containers.verticalFlow(Sizing.fill(96), Sizing.content())
                .gap(8);

        // Add all sections to the content
        sidebarContent.child(createInstructionsSection());
        sidebarContent.child(createFileListSection());
        sidebarContent.child(createImportOptionsSection());

        // Wrap content in scroll container and add to sidebar
        sidebar.child(ScreenUIComponents.createScrollContainer(sidebarContent));

        return sidebar;
    }




    private FlowLayout createInstructionsSection() {
        FlowLayout section = ScreenUIComponents.createSection("How to Import", 0);

        // Instructions
        LabelComponent instructions = (LabelComponent) Components.label(
                        Text.literal("""
                                1. Click 'Open Imports Folder'
                                2. Place your config .zip file there
                                3. Click 'Refresh' to see it
                                4. Select and import""")
                ).color(color(TEXT_PRIMARY))
                .horizontalSizing(Sizing.fill(95));

        section.child(instructions);

        // Button row
        FlowLayout buttonRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        buttonRow.child(ScreenUIComponents.createButton("Open Folder",
                btn -> openImportsFolder(), 90, 20));

        buttonRow.child(ScreenUIComponents.createButton("Refresh",
                btn -> refreshFileList(), 90, 20));

        section.child(buttonRow);

        // Folder path display
        LabelComponent pathLabel = (LabelComponent) Components.label(
                        Text.literal("Path: " + ConfigImportService.getImportsFolderPath())
                ).color(color(TEXT_SECONDARY))
                .horizontalSizing(Sizing.fill(95));

        section.child(pathLabel);

        return section;
    }

    private FlowLayout createFileListSection() {
        FlowLayout section = ScreenUIComponents.createSection("Available Files", 60);

        fileListContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2);

        section.child(ScreenUIComponents.createScrollContainer(fileListContainer));

        return section;
    }

    private FlowLayout createImportOptionsSection() {
        FlowLayout section = ScreenUIComponents.createSection("Import Options", 0);

        // Status
        statusLabel = Components.label(Text.literal("Select a file to import"))
                .color(color(TEXT_SECONDARY));
        section.child(statusLabel);

        // Progress
        progressPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        section.child(progressPanel);

        // Options
        applyImmediatelyCheckbox = Components.checkbox(Text.literal("Apply Immediately"));
        applyImmediatelyCheckbox.checked(false);
        applyImmediatelyCheckbox.tooltip(Text.literal(
                "If checked, the game will restart and apply this configuration"));
        section.child(applyImmediatelyCheckbox);

        deleteAfterImportCheckbox = Components.checkbox(Text.literal("Delete After Import"));
        deleteAfterImportCheckbox.checked(true);
        deleteAfterImportCheckbox.tooltip(Text.literal(
                "Remove the file from imports folder after successful import"));
        section.child(deleteAfterImportCheckbox);

        // Import button
        importButton = ScreenUIComponents.createButton("Import",
                btn -> handleImportClick());
        importButton.active(false);

        section.child(importButton);

        return section;
    }

    // ===== Preview Panel =====

    private FlowLayout createPreviewPanel() {
        previewContainer = ScreenUIComponents.createInfoPanel(60);
        showEmptyPreview();
        return previewContainer;
    }

    private void showEmptyPreview() {
        previewContainer.clearChildren();
        previewContainer.horizontalAlignment(HorizontalAlignment.CENTER);
        previewContainer.verticalAlignment(VerticalAlignment.CENTER);

        previewContainer.child(Components.label(
                Text.literal("Configuration Preview").setStyle(Style.EMPTY.withBold(true))
        ).color(color(ACCENT_SECONDARY)));

        previewContainer.child(Components.label(
                Text.literal("Select a file to preview its contents")
        ).color(color(TEXT_SECONDARY)));
    }

    // ===== File Operations =====

    private void openImportsFolder() {
        updateStatus("Opening folder...", TEXT_SECONDARY);

        ConfigImportService.openImportsFolder().thenAccept(success -> {
            MinecraftClient.getInstance().execute(() -> {
                if (success) {
                    updateStatus("Folder opened - place files there and click Refresh", SUCCESS_BORDER);
                } else {
                    updateStatus("Could not open folder - check game directory manually", WARNING_BORDER);
                }
            });
        }).exceptionally(throwable -> {
            MinecraftClient.getInstance().execute(() ->
                    updateStatus("Error: " + throwable.getMessage(), ERROR_BORDER));
            return null;
        });
    }

    private void refreshFileList() {
        updateStatus("Scanning for files...", TEXT_SECONDARY);
        fileListContainer.clearChildren();
        entryComponents.clear();

        // Show loading indicator
        fileListContainer.child(Components.label(Text.literal("Scanning imports folder..."))
                .color(color(TEXT_SECONDARY)));

        ConfigImportService.scanImportsFolder().thenAccept(files -> {
            MinecraftClient.getInstance().execute(() -> {
                availableFiles = files;
                fileListContainer.clearChildren();

                if (files.isEmpty()) {
                    fileListContainer.child(Components.label(
                            Text.literal("No config files found\n\nPlace .zip files in the imports folder")
                    ).color(color(TEXT_SECONDARY)));
                    updateStatus("No files found - add files and refresh", TEXT_SECONDARY);
                } else {
                    for (ImportableFile file : files) {
                        fileListContainer.child(createFileEntry(file));
                    }
                    updateStatus("Found " + files.size() + " file(s)", SUCCESS_BORDER);
                }
            });
        }).exceptionally(throwable -> {
            MinecraftClient.getInstance().execute(() -> {
                fileListContainer.clearChildren();
                fileListContainer.child(Components.label(
                        Text.literal("Error scanning folder: " + throwable.getMessage())
                ).color(color(ERROR_BORDER)));
                updateStatus("Error scanning folder", ERROR_BORDER);
            });
            return null;
        });
    }

    private FlowLayout createFileEntry(ImportableFile file) {
        FlowLayout entry = ScreenUIComponents.createListEntry();

        // Main file name
        String displayName = file.getDisplayName();
        if (displayName.length() > 35) {
            displayName = displayName.substring(0, 32) + "...";
        }

        entry.child(Components.label(Text.literal(displayName))
                .color(color(TEXT_PRIMARY)));

        // Info row
        FlowLayout infoRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(6);

        // Validation status
        if (file.isValid()) {
            infoRow.child(Components.label(Text.literal("✓ Valid"))
                    .color(color(SUCCESS_BORDER)));
        } else {
            infoRow.child(Components.label(Text.literal("✗ Invalid"))
                    .color(color(ERROR_BORDER)));
        }

        // File size
        infoRow.child(Components.label(Text.literal(file.getFileSizeFormatted()))
                .color(color(TEXT_SECONDARY)));

        entry.child(infoRow);

        // Store reference
        entryComponents.put(file, entry);

        // Apply hover and selection (only for valid files)
        if (file.isValid()) {
            ScreenUIComponents.applyHoverEffects(entry, () -> selectFile(file));
        } else {
            // Show tooltip for invalid files
            entry.tooltip(Text.literal(file.validation().errorMessage()));
            entry.surface(Surface.flat(ERROR_BG).and(Surface.outline(ERROR_BORDER)));
        }

        return entry;
    }

    private void selectFile(ImportableFile file) {
        // Reset previous selection
        if (selectedFile != null && entryComponents.containsKey(selectedFile)) {
            ScreenUIComponents.applySelectedState(entryComponents.get(selectedFile), false);
        }

        // Set new selection
        selectedFile = file;
        if (entryComponents.containsKey(file)) {
            ScreenUIComponents.applySelectedState(entryComponents.get(file), true);
        }

        showPreview();
        importButton.active(file.isValid());
        updateStatus("Ready to import: " + file.getDisplayName(), SUCCESS_BORDER);
    }

    private void showPreview() {
        if (selectedFile == null) {
            showEmptyPreview();
            return;
        }

        previewContainer.clearChildren();
        previewContainer.horizontalAlignment(HorizontalAlignment.LEFT);
        previewContainer.verticalAlignment(VerticalAlignment.TOP);

        // Validation status
        if (!selectedFile.isValid()) {
            previewContainer.child(ScreenUIComponents.createErrorCard(
                    "Invalid Configuration",
                    selectedFile.validation().errorMessage()
            ));
            return;
        }

        ConfigMetadata meta = selectedFile.metadata();
        if (meta == null) {
            previewContainer.child(Components.label(
                    Text.literal("Could not read metadata")
            ).color(color(ERROR_BORDER)));
            return;
        }

        // Header
        previewContainer.child(Components.label(
                Text.literal(meta.getName()).setStyle(Style.EMPTY.withBold(true))
        ).color(color(ACCENT_SECONDARY)));

        // Info box
        FlowLayout infoBox = ScreenUIComponents.createInfoBox();
        infoBox.child(ScreenUIComponents.createInfoRow("File:", selectedFile.fileName()));
        infoBox.child(ScreenUIComponents.createInfoRow("Size:", selectedFile.getFileSizeFormatted()));
        infoBox.child(ScreenUIComponents.createInfoRow("Version:", meta.getVersion()));
        infoBox.child(ScreenUIComponents.createInfoRow("Author:", meta.getAuthor()));
        infoBox.child(ScreenUIComponents.createInfoRow("Resolution:", meta.getTargetResolution()));

        if (meta.getCreatedDate() != null) {
            infoBox.child(ScreenUIComponents.createInfoRow("Created:",
                    ScreenUIComponents.formatTimestamp(meta.getCreatedDate())));
        }

        previewContainer.child(infoBox);

        // Description
        if (meta.getDescription() != null && !meta.getDescription().isEmpty()) {
            previewContainer.child(Components.label(
                    Text.literal("Description:").setStyle(Style.EMPTY.withBold(true))
            ).color(color(ACCENT_SECONDARY)));

            previewContainer.child(Components.label(Text.literal(meta.getDescription()))
                    .color(color(TEXT_PRIMARY))
                    .sizing(Sizing.fill(95), Sizing.content()));
        }

        // Mods list (if present)
        if (meta.getMods() != null && !meta.getMods().isEmpty()) {
            previewContainer.child(createModsList(meta.getMods()));
        }

        // Delete button for this file
        FlowLayout actionButtons = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.top(12));

        actionButtons.child(ScreenUIComponents.createButton("Delete File",
                btn -> confirmDeleteFile(), 100, 20));

        previewContainer.child(actionButtons);
    }

    private Component createModsList(java.util.List<String> mods) {
        FlowLayout container = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        container.child(Components.label(
                Text.literal("Included Mods:").setStyle(Style.EMPTY.withBold(true))
        ).color(color(ACCENT_SECONDARY)));

        FlowLayout modsContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2)
                .surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ENTRY_BORDER)))
                .padding(Insets.of(8));

        int displayCount = Math.min(10, mods.size());
        for (int i = 0; i < displayCount; i++) {
            modsContainer.child(Components.label(Text.literal("• " + mods.get(i)))
                    .color(color(TEXT_PRIMARY)));
        }

        if (mods.size() > displayCount) {
            modsContainer.child(Components.label(
                    Text.literal("... and " + (mods.size() - displayCount) + " more")
            ).color(color(TEXT_SECONDARY)));
        }

        container.child(ScreenUIComponents.createScrollContainer(modsContainer)
                .sizing(Sizing.fill(100), Sizing.fixed(150)));

        return container;
    }

    // ===== Import Operations =====

    private void handleImportClick() {
        if (selectedFile == null || !selectedFile.isValid()) return;

        // Show confirmation if applying immediately
        if (applyImmediatelyCheckbox.isChecked()) {
            showRestartWarningDialog();
        } else {
            performImport();
        }
    }

    private void showRestartWarningDialog() {
        FlowLayout dialog = ScreenUIComponents.createWarningDialog(
                "Restart Required",
                null,
                500
        );

        // Configuration info
        dialog.child(Components.label(
                Text.literal("Configuration: " + selectedFile.getDisplayName())
                        .setStyle(Style.EMPTY.withBold(true))
        ).color(color(TEXT_PRIMARY)));

        // Warning box
        FlowLayout warningBox = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(WARNING_BORDER)))
                .padding(Insets.of(12));

        warningBox.child(Components.label(Text.literal("This will:"))
                .color(color(TEXT_PRIMARY)));
        warningBox.child(Components.label(Text.literal("• Import and apply the configuration"))
                .color(color(TEXT_SECONDARY)));
        warningBox.child(Components.label(Text.literal("• Restart Minecraft automatically"))
                .color(color(TEXT_SECONDARY)));
        warningBox.child(Components.label(Text.literal("• Replace your current configuration"))
                .color(color(TEXT_SECONDARY)));

        dialog.child(warningBox);

        // Backup notice
        dialog.child(ScreenUIComponents.createErrorCard(
                "BACKUP RECOMMENDATION",
                "Please export your current configuration before proceeding. " +
                        "If you do not export a backup, you will lose your current configuration and cannot revert!"
        ));

        // Buttons
        dialog.child(ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Export First", btn -> {
                    closeTopOverlay();
                    MinecraftClient.getInstance().setScreen(new ExportConfigScreen());
                }),
                ScreenUIComponents.createButton("Import & Restart", btn -> {
                    closeTopOverlay();
                    performImport();
                }),
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay())
        ));

        showOverlay(dialog, false);
    }

    private void performImport() {
        if (selectedFile == null || !selectedFile.isValid()) return;

        importButton.active(false);
        progressPanel.clearChildren();

        boolean applyImmediately = applyImmediatelyCheckbox.isChecked();
        boolean deleteAfterImport = deleteAfterImportCheckbox.isChecked();

        ConfigImportService.importConfigAndCleanup(
                selectedFile.path(),
                applyImmediately,
                deleteAfterImport,
                new ProgressListener() {
                    @Override
                    public void onProgress(String message, int percentage) {
                        MinecraftClient.getInstance().execute(() -> {
                            progressPanel.clearChildren();
                            progressPanel.child(Components.label(
                                    Text.literal(message + " (" + percentage + "%)")
                            ).color(color(ACCENT_SECONDARY)));
                        });
                    }

                    @Override
                    public void onComplete(boolean success, String message) {
                        MinecraftClient.getInstance().execute(() -> {
                            progressPanel.clearChildren();

                            if (success) {
                                updateStatus("Success!", SUCCESS_BORDER);
                                if (applyImmediately) {
                                    progressPanel.child(Components.label(
                                            Text.literal("Game will restart to apply configuration...")
                                    ).color(color(SUCCESS_BORDER)));
                                } else {
                                    progressPanel.child(Components.label(Text.literal(message))
                                            .color(color(SUCCESS_BORDER)));

                                    // Refresh the file list after successful import
                                    refreshFileList();
                                    selectedFile = null;
                                    showEmptyPreview();
                                    importButton.active(false);
                                }
                            } else {
                                updateStatus("Import failed", ERROR_BORDER);
                                progressPanel.child(Components.label(Text.literal(message))
                                        .color(color(ERROR_BORDER)));
                                importButton.active(true);
                            }
                        });
                    }
                }
        );
    }

    private void confirmDeleteFile() {
        if (selectedFile == null) return;

        FlowLayout dialog = ScreenUIComponents.createDialog(
                "Delete File?",
                "Are you sure you want to delete:\n" + selectedFile.fileName() +
                        "\n\nThis action cannot be undone.",
                400
        );

        FlowLayout buttons = ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Delete", btn -> {
                    closeTopOverlay();
                    deleteSelectedFile();
                }, 90, 20),
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay(), 90, 20)
        );

        dialog.child(buttons);
        showOverlay(dialog, false);
    }

    private void deleteSelectedFile() {
        if (selectedFile == null) return;

        boolean deleted = ConfigImportService.deleteImportFile(selectedFile.path());

        if (deleted) {
            updateStatus("File deleted", SUCCESS_BORDER);
            selectedFile = null;
            showEmptyPreview();
            refreshFileList();
        } else {
            updateStatus("Failed to delete file", ERROR_BORDER);
        }
    }

    // ===== Utility =====

    private void updateStatus(String message, int color) {
        statusLabel.text(Text.literal(message));
        statusLabel.color(color(color));
    }
}