package com.github.kd_gaming1.packcore.ui.screen.configmanager;

import com.github.kd_gaming1.packcore.config.imports.ConfigImportService;
import com.github.kd_gaming1.packcore.config.model.ConfigMetadata;
import com.github.kd_gaming1.packcore.ui.screen.base.BasePackCoreScreen;
import com.github.kd_gaming1.packcore.ui.screen.components.ScreenUIComponents;
import com.github.kd_gaming1.packcore.util.task.ProgressListener;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Configuration import screen - refactored for improved code clarity.
 * Demonstrates handling of file selection, preview, and async operations.
 */
public class ImportConfigScreen extends BasePackCoreScreen {

    private Path selectedFile = null;
    private ConfigMetadata previewMetadata = null;

    // UI State
    private LabelComponent selectedFileLabel;
    private LabelComponent statusLabel;
    private FlowLayout previewContainer;
    private CheckboxComponent applyImmediatelyCheckbox;
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

        return mainContent;
    }

    // ===== Sidebar =====

    private FlowLayout createSidebar() {
        FlowLayout sidebar = ScreenUIComponents.createSidebar(35);

        FlowLayout scrollContent = Containers.verticalFlow(Sizing.fill(96), Sizing.content())
                .gap(8);

        scrollContent.child(createInstructions());
        scrollContent.child(createFileSelectionSection());
        scrollContent.child(createStatusSection());
        scrollContent.child(createImportOptionsSection());

        sidebar.child(ScreenUIComponents.createScrollContainer(scrollContent));

        return sidebar;
    }

    private Component createInstructions() {
        FlowLayout container = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .padding(Insets.of(8));

        container.child(Components.label(
                Text.literal("Select a configuration ZIP file to import into your modpack.")
        ).color(color(TEXT_PRIMARY)).horizontalSizing(Sizing.fill(100)));

        return container;
    }

    private FlowLayout createFileSelectionSection() {
        FlowLayout section = ScreenUIComponents.createSection("Select File", 0);
        section.horizontalAlignment(HorizontalAlignment.CENTER);

        section.child(ScreenUIComponents.createButton("Browse Files",
                btn -> selectConfigFile(), 120, 21));

        selectedFileLabel = Components.label(Text.literal("No file selected"))
                .color(color(TEXT_SECONDARY));
        section.child(selectedFileLabel);

        return section;
    }

    private FlowLayout createStatusSection() {
        FlowLayout section = ScreenUIComponents.createSection("Status", 0);

        statusLabel = Components.label(Text.literal("Ready"))
                .color(color(TEXT_SECONDARY));
        section.child(statusLabel);

        progressPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        section.child(progressPanel);

        return section;
    }

    private FlowLayout createImportOptionsSection() {
        FlowLayout section = ScreenUIComponents.createSection("Import Options", 0);

        applyImmediatelyCheckbox = Components.checkbox(Text.literal("Apply Immediately"));
        applyImmediatelyCheckbox.checked(false);
        applyImmediatelyCheckbox.tooltip(Text.literal(
                "If checked, the game will restart and apply this configuration"));
        section.child(applyImmediatelyCheckbox);

        importButton = ScreenUIComponents.createButton("Import",
                btn -> handleImportClick());
        importButton.active(false);

        section.child(importButton);

        return section;
    }

    // ===== Preview Panel =====

    private FlowLayout createPreviewPanel() {
        previewContainer = ScreenUIComponents.createInfoPanel(65);
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

    private void selectConfigFile() {
        updateStatus("Opening file browser...", TEXT_SECONDARY);

        ConfigImportService.selectConfigFile().thenAccept(path -> {
            MinecraftClient.getInstance().execute(() -> {
                if (path != null) {
                    selectedFile = path;
                    selectedFileLabel.text(Text.literal(path.getFileName().toString()));
                    selectedFileLabel.color(color(TEXT_PRIMARY));
                    previewFile();
                } else {
                    updateStatus("Selection cancelled", TEXT_SECONDARY);
                }
            });
        }).exceptionally(throwable -> {
            MinecraftClient.getInstance().execute(() ->
                    updateStatus("Error: " + throwable.getMessage(), ERROR_BORDER));
            return null;
        });
    }

    private void previewFile() {
        if (selectedFile == null) return;

        updateStatus("Reading metadata...", TEXT_SECONDARY);
        previewMetadata = ConfigImportService.previewConfig(selectedFile);

        if (previewMetadata != null) {
            showPreview();
            importButton.active(true);
            updateStatus("Ready to import", SUCCESS_BORDER);
        } else {
            updateStatus("Could not read file", ERROR_BORDER);
            importButton.active(false);
        }
    }

    private void showPreview() {
        if (previewMetadata == null) return;

        previewContainer.clearChildren();
        previewContainer.horizontalAlignment(HorizontalAlignment.LEFT);
        previewContainer.verticalAlignment(VerticalAlignment.TOP);

        // Header
        previewContainer.child(Components.label(
                Text.literal(previewMetadata.getName()).setStyle(Style.EMPTY.withBold(true))
        ).color(color(ACCENT_SECONDARY)));

        // Info box
        FlowLayout infoBox = ScreenUIComponents.createInfoBox();
        infoBox.child(ScreenUIComponents.createInfoRow("Version:", previewMetadata.getVersion()));
        infoBox.child(ScreenUIComponents.createInfoRow("Author:", previewMetadata.getAuthor()));
        infoBox.child(ScreenUIComponents.createInfoRow("Resolution:", previewMetadata.getTargetResolution()));

        if (previewMetadata.getCreatedDate() != null) {
            infoBox.child(ScreenUIComponents.createInfoRow("Created:",
                    ScreenUIComponents.formatTimestamp(previewMetadata.getCreatedDate())));
        }

        previewContainer.child(infoBox);

        // Description
        if (previewMetadata.getDescription() != null && !previewMetadata.getDescription().isEmpty()) {
            previewContainer.child(Components.label(
                    Text.literal("Description:").setStyle(Style.EMPTY.withBold(true))
            ).color(color(ACCENT_SECONDARY)));

            previewContainer.child(Components.label(Text.literal(previewMetadata.getDescription()))
                    .color(color(TEXT_PRIMARY))
                    .sizing(Sizing.fill(95), Sizing.content()));
        }

        // Mods list (if present)
        if (previewMetadata.getMods() != null && !previewMetadata.getMods().isEmpty()) {
            previewContainer.child(createModsList(previewMetadata.getMods()));
        }
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
        if (selectedFile == null || previewMetadata == null) return;

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
                Text.literal("Configuration: " + previewMetadata.getName())
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
        if (selectedFile == null || previewMetadata == null) return;

        importButton.active(false);
        progressPanel.clearChildren();

        boolean applyImmediately = applyImmediatelyCheckbox.isChecked();

        ConfigImportService.importConfig(selectedFile, applyImmediately,
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
                                    importButton.active(true);
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

    // ===== Utility =====

    private void updateStatus(String message, int color) {
        statusLabel.text(Text.literal(message));
        statusLabel.color(color(color));
    }
}