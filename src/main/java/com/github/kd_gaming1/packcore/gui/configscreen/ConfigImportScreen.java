package com.github.kd_gaming1.packcore.gui.configscreen;

import com.github.kd_gaming1.packcore.gui.configscreen.ui.UITheme;
import com.github.kd_gaming1.packcore.util.ConfigImportManager;
import com.github.kd_gaming1.packcore.util.ConfigMetadata;
import io.wispforest.owo.ui.base.BaseOwoScreen;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class ConfigImportScreen extends BaseOwoScreen<FlowLayout> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Identifier backgroundTexture;
    private Path selectedFile = null;
    private ConfigMetadata previewMetadata = null;
    private FlowLayout previewPanel;
    private FlowLayout importPanel;
    private CheckboxComponent applyImmediatelyCheckbox;
    private ButtonComponent importButton;
    private LabelComponent statusLabel;
    private FlowLayout progressPanel;

    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        int bgW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int bgH = MinecraftClient.getInstance().getWindow().getScaledHeight();
        backgroundTexture = Identifier.of(MOD_ID, "textures/gui/wizard/test_temp.png");
        rootComponent.surface(Surface.tiled(backgroundTexture, bgW, bgH));
        rootComponent.padding(Insets.of(16));

        rootComponent.child(createHeader());

        FlowLayout contentArea = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        contentArea.gap(12);

        contentArea.child(createFileSelectionPanel());

        previewPanel = createPreviewPanel();
        contentArea.child(previewPanel);

        rootComponent.child(contentArea);

        rootComponent.child(createBottomPanel());
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        int bgW = client.getWindow().getScaledWidth();
        int bgH = client.getWindow().getScaledHeight();
        this.uiAdapter.rootComponent.surface(Surface.tiled(backgroundTexture, bgW, bgH));
    }

    private FlowLayout createHeader() {
        FlowLayout header = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        header.padding(Insets.of(12));
        header.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent titleLabel = Components.label(Text.literal("Import Config")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.TEXT_WHITE));

        ButtonComponent backButton = (ButtonComponent) Components.button(Text.literal("← Back"), button -> {
                    MinecraftClient.getInstance().setScreen(new ModpackConfigMenuScreen());
                })
                .renderer(UITheme.defaultEntryRenderer())
                .sizing(Sizing.fixed(60), Sizing.fixed(20));

        FlowLayout rightSection = Containers.horizontalFlow(Sizing.expand(), Sizing.content());
        rightSection.horizontalAlignment(HorizontalAlignment.RIGHT);
        rightSection.child(backButton);

        header.child(titleLabel);
        header.child(rightSection);
        header.margins(Insets.bottom(8));

        return header;
    }

    private FlowLayout createFileSelectionPanel() {
        FlowLayout selectionPanel = Containers.verticalFlow(Sizing.fill(40), Sizing.expand());
        selectionPanel.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        selectionPanel.padding(Insets.of(12));
        selectionPanel.gap(8);

        LabelComponent headerLabel = Components.label(Text.literal("Select Config File")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.ACCENT_GOLD));
        selectionPanel.child(headerLabel);

        // Info section
        FlowLayout infoSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        infoSection.surface(Surface.flat(0xC0_2A3A2A).and(Surface.outline(0xFF_4A7C59)));
        infoSection.padding(Insets.of(8));
        infoSection.gap(4);

        LabelComponent infoText = (LabelComponent) Components.label(Text.literal("Choose a .zip config file from your computer to import into your modpack configurations."))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content());

        infoSection.child(infoText);
        selectionPanel.child(infoSection);

        // File selection button
        ButtonComponent selectFileButton = (ButtonComponent) Components.button(Text.literal("Browse for Config File"), button -> {
                    selectConfigFile();
                })
                .renderer(UITheme.successRenderer())
                .sizing(Sizing.fill(80), Sizing.fixed(25));

        selectionPanel.child(selectFileButton);

        // Selected file display
        FlowLayout fileDisplayPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        fileDisplayPanel.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        fileDisplayPanel.padding(Insets.of(8));
        fileDisplayPanel.gap(4);

        LabelComponent selectedFileLabel = (LabelComponent) Components.label(Text.literal("No file selected"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY))
                .sizing(Sizing.fill(95), Sizing.content());

        fileDisplayPanel.child(selectedFileLabel);
        selectionPanel.child(fileDisplayPanel);

        // Status label
        statusLabel = (LabelComponent) Components.label(Text.literal(""))
                .color(UITheme.color(UITheme.TEXT_SECONDARY))
                .sizing(Sizing.fill(95), Sizing.content());
        selectionPanel.child(statusLabel);

        return selectionPanel;
    }

    private FlowLayout createPreviewPanel() {
        FlowLayout previewContainer = Containers.verticalFlow(Sizing.expand(60), Sizing.expand());
        previewContainer.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        previewContainer.padding(Insets.of(12));
        previewContainer.gap(8);

        LabelComponent headerLabel = Components.label(Text.literal("Config Preview")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.ACCENT_GOLD));
        previewContainer.child(headerLabel);

        // Empty state
        FlowLayout emptyState = Containers.verticalFlow(Sizing.fill(100), Sizing.expand());
        emptyState.verticalAlignment(VerticalAlignment.CENTER);
        emptyState.horizontalAlignment(HorizontalAlignment.CENTER);

        LabelComponent emptyLabel = Components.label(Text.literal("Select a config file to preview its contents"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY));
        emptyState.child(emptyLabel);

        previewContainer.child(emptyState);

        return previewContainer;
    }

    private FlowLayout createBottomPanel() {
        FlowLayout bottomPanel = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        bottomPanel.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        bottomPanel.padding(Insets.of(12));
        bottomPanel.gap(8);
        bottomPanel.verticalAlignment(VerticalAlignment.CENTER);
        bottomPanel.margins(Insets.top(8));

        // Apply immediately checkbox
        applyImmediatelyCheckbox = Components.checkbox(Text.literal("Apply config immediately (will restart game)"));
        applyImmediatelyCheckbox.checked(false);

        // Progress panel (initially hidden)
        progressPanel = Containers.verticalFlow(Sizing.expand(), Sizing.content());
        progressPanel.gap(4);
        progressPanel.child(Components.label(Text.literal("Importing..."))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));

        // Import button
        importButton = (ButtonComponent) Components.button(Text.literal("Import Config"), button -> {
                    performImport();
                })
                .renderer(UITheme.successRenderer())
                .sizing(Sizing.fixed(120), Sizing.fixed(25));
        importButton.active(false);

        bottomPanel.child(applyImmediatelyCheckbox);
        bottomPanel.child(progressPanel);

        FlowLayout rightSection = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        rightSection.horizontalAlignment(HorizontalAlignment.RIGHT);
        rightSection.child(importButton);
        bottomPanel.child(rightSection);

        // Hide progress panel initially
        progressPanel.remove();

        return bottomPanel;
    }

    private void selectConfigFile() {
        statusLabel.text(Text.literal("Opening file browser..."));
        statusLabel.color(UITheme.color(UITheme.TEXT_SECONDARY));

        ConfigImportManager.selectConfigFile().thenAccept(selectedPath -> {
            MinecraftClient.getInstance().execute(() -> {
                if (selectedPath != null) {
                    this.selectedFile = selectedPath;
                    updateFileSelection();
                    previewSelectedFile();
                } else {
                    statusLabel.text(Text.literal("File selection cancelled"));
                    statusLabel.color(UITheme.color(UITheme.TEXT_SECONDARY));
                }
            });
        }).exceptionally(throwable -> {
            MinecraftClient.getInstance().execute(() -> {
                statusLabel.text(Text.literal("Error opening file browser: " + throwable.getMessage()));
                statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
            });
            return null;
        });
    }

    private void updateFileSelection() {
        if (selectedFile == null) return;

        // Update the file display in the selection panel
        FlowLayout root = (FlowLayout) this.uiAdapter.rootComponent;
        FlowLayout contentArea = (FlowLayout) root.children().get(1);
        FlowLayout selectionPanel = (FlowLayout) contentArea.children().get(0);
        FlowLayout fileDisplayPanel = (FlowLayout) selectionPanel.children().get(3);

        fileDisplayPanel.clearChildren();

        String fileName = selectedFile.getFileName().toString();
        LabelComponent fileNameLabel = (LabelComponent) Components.label(Text.literal("Selected: " + fileName))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content());

        LabelComponent filePathLabel = (LabelComponent) Components.label(Text.literal(selectedFile.getParent().toString()))
                .color(UITheme.color(UITheme.TEXT_SECONDARY))
                .sizing(Sizing.fill(95), Sizing.content());

        fileDisplayPanel.child(fileNameLabel);
        fileDisplayPanel.child(filePathLabel);

        statusLabel.text(Text.literal("File selected successfully"));
        statusLabel.color(UITheme.color(UITheme.STATUS_SUCCESS_BORDER));
    }

    private void previewSelectedFile() {
        if (selectedFile == null) return;

        statusLabel.text(Text.literal("Reading config metadata..."));
        statusLabel.color(UITheme.color(UITheme.TEXT_SECONDARY));

        try {
            previewMetadata = ConfigImportManager.previewConfigMetadata(selectedFile);

            if (previewMetadata != null) {
                updatePreviewPanel();
                importButton.active(true);
                statusLabel.text(Text.literal("Config file loaded successfully"));
                statusLabel.color(UITheme.color(UITheme.STATUS_SUCCESS_BORDER));
            } else {
                statusLabel.text(Text.literal("Could not read config metadata"));
                statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
                importButton.active(false);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to preview config", e);
            statusLabel.text(Text.literal("Error reading config: " + e.getMessage()));
            statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
            importButton.active(false);
        }
    }

    private void updatePreviewPanel() {
        if (previewMetadata == null) return;

        previewPanel.clearChildren();

        LabelComponent headerLabel = Components.label(Text.literal("Config Preview")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.ACCENT_GOLD));
        previewPanel.child(headerLabel);

        // Create scrollable content
        FlowLayout scrollableContent = ConfigImportManager.createMetadataPreview(previewMetadata);

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(100), Sizing.expand(), scrollableContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollStep(15);

        previewPanel.child(scrollContainer);
    }

    private void performImport() {
        if (selectedFile == null || previewMetadata == null) {
            statusLabel.text(Text.literal("No file selected"));
            statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
            return;
        }

        importButton.active(false);

        // Show progress panel
        FlowLayout bottomPanel = (FlowLayout) this.uiAdapter.rootComponent.children().get(2);
        if (!bottomPanel.children().contains(progressPanel)) {
            bottomPanel.child(1, progressPanel);
        }

        boolean applyImmediately = applyImmediatelyCheckbox.isChecked();

        ConfigImportManager.importConfig(selectedFile, applyImmediately, new ConfigImportManager.ImportProgressCallback() {
            @Override
            public void onProgress(String stage, int percentage) {
                MinecraftClient.getInstance().execute(() -> {
                    progressPanel.clearChildren();
                    progressPanel.child(Components.label(Text.literal(stage + " (" + percentage + "%)"))
                            .color(UITheme.color(UITheme.ACCENT_GOLD)));
                });
            }

            @Override
            public void onComplete(boolean success, String message) {
                MinecraftClient.getInstance().execute(() -> {
                    progressPanel.remove();

                    if (success) {
                        statusLabel.text(Text.literal(message));
                        statusLabel.color(UITheme.color(UITheme.STATUS_SUCCESS_BORDER));

                        if (applyImmediately) {
                            // Config will be applied on restart, game will close soon
                        } else {
                            importButton.active(true);
                        }
                    } else {
                        statusLabel.text(Text.literal("Import failed: " + message));
                        statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
                        importButton.active(true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                MinecraftClient.getInstance().execute(() -> {
                    progressPanel.remove();
                    statusLabel.text(Text.literal("Error: " + error));
                    statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
                    importButton.active(true);
                });
            }
        });
    }
}