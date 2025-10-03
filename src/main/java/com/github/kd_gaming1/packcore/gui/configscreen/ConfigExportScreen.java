package com.github.kd_gaming1.packcore.gui.configscreen;

import com.github.kd_gaming1.packcore.gui.util.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.component.PlaceholderTextAreaComponent;
import com.github.kd_gaming1.packcore.gui.ui.UITheme;
import com.github.kd_gaming1.packcore.gui.configscreen.util.FileTreeNode;
import com.github.kd_gaming1.packcore.util.ConfigExportManager;
import com.github.kd_gaming1.packcore.util.ConfigExportManager.ExportRequest;
import com.github.kd_gaming1.packcore.util.ConfigExportManager.PresetType;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;
import static com.github.kd_gaming1.packcore.gui.ui.UITheme.*;

public class ConfigExportScreen extends BaseOwoScreen<FlowLayout> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String DEFAULT_VERSION = "1.0.0";

    private ConfigExportManager exportManager;
    private Set<Path> selectedPaths = new HashSet<>();
    private Map<String, Boolean> modsToInclude = new LinkedHashMap<>();
    private FileTreeNode rootNode;

    // UI Components
    private FlowLayout treeContainer;
    private FlowLayout contentPanel;
    private LabelComponent selectionInfoLabel;
    private ButtonComponent nextButton;

    // Metadata input fields
    private TextBoxComponent nameField;
    private TextAreaComponent descriptionArea;
    private TextBoxComponent versionField;
    private TextBoxComponent authorField;
    private ButtonComponent resolutionButton;
    private FlowLayout modsListContainer;

    // UI State
    private boolean showingMetadata = false;
    private String selectedResolution;
    private String currentResolution;

    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        exportManager = new ConfigExportManager();
        detectCurrentResolution();

        rootComponent.surface(UiSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png"), 1920, 1082));
        rootComponent.padding(Insets.of(8));

        rootComponent.child(createHeader());
        rootComponent.child(createMainContent());

        // Initialize file tree
        rootNode = exportManager.buildFileTree();
        refreshFileTree();
        scanMods();
    }

    private void detectCurrentResolution() {
        var mc = MinecraftClient.getInstance();
        currentResolution = mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight();
    }

    private FlowLayout createHeader() {
        var header = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(50));
        header.gap(8);
        header.verticalAlignment(VerticalAlignment.CENTER);

        header.child(Components.texture(
                Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png"),
                0, 0, 40, 40, 40, 40));

        header.child(Components.label(
                        Text.literal("Export Configuration - " + getModpackInfo().getName())
                                .styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))))
                .color(UITheme.color(TEXT_WHITE)));

        var backContainer = Containers.horizontalFlow(Sizing.expand(), Sizing.content());
        backContainer.horizontalAlignment(HorizontalAlignment.RIGHT);
        backContainer.child(createBackButton());
        header.child(backContainer);

        return header;
    }

    private ButtonComponent createBackButton() {
        return (ButtonComponent) Components.button(Text.literal("Back"),
                        btn -> MinecraftClient.getInstance().setScreen(new ModpackConfigMenuScreen()))
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/previous.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19));
    }

    private FlowLayout createMainContent() {
        var mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        mainContent.gap(8);
        mainContent.child(createSidebar());
        mainContent.child(createContentArea());
        return mainContent;
    }

    private FlowLayout createSidebar() {
        var sidebar = Containers.verticalFlow(Sizing.fill(35), Sizing.expand());
        sidebar.gap(8);
        sidebar.surface(UiSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/menu/notif_box.png"), 607, 755));
        sidebar.padding(Insets.of(12));

        var scrollContent = Containers.verticalFlow(Sizing.fill(98), Sizing.content());
        scrollContent.gap(8);

        scrollContent.child(createInfoSection());
        scrollContent.child(createPresetSection());
        scrollContent.child(createSelectionInfo());

        var scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), scrollContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        sidebar.child(scrollContainer);

        nextButton = (ButtonComponent) Components.button(
                        Text.literal("Next: Add Details"),
                        btn -> showMetadataView())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 120, 60))
                .sizing(Sizing.fixed(120), Sizing.fixed(20));

        nextButton.active(false);

        sidebar.child(nextButton);
        return sidebar;
    }

    private FlowLayout createInfoSection() {
        var section = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        section.padding(Insets.of(8));
        section.child(Components.label(
                        Text.literal("Select files and folders to include in your configuration export."))
                .color(UITheme.color(TEXT_WHITE)));
        return section;
    }

    private FlowLayout createPresetSection() {
        var section = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        section.gap(4);
        section.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        section.padding(Insets.of(8));
        section.horizontalAlignment(HorizontalAlignment.CENTER);

        section.child(Components.label(Text.literal("Quick Presets")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(ACCENT_GOLD)));

        for (PresetType preset : PresetType.values()) {
            section.child(createPresetButton(preset));
        }

        return section;
    }

    private ButtonComponent createPresetButton(PresetType preset) {
        return (ButtonComponent) Components.button(
                        Text.literal(preset.getDisplayName()),
                        btn -> applyPreset(preset))
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 57))
                .sizing(Sizing.fixed(90), Sizing.fixed(19));
    }

    private FlowLayout createSelectionInfo() {
        var section = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        section.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        section.padding(Insets.of(8));
        section.horizontalAlignment(HorizontalAlignment.CENTER);

        section.child(Components.label(Text.literal("Selection Info")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(ACCENT_GOLD)));

        selectionInfoLabel = Components.label(Text.literal("0 items selected\nSize: 0 KB"))
                .color(UITheme.color(TEXT_SECONDARY));
        section.child(selectionInfoLabel);

        return section;
    }

    private FlowLayout createContentArea() {
        contentPanel = Containers.verticalFlow(Sizing.fill(65), Sizing.expand());
        contentPanel.surface(UiSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/menu/info_box.png"), 1142, 934));
        contentPanel.padding(Insets.of(14));

        showFileTreeView();
        return contentPanel;
    }

    private void showFileTreeView() {
        contentPanel.clearChildren();
        showingMetadata = false;
        updateNextButton();

        contentPanel.child(Components.label(Text.literal("Select Files to Export")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(ACCENT_GOLD)));

        treeContainer = Containers.verticalFlow(Sizing.fill(98), Sizing.content());
        var scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), treeContainer);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());

        contentPanel.child(scrollContainer);
        refreshFileTree();
    }

    private void showMetadataView() {
        contentPanel.clearChildren();
        showingMetadata = true;
        updateNextButton();

        contentPanel.child(Components.label(Text.literal("Configuration Details")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(UITheme.color(ACCENT_GOLD)));

        var formContainer = Containers.verticalFlow(Sizing.fill(98), Sizing.content());
        formContainer.gap(8);

        // Name field
        nameField = Components.textBox(Sizing.fill(70), "");
        nameField.setPlaceholder(Text.literal("Enter configuration name"));
        formContainer.child(createFormRow("Name*:", nameField));

        // Description area
        descriptionArea = PlaceholderTextAreaComponent.create(
                Sizing.fill(70),
                Sizing.fixed(80),
                Text.literal("Describe what this configuration does...")
        );
        formContainer.child(createFormRow("Description:", descriptionArea));

        // Version field
        versionField = Components.textBox(Sizing.fixed(120), DEFAULT_VERSION);
        formContainer.child(createFormRow("Version:", versionField));

        // Author field (pre-filled)
        authorField = Components.textBox(Sizing.fill(70),
                MinecraftClient.getInstance().getSession().getUsername());
        formContainer.child(createFormRow("Author:", authorField));

        // Resolution dropdown
        populateResolutionDropdown();
        formContainer.child(createFormRow("Target Resolution:", resolutionButton));

        // Mods list
        formContainer.child(Components.label(Text.literal("Installed mods when the configs was exported:"))
                        .color(UITheme.color(TEXT_WHITE)))
                .horizontalSizing(Sizing.fill(90));

        modsListContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.fixed(120));
        modsListContainer.surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
        modsListContainer.padding(Insets.of(8));

        var modsScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fixed(120), modsListContainer);
        modsScroll.scrollbar(ScrollContainer.Scrollbar.vanilla());
        formContainer.child(modsScroll);

        populateModsList();

        var scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), formContainer);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        contentPanel.child(scrollContainer);

        // Action buttons
        var buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8);
        buttonRow.margins(Insets.top(6));
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonRow.child(Components.button(Text.literal("Back"), btn -> showFileTreeView())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 60))
                .sizing(Sizing.fixed(90), Sizing.fixed(20)));

        buttonRow.child(Components.button(Text.literal("Export"), btn -> performExport())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 60))
                .sizing(Sizing.fixed(90), Sizing.fixed(20)));

        contentPanel.child(buttonRow);
    }

    private FlowLayout createFormRow(String label, Component field) {
        var row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(8);
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.child(Components.label(Text.literal(label))
                .color(UITheme.color(TEXT_WHITE))
                .sizing(Sizing.fixed(120), Sizing.content()));
        row.child(field);
        return row;
    }

    private void populateResolutionDropdown() {
        // Create a button that shows the current selection and opens a dropdown when clicked
        resolutionButton = (ButtonComponent) Components.button(
                        Text.literal(currentResolution),
                        btn -> openResolutionDropdown(btn)
                )
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 120, 60))
                .sizing(Sizing.fixed(120), Sizing.fixed(20));

        selectedResolution = currentResolution;
    }

    private void openResolutionDropdown(ButtonComponent button) {
        var commonResolutions = List.of(
                "1920x1080", "2560x1440", "3840x2160", "1280x720", "1366x768",
                "1600x900", "1440x900", currentResolution
        );

        // Remove duplicates while preserving order
        var uniqueResolutions = commonResolutions.stream()
                .distinct()
                .collect(Collectors.toList());

        // Create and open the dropdown menu
        DropdownComponent.openContextMenu(
                        this, // screen
                        this.uiAdapter.rootComponent, // root component
                        (root, dropdown) -> root.child(dropdown), // mount function
                        button.x(), // mouse x (button position)
                        button.y() + button.height(), // mouse y (below button)
                        dropdown -> {
                            // Add resolution options
                            for (String resolution : uniqueResolutions) {
                                dropdown.button(Text.literal(resolution), selectedDropdown -> {
                                    selectedResolution = resolution;
                                    currentResolution = resolution;
                                    resolutionButton.setMessage(Text.literal(resolution));
                                    selectedDropdown.parent().removeChild(selectedDropdown);
                                });
                            }

                            // Add divider and custom resolution option
                            dropdown.divider();
                            dropdown.button(Text.literal("Custom..."), selectedDropdown -> {
                                openCustomResolutionDialog();
                                selectedDropdown.parent().removeChild(selectedDropdown);
                            });
                        }
                )
                .zIndex(8);
    }

    private void openCustomResolutionDialog() {
        // Create a simple dialog for custom resolution input
        var dialogContainer = Containers.verticalFlow(Sizing.fixed(300), Sizing.content());
        dialogContainer.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        dialogContainer.padding(Insets.of(16));
        dialogContainer.positioning(Positioning.absolute(
                (this.width - 300) / 2,
                (this.height - 150) / 2
        )).zIndex(10);

        dialogContainer.child(Components.label(Text.literal("Enter Custom Resolution"))
                .color(UITheme.color(ACCENT_GOLD)));

        var customResolutionField = Components.textBox(Sizing.fixed(200), "1920x1080");
        customResolutionField.setPlaceholder(Text.literal("Width x Height (e.g. 1920x1080)"));
        dialogContainer.child(customResolutionField);

        var buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonRow.gap(8);
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonRow.child(Components.button(Text.literal("Cancel"), btn -> {
            this.uiAdapter.rootComponent.removeChild(dialogContainer);
        }).sizing(Sizing.fixed(60), Sizing.fixed(20)));

        buttonRow.child(Components.button(Text.literal("OK"), btn -> {
            String customRes = customResolutionField.getText().trim();
            if (customRes.matches("\\d+x\\d+")) { // Basic validation
                selectedResolution = customRes;
                currentResolution = customRes;
                resolutionButton.setMessage(Text.literal(customRes));
            }
            this.uiAdapter.rootComponent.removeChild(dialogContainer);
        }).sizing(Sizing.fixed(60), Sizing.fixed(20)));

        dialogContainer.child(buttonRow);
        this.uiAdapter.rootComponent.child(dialogContainer);
    }

    private void refreshFileTree() {
        if (treeContainer == null || rootNode == null) return;

        treeContainer.clearChildren();
        addTreeNode(rootNode, 0);
    }

    private void addTreeNode(FileTreeNode node, int depth) {
        if (node.isHidden()) return;

        var nodeRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        nodeRow.gap(4);
        nodeRow.padding(Insets.left(depth * 16));
        nodeRow.verticalAlignment(VerticalAlignment.CENTER);

        // Expand/collapse button for directories
        if (node.isDirectory() && !node.getChildren().isEmpty()) {
            nodeRow.child(Components.button(
                            Text.literal(node.isExpanded() ? "▼" : "▶"),
                            btn -> {
                                node.setExpanded(!node.isExpanded());
                                refreshFileTree();
                            })
                    .renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ACCENT_GOLD, ENTRY_BORDER))
                    .sizing(Sizing.fixed(16), Sizing.fixed(16)));
        } else {
            var placeholder = Components.box(Sizing.fixed(16), Sizing.fixed(16));
            placeholder.fill(true);
            placeholder.color(Color.ofArgb(0x00000000));
            nodeRow.child(placeholder);
        }

        // Selection checkbox
        boolean isSelected = selectedPaths.contains(node.getPath());
        nodeRow.child(Components.checkbox(Text.empty())
                .checked(isSelected)
                .onChanged(checked -> toggleSelection(node, checked)));

        // Node label
        String icon = node.isDirectory() ? "📁" : "📄";
        nodeRow.child(Components.label(Text.literal(icon + " " + node.getName()))
                .color(UITheme.color(isSelected ? ACCENT_GOLD : TEXT_WHITE)));

        treeContainer.child(nodeRow);

        // Add children if expanded
        if (node.isDirectory() && node.isExpanded()) {
            for (FileTreeNode child : node.getChildren()) {
                addTreeNode(child, depth + 1);
            }
        }
    }

    private void toggleSelection(FileTreeNode node, boolean selected) {
        if (selected) {
            selectedPaths.add(node.getPath());
            // Also select all descendants for directories
            if (node.isDirectory()) {
                addDescendants(node);
            }
        } else {
            selectedPaths.remove(node.getPath());
            // Also deselect all descendants
            if (node.isDirectory()) {
                removeDescendants(node);
            }
        }

        updateSelectionInfo();
        refreshFileTree();
    }

    private void addDescendants(FileTreeNode node) {
        for (FileTreeNode child : node.getChildren()) {
            selectedPaths.add(child.getPath());
            if (child.isDirectory()) {
                addDescendants(child);
            }
        }
    }

    private void removeDescendants(FileTreeNode node) {
        for (FileTreeNode child : node.getChildren()) {
            selectedPaths.remove(child.getPath());
            if (child.isDirectory()) {
                removeDescendants(child);
            }
        }
    }

    private void updateSelectionInfo() {
        int count = selectedPaths.size();
        long size = exportManager.calculateSelectionSize(selectedPaths);

        String sizeText = formatSize(size);
        selectionInfoLabel.text(Text.literal(
                count + " item" + (count != 1 ? "s" : "") + " selected\nSize: " + sizeText));

        updateNextButton();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void updateNextButton() {
        nextButton.active(!selectedPaths.isEmpty() && !showingMetadata);
        if (showingMetadata) {
            nextButton.setMessage(Text.literal("Currently editing..."));
        } else {
            nextButton.setMessage(Text.literal("Next: Add Details"));
        }
    }

    private void applyPreset(PresetType preset) {
        // Clear current selection
        selectedPaths.clear();

        // Get preset paths from the export manager
        Set<Path> presetPaths = exportManager.getPresetPaths(preset);

        // For each preset path, use the proper selection logic
        for (Path path : presetPaths) {
            // Find the corresponding tree node
            FileTreeNode node = findNodeByPath(rootNode, path);
            if (node != null) {
                // Use the same logic as manual selection
                selectedPaths.add(node.getPath());
                if (node.isDirectory()) {
                    addDescendants(node);
                }

                // Expand nodes for selected paths
                expandToPath(rootNode, path);
            }
        }

        updateSelectionInfo();
        refreshFileTree();
    }

    /**
     * Find a tree node by its path recursively
     */
    private FileTreeNode findNodeByPath(FileTreeNode currentNode, Path targetPath) {
        if (currentNode.getPath().equals(targetPath)) {
            return currentNode;
        }

        // Search in children
        for (FileTreeNode child : currentNode.getChildren()) {
            FileTreeNode result = findNodeByPath(child, targetPath);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private boolean expandToPath(FileTreeNode node, Path target) {
        if (node.getPath().equals(target)) {
            return true;
        }

        if (target.startsWith(node.getPath()) && node.isDirectory()) {
            for (FileTreeNode child : node.getChildren()) {
                if (expandToPath(child, target)) {
                    node.setExpanded(true);
                    return true;
                }
            }
        }

        return false;
    }

    private void scanMods() {
        List<String> mods = exportManager.scanInstalledMods();
        modsToInclude.clear();
        for (String mod : mods) {
            modsToInclude.put(mod, true);  // Include all by default
        }
    }

    private void populateModsList() {
        modsListContainer.clearChildren();

        if (modsToInclude.isEmpty()) {
            modsListContainer.child(Components.label(Text.literal("No mods found"))
                    .color(UITheme.color(TEXT_SECONDARY)));
            return;
        }

        for (Map.Entry<String, Boolean> entry : modsToInclude.entrySet()) {
            var checkbox = Components.checkbox(Text.literal(entry.getKey()))
                    .checked(entry.getValue())
                    .onChanged(checked -> modsToInclude.put(entry.getKey(), checked));
            modsListContainer.child(checkbox);
        }
    }

    private void performExport() {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                showError("Configuration name is required!");
                return;
            }

            List<String> includedMods = modsToInclude.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            ExportRequest request = new ExportRequest(
                    selectedPaths,
                    name,
                    descriptionArea.getText().trim(),
                    versionField.getText().trim(),
                    authorField.getText().trim(),
                    selectedResolution, // Use selectedResolution instead of currentResolution
                    includedMods
            );

            Path exportedPath = exportManager.exportConfig(request);
            exportManager.openExportFolder();

            MinecraftClient.getInstance().setScreen(new ModpackConfigMenuScreen());

        } catch (IOException e) {
            LOGGER.error("Failed to export configuration", e);
            showError("Export failed: " + e.getMessage());
        }
    }

    private void showError(String message) {
        // You could show a popup here, for now just log
        LOGGER.error(message);
    }
}