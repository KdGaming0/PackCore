package com.github.kd_gaming1.packcore.ui.screen.configmanager;

import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import com.github.kd_gaming1.packcore.ui.component.PlaceholderTextArea;
import com.github.kd_gaming1.packcore.ui.theme.UITheme;
import com.github.kd_gaming1.packcore.ui.component.tree.FileTreeNode;
import com.github.kd_gaming1.packcore.config.export.ConfigExportService;
import com.github.kd_gaming1.packcore.config.export.ConfigExportService.ExportRequest;
import com.github.kd_gaming1.packcore.config.export.ConfigExportService.PresetType;
import com.github.kd_gaming1.packcore.notification.ExportNotifications;
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
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

public class ExportConfigScreen extends BaseOwoScreen<FlowLayout> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String DEFAULT_VERSION = "1.0.0";
    private ScheduledExecutorService asyncExecutor;

    private ConfigExportService exportManager;
    private final Set<Path> selectedPaths = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> modsToInclude = new LinkedHashMap<>();
    private FileTreeNode rootNode;

    private FlowLayout treeContainer;
    private ScrollContainer<FlowLayout> treeScrollContainer;
    private FlowLayout contentPanel;
    private LabelComponent selectionInfoLabel;
    private ButtonComponent nextButton;

    private TextBoxComponent nameField;
    private TextAreaComponent descriptionArea;
    private TextBoxComponent versionField;
    private TextBoxComponent authorField;
    private ButtonComponent resolutionButton;
    private FlowLayout modsListContainer;

    private boolean showingMetadata = false;
    private String selectedResolution;
    private String currentResolution;

    private final Map<FileTreeNode, FlowLayout> nodeRowCache = new ConcurrentHashMap<>();
    private final Map<FileTreeNode, CheckboxComponent> nodeCheckboxCache = new ConcurrentHashMap<>();
    private volatile boolean isLoading = false;
    private FlowLayout loadingIndicator;
    private FlowLayout exportProgressDialog;
    private LabelComponent exportProgressLabel;

    private volatile boolean exportInBackground = false;
    private volatile String currentExportName = "";


    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        // Create a new executor for each screen instance
        asyncExecutor = Executors.newScheduledThreadPool(2);

        exportManager = new ConfigExportService();
        detectCurrentResolution();

        rootComponent.surface(TextureSurfaces.stretched(
                Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png"), 1920, 1082));
        rootComponent.padding(Insets.of(8));

        rootComponent.child(createHeader());
        rootComponent.child(createMainContent());

        // Load initial tree asynchronously with the new executor
        CompletableFuture.runAsync(() -> {
            rootNode = exportManager.buildFileTree();
            scanMods();
        }, asyncExecutor).thenRun(() -> MinecraftClient.getInstance().execute(() -> {
            if (treeContainer != null) {
                displayInitialTree();
            }
        }));
    }

    private FlowLayout createLoadingIndicator() {
        var loading = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        loading.child(Components.label(Text.literal("Loading..."))
                .color(UITheme.color(TEXT_SECONDARY)));
        return loading;
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
                        btn -> {
                            shutdownExecutor();
                            MinecraftClient.getInstance().setScreen(new ConfigManagerScreen());
                        })
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
        sidebar.surface(TextureSurfaces.stretched(
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
        contentPanel.surface(TextureSurfaces.stretched(
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
        treeScrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), treeContainer);
        treeScrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());

        contentPanel.child(treeScrollContainer);

        loadingIndicator = createLoadingIndicator();
        if (rootNode == null) {
            treeContainer.child(loadingIndicator);
        } else {
            displayInitialTree();
        }
    }

    private void displayInitialTree() {
        if (treeContainer == null || rootNode == null) return;

        treeContainer.clearChildren();
        nodeRowCache.clear();
        nodeCheckboxCache.clear();

        for (FileTreeNode child : rootNode.getChildren()) {
            addTreeNodeOptimized(child, 0);
        }
    }

    private void addTreeNodeOptimized(FileTreeNode node, int depth) {
        if (node.isHidden() || depth > 10) return;

        var nodeRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        nodeRow.gap(4);
        nodeRow.padding(Insets.left(depth * 16));
        nodeRow.verticalAlignment(VerticalAlignment.CENTER);

        // Expand/collapse button for directories
        if (node.isDirectory() && (!node.getChildren().isEmpty() || node.hasUnloadedChildren())) {
            nodeRow.child(Components.button(
                            Text.literal(node.isExpanded() ? "▼" : "▶"),
                            btn -> toggleNodeExpansion(node))
                    .renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ACCENT_GOLD, ENTRY_BORDER))
                    .sizing(Sizing.fixed(16), Sizing.fixed(16)));
        } else {
            var placeholder = Components.box(Sizing.fixed(16), Sizing.fixed(16));
            placeholder.fill(true);
            placeholder.color(Color.ofArgb(0x00000000));
            nodeRow.child(placeholder);
        }

        // Checkbox
        boolean isSelected = selectedPaths.contains(node.getPath());
        var checkbox = Components.checkbox(Text.empty())
                .checked(isSelected)
                .onChanged(checked -> toggleSelectionAsync(node, checked));
        nodeRow.child(checkbox);
        nodeCheckboxCache.put(node, checkbox);

        // Label
        String icon = node.isDirectory() ? "📁" : "📄";
        nodeRow.child(Components.label(Text.literal(icon + " " + node.getName()))
                .color(UITheme.color(isSelected ? ACCENT_GOLD : TEXT_WHITE)));

        nodeRowCache.put(node, nodeRow);
        treeContainer.child(nodeRow);

        // Add expanded children
        if (node.isDirectory() && node.isExpanded() && node.isChildrenLoaded()) {
            for (FileTreeNode child : node.getChildren()) {
                addTreeNodeOptimized(child, depth + 1);
            }
        }
    }

    private void toggleNodeExpansion(FileTreeNode node) {
        if (isLoading) return;

        if (!node.isExpanded() && !node.isChildrenLoaded()) {
            // Load children asynchronously
            isLoading = true;
            showLoadingForNode(node);

            CompletableFuture.runAsync(() -> exportManager.loadNodeChildren(node), asyncExecutor).thenRun(() -> { // Changed from ASYNC_EXECUTOR
                MinecraftClient.getInstance().execute(() -> {
                    node.setExpanded(true);
                    updateNodeExpansion(node);
                    isLoading = false;
                });
            });
        } else {
            node.setExpanded(!node.isExpanded());
            updateNodeExpansion(node);
        }
    }

    private void showLoadingForNode(FileTreeNode node) {
        FlowLayout nodeRow = nodeRowCache.get(node);
        if (nodeRow != null && !nodeRow.children().isEmpty()) {
            Component firstChild = nodeRow.children().getFirst();
            if (firstChild instanceof ButtonComponent btn) {
                btn.setMessage(Text.literal("⏳"));
            }
        }
    }

    private void updateNodeExpansion(FileTreeNode node) {
        // Find the node's row in the tree
        int nodeIndex = -1;
        for (int i = 0; i < treeContainer.children().size(); i++) {
            if (treeContainer.children().get(i) == nodeRowCache.get(node)) {
                nodeIndex = i;
                break;
            }
        }

        if (nodeIndex == -1) return;

        // Update expand button
        FlowLayout nodeRow = nodeRowCache.get(node);
        if (nodeRow != null && !nodeRow.children().isEmpty()) {
            Component firstChild = nodeRow.children().getFirst();
            if (firstChild instanceof ButtonComponent btn) {
                btn.setMessage(Text.literal(node.isExpanded() ? "▼" : "▶"));
            }
        }

        if (node.isExpanded()) {
            // Add children after this node
            int depth = calculateDepth(node);
            int insertIndex = nodeIndex + 1;
            for (FileTreeNode child : node.getChildren()) {
                if (!nodeRowCache.containsKey(child)) {
                    createNodeRow(child, depth + 1, insertIndex++);
                }
            }
        } else {
            // Remove children
            removeChildrenFromTree(node);
        }
    }

    private void createNodeRow(FileTreeNode node, int depth, int insertIndex) {
        if (node.isHidden()) return;

        var nodeRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        nodeRow.gap(4);
        nodeRow.padding(Insets.left(depth * 16));
        nodeRow.verticalAlignment(VerticalAlignment.CENTER);

        // Expand/collapse button
        if (node.isDirectory() && (!node.getChildren().isEmpty() || node.hasUnloadedChildren())) {
            nodeRow.child(Components.button(
                            Text.literal(node.isExpanded() ? "▼" : "▶"),
                            btn -> toggleNodeExpansion(node))
                    .renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ACCENT_GOLD, ENTRY_BORDER))
                    .sizing(Sizing.fixed(16), Sizing.fixed(16)));
        } else {
            var placeholder = Components.box(Sizing.fixed(16), Sizing.fixed(16));
            placeholder.fill(true);
            placeholder.color(Color.ofArgb(0x00000000));
            nodeRow.child(placeholder);
        }

        // Checkbox
        boolean isSelected = selectedPaths.contains(node.getPath());
        var checkbox = Components.checkbox(Text.empty())
                .checked(isSelected)
                .onChanged(checked -> toggleSelectionAsync(node, checked));
        nodeRow.child(checkbox);
        nodeCheckboxCache.put(node, checkbox);

        // Label
        String icon = node.isDirectory() ? "📁" : "📄";
        nodeRow.child(Components.label(Text.literal(icon + " " + node.getName()))
                .color(UITheme.color(isSelected ? ACCENT_GOLD : TEXT_WHITE)));

        nodeRowCache.put(node, nodeRow);

        // Insert at specific index
        List<Component> children = new ArrayList<>(treeContainer.children());
        children.add(Math.min(insertIndex, children.size()), nodeRow);
        treeContainer.clearChildren();
        children.forEach(treeContainer::child);

        // Recursively add expanded children
        if (node.isDirectory() && node.isExpanded() && node.isChildrenLoaded()) {
            int childIndex = insertIndex + 1;
            for (FileTreeNode child : node.getChildren()) {
                createNodeRow(child, depth + 1, childIndex++);
            }
        }
    }

    private void removeChildrenFromTree(FileTreeNode node) {
        for (FileTreeNode child : node.getChildren()) {
            FlowLayout childRow = nodeRowCache.remove(child);
            nodeCheckboxCache.remove(child);
            if (childRow != null) {
                treeContainer.removeChild(childRow);
            }
            if (child.isDirectory()) {
                removeChildrenFromTree(child);
            }
        }
    }

    private int calculateDepth(FileTreeNode node) {
        FlowLayout nodeRow = nodeRowCache.get(node);
        if (nodeRow == null) return 0;

        Insets padding = nodeRow.padding().get();
        return padding.left() / 16;
    }

    private void toggleSelectionAsync(FileTreeNode node, boolean selected) {
        CompletableFuture.runAsync(() -> {
            if (selected) {
                selectedPaths.add(node.getPath());
                if (node.isDirectory()) {
                    addDescendantsAsync(node);
                }
            } else {
                selectedPaths.remove(node.getPath());
                if (node.isDirectory()) {
                    removeDescendantsAsync(node);
                }
            }
        }, asyncExecutor).thenRun(() -> { // Changed from ASYNC_EXECUTOR
            MinecraftClient.getInstance().execute(() -> {
                updateNodeVisualsRecursive(node);
                updateSelectionInfo();
            });
        });
    }


    private void addDescendantsAsync(FileTreeNode node) {
        for (FileTreeNode child : node.getChildren()) {
            selectedPaths.add(child.getPath());
            if (child.isDirectory()) {
                addDescendantsAsync(child);
            }
        }
    }

    private void removeDescendantsAsync(FileTreeNode node) {
        for (FileTreeNode child : node.getChildren()) {
            selectedPaths.remove(child.getPath());
            if (child.isDirectory()) {
                removeDescendantsAsync(child);
            }
        }
    }

    private void updateNodeVisualsRecursive(FileTreeNode node) {
        // Update checkbox
        CheckboxComponent checkbox = nodeCheckboxCache.get(node);
        if (checkbox != null) {
            checkbox.checked(selectedPaths.contains(node.getPath()));
        }

        // Update children if expanded
        if (node.isDirectory() && node.isExpanded()) {
            for (FileTreeNode child : node.getChildren()) {
                updateNodeVisualsRecursive(child);
            }
        }
    }

    private void updateSelectionInfo() {
        CompletableFuture.supplyAsync(() -> {
            int count = selectedPaths.size();
            long size = exportManager.calculateSelectionSize(selectedPaths);
            return new SelectionInfo(count, size);
        }, asyncExecutor).thenAccept(info -> { // Changed from ASYNC_EXECUTOR
            MinecraftClient.getInstance().execute(() -> {
                String sizeText = formatSize(info.size);
                selectionInfoLabel.text(Text.literal(
                        info.count + " item" + (info.count != 1 ? "s" : "") + " selected\nSize: " + sizeText));
                updateNextButton();
            });
        });
    }

    private record SelectionInfo(int count, long size) {}

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
        CompletableFuture.supplyAsync(() -> {
            Set<Path> presetPaths = exportManager.getPresetPaths(preset);
            selectedPaths.clear();

            for (Path path : presetPaths) {
                FileTreeNode node = findNodeByPath(rootNode, path);
                if (node != null) {
                    selectedPaths.add(node.getPath());
                    if (node.isDirectory()) {
                        addDescendantsAsync(node);
                    }
                }
            }
            return presetPaths;
        }, asyncExecutor).thenAccept(paths -> { // Changed from ASYNC_EXECUTOR
            MinecraftClient.getInstance().execute(() -> {
                // Update all checkboxes
                nodeCheckboxCache.forEach((node, checkbox) -> checkbox.checked(selectedPaths.contains(node.getPath())));
                updateSelectionInfo();
            });
        });
    }

    private FileTreeNode findNodeByPath(FileTreeNode currentNode, Path targetPath) {
        if (currentNode.getPath().equals(targetPath)) {
            return currentNode;
        }

        for (FileTreeNode child : currentNode.getChildren()) {
            FileTreeNode result = findNodeByPath(child, targetPath);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private void scanMods() {
        List<String> mods = exportManager.scanInstalledMods();
        modsToInclude.clear();
        for (String mod : mods) {
            modsToInclude.put(mod, true);
        }
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

        nameField = Components.textBox(Sizing.fill(70), "");
        nameField.setPlaceholder(Text.literal("Enter configuration name"));
        formContainer.child(createFormRow("Name*:", nameField));

        descriptionArea = PlaceholderTextArea.create(
                Sizing.fill(70),
                Sizing.fixed(80),
                Text.literal("Describe what this configuration does...")
        );
        formContainer.child(createFormRow("Description:", descriptionArea));

        versionField = Components.textBox(Sizing.fixed(120), DEFAULT_VERSION);
        formContainer.child(createFormRow("Version:", versionField));

        authorField = Components.textBox(Sizing.fill(70),
                MinecraftClient.getInstance().getSession().getUsername());
        formContainer.child(createFormRow("Author:", authorField));

        populateResolutionDropdown();
        formContainer.child(createFormRow("Target Resolution:", resolutionButton));

        formContainer.child(Components.label(Text.literal("Installed mods when the configs was exported:"))
                .color(UITheme.color(TEXT_WHITE))
                .horizontalSizing(Sizing.fill(100)));

        var modsListWrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.fixed(125));
        modsListWrapper.surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
        modsListWrapper.padding(Insets.of(8));

        modsListContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .padding(Insets.bottom(8));

        var modsScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fixed(120), modsListContainer)
                .scrollbar(ScrollContainer.Scrollbar.vanilla());

        modsListWrapper.child(modsScroll);
        formContainer.child(modsListWrapper);

        populateModsList();

        var scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), formContainer);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        contentPanel.child(scrollContainer);

        var buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8);
        buttonRow.margins(Insets.top(6));
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonRow.child(Components.button(Text.literal("Back"), btn -> showFileTreeView())
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 60))
                .sizing(Sizing.fixed(90), Sizing.fixed(20)));

        buttonRow.child(Components.button(Text.literal("Export"), btn -> showExportWarningDialog())
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
                .sizing(Sizing.fixed(60), Sizing.content()));
        row.child(field);
        return row;
    }

    private void populateResolutionDropdown() {
        resolutionButton = (ButtonComponent) Components.button(
                        Text.literal(currentResolution),
                        this::openResolutionDropdown)
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 120, 60))
                .sizing(Sizing.fixed(120), Sizing.fixed(20));

        selectedResolution = currentResolution;
    }

    private void openResolutionDropdown(ButtonComponent button) {
        var commonResolutions = List.of(
                "1280×720", "1920×1080", "1920×1200",
                "2560×1440", "2560×1080", "3440×1440", "3840×2160",
                currentResolution
        );

        var uniqueResolutions = commonResolutions.stream()
                .distinct()
                .toList();

        DropdownComponent.openContextMenu(
                        this,
                        this.uiAdapter.rootComponent,
                        FlowLayout::child,
                        button.x(),
                        button.y() + button.height(),
                        dropdown -> {
                            for (String resolution : uniqueResolutions) {
                                dropdown.button(Text.literal(resolution), selectedDropdown -> {
                                    selectedResolution = resolution;
                                    currentResolution = resolution;
                                    resolutionButton.setMessage(Text.literal(resolution));
                                    selectedDropdown.parent().removeChild(selectedDropdown);
                                });
                            }

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

        buttonRow.child(Components.button(Text.literal("Cancel"), btn ->
                this.uiAdapter.rootComponent.removeChild(dialogContainer)).sizing(Sizing.fixed(60), Sizing.fixed(20)));

        buttonRow.child(Components.button(Text.literal("OK"), btn -> {
            String customRes = customResolutionField.getText().trim();
            if (customRes.matches("\\d+x\\d+")) {
                selectedResolution = customRes;
                currentResolution = customRes;
                resolutionButton.setMessage(Text.literal(customRes));
            }
            this.uiAdapter.rootComponent.removeChild(dialogContainer);
        }).sizing(Sizing.fixed(60), Sizing.fixed(20)));

        dialogContainer.child(buttonRow);
        this.uiAdapter.rootComponent.child(dialogContainer);
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

    private void showExportWarningDialog() {
        var dialogContainer = Containers.verticalFlow(Sizing.fixed(400), Sizing.content());
        dialogContainer.surface(Surface.flat(DARK_PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        dialogContainer.padding(Insets.of(16));
        dialogContainer.positioning(Positioning.absolute(
                (this.width - 400) / 2,
                (this.height - 200) / 2
        )).zIndex(10);

        dialogContainer.child(Components.label(Text.literal("Export Warning"))
                .color(UITheme.color(ACCENT_GOLD))
                .margins(Insets.bottom(8)));

        var warningText = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        warningText.gap(4);

        warningText.child(Components.label(Text.literal("⚠ Important Notice:"))
                .color(UITheme.color(TEXT_WHITE))
                .margins(Insets.bottom(4)));

        warningText.child(Components.label(Text.literal("• The export will run in the background"))
                .color(UITheme.color(TEXT_WHITE)));

        warningText.child(Components.label(Text.literal("• A progress indicator will show the status"))
                .color(UITheme.color(TEXT_WHITE)));

        warningText.child(Components.label(Text.literal("• You can continue using the interface"))
                .color(UITheme.color(TEXT_WHITE))
                .margins(Insets.bottom(8)));

        dialogContainer.child(warningText);

        var buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonRow.gap(8);
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        buttonRow.child(Components.button(Text.literal("Cancel"), btn ->
                this.uiAdapter.rootComponent.removeChild(dialogContainer)).sizing(Sizing.fixed(80), Sizing.fixed(20)));

        buttonRow.child(Components.button(Text.literal("Continue Export"), btn -> {
            this.uiAdapter.rootComponent.removeChild(dialogContainer);
            performAsyncExport();
        }).sizing(Sizing.fixed(120), Sizing.fixed(20)));

        dialogContainer.child(buttonRow);
        this.uiAdapter.rootComponent.child(dialogContainer);
    }

    private void showExportProgressDialog() {
        exportProgressDialog = Containers.verticalFlow(Sizing.fixed(350), Sizing.content());
        exportProgressDialog.surface(Surface.flat(DARK_PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        exportProgressDialog.padding(Insets.of(16));
        exportProgressDialog.positioning(Positioning.absolute(
                (this.width - 350) / 2,
                (this.height - 150) / 2
        )).zIndex(15);

        // Title
        exportProgressDialog.child(Components.label(Text.literal("Exporting Configuration"))
                .color(UITheme.color(ACCENT_GOLD))
                .margins(Insets.bottom(8)));

        // Progress label
        exportProgressLabel = (LabelComponent) Components.label(Text.literal("Preparing export..."))
                .color(UITheme.color(TEXT_WHITE))
                .margins(Insets.bottom(12));
        exportProgressDialog.child(exportProgressLabel);

        // Button row
        FlowLayout buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonRow.gap(8);
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        // Continue in background button
        ButtonComponent backgroundButton = (ButtonComponent) Components.button(
                Text.literal("Continue in Background"),
                btn -> {
                    exportInBackground = true;
                    closeExportProgressDialog();
                }
        ).horizontalSizing(Sizing.content());

        buttonRow.child(backgroundButton);
        exportProgressDialog.child(buttonRow);

        this.uiAdapter.rootComponent.child(exportProgressDialog);
    }

    private void updateExportProgress(String message) {
        MinecraftClient.getInstance().execute(() -> {
            if (exportProgressLabel != null && !exportInBackground) {
                exportProgressLabel.text(Text.literal(message));
            }
        });
    }

    private void closeExportProgressDialog() {
        MinecraftClient.getInstance().execute(() -> {
            if (exportProgressDialog != null) {
                this.uiAdapter.rootComponent.removeChild(exportProgressDialog);
                exportProgressDialog = null;
                exportProgressLabel = null;
            }
        });
    }

    private void performAsyncExport() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Configuration name is required!");
            return;
        }

        currentExportName = name;
        exportInBackground = false;
        showExportProgressDialog();

        List<String> includedMods = modsToInclude.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        ExportRequest request = new ExportRequest(
                new HashSet<>(selectedPaths),
                name,
                descriptionArea.getText().trim(),
                versionField.getText().trim(),
                authorField.getText().trim(),
                selectedResolution,
                includedMods
        );

        CompletableFuture.runAsync(() -> {
            try {
                updateExportProgress("Copying files...");
                Path exportedPath = exportManager.exportConfigAsync(request, this::updateExportProgress);

                MinecraftClient.getInstance().execute(() -> {
                    // Close progress dialog if still open
                    closeExportProgressDialog();

                    // Notify completion
                    ExportNotifications.notifyExportComplete(currentExportName, exportedPath);

                    // If still on the export screen, auto-open folder
                    if (MinecraftClient.getInstance().currentScreen == this) {
                        try {
                            Util.getOperatingSystem().open(exportedPath.getParent().toFile());
                        } catch (Exception e) {
                            LOGGER.warn("Failed to auto-open export folder", e);
                        }
                    }

                    // Return to main menu only if not in background
                    if (!exportInBackground) {
                        shutdownExecutor();
                        MinecraftClient.getInstance().setScreen(new ConfigManagerScreen());
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Failed to export configuration", e);
                MinecraftClient.getInstance().execute(() -> {
                    closeExportProgressDialog();
                    showError("Export failed: " + e.getMessage());
                });
            }
        }, asyncExecutor); // Changed from ASYNC_EXECUTOR
    }

    private void shutdownExecutor() {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void showError(String message) {
        LOGGER.error(message);
        // You can add a visual error dialog here if needed
    }

    @Override
    public void close() {
        shutdownExecutor();
        super.close();
    }

}