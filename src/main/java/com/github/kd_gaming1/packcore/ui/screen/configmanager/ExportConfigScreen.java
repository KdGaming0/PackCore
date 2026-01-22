package com.github.kd_gaming1.packcore.ui.screen.configmanager;

import com.github.kd_gaming1.packcore.config.export.ConfigExportService;
import com.github.kd_gaming1.packcore.config.export.ConfigExportService.ExportRequest;
import com.github.kd_gaming1.packcore.config.export.ConfigExportService.PresetType;
import com.github.kd_gaming1.packcore.notification.ExportNotifications;
import com.github.kd_gaming1.packcore.ui.component.PlaceholderTextArea;
import com.github.kd_gaming1.packcore.ui.component.tree.FileTreeNode;
import com.github.kd_gaming1.packcore.ui.screen.base.BasePackCoreScreen;
import com.github.kd_gaming1.packcore.ui.screen.components.ScreenUIComponents;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
//? if >= 1.21.10 {
import net.minecraft.text.StyleSpriteSource;
//?}
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
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

/**
 * Export configuration screen - refactored for improved code clarity.
 * Handles file selection, metadata input, and async export operations.
 */
public class ExportConfigScreen extends BasePackCoreScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String DEFAULT_VERSION = "1.0.0";

    private final ScheduledExecutorService asyncExecutor;
    private final ConfigExportService exportManager;

    // State
    private final Set<Path> selectedPaths = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> modsToInclude = new LinkedHashMap<>();
    private FileTreeNode rootNode;

    // UI Components
    private FlowLayout treeContainer;
    private FlowLayout contentPanel;
    private LabelComponent selectionInfoLabel;
    private ButtonComponent nextButton;

    // Metadata form
    private TextBoxComponent nameField;
    private TextAreaComponent descriptionArea;
    private TextBoxComponent versionField;
    private TextBoxComponent authorField;
    private ButtonComponent resolutionButton;
    private FlowLayout modsListContainer;

    private boolean showingMetadata = false;
    private String selectedResolution;
    private String currentResolution;

    // Tree caching
    private final Map<FileTreeNode, FlowLayout> nodeRowCache = new ConcurrentHashMap<>();
    private final Map<FileTreeNode, CheckboxComponent> nodeCheckboxCache = new ConcurrentHashMap<>();
    private volatile boolean isLoading = false;

    // Progress tracking
    private FlowLayout exportProgressDialog;
    private LabelComponent exportProgressLabel;
    private volatile boolean exportInBackground = false;
    private volatile String currentExportName = "";

    public ExportConfigScreen() {
        super(new ConfigManagerScreen());

        asyncExecutor = Executors.newScheduledThreadPool(2);
        exportManager = new ConfigExportService();
        detectCurrentResolution();

        // Load initial tree asynchronously
        CompletableFuture.runAsync(() -> {
            rootNode = exportManager.buildFileTree();
            scanMods();
        }, asyncExecutor).thenRun(() -> MinecraftClient.getInstance().execute(() -> {
            if (treeContainer != null) {
                displayInitialTree();
            }
        }));
    }

    @Override
    protected Component createTitleLabel() {
        return Components.label(
                Text.literal("Export Configuration - " + getModpackInfo().getName())
                        //? if <= 1.21.8 {
                        /*.styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))
                         *///?} else
                        .styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte"))))
        ).color(color(TEXT_PRIMARY));
    }

    @Override
    protected FlowLayout createMainContent() {
        FlowLayout mainContent = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.expand())
                .gap(8);

        mainContent.child(createSidebar());
        mainContent.child(createContentArea());

        return mainContent;
    }

    // ===== Sidebar =====

    private FlowLayout createSidebar() {
        FlowLayout sidebar = ScreenUIComponents.createSidebar(35);

        FlowLayout scrollContent = Containers.verticalFlow(Sizing.fill(96), Sizing.content())
                .gap(8);

        scrollContent.child(createInfoSection());
        scrollContent.child(createPresetSection());
        scrollContent.child(createSelectionInfo());

        sidebar.child(ScreenUIComponents.createScrollContainer(scrollContent));

        // Next button
        nextButton = ScreenUIComponents.createButton("Next: Add Details",
                btn -> showMetadataView(), 120, 20);
        nextButton.active(false);

        sidebar.child(nextButton).horizontalAlignment(HorizontalAlignment.CENTER);

        return sidebar;
    }

    private Component createInfoSection() {
        FlowLayout section = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .padding(Insets.of(8));

        section.child(Components.label(
                Text.literal("Select files and folders to include in your configuration export.")
        ).color(color(TEXT_PRIMARY)).horizontalSizing(Sizing.fill(100)));

        return section;
    }

    private FlowLayout createPresetSection() {
        FlowLayout section = ScreenUIComponents.createSection("Quick Presets", 0);
        section.horizontalAlignment(HorizontalAlignment.CENTER);

        for (PresetType preset : PresetType.values()) {
            section.child(ScreenUIComponents.createButton(preset.getDisplayName(),
                    btn -> applyPreset(preset), 90, 19));
        }

        return section;
    }

    private FlowLayout createSelectionInfo() {
        FlowLayout section = ScreenUIComponents.createSection("Selection Info", 0);
        section.horizontalAlignment(HorizontalAlignment.CENTER);

        selectionInfoLabel = Components.label(Text.literal("0 items selected\nSize: 0 KB"))
                .color(color(TEXT_SECONDARY));
        section.child(selectionInfoLabel);

        return section;
    }

    // ===== Content Area =====

    private FlowLayout createContentArea() {
        contentPanel = ScreenUIComponents.createInfoPanel(65);
        showFileTreeView();
        return contentPanel;
    }

    private void showFileTreeView() {
        contentPanel.clearChildren();
        showingMetadata = false;
        updateNextButton();

        contentPanel.child(Components.label(Text.literal("Select Files to Export")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(color(ACCENT_SECONDARY)));

        treeContainer = Containers.verticalFlow(Sizing.fill(98), Sizing.content());

        ScrollContainer<FlowLayout> treeScrollContainer = ScreenUIComponents.createScrollContainer(treeContainer);
        contentPanel.child(treeScrollContainer);

        if (rootNode == null) {
            treeContainer.child(ScreenUIComponents.createEmptyState("Loading files..."));
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

        FlowLayout nodeRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .padding(Insets.left(depth * 16))
                .verticalAlignment(VerticalAlignment.CENTER);

        // Expand/collapse button for directories
        if (node.isDirectory() && (!node.getChildren().isEmpty() || node.hasUnloadedChildren())) {
            nodeRow.child(Components.button(
                            Text.literal(node.isExpanded() ? "▼" : "▶"),
                            btn -> toggleNodeExpansion(node)
                    ).renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ACCENT_SECONDARY, ENTRY_BORDER))
                    .sizing(Sizing.fixed(16), Sizing.fixed(16)));
        } else {
            BoxComponent placeholder = Components.box(Sizing.fixed(16), Sizing.fixed(16));
            placeholder.fill(true);
            placeholder.color(Color.ofArgb(0x00000000));
            nodeRow.child(placeholder);
        }

        // Checkbox
        boolean isSelected = selectedPaths.contains(node.getPath());
        CheckboxComponent checkbox = Components.checkbox(Text.empty())
                .checked(isSelected)
                .onChanged(checked -> toggleSelectionAsync(node, checked));
        nodeRow.child(checkbox);
        nodeCheckboxCache.put(node, checkbox);

        // Label
        String icon = node.isDirectory() ? "📁" : "📄";
        nodeRow.child(Components.label(Text.literal(icon + " " + node.getName()))
                .color(color(isSelected ? ACCENT_SECONDARY : TEXT_PRIMARY)));

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
            isLoading = true;
            showLoadingForNode(node);

            CompletableFuture.runAsync(() -> exportManager.loadNodeChildren(node), asyncExecutor)
                    .thenRun(() -> MinecraftClient.getInstance().execute(() -> {
                        node.setExpanded(true);
                        updateNodeExpansion(node);
                        isLoading = false;
                    }));
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
        int nodeIndex = -1;
        for (int i = 0; i < treeContainer.children().size(); i++) {
            if (treeContainer.children().get(i) == nodeRowCache.get(node)) {
                nodeIndex = i;
                break;
            }
        }

        if (nodeIndex == -1) return;

        FlowLayout nodeRow = nodeRowCache.get(node);
        if (nodeRow != null && !nodeRow.children().isEmpty()) {
            Component firstChild = nodeRow.children().getFirst();
            if (firstChild instanceof ButtonComponent btn) {
                btn.setMessage(Text.literal(node.isExpanded() ? "▼" : "▶"));
            }
        }

        if (node.isExpanded()) {
            boolean parentSelected = selectedPaths.contains(node.getPath());

            int depth = calculateDepth(node);
            int insertIndex = nodeIndex + 1;
            for (FileTreeNode child : node.getChildren()) {
                if (parentSelected) {
                    selectedPaths.add(child.getPath());
                    if (child.isDirectory()) {
                        addDescendantsAsync(child);
                    }
                }

                if (!nodeRowCache.containsKey(child)) {
                    createNodeRow(child, depth + 1, insertIndex++);
                }
            }

            if (parentSelected) {
                updateSelectionInfo();
            }
        } else {
            removeChildrenFromTree(node);
        }
    }

    private void createNodeRow(FileTreeNode node, int depth, int insertIndex) {
        if (node.isHidden()) return;

        FlowLayout nodeRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .padding(Insets.left(depth * 16))
                .verticalAlignment(VerticalAlignment.CENTER);

        // Expand/collapse button
        if (node.isDirectory() && (!node.getChildren().isEmpty() || node.hasUnloadedChildren())) {
            nodeRow.child(Components.button(
                            Text.literal(node.isExpanded() ? "▼" : "▶"),
                            btn -> toggleNodeExpansion(node)
                    ).renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ACCENT_SECONDARY, ENTRY_BORDER))
                    .sizing(Sizing.fixed(16), Sizing.fixed(16)));
        } else {
            BoxComponent placeholder = Components.box(Sizing.fixed(16), Sizing.fixed(16));
            placeholder.fill(true);
            placeholder.color(Color.ofArgb(0x00000000));
            nodeRow.child(placeholder);
        }

        // Checkbox
        boolean isSelected = selectedPaths.contains(node.getPath());
        CheckboxComponent checkbox = Components.checkbox(Text.empty())
                .checked(isSelected)
                .onChanged(checked -> toggleSelectionAsync(node, checked));
        nodeRow.child(checkbox);
        nodeCheckboxCache.put(node, checkbox);

        // Label
        String icon = node.isDirectory() ? "📁" : "📄";
        nodeRow.child(Components.label(Text.literal(icon + " " + node.getName()))
                .color(color(isSelected ? ACCENT_SECONDARY : TEXT_PRIMARY)));

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
                    if (!node.isChildrenLoaded()) {
                        exportManager.loadNodeChildren(node);
                    }
                    addDescendantsAsync(node);
                }
            } else {
                selectedPaths.remove(node.getPath());
                if (node.isDirectory()) {
                    if (!node.isChildrenLoaded()) {
                        exportManager.loadNodeChildren(node);
                    }
                    removeDescendantsAsync(node);
                }
            }
        }, asyncExecutor).thenRun(() -> MinecraftClient.getInstance().execute(() -> {
            updateNodeVisualsRecursive(node);
            updateSelectionInfo();
        }));
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
        CheckboxComponent checkbox = nodeCheckboxCache.get(node);
        if (checkbox != null) {
            checkbox.checked(selectedPaths.contains(node.getPath()));
        }

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
        }, asyncExecutor).thenAccept(info -> MinecraftClient.getInstance().execute(() -> {
            String sizeText = ScreenUIComponents.formatSize(info.size);
            selectionInfoLabel.text(Text.literal(
                    info.count + " item" + (info.count != 1 ? "s" : "") + " selected\nSize: " + sizeText));
            updateNextButton();
        }));
    }

    private record SelectionInfo(int count, long size) {}

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
        }, asyncExecutor).thenAccept(paths -> MinecraftClient.getInstance().execute(() -> {
            nodeCheckboxCache.forEach((node, checkbox) ->
                    checkbox.checked(selectedPaths.contains(node.getPath())));
            updateSelectionInfo();
        }));
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

    // ===== Metadata View =====

    private void showMetadataView() {
        contentPanel.clearChildren();
        showingMetadata = true;
        updateNextButton();

        contentPanel.child(Components.label(Text.literal("Configuration Details")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(color(ACCENT_SECONDARY)));

        FlowLayout formContainer = Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(8);

        // Name field
        nameField = Components.textBox(Sizing.fill(70), "");
        nameField.setPlaceholder(Text.literal("Enter configuration name"));
        formContainer.child(createFormRow("Name*:", nameField));

        // Description field
        descriptionArea = PlaceholderTextArea.create(
                Sizing.fill(70),
                Sizing.fixed(80),
                Text.literal("Describe what this configuration does...")
        );
        formContainer.child(createFormRow("Description:", descriptionArea));

        // Version field
        versionField = Components.textBox(Sizing.fixed(120), DEFAULT_VERSION);
        formContainer.child(createFormRow("Version:", versionField));

        // Author field
        authorField = Components.textBox(Sizing.fill(70),
                MinecraftClient.getInstance().getSession().getUsername());
        formContainer.child(createFormRow("Author:", authorField));

        // Resolution dropdown
        populateResolutionDropdown();
        formContainer.child(createFormRow("Target Resolution:", resolutionButton));

        // Mods list
        formContainer.child(Components.label(Text.literal("Installed mods when the configs was exported:"))
                .color(color(TEXT_PRIMARY))
                .horizontalSizing(Sizing.fill(100)));

        FlowLayout modsListWrapper = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fixed(125))
                .surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ENTRY_BORDER)))
                .padding(Insets.of(8));

        modsListContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .padding(Insets.bottom(8));

        modsListWrapper.child(ScreenUIComponents.createScrollContainer(modsListContainer)
                .sizing(Sizing.fill(100), Sizing.fixed(120)));

        formContainer.child(modsListWrapper);
        populateModsList();

        contentPanel.child(ScreenUIComponents.createScrollContainer(formContainer));

        // Action buttons
        FlowLayout buttonRow = ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Back", btn -> showFileTreeView(), 90, 20),
                ScreenUIComponents.createButton("Export", btn -> showExportWarningDialog())
        );
        buttonRow.margins(Insets.top(6));
        contentPanel.child(buttonRow);
    }

    private FlowLayout createFormRow(String label, Component field) {
        FlowLayout row = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        row.child(Components.label(Text.literal(label))
                .color(color(TEXT_PRIMARY))
                .sizing(Sizing.fixed(60), Sizing.content()));

        row.child(field);

        return row;
    }

    private void populateResolutionDropdown() {
        resolutionButton = ScreenUIComponents.createButton(currentResolution,
                btn -> openResolutionDropdown(), 120, 20);
        selectedResolution = currentResolution;
    }

    private void openResolutionDropdown() {
        List<String> commonResolutions = List.of(
                "1280×720", "1920×1080", "1920×1200",
                "2560×1440", "2560×1080", "3440×1440", "3840×2160",
                currentResolution
        );

        List<String> uniqueResolutions = commonResolutions.stream()
                .distinct()
                .toList();

        DropdownComponent.openContextMenu(
                this,
                this.uiAdapter.rootComponent,
                FlowLayout::child,
                resolutionButton.x(),
                resolutionButton.y() + resolutionButton.height(),
                dropdown -> {
                    for (String resolution : uniqueResolutions) {
                        dropdown.button(Text.literal(resolution), selectedDropdown -> {
                            selectedResolution = resolution;
                            currentResolution = resolution;
                            resolutionButton.setMessage(Text.literal(resolution));
                            assert selectedDropdown.parent() != null;
                            selectedDropdown.parent().removeChild(selectedDropdown);
                        });
                    }

                    dropdown.divider();
                    dropdown.button(Text.literal("Custom..."), selectedDropdown -> {
                        openCustomResolutionDialog();
                        assert selectedDropdown.parent() != null;
                        selectedDropdown.parent().removeChild(selectedDropdown);
                    });
                }
        //? if <=1.21.8 {
        /*).zIndex(8);
        *///?} else
        );
    }
    private void openCustomResolutionDialog() {
        FlowLayout dialog = ScreenUIComponents.createDialog(
                "Enter Custom Resolution",
                null,
                300
        );

        TextBoxComponent customResolutionField = Components.textBox(Sizing.fixed(200), "1920x1080");
        customResolutionField.setPlaceholder(Text.literal("Width x Height (e.g. 1920x1080)"));
        dialog.child(customResolutionField);

        dialog.child(ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay(), 60, 20),
                ScreenUIComponents.createButton("OK", btn -> {
                    String customRes = customResolutionField.getText().trim();
                    if (customRes.matches("\\d+x\\d+")) {
                        selectedResolution = customRes;
                        currentResolution = customRes;
                        resolutionButton.setMessage(Text.literal(customRes));
                    }
                    closeTopOverlay();
                }, 60, 20)
        ));

        showOverlay(dialog, false);
    }

    private void populateModsList() {
        modsListContainer.clearChildren();

        if (modsToInclude.isEmpty()) {
            modsListContainer.child(Components.label(Text.literal("No mods found"))
                    .color(color(TEXT_SECONDARY)));
            return;
        }

        for (Map.Entry<String, Boolean> entry : modsToInclude.entrySet()) {
            CheckboxComponent checkbox = Components.checkbox(Text.literal(entry.getKey()))
                    .checked(entry.getValue())
                    .onChanged(checked -> modsToInclude.put(entry.getKey(), checked));
            modsListContainer.child(checkbox);
        }
    }

    private void detectCurrentResolution() {
        MinecraftClient mc = MinecraftClient.getInstance();
        currentResolution = mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight();
    }

    // ===== Export Operations =====

    private void showExportWarningDialog() {
        FlowLayout dialog = ScreenUIComponents.createWarningDialog(
                "Export Warning",
                null,
                400
        );

        FlowLayout warningText = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        warningText.child(Components.label(Text.literal("⚠ Important Notice:"))
                .color(color(TEXT_PRIMARY))
                .margins(Insets.bottom(4)));

        warningText.child(Components.label(Text.literal("• The export will run in the background"))
                .color(color(TEXT_PRIMARY)));

        warningText.child(Components.label(Text.literal("• A progress indicator will show the status"))
                .color(color(TEXT_PRIMARY)));

        warningText.child(Components.label(Text.literal("• You can continue using the interface"))
                .color(color(TEXT_PRIMARY))
                .margins(Insets.bottom(8)));

        dialog.child(warningText);

        dialog.child(ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay(), 80, 20),
                ScreenUIComponents.createButton("Continue Export", btn -> {
                    closeTopOverlay();
                    performAsyncExport();
                }, 120, 20)
        ));

        showOverlay(dialog, false);
    }

    private void performAsyncExport() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showErrorDialog("Configuration name is required!");
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
                    closeExportProgressDialog();

                    ExportNotifications.notifyExportComplete(currentExportName, exportedPath);

                    if (MinecraftClient.getInstance().currentScreen == this) {
                        try {
                            Util.getOperatingSystem().open(exportedPath.getParent().toFile());
                        } catch (Exception e) {
                            LOGGER.warn("Failed to auto-open export folder", e);
                        }
                    }

                    if (!exportInBackground) {
                        shutdownExecutor();
                        MinecraftClient.getInstance().setScreen(new ConfigManagerScreen());
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Failed to export configuration", e);
                MinecraftClient.getInstance().execute(() -> {
                    closeExportProgressDialog();
                    showErrorDialog("Export failed: " + e.getMessage());
                });
            }
        }, asyncExecutor);
    }

    private void showExportProgressDialog() {
        exportProgressDialog = ScreenUIComponents.createDialog("Exporting Configuration", null, 350);
        exportProgressDialog.positioning(Positioning.absolute(
                (this.width - 350) / 2,
                (this.height - 150) / 2
        ));
        //? if <=1.21.8 {
        /*exportProgressDialog.zIndex(15);
         *///?}

        exportProgressLabel = (LabelComponent) Components.label(Text.literal("Preparing export..."))
                .color(color(TEXT_PRIMARY))
                .margins(Insets.bottom(12));
        exportProgressDialog.child(exportProgressLabel);

        FlowLayout buttonRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        ButtonComponent backgroundButton = (ButtonComponent) Components.button(
                Text.literal("Continue in Background"),
                btn -> {
                    exportInBackground = true;
                    closeExportProgressDialog();
                }
        ).horizontalSizing(Sizing.content());

        buttonRow.child(backgroundButton);
        exportProgressDialog.child(buttonRow);

        rootComponent.child(exportProgressDialog);
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
                rootComponent.removeChild(exportProgressDialog);
                exportProgressDialog = null;
                exportProgressLabel = null;
            }
        });
    }

    private void showErrorDialog(String message) {
        FlowLayout dialog = ScreenUIComponents.createDialog("Error", message, 350);
        dialog.surface(Surface.flat(DARK_PANEL_BACKGROUND).and(Surface.outline(ERROR_BORDER)));
        dialog.positioning(Positioning.absolute(
                (this.width - 350) / 2,
                (this.height - 120) / 2
        ));
        //? if <=1.21.8 {
        /*dialog.zIndex(20);
         *///?}

        dialog.child(ScreenUIComponents.createButton("OK",
                        btn -> rootComponent.removeChild(dialog), 80, 20)
                .horizontalSizing(Sizing.content()));

        rootComponent.child(dialog);
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

    @Override
    public void close() {
        shutdownExecutor();
        super.close();
    }
}