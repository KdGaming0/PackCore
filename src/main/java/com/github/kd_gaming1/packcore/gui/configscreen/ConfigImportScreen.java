package com.github.kd_gaming1.packcore.gui.configscreen;

import com.github.kd_gaming1.packcore.gui.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.configscreen.ui.UITheme;
import com.github.kd_gaming1.packcore.util.ConfigImportManager;
import com.github.kd_gaming1.packcore.util.ConfigMetadata;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextureComponent;
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
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;

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
    private LabelComponent selectedFileLabel;

    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        backgroundTexture = Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png");
        rootComponent.surface(UiSurfaces.stretched(backgroundTexture, 1920, 1082));
        rootComponent.padding(Insets.of(6, 8, 8, 8));

        rootComponent.child(createHeader());

        FlowLayout contentArea = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        contentArea.gap(6);

        contentArea.child(createSidebar());

        previewPanel = createPreviewPanel();
        contentArea.child(previewPanel);

        rootComponent.child(contentArea);
    }

    private FlowLayout createHeader() {
        FlowLayout header = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.padding(Insets.of(4));
        header.verticalAlignment(VerticalAlignment.CENTER);

        Identifier logoId = Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png");
        TextureComponent logo = Components.texture(logoId, 0, 0, 40, 40, 40, 40);

        Text titleText = Text.literal("Import Configs - " + getModpackInfo().getName())
                .styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")));
        LabelComponent titleLabel = Components.label(titleText).color(UITheme.color(UITheme.TEXT_WHITE));
        titleLabel.margins(Insets.of(4, 0, 4, 0));

        // Back button
        ButtonComponent backButton = (ButtonComponent) Components.button(Text.literal("Back"), button -> {
                    MinecraftClient.getInstance().setScreen(new ModpackConfigMenuScreen());
                })
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/previous.png"), 0, 0, 90, 57))
                .horizontalSizing(Sizing.fixed(90))
                .verticalSizing(Sizing.fixed(19));

        FlowLayout rightSection = Containers.horizontalFlow(Sizing.expand(), Sizing.content());
        rightSection.horizontalAlignment(HorizontalAlignment.RIGHT);
        rightSection.child(backButton);

        header.child(logo);
        header.child(titleLabel);
        header.child(rightSection);
        header.margins(Insets.bottom(6));

        return header;
    }

    private FlowLayout createSidebar() {
        FlowLayout sidebar = Containers.verticalFlow(Sizing.fill(35), Sizing.expand());
        sidebar.gap(4);
        sidebar.surface(UiSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/menu/notif_box.png"), 607, 755));
        sidebar.padding(Insets.of(12));
        sidebar.horizontalAlignment(HorizontalAlignment.CENTER);

        // This will hold all scrollable sections
        FlowLayout scrollContent = Containers.verticalFlow(Sizing.fill(96), Sizing.content());
        scrollContent.gap(6);

        // Info section
        FlowLayout infoSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .padding(Insets.of(2));
        LabelComponent infoLabel = (LabelComponent) Components.label(Text.literal("Choose a .zip file containing a configuration from another location/user from your computer to import into your modpack configurations."))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .horizontalSizing(Sizing.fill(100));
        infoSection.child(infoLabel);
        scrollContent.child(infoSection);

        // File selection section
        FlowLayout fileSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)))
                .padding(Insets.of(6))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        LabelComponent fileHeader = Components.label(Text.literal("Select Config File")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD));
        fileSection.child(fileHeader);

        ButtonComponent selectFileButton = (ButtonComponent) Components.button(Text.literal("Browse for Configs"), button -> selectConfigFile())
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 120, 63))
                .horizontalSizing(Sizing.fixed(120))
                .verticalSizing(Sizing.fixed(21));
        fileSection.child(selectFileButton);

        FlowLayout fileDisplayPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        fileDisplayPanel.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        fileDisplayPanel.padding(Insets.of(6));
        fileDisplayPanel.gap(2);

        selectedFileLabel = (LabelComponent) Components.label(Text.literal("No file selected"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY))
                .sizing(Sizing.fill(95), Sizing.content());
        fileDisplayPanel.child(selectedFileLabel);

        fileSection.child(fileDisplayPanel);
        scrollContent.child(fileSection);

        // Status section
        FlowLayout statusSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)))
                .padding(Insets.of(8))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        LabelComponent statusHeader = Components.label(Text.literal("Status")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD));
        statusSection.child(statusHeader);

        statusLabel = (LabelComponent) Components.label(Text.literal("Ready to select file"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY))
                .sizing(Sizing.fill(95), Sizing.content());
        statusSection.child(statusLabel);

        progressPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        progressPanel.gap(4);
        statusSection.child(progressPanel);

        scrollContent.child(statusSection);

        // Import options section
        FlowLayout importOptionsSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)))
                .padding(Insets.of(6))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.bottom(6));

        LabelComponent optionsHeader = Components.label(Text.literal("Import Options")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD));
        importOptionsSection.child(optionsHeader);

        applyImmediatelyCheckbox = (CheckboxComponent) Components.checkbox(Text.literal("Apply config immediately (will restart game)")).horizontalSizing(Sizing.fill(100));
        applyImmediatelyCheckbox.checked(false);
        importOptionsSection.child(applyImmediatelyCheckbox);

        importButton = (ButtonComponent) Components.button(Text.literal("Import Config"), button -> performImport())
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20));
        importButton.active(false);
        importOptionsSection.child(importButton);

        scrollContent.child(importOptionsSection);

        // Wrap scrollable content
        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), scrollContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollStep(15);

        // Add scroll container to sidebar
        sidebar.child(scrollContainer);

        return sidebar;
    }

    private FlowLayout createPreviewPanel() {
        FlowLayout previewContainer = Containers.verticalFlow(Sizing.expand(65), Sizing.expand());
        previewContainer.surface(UiSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/menu/info_box.png"), 1142, 934));
        previewContainer.padding(Insets.of(14));
        previewContainer.gap(4);
        previewContainer.horizontalAlignment(HorizontalAlignment.CENTER);
        previewContainer.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent headerLabel = Components.label(Text.literal("Config Preview")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.ACCENT_GOLD));
        previewContainer.child(headerLabel);

        // Empty state
        LabelComponent emptyLabel = Components.label(Text.literal("Select a config file to preview its contents"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY));
        previewContainer.child(emptyLabel);

        return previewContainer;
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

        String fileName = selectedFile.getFileName().toString();
        selectedFileLabel.text(Text.literal("Selected: " + fileName));
        selectedFileLabel.color(UITheme.color(UITheme.TEXT_WHITE));

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

        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)))
                .padding(Insets.of(6))
                .verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent configNameLabel = Components.label(Text.literal(previewMetadata.getName())
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.TEXT_WHITE));
        header.child(configNameLabel);
        previewPanel.child(header);

        FlowLayout extractInfo = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2)
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)))
                .padding(Insets.of(6));

        extractInfo.child(Components.label(Text.literal("Source: " + previewMetadata.getSource()))
                .color(UITheme.color(UITheme.TEXT_WHITE)));
        extractInfo.child(Components.label(Text.literal("Author: " + previewMetadata.getAuthor()))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));
        extractInfo.child(Components.label(Text.literal("Version: " + previewMetadata.getVersion()))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));
        if (previewMetadata.getCreatedDate() != null && !previewMetadata.getCreatedDate().isEmpty()) {
            extractInfo.child(Components.label(Text.literal("Created: " + previewMetadata.getCreatedDate()))
                    .color(UITheme.color(UITheme.TEXT_SECONDARY)));
        }

        previewPanel.child(extractInfo);

        FlowLayout contentWrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.expand());
        contentWrapper.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        contentWrapper.padding(Insets.of(2));

        FlowLayout scrollableContent = (FlowLayout) Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(4)
                .padding(Insets.of(6));

        scrollableContent.child(Components.label(Text.literal("Description:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));
        scrollableContent.child(Components.label(Text.literal(previewMetadata.getDescription()))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content()));

        scrollableContent.child(Components.label(Text.literal("Technical Details:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));
        scrollableContent.child(Components.label(Text.literal("Resolution: " + previewMetadata.getTargetResolution()))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        // Features
        if (previewMetadata.getFeatures() != null && !previewMetadata.getFeatures().isEmpty()) {
            scrollableContent.child(Components.label(Text.literal("Features:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));
            for (String feature : previewMetadata.getFeatures()) {
                scrollableContent.child(Components.label(Text.literal("• " + feature))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }
        }

        // Requirements
        if (previewMetadata.getRequirements() != null && !previewMetadata.getRequirements().isEmpty()) {
            scrollableContent.child(Components.label(Text.literal("Requirements:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));
            for (String req : previewMetadata.getRequirements()) {
                scrollableContent.child(Components.label(Text.literal("• " + req))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }
        }

        // Mods list
        if (previewMetadata.getMods() != null && !previewMetadata.getMods().isEmpty()) {
            scrollableContent.child(Components.label(Text.literal("Mods:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));
            for (String mod : previewMetadata.getMods()) {
                scrollableContent.child(Components.label(Text.literal("• " + mod))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }
        }

        ScrollContainer<FlowLayout> contentScrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), scrollableContent);
        contentScrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        contentScrollContainer.scrollStep(15);

        contentWrapper.child(contentScrollContainer);
        previewPanel.child(contentWrapper);
    }

    private void performImport() {
        if (selectedFile == null || previewMetadata == null) {
            statusLabel.text(Text.literal("No file selected"));
            statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
            return;
        }

        importButton.active(false);

        // Show progress
        progressPanel.clearChildren();
        progressPanel.child(Components.label(Text.literal("Starting import..."))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));

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
                    progressPanel.clearChildren();

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
                    progressPanel.clearChildren();
                    statusLabel.text(Text.literal("Error: " + error));
                    statusLabel.color(UITheme.color(UITheme.STATUS_ERROR_BORDER));
                    importButton.active(true);
                });
            }
        });
    }
}