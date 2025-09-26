package com.github.kd_gaming1.packcore.gui.configscreen;

import com.github.kd_gaming1.packcore.gui.util.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.ui.UITheme;
import com.github.kd_gaming1.packcore.util.ConfigImportManager;
import com.github.kd_gaming1.packcore.util.ImportCallback;
import com.github.kd_gaming1.packcore.util.ConfigMetadata;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;

/**
 * Simplified import screen with clean metadata display
 */
public class ConfigImportScreen extends BaseOwoScreen<FlowLayout> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Path selectedFile = null;
    private ConfigMetadata previewMetadata = null;

    // UI Components
    private LabelComponent selectedFileLabel;
    private LabelComponent statusLabel;
    private FlowLayout previewContainer;
    private CheckboxComponent applyImmediatelyCheckbox;
    private ButtonComponent importButton;
    private FlowLayout progressPanel;
    private FlowLayout rootComponent; // Store reference to root for overlay
    private OverlayContainer<FlowLayout> currentOverlay = null; // Store current overlay reference

    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.rootComponent = rootComponent; // Store reference for overlay usage

        rootComponent.surface(UiSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png"), 1920, 1082));
        rootComponent.padding(Insets.of(8));

        rootComponent.child(createHeader());
        rootComponent.child(createContent());
    }

    private FlowLayout createHeader() {
        var header = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(50));
        header.gap(8);
        header.verticalAlignment(VerticalAlignment.CENTER);

        header.child(Components.texture(
                Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png"),
                0, 0, 40, 40, 40, 40));

        header.child(Components.label(
                        Text.literal("Import Configuration - " + getModpackInfo().getName())
                                .styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

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

    private FlowLayout createContent() {
        var content = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        content.gap(8);
        content.child(createSidebar());
        content.child(createPreviewPanel());
        return content;
    }

    private FlowLayout createSidebar() {
        var sidebar = Containers.verticalFlow(Sizing.fill(35), Sizing.expand());
        sidebar.surface(UiSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/menu/notif_box.png"), 607, 755));
        sidebar.padding(Insets.of(12));

        var scrollContent = Containers.verticalFlow(Sizing.fill(98), Sizing.content());
        scrollContent.gap(8);

        // Instructions
        var instructionsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        instructionsContainer.padding(Insets.of(8));
        instructionsContainer.child(Components.label(
                        Text.literal("Select a configuration ZIP file to import into your modpack."))
                .color(UITheme.color(UITheme.TEXT_WHITE)));
        scrollContent.child(instructionsContainer);

        // File selection section
        var fileSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        fileSection.gap(6);
        fileSection.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        fileSection.padding(Insets.of(8));
        fileSection.horizontalAlignment(HorizontalAlignment.CENTER);

        fileSection.child(Components.label(Text.literal("Select File")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));

        fileSection.child(Components.button(Text.literal("Browse Files"),
                        btn -> selectConfigFile())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 120, 63))
                .sizing(Sizing.fixed(120), Sizing.fixed(21)));

        selectedFileLabel = Components.label(Text.literal("No file selected"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY));
        fileSection.child(selectedFileLabel);

        scrollContent.child(fileSection);

        // Status section
        var statusSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        statusSection.gap(6);
        statusSection.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        statusSection.padding(Insets.of(8));

        statusSection.child(Components.label(Text.literal("Status")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));

        statusLabel = Components.label(Text.literal("Ready"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY));
        statusSection.child(statusLabel);

        progressPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        statusSection.child(progressPanel);

        scrollContent.child(statusSection);

        // Import options
        var optionsSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        optionsSection.gap(6);
        optionsSection.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        optionsSection.padding(Insets.of(8));

        optionsSection.child(Components.label(Text.literal("Import Options")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));

        applyImmediatelyCheckbox = Components.checkbox(
                Text.literal("Apply Immediately"));
        applyImmediatelyCheckbox.checked(false);
        applyImmediatelyCheckbox.tooltip(Text.literal(
                "If checked, the game will restart and apply this configuration"));
        optionsSection.child(applyImmediatelyCheckbox);

        // Updated import button logic - check if we need confirmation first
        importButton = (ButtonComponent) Components.button(Text.literal("Import"),
                        btn -> handleImportClick())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20));

        importButton.active(false);

        optionsSection.child(importButton);

        scrollContent.child(optionsSection);

        var scrollContainer = Containers.verticalScroll(Sizing.fill(98), Sizing.expand(), scrollContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        sidebar.child(scrollContainer);

        return sidebar;
    }

    private FlowLayout createPreviewPanel() {
        previewContainer = Containers.verticalFlow(Sizing.fill(65), Sizing.expand());
        previewContainer.gap(8);
        previewContainer.surface(UiSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/menu/info_box.png"), 1142, 934));
        previewContainer.padding(Insets.of(14));

        showEmptyState();
        return previewContainer;
    }

    private void showEmptyState() {
        previewContainer.clearChildren();

        previewContainer.horizontalAlignment(HorizontalAlignment.CENTER);
        previewContainer.verticalAlignment(VerticalAlignment.CENTER);
        previewContainer.child(Components.label(Text.literal("Configuration Preview")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));
        previewContainer.child(Components.label(Text.literal("Select a file to preview its contents"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));
    }

    private void selectConfigFile() {
        statusLabel.text(Text.literal("Opening file browser..."));

        ConfigImportManager.selectConfigFile().thenAccept(path -> {
            MinecraftClient.getInstance().execute(() -> {
                if (path != null) {
                    selectedFile = path;
                    selectedFileLabel.text(Text.literal(path.getFileName().toString()));
                    selectedFileLabel.color(UITheme.color(UITheme.TEXT_WHITE));
                    previewFile();
                } else {
                    statusLabel.text(Text.literal("Selection cancelled"));
                }
            });
        }).exceptionally(throwable -> {
            MinecraftClient.getInstance().execute(() -> {
                statusLabel.text(Text.literal("Error: " + throwable.getMessage()));
                statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
            });
            return null;
        });
    }

    private void previewFile() {
        if (selectedFile == null) return;

        statusLabel.text(Text.literal("Reading metadata..."));
        previewMetadata = ConfigImportManager.previewConfig(selectedFile);

        if (previewMetadata != null) {
            showPreview();
            importButton.active(true);
            statusLabel.text(Text.literal("Ready to import"));
            statusLabel.color(UITheme.color(UITheme.STATUS_SUCCESS_BORDER));
        } else {
            statusLabel.text(Text.literal("Could not read file"));
            statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
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
                        Text.literal(previewMetadata.getName())
                                .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));

        // Metadata info box
        var infoBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        infoBox.gap(4);
        infoBox.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        infoBox.padding(Insets.of(8));

        infoBox.child(createInfoRow("Version:", previewMetadata.getVersion()));
        infoBox.child(createInfoRow("Author:", previewMetadata.getAuthor()));
        infoBox.child(createInfoRow("Resolution:", previewMetadata.getTargetResolution()));

        if (previewMetadata.getCreatedDate() != null && !previewMetadata.getCreatedDate().isEmpty()) {
            infoBox.child(createInfoRow("Created:", formatDate(previewMetadata.getCreatedDate())));
        }

        previewContainer.child(infoBox);

        // Description
        if (previewMetadata.getDescription() != null && !previewMetadata.getDescription().isEmpty()) {
            previewContainer.child(Components.label(Text.literal("Description:")
                            .setStyle(Style.EMPTY.withBold(true)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));

            previewContainer.child(Components.label(Text.literal(previewMetadata.getDescription()))
                    .color(UITheme.color(UITheme.TEXT_WHITE))
                    .sizing(Sizing.fill(95), Sizing.content()));
        }

        // Mods list (if present)
        if (previewMetadata.getMods() != null && !previewMetadata.getMods().isEmpty()) {
            previewContainer.child(Components.label(Text.literal("Included Mods:")
                            .setStyle(Style.EMPTY.withBold(true)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));

            var modsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            modsContainer.gap(2);
            modsContainer.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
            modsContainer.padding(Insets.of(8));

            int displayCount = Math.min(10, previewMetadata.getMods().size());
            for (int i = 0; i < displayCount; i++) {
                modsContainer.child(Components.label(Text.literal("• " + previewMetadata.getMods().get(i)))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }

            if (previewMetadata.getMods().size() > displayCount) {
                modsContainer.child(Components.label(
                                Text.literal("... and " + (previewMetadata.getMods().size() - displayCount) + " more"))
                        .color(UITheme.color(UITheme.TEXT_SECONDARY)));
            }

            var scrollableMods = Containers.verticalScroll(Sizing.fill(100), Sizing.fixed(150), modsContainer);
            scrollableMods.scrollbar(ScrollContainer.Scrollbar.vanilla());
            previewContainer.child(scrollableMods);
        }
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
            // Simple formatting - you could use DateTimeFormatter for better formatting
            return isoDate.replace('T', ' ').substring(0, Math.min(isoDate.length(), 19));
        } catch (Exception e) {
            return isoDate;
        }
    }

    // FIXED METHOD: Only show confirmation if applying immediately
    private void handleImportClick() {
        if (selectedFile == null || previewMetadata == null) return;

        // Check if user wants to apply immediately - if so, show warning popup
        if (applyImmediatelyCheckbox.isChecked()) {
            showRestartWarningDialog();
        } else {
            // Direct import without confirmation
            performImport();
        }
    }

    // NEW METHOD: Improved restart warning dialog
    private void showRestartWarningDialog() {
        if (selectedFile == null || previewMetadata == null) return;

        // Create a better-styled confirmation popup
        var popup = Containers.verticalFlow(Sizing.fixed(500), Sizing.content());
        popup.gap(12);
        popup.surface(Surface.flat(UITheme.PANEL_BACKGROUND)
                .and(Surface.outline(UITheme.STATUS_WARNING_BORDER)));
        popup.padding(Insets.of(20));

        // Warning icon and title
        var headerRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        headerRow.gap(8);
        headerRow.verticalAlignment(VerticalAlignment.CENTER);

        headerRow.child(Components.label(Text.literal("⚠"))
                .color(UITheme.color(UITheme.STATUS_WARNING_BORDER))
                .sizing(Sizing.fixed(24), Sizing.content()));

        headerRow.child(Components.label(Text.literal("Restart Required")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.STATUS_WARNING_BORDER)));

        popup.child(headerRow);

        // Configuration info
        popup.child(Components.label(Text.literal("Configuration: " + previewMetadata.getName())
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        // Warning messages
        var warningBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        warningBox.gap(4);
        warningBox.surface(Surface.flat(UITheme.ENTRY_BACKGROUND)
                .and(Surface.outline(UITheme.STATUS_WARNING_BORDER)));
        warningBox.padding(Insets.of(12));

        warningBox.child(Components.label(Text.literal("This will:"))
                .color(UITheme.color(UITheme.TEXT_WHITE)));

        warningBox.child(Components.label(Text.literal("• Import and apply the configuration"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        warningBox.child(Components.label(Text.literal("• Restart Minecraft automatically"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        warningBox.child(Components.label(Text.literal("• Replace your current configuration"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        popup.child(warningBox);

        // Important backup notice
        var backupNotice = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        backupNotice.gap(4);
        backupNotice.surface(Surface.flat(UITheme.STATUS_ERROR_BG)
                .and(Surface.outline(UITheme.STATUS_ERROR_BORDER)));
        backupNotice.padding(Insets.of(12));

        backupNotice.child(Components.label(Text.literal("⚠ BACKUP RECOMMENDATION")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.STATUS_ERROR_BORDER)));

        backupNotice.child(Components.label(
                        Text.literal("Please export your current configuration before proceeding. " +
                                "If you do not export a backup, you will lose your current configuration and cannot revert!"))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content()));

        popup.child(backupNotice);

        // Action buttons
        var buttons = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttons.gap(12);
        buttons.horizontalAlignment(HorizontalAlignment.CENTER);

        buttons.child(Components.button(Text.literal("Export First"), btn -> {
                    closeConfirmationDialog();
                    MinecraftClient.getInstance().setScreen(new ConfigExportScreen());
                }).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        buttons.child(Components.button(Text.literal("Import & Restart"), btn -> {
                    closeConfirmationDialog();
                    performImport();
                }).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        buttons.child(Components.button(Text.literal("Cancel"), btn -> closeConfirmationDialog())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .sizing(Sizing.fixed(100), Sizing.fixed(20)));

        popup.child(buttons);

        // Create overlay and add to root
        currentOverlay = Containers.overlay(popup);
        currentOverlay.positioning(Positioning.relative(50, 40));
        currentOverlay.zIndex(15);

        rootComponent.child(currentOverlay);
    }

    private void closeConfirmationDialog() {
        // Remove the stored overlay component if it exists
        if (currentOverlay != null) {
            rootComponent.removeChild(currentOverlay);
            currentOverlay = null;
        }
    }

    private void performImport() {
        if (selectedFile == null || previewMetadata == null) return;

        importButton.active(false);
        progressPanel.clearChildren();

        boolean applyImmediately = applyImmediatelyCheckbox.isChecked();

        ConfigImportManager.importConfig(selectedFile, applyImmediately,
                new ImportCallback() {
                    @Override
                    public void onProgress(String message, int percentage) {
                        MinecraftClient.getInstance().execute(() -> {
                            progressPanel.clearChildren();
                            progressPanel.child(Components.label(
                                            Text.literal(message + " (" + percentage + "%)"))
                                    .color(UITheme.color(UITheme.ACCENT_GOLD)));
                        });
                    }

                    @Override
                    public void onComplete(boolean success, String message) {
                        MinecraftClient.getInstance().execute(() -> {
                            progressPanel.clearChildren();

                            if (success) {
                                statusLabel.text(Text.literal("Success!"));
                                statusLabel.color(UITheme.color(UITheme.STATUS_SUCCESS_BORDER));

                                if (applyImmediately) {
                                    progressPanel.child(Components.label(
                                                    Text.literal("Game will restart to apply configuration..."))
                                            .color(UITheme.color(UITheme.STATUS_SUCCESS_BORDER)));
                                } else {
                                    progressPanel.child(Components.label(Text.literal(message))
                                            .color(UITheme.color(UITheme.STATUS_SUCCESS_BORDER)));
                                    importButton.active(true);
                                }
                            } else {
                                statusLabel.text(Text.literal("Import failed"));
                                statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
                                progressPanel.child(Components.label(Text.literal(message))
                                        .color(UITheme.color(UITheme.STATUS_ERROR_BORDER)));
                                importButton.active(true);
                            }
                        });
                    }
                });
    }
}