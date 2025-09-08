package com.github.kd_gaming1.packcore.gui.configscreen;

import com.github.kd_gaming1.packcore.gui.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.configscreen.ui.UITheme;
import com.github.kd_gaming1.packcore.gui.configscreen.util.FileTreeNode;
import com.github.kd_gaming1.packcore.util.ConfigExportManager;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;
import static com.github.kd_gaming1.packcore.gui.configscreen.ui.UITheme.*;

public class ConfigExportScreen extends BaseOwoScreen<FlowLayout> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Identifier backgroundTexture;
    private ConfigExportManager exportManager;
    private Set<Path> selectedPaths = new HashSet<>();
    private FileTreeNode rootNode;

    // UI Components & state
    private FlowLayout treeContainer;
    private FlowLayout treePanelContainer;
    private FlowLayout metadataPanel;
    private LabelComponent selectionCountLabel;
    private LabelComponent selectionSizeLabel;
    private ButtonComponent nextButton;
    private boolean showingMetadata = false;

    // Metadata fields
    private TextBoxComponent nameField;
    private TextAreaComponent descriptionArea;
    private TextBoxComponent versionField;
    private TextBoxComponent authorField;

    // Resolution dropdown
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private final String defaultResolution = screenSize.width + "x" + screenSize.height;
    private ButtonComponent resolutionButton;
    private List<String> resolutionPresets = new ArrayList<>(List.of(
            "1920x1080", "2560x1440", "3840x2160"
    ));
    private String selectedResolution;
    private OverlayContainer<FlowLayout> resolutionOverlay = null;

    // Features / requirements
    private TextAreaComponent featuresArea;
    private TextAreaComponent requirementsArea;

    // Mods auto-detection & toggles
    private final Map<String, Boolean> modsSelected = new LinkedHashMap<>();
    private FlowLayout modsListContainer;

    // Placeholder info
    private boolean nameFieldHasPlaceholder = true;
    private boolean descriptionAreaHasPlaceholder = true;
    private boolean versionFieldHasPlaceholder = true;
    private boolean authorFieldHasPlaceholder = true;
    private boolean featuresAreaHasPlaceholder = true;
    private boolean requirementsAreaHasPlaceholder = true;

    public ConfigExportScreen() {
    }

    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        if (resolutionPresets.contains(defaultResolution)) {
            selectedResolution = defaultResolution;
        } else {
            selectedResolution = "Click Me"; // fallback
        }

        exportManager = new ConfigExportManager();
        backgroundTexture = Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png");

        rootComponent.surface(UiSurfaces.stretched(backgroundTexture, 1920, 1082));
        rootComponent.padding(Insets.of(6, 8, 8, 8));

        rootComponent.child(createHeader());

        FlowLayout contentArea = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        contentArea.gap(6);

        contentArea.child(createSidebar());

        FlowLayout mainPanel = Containers.verticalFlow(Sizing.expand(65), Sizing.expand());

        treePanelContainer = createTreePanel();
        metadataPanel = createMetadataPanel();

        // start with tree view
        mainPanel.child(treePanelContainer);

        contentArea.child(mainPanel);

        rootComponent.child(contentArea);

        // Build initial tree and scan mods
        rootNode = exportManager.buildFileTree();
        populateFileTree();
        scanAndPopulateMods();
    }

    private FlowLayout createHeader() {
        FlowLayout header = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.padding(Insets.of(4));
        header.verticalAlignment(VerticalAlignment.CENTER);

        Identifier logoId = Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png");
        TextureComponent logo = Components.texture(logoId, 0, 0, 40, 40, 40, 40);

        Text titleText = Text.literal("Export Configs - " + getModpackInfo().getName())
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
        sidebar.padding(Insets.of(12,12,10,10));
        sidebar.horizontalAlignment(HorizontalAlignment.CENTER);

        // Wrap scrollable content in a container
        FlowLayout scrollContent = Containers.verticalFlow(Sizing.fill(96), Sizing.content());
        scrollContent.gap(6);

        // Info section
        FlowLayout infoSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .padding(Insets.of(2));

        LabelComponent infoLabel = (LabelComponent) Components.label(Text.literal(
                        "Select folders and files from your game directory to export as a config. " +
                                "Choose what to include, then fill in the metadata details."
                )).color(UITheme.color(UITheme.TEXT_WHITE))
                .horizontalSizing(Sizing.fill(100));

        infoSection.child(infoLabel);
        scrollContent.child(infoSection);

        // Preset section
        FlowLayout presetSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2)
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)))
                .padding(Insets.of(6))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        LabelComponent presetLabel = Components.label(Text.literal("Quick Presets")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD));
        presetSection.child(presetLabel);

        presetSection.child(makePresetButton("Mod Configs Only", "mod_only"));
        presetSection.child(makePresetButton("MC Configs Only", "mc_only"));
        presetSection.child(makePresetButton("Both Configs", "both"));
        presetSection.child(makePresetButton("Clear All", "clear"));

        scrollContent.child(presetSection);

        // Selection info
        FlowLayout selectionInfo = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)))
                .padding(Insets.of(8))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        LabelComponent selectionLabel = (LabelComponent) Components.label(Text.literal("Selection Info")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(UITheme.ACCENT_GOLD))
                .margins(Insets.bottom(2));
        selectionInfo.child(selectionLabel);

        selectionCountLabel = Components.label(Text.literal("Selected: 0 items"))
                .color(UITheme.color(TEXT_SECONDARY));
        selectionInfo.child(selectionCountLabel);

        selectionSizeLabel = Components.label(Text.literal("Estimated size: 0 MB"))
                .color(UITheme.color(TEXT_SECONDARY));
        selectionInfo.child(selectionSizeLabel);

        selectionInfo.sizing(Sizing.fill(100), Sizing.content());
        scrollContent.child(selectionInfo);

        // Wrap scrollContent in a ScrollContainer
        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), scrollContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollStep(10);

        sidebar.child(scrollContainer);

        // Next button stays outside of the scroll area
        nextButton = (ButtonComponent) Components.button(Text.literal("Next: Add Metadata"),
                        button -> showMetadataPanel())
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 120, 60))
                .horizontalSizing(Sizing.fixed(120))
                .verticalSizing(Sizing.fixed(20));

        updateNextButton();
        sidebar.child(nextButton);

        return sidebar;
    }

    private ButtonComponent makePresetButton(String text, String presetType) {
        return (ButtonComponent) Components.button(Text.literal(text), button -> applyPreset(presetType))
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"),
                        0, 0, 90, 57))
                .horizontalSizing(Sizing.fixed(90))
                .verticalSizing(Sizing.fixed(19));
    }


    private FlowLayout createTreePanel() {
        FlowLayout treePanel = Containers.verticalFlow(Sizing.fill(100), Sizing.expand());
        treePanel.gap(4);
        treePanel.surface(UiSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/menu/info_box.png"), 1142, 934));
        treePanel.padding(Insets.of(14));

        LabelComponent header = Components.label(Text.literal("Game Directory Structure")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.TEXT_WHITE));
        treePanel.child(header);

        treeContainer = Containers.verticalFlow(Sizing.fill(98), Sizing.content());
        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), treeContainer);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollStep(10);
        treePanel.child(scrollContainer);

        return treePanel;
    }

    private FlowLayout createMetadataPanel() {
        FlowLayout panel = Containers.verticalFlow(Sizing.fill(100), Sizing.expand());
        panel.gap(6);
        panel.surface(UiSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/menu/info_box.png"), 1142, 934));
        panel.padding(Insets.of(14));

        LabelComponent header = Components.label(Text.literal("Config Metadata")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.TEXT_WHITE));
        panel.child(header);

        FlowLayout formContainer = Containers.verticalFlow(Sizing.fill(98), Sizing.content());
        formContainer.gap(8);

        // Name field with placeholder
        nameField = Components.textBox(Sizing.fill(65));
        nameField.setText("Enter a descriptive name for your config");
        nameField.setEditableColor(UITheme.TEXT_SECONDARY);
        setupTextBoxPlaceholder(nameField, "Enter a descriptive name for your config",
                () -> nameFieldHasPlaceholder, (value) -> nameFieldHasPlaceholder = value);
        formContainer.child(createFieldRow("Name*:", nameField));

        // Description area with placeholder
        descriptionArea = Components.textArea(Sizing.fill(65), Sizing.content());
        descriptionArea.maxLines(12);
        descriptionArea.displayCharCount(false);
        descriptionArea.setText("Describe what this config does, what features it enables, and any special instructions for users...");
        setupTextAreaPlaceholder(descriptionArea, "Describe what this config does, what features it enables, and any special instructions for users...",
                () -> descriptionAreaHasPlaceholder, (value) -> descriptionAreaHasPlaceholder = value);
        formContainer.child(createFieldRow("Description*:", descriptionArea));

        // Version field with placeholder
        versionField = Components.textBox(Sizing.fill(65));
        versionField.setText("1.0.0");
        versionField.setEditableColor(UITheme.TEXT_SECONDARY);
        setupTextBoxPlaceholder(versionField, "1.0.0",
                () -> versionFieldHasPlaceholder, (value) -> versionFieldHasPlaceholder = value);
        formContainer.child(createFieldRow("Version:", versionField));

        // Author field - pre-fill with username, not a placeholder
        authorField = Components.textBox(Sizing.fill(65));
        authorField.setText(MinecraftClient.getInstance().getSession().getUsername());
        formContainer.child(createFieldRow("Author:", authorField));

        // Resolution button (unchanged)
        resolutionButton = (ButtonComponent) Components.button(Text.literal(selectedResolution), b -> {
                    showResolutionDropdown(b);
                }).renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 57))
                .horizontalSizing(Sizing.fixed(90))
                .verticalSizing(Sizing.fixed(19));
        formContainer.child(createFieldRow("Target Resolution:", resolutionButton));

        // Features area with placeholder
        featuresArea = Components.textArea(Sizing.fill(65), Sizing.fixed(60));
        featuresArea.maxLines(6);
        featuresArea.setText("List the key features this config provides (one per line):\nBetter performance\nEnhanced visuals\nQuality of life improvements");
        setupTextAreaPlaceholder(featuresArea, "List the key features this config provides (one per line):\nBetter performance\nEnhanced visuals\nQuality of life improvements",
                () -> featuresAreaHasPlaceholder, (value) -> featuresAreaHasPlaceholder = value);
        formContainer.child(createFieldRow("Features (one per line):", featuresArea));

        // Requirements area with placeholder
        requirementsArea = Components.textArea(Sizing.fill(65), Sizing.fixed(60));
        requirementsArea.maxLines(6);
        requirementsArea.setText("List what users need to use this config (one per line):\nMinecraft 1.21.5\nFabric API\nSpecific mods if required");
        setupTextAreaPlaceholder(requirementsArea, "List what users need to use this config (one per line):\nMinecraft 1.21.5\nFabric API\nSpecific mods if required",
                () -> requirementsAreaHasPlaceholder, (value) -> requirementsAreaHasPlaceholder = value);
        formContainer.child(createFieldRow("Requirements (one per line):", requirementsArea));

        // Rest of the method remains the same...
        LabelComponent modsLabel = Components.label(Text.literal("Mods (auto-detected) (These are just to let the import user know what mods where used when the config was created):"))
                .color(UITheme.color(UITheme.TEXT_WHITE));
        FlowLayout modsRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        modsRow.gap(8);
        modsRow.child(modsLabel);

        modsListContainer = Containers.verticalFlow(Sizing.fill(65), Sizing.fixed(90));
        modsListContainer.gap(2);
        modsListContainer.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        modsListContainer.padding(Insets.of(4));
        modsListContainer.child(Components.label(Text.literal("Scanning mods folder...")).color(UITheme.color(TEXT_SECONDARY)));

        formContainer.child(modsRow);
        formContainer.child(modsListContainer);

        formContainer.child(Components.label(Text.literal(" ")).sizing(Sizing.fill(100), Sizing.fixed(20)));

        ScrollContainer<FlowLayout> formScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), formContainer);
        formScroll.scrollbar(ScrollContainer.Scrollbar.vanilla());
        formScroll.scrollStep(10);

        panel.child(formScroll);

        // Buttons (unchanged)
        FlowLayout buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonRow.gap(8);
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        ButtonComponent backToSelectionButton = (ButtonComponent) Components.button(Text.literal("Back to Selection"),
                        button -> hideMetadataPanel())
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20));

        ButtonComponent exportButton = (ButtonComponent) Components.button(Text.literal("Export Config"),
                        button -> exportConfig())
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20));

        buttonRow.child(backToSelectionButton);
        buttonRow.child(exportButton);
        panel.child(buttonRow);

        return panel;
    }

    // Helper method for TextBoxComponent placeholder behavior
    private void setupTextBoxPlaceholder(TextBoxComponent textBox, String placeholderText,
                                         java.util.function.Supplier<Boolean> hasPlaceholder,
                                         java.util.function.Consumer<Boolean> setPlaceholder) {

        // Handle focus events using owo-lib's event system
        textBox.focusGained().subscribe(focusSource -> {
            if (hasPlaceholder.get()) {
                textBox.setText("");
                textBox.setEditableColor(UITheme.TEXT_WHITE);
                setPlaceholder.accept(false);
            }
        });


        textBox.focusLost().subscribe(() -> {
            String currentText = textBox.getText();

            if (currentText.trim().isEmpty()) {
                textBox.setText(placeholderText);
                textBox.setEditableColor(UITheme.TEXT_SECONDARY);
                setPlaceholder.accept(true);
            }
        });
    }

    // Helper method for TextAreaComponent placeholder behavior
    private void setupTextAreaPlaceholder(TextAreaComponent textArea, String placeholderText,
                                          java.util.function.Supplier<Boolean> hasPlaceholder,
                                          java.util.function.Consumer<Boolean> setPlaceholder) {

        // Handle focus events using owo-lib's event system
        textArea.focusGained().subscribe(focusSource -> {
            if (hasPlaceholder.get()) {
                textArea.setText("");
                setPlaceholder.accept(false);
            }
        });

        textArea.focusLost().subscribe(() -> {
            String currentText = textArea.getText();

            if (currentText.trim().isEmpty()) {
                textArea.setText(placeholderText);
                setPlaceholder.accept(true);
            }
        });
    }

    private FlowLayout createFieldRow(String labelText, io.wispforest.owo.ui.core.Component field) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(8);
        row.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent label = Components.label(Text.literal(labelText)).color(UITheme.color(UITheme.TEXT_WHITE));
        label.sizing(Sizing.fill(30), Sizing.content());

        row.child(label);
        row.child(field);

        return row;
    }

    private void populateFileTree() {
        treeContainer.clearChildren();
        if (rootNode != null) {
            addTreeNode(rootNode, 0);
        }
    }

    private void addTreeNode(FileTreeNode node, int depth) {
        if (node.isHidden()) return;

        FlowLayout nodeEntry = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        nodeEntry.gap(4);
        nodeEntry.padding(Insets.left(depth * 16));
        nodeEntry.verticalAlignment(VerticalAlignment.CENTER);

        if (node.isDirectory() && !node.getChildren().isEmpty()) {
            String expandIcon = node.isExpanded() ? "▼" : "▶";
            ButtonComponent expandButton = (ButtonComponent) Components.button(
                            Text.literal(expandIcon),
                            button -> {
                                node.setExpanded(!node.isExpanded());
                                populateFileTree();
                            })
                    .renderer(ButtonComponent.Renderer.flat(UITheme.ENTRY_BACKGROUND, UITheme.ACCENT_GOLD, UITheme.ENTRY_BORDER))
                    .sizing(Sizing.fixed(16), Sizing.fixed(16));
            nodeEntry.child(expandButton);
        } else {
            nodeEntry.child(Components.label(Text.literal("  ")).sizing(Sizing.fixed(16), Sizing.fixed(16)));
        }

        boolean isSelected = selectedPaths.contains(node.getPath());
        String checkboxText = isSelected ? "☑" : "☐";
        int checkboxColor = isSelected ? UITheme.STATUS_SUCCESS_BORDER : UITheme.ENTRY_BORDER;
        ButtonComponent checkbox = (ButtonComponent) Components.button(Text.literal(checkboxText),
                        button -> toggleSelection(node))
                .renderer(ButtonComponent.Renderer.flat(UITheme.ENTRY_BACKGROUND, checkboxColor, UITheme.ENTRY_BORDER))
                .sizing(Sizing.fixed(20), Sizing.fixed(16));
        nodeEntry.child(checkbox);

        String icon = node.isDirectory() ? "📁" : "📄";
        int nameColor = selectedPaths.contains(node.getPath()) ? UITheme.ACCENT_GOLD : UITheme.TEXT_WHITE;
        LabelComponent nameLabel = Components.label(Text.literal(icon + " " + node.getName()))
                .color(UITheme.color(nameColor));
        nodeEntry.child(nameLabel);

        nodeEntry.mouseEnter().subscribe(() -> {
            if (!selectedPaths.contains(node.getPath())) {
                nodeEntry.surface(Surface.flat(UITheme.ENTRY_HOVER));
            }
        });

        nodeEntry.mouseLeave().subscribe(() -> {
            if (!selectedPaths.contains(node.getPath())) {
                nodeEntry.surface(Surface.BLANK);
            }
        });

        if (selectedPaths.contains(node.getPath())) {
            nodeEntry.surface(Surface.flat(UITheme.ENTRY_SELECTED));
        }

        treeContainer.child(nodeEntry);

        if (node.isDirectory() && node.isExpanded()) {
            for (FileTreeNode child : node.getChildren()) {
                addTreeNode(child, depth + 1);
            }
        }
    }

    private void toggleSelection(FileTreeNode node) {
        if (node == null) return;

        if (node.isDirectory()) {
            Set<Path> descendants = collectDescendantPaths(node);
            boolean alreadySelected = selectedPaths.contains(node.getPath());

            if (alreadySelected) selectedPaths.removeAll(descendants);
            else selectedPaths.addAll(descendants);
        } else {
            Path path = node.getPath();
            if (selectedPaths.contains(path)) selectedPaths.remove(path);
            else selectedPaths.add(path);
        }

        updateSelectionInfo();
        updateNextButton();
        populateFileTree();
    }

    private Set<Path> collectDescendantPaths(FileTreeNode node) {
        Set<Path> paths = new HashSet<>();
        if (node == null) return paths;
        paths.add(node.getPath());
        if (node.isDirectory()) {
            for (FileTreeNode child : node.getChildren()) {
                paths.addAll(collectDescendantPaths(child));
            }
        }
        return paths;
    }

    private FileTreeNode findNodeByPath(FileTreeNode current, Path target) {
        if (current == null || target == null) return null;
        if (current.getPath().equals(target)) return current;
        if (current.isDirectory()) {
            for (FileTreeNode child : current.getChildren()) {
                FileTreeNode found = findNodeByPath(child, target);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void updateSelectionInfo() {
        int count = selectedPaths.size();
        long estimatedSize = exportManager.calculateSelectionSize(selectedPaths);

        selectionCountLabel.text(Text.literal("Selected: " + count + " item" + (count == 1 ? "" : "s")));

        String sizeText;
        if (estimatedSize < 1024) sizeText = estimatedSize + " B";
        else if (estimatedSize < 1024 * 1024) sizeText = (estimatedSize / 1024) + " KB";
        else sizeText = (estimatedSize / (1024 * 1024)) + " MB";

        selectionSizeLabel.text(Text.literal("Estimated size: " + sizeText));
    }

    private void updateNextButton() {
        boolean hasSelection = !selectedPaths.isEmpty();
        nextButton.active = hasSelection && !showingMetadata;
    }

    private void applyPreset(String presetType) {
        selectedPaths.clear();
        selectedPaths.addAll(exportManager.getPresetPaths(presetType));

        if (rootNode != null && !selectedPaths.isEmpty()) {
            Set<Path> expandedSelection = new HashSet<>();
            for (Path base : new HashSet<>(selectedPaths)) {
                FileTreeNode node = findNodeByPath(rootNode, base);
                if (node != null) {
                    node.setExpanded(true);
                    expandedSelection.addAll(collectDescendantPaths(node));
                } else {
                    expandedSelection.add(base);
                }
            }
            selectedPaths.clear();
            selectedPaths.addAll(expandedSelection);
        }

        updateSelectionInfo();
        updateNextButton();
        populateFileTree();
    }

    private void showMetadataPanel() {
        if (selectedPaths.isEmpty()) return;

        showingMetadata = true;
        nextButton.active = false;

        FlowLayout mainPanel = (FlowLayout) treePanelContainer.parent();
        mainPanel.removeChild(treePanelContainer);
        mainPanel.child(metadataPanel);

        updateNextButton();
    }

    private void hideMetadataPanel() {
        showingMetadata = false;

        FlowLayout mainPanel = (FlowLayout) metadataPanel.parent();
        mainPanel.removeChild(metadataPanel);
        mainPanel.child(treePanelContainer);

        updateNextButton();
    }

    // --- Resolution dropdown and custom popup ---
    private void showResolutionDropdown(ButtonComponent anchor) {
        // Remove previous overlay if present
        if (resolutionOverlay != null) {
            this.uiAdapter.rootComponent.removeChild(resolutionOverlay);
            resolutionOverlay = null;
        }

        FlowLayout list = Containers.verticalFlow(Sizing.fixed(140), Sizing.content());
        list.gap(4);
        list.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        list.padding(Insets.of(6));

        for (String r : resolutionPresets) {
            ButtonComponent opt = (ButtonComponent) Components.button(Text.literal(r), b -> {
                selectedResolution = r;
                resolutionButton.setMessage(Text.literal(selectedResolution));
                if (resolutionOverlay != null) this.uiAdapter.rootComponent.removeChild(resolutionOverlay);
                resolutionOverlay = null;
            }).sizing(Sizing.fill(100), Sizing.fixed(18));
            list.child(opt);
        }

        ButtonComponent custom = (ButtonComponent) Components.button(Text.literal("Add custom..."), b -> {
            if (resolutionOverlay != null) this.uiAdapter.rootComponent.removeChild(resolutionOverlay);
            resolutionOverlay = null;
            showAddCustomResolutionPopup();
        }).sizing(Sizing.fill(100), Sizing.fixed(18));
        list.child(custom);

        resolutionOverlay = Containers.overlay(list);
        resolutionOverlay.positioning(Positioning.relative(50, 40));
        resolutionOverlay.zIndex(50);
        resolutionOverlay.child(list);
        this.uiAdapter.rootComponent.child(resolutionOverlay);
    }

    private void showAddCustomResolutionPopup() {
        FlowLayout popup = Containers.verticalFlow(Sizing.fill(60), Sizing.content());
        popup.gap(6);
        popup.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)));
        popup.padding(Insets.of(8));

        TextBoxComponent input = Components.textBox(Sizing.fill(60), "");
        ButtonComponent add = (ButtonComponent) Components.button(Text.literal("Add"), b -> {
            String val = input.getText().trim();
            if (!val.isEmpty() && !resolutionPresets.contains(val)) {
                resolutionPresets.add(0, val);
            }
            if (!val.isEmpty()) selectedResolution = val;
            resolutionButton.setMessage(Text.literal(selectedResolution));
            // remove overlays
            for (var child : new ArrayList<>(this.uiAdapter.rootComponent.children())) {
                if (child instanceof OverlayContainer) this.uiAdapter.rootComponent.removeChild(child);
            }
        }).sizing(Sizing.fixed(40), Sizing.fixed(20));

        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(8);
        row.child(input);
        row.child(add);
        popup.child(row);

        OverlayContainer<FlowLayout> ov = Containers.overlay(popup);
        ov.positioning(Positioning.relative(50, 40));
        ov.zIndex(60);
        ov.child(popup);
        this.uiAdapter.rootComponent.child(ov);
    }

    // --- Mods scanning and UI population ---
    private void scanAndPopulateMods() {
        // Run quickly on UI thread; scanning small directory is fine. If repo expects heavy scanning, spawn background thread.
        try {
            Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
            Path modsDir = gameDir.resolve("mods");
            List<String> discovered = new ArrayList<>();

            if (Files.exists(modsDir) && Files.isDirectory(modsDir)) {
                try (Stream<Path> stream = Files.list(modsDir)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> {
                                String s = p.getFileName().toString().toLowerCase();
                                return s.endsWith(".jar") || s.endsWith(".zip");
                            })
                            .forEach(p -> {
                                String filename = p.getFileName().toString();
                                String modId = filename.replaceAll("\\.(jar|zip)$", "");
                                discovered.add(modId);
                            });
                }
            }

            populateModsList(discovered);
        } catch (Exception e) {
            LOGGER.error("Failed to scan mods folder", e);
            modsListContainer.clearChildren();
            modsListContainer.child(Components.label(Text.literal("Failed to scan mods")).color(Color.ofRgb(TEXT_SECONDARY)));
        }
    }

    private void populateModsList(List<String> mods) {
        modsSelected.clear();
        modsListContainer.clearChildren();

        if (mods.isEmpty()) {
            modsListContainer.child(Components.label(Text.literal("No mods found")).color(Color.ofRgb(TEXT_SECONDARY)));
            return;
        }

        for (String modId : mods) {
            modsSelected.put(modId, Boolean.TRUE); // default included

            ButtonComponent toggle = (ButtonComponent) Components.button(Text.literal("☑ " + modId), b -> {
                boolean prev = modsSelected.getOrDefault(modId, true);
                boolean now = !prev;
                modsSelected.put(modId, now);
                b.setMessage(Text.literal((now ? "☑ " : "☐ ") + modId));
            }).sizing(Sizing.fill(100), Sizing.fixed(18));

            modsListContainer.child(toggle);
        }
    }

    // --- Export wiring ---
    private void exportConfig() {
        try {
            // Get actual text values, checking for placeholders
            String name = nameFieldHasPlaceholder ? "" : nameField.getText().trim();
            String description = descriptionAreaHasPlaceholder ? "" : descriptionArea.getText().trim();
            String version = versionFieldHasPlaceholder ? "1.0.0" : versionField.getText().trim();
            String author = authorField.getText().trim();
            String resolution = selectedResolution;

            if (name.isEmpty() || description.isEmpty()) {
                showTemporaryMessage("Name and Description are required fields!", STATUS_ERROR_BG, STATUS_ERROR_BORDER);
                return;
            }

            String featuresText = featuresAreaHasPlaceholder ? "" : featuresArea.getText();
            String requirementsText = requirementsAreaHasPlaceholder ? "" : requirementsArea.getText();

            List<String> features = featuresText.isEmpty() ? new ArrayList<>() :
                    Arrays.stream(featuresText.split("[\\r\\n,]+"))
                            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

            List<String> requirements = requirementsText.isEmpty() ? new ArrayList<>() :
                    Arrays.stream(requirementsText.split("[\\r\\n,]+"))
                            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

            List<String> mods = modsSelected.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            showTemporaryMessage("Exporting config... This may take a moment.", STATUS_SUCCESS_BG, STATUS_SUCCESS_BORDER);

            Path exportedPath = exportManager.exportConfig(
                    selectedPaths,
                    name,
                    description,
                    version.isEmpty() ? "1.0.0" : version,
                    author.isEmpty() ? "Unknown" : author,
                    resolution == null || resolution.isEmpty() ? "1920x1080" : resolution,
                    features,
                    requirements,
                    mods
            );

            exportManager.openExportFolder();
            MinecraftClient.getInstance().setScreen(new ModpackConfigMenuScreen());
        } catch (IOException e) {
            LOGGER.error("Failed to export config", e);
            showTemporaryMessage("Failed to export config: " + e.getMessage(), STATUS_ERROR_BG, STATUS_ERROR_BORDER);
        }
    }

    // Temporary message helper (keeps same UX)
    private void showTemporaryMessage(String message, int bgColor, int borderColor) {
        FlowLayout messagePanel = Containers.verticalFlow(Sizing.fill(60), Sizing.content());
        messagePanel.surface(Surface.flat(bgColor).and(Surface.outline(borderColor)));
        messagePanel.padding(Insets.of(16));
        messagePanel.gap(8);
        messagePanel.positioning(Positioning.relative(50, 40));
        messagePanel.zIndex(10);

        OverlayContainer<FlowLayout> messageOverlay = Containers.overlay(messagePanel);

        LabelComponent messageLabel = (LabelComponent) Components.label(Text.literal(message))
                .color(Color.ofRgb(TEXT_WHITE))
                .sizing(Sizing.fill(90), Sizing.content());

        ButtonComponent okButton = (ButtonComponent) Components.button(Text.literal("OK"), button -> {
                    this.uiAdapter.rootComponent.removeChild(messageOverlay);
                }).renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ACCENT_GOLD, ENTRY_BORDER))
                .sizing(Sizing.fixed(60), Sizing.fixed(25));

        FlowLayout buttonContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonContainer.horizontalAlignment(HorizontalAlignment.CENTER);
        buttonContainer.child(okButton);

        messagePanel.child(messageLabel);
        messagePanel.child(buttonContainer);
        messageOverlay.child(messagePanel);

        this.uiAdapter.rootComponent.child(messageOverlay);

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                if (this.uiAdapter.rootComponent.children().contains(messageOverlay)) {
                    MinecraftClient.getInstance().execute(() -> this.uiAdapter.rootComponent.removeChild(messageOverlay));
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}