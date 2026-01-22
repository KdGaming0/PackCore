package com.github.kd_gaming1.packcore.ui.screen.configmanager;

import com.github.kd_gaming1.packcore.config.apply.FileDescriptionRegistry;
import com.github.kd_gaming1.packcore.config.apply.SelectiveConfigApplyService;
import com.github.kd_gaming1.packcore.config.apply.SelectiveConfigApplyService.SelectableFile;
import com.github.kd_gaming1.packcore.config.backup.BackupManager;
import com.github.kd_gaming1.packcore.config.backup.SelectiveBackupRestoreService;
import com.github.kd_gaming1.packcore.config.storage.ConfigFileRepository;
import com.github.kd_gaming1.packcore.ui.component.tree.FileTreeNode;
import com.github.kd_gaming1.packcore.ui.component.tree.FileTreeUIHelper;
import com.github.kd_gaming1.packcore.ui.screen.base.BasePackCoreScreen;
import com.github.kd_gaming1.packcore.ui.screen.components.ScreenUIComponents;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
//? if >= 1.21.10 {
import net.minecraft.text.StyleSpriteSource;
//?}
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Unified screen for selectively applying/restoring files.
 * Two-column layout consistent with ExportConfigScreen.
 */
public class SelectiveFileApplicationScreen extends BasePackCoreScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Mode enum
    public enum Mode {
        CONFIG_APPLY("Apply Configuration Files"),
        BACKUP_RESTORE("Restore Backup Files");

        private final String title;
        Mode(String title) { this.title = title; }
        public String getTitle() { return title; }
    }

    private final Mode mode;
    private final ConfigFileRepository.ConfigFile sourceConfig;
    private final BackupManager.BackupInfo sourceBackup;
    private final Set<String> selectedPaths = ConcurrentHashMap.newKeySet();

    // UI Components
    private FlowLayout sidebar;
    private FlowLayout mainContent;
    private FlowLayout fileTreeContainer;
    private FlowLayout detailsPanel;

    private ButtonComponent applyButton;
    private LabelComponent selectionInfoLabel;
    private ScrollContainer<FlowLayout> fileTreeScrollContainer;

    // File data
    private List<SelectableFile> allFiles = new ArrayList<>();
    private FileTreeNode rootNode;
    private final Map<FileTreeNode, FlowLayout> treeNodeRows = new HashMap<>();
    private Path virtualRoot; // For building tree structure

    /**
     * Constructor for config application mode
     */
    public SelectiveFileApplicationScreen(ConfigFileRepository.ConfigFile sourceConfig, Screen parent) {
        super(parent);
        this.mode = Mode.CONFIG_APPLY;
        this.sourceConfig = sourceConfig;
        this.sourceBackup = null;
    }

    /**
     * Constructor for backup restoration mode
     */
    public SelectiveFileApplicationScreen(BackupManager.BackupInfo sourceBackup, Screen parent) {
        super(parent);
        this.mode = Mode.BACKUP_RESTORE;
        this.sourceConfig = null;
        this.sourceBackup = sourceBackup;
    }

    @Override
    protected Component createTitleLabel() {
        String sourceName = mode == Mode.CONFIG_APPLY
                ? sourceConfig.getDisplayName()
                : sourceBackup.getDisplayName();

        return Components.label(
                Text.literal(mode.getTitle() + " - " + sourceName)
                        //? if <= 1.21.8 {
                        /*.styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))
                         *///?} else
                        .styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte"))))
        ).color(color(TEXT_PRIMARY));
    }

    @Override
    protected FlowLayout createMainContent() {
        FlowLayout mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand())
                .gap(8);

        mainContent.child(createSidebar().padding(Insets.of(20,16,14,14)));
        mainContent.child(createContentArea());

        // Load files
        loadFiles();

        return mainContent;
    }

    // ===== Sidebar (35%) =====

    private FlowLayout createSidebar() {
        sidebar = ScreenUIComponents.createSidebar(35);

        FlowLayout scrollContent = Containers.verticalFlow(Sizing.fill(96), Sizing.content())
                .gap(8);

        scrollContent.child(createInstructions());
        scrollContent.child(createQuickSelectButtons());
        scrollContent.child(createSelectionInfo());

        sidebar.child(ScreenUIComponents.createScrollContainer(scrollContent));

        // Apply button at bottom
        applyButton = ScreenUIComponents.createButton(
                mode == Mode.CONFIG_APPLY ? "Apply Selected" : "Restore Selected",
                btn -> showConfirmationDialog(),
                120, 20
        );
        applyButton.active(false);
        sidebar.child(applyButton).horizontalAlignment(HorizontalAlignment.CENTER);

        return sidebar;
    }

    private FlowLayout createInstructions() {
        FlowLayout section = ScreenUIComponents.createSection("Instructions", 0);

        String instructions = mode == Mode.CONFIG_APPLY
                ? "Select specific files from the configuration to apply. " +
                "Expand folders to see contents. Check items to select them."
                : "Choose which files to restore from this backup. " +
                "A safety backup will be created before restoration.";

        LabelComponent label = (LabelComponent) Components.label(Text.literal(instructions))
                .color(color(TEXT_PRIMARY))
                .horizontalSizing(Sizing.fill(95));

        section.child(label);
        return section;
    }

    private FlowLayout createQuickSelectButtons() {
        FlowLayout section = ScreenUIComponents.createSection("Quick Select", 0);
        section.horizontalAlignment(HorizontalAlignment.CENTER);

        FlowLayout row1 = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        row1.child(ScreenUIComponents.createButton("Select All",
                btn -> selectAll(), 90, 19));
        row1.child(ScreenUIComponents.createButton("Clear All",
                btn -> deselectAll(), 90, 19));

        section.child(row1);

        FlowLayout row2 = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        row2.child(ScreenUIComponents.createButton("Configs Only",
                btn -> selectByType(SelectableFile.FileType.MOD_CONFIG), 90, 19));
        row2.child(ScreenUIComponents.createButton("Game Options",
                btn -> selectByType(SelectableFile.FileType.GAME_OPTIONS), 90, 19));

        section.child(row2);

        return section;
    }

    private FlowLayout createSelectionInfo() {
        FlowLayout section = ScreenUIComponents.createSection("Selection", 0);
        section.horizontalAlignment(HorizontalAlignment.CENTER);

        selectionInfoLabel = Components.label(Text.literal("0 files selected\n0 B"))
                .color(color(TEXT_SECONDARY));
        section.child(selectionInfoLabel);

        return section;
    }

    // ===== Main Content Area (65%) =====

    private FlowLayout createContentArea() {
        mainContent = ScreenUIComponents.createInfoPanel(65);
        mainContent.verticalAlignment(VerticalAlignment.TOP);

        // File tree section
        mainContent.child(Components.label(Text.literal("File Structure")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(color(ACCENT_SECONDARY))
                .margins(Insets.of(6, 4,8,4)));

        fileTreeContainer = Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(2);

        mainContent.child(ScreenUIComponents.createScrollContainer(fileTreeContainer)
                .sizing(Sizing.fill(100), Sizing.expand(60)));

        // Details panel section
        mainContent.child(createDetailsSection());

        return mainContent;
    }

    private FlowLayout createDetailsSection() {
        FlowLayout section = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand(35))
                .gap(4)
                .surface(Surface.flat(ENTRY_BACKGROUND).and(Surface.outline(ACCENT_SECONDARY)))
                .padding(Insets.of(8))
                .margins(Insets.top(8));

        section.child(Components.label(Text.literal("File Details")
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(color(ACCENT_SECONDARY)));

        detailsPanel = Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(4);

        showEmptyDetails();

        section.child(ScreenUIComponents.createScrollContainer(detailsPanel)
                .sizing(Sizing.fill(100), Sizing.expand()));

        return section;
    }

    private void showEmptyDetails() {
        detailsPanel.clearChildren();
        detailsPanel.child(Components.label(Text.literal("Hover over a file to see details"))
                .color(color(TEXT_SECONDARY)));
    }

    private void showFileDetails(SelectableFile file) {
        detailsPanel.clearChildren();

        // Get enhanced description from registry
        var description = FileDescriptionRegistry.getDescription(file.path());
        String icon = description
                .map(FileDescriptionRegistry.FileDescription::icon)
                .orElse(FileDescriptionRegistry.getIcon(file.path()));

        // Header with icon
        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        header.child(Components.label(Text.literal(icon))
                .color(color(ACCENT_SECONDARY)));

        header.child(Components.label(Text.literal(file.displayName())
                        .setStyle(Style.EMPTY.withBold(true)))
                .color(color(ACCENT_SECONDARY))
                .horizontalSizing(Sizing.expand()));

        detailsPanel.child(header);

        // Info
        detailsPanel.child(ScreenUIComponents.createInfoRow("Type:", file.type().getDisplayName()));
        detailsPanel.child(ScreenUIComponents.createInfoRow("Size:",
                ScreenUIComponents.formatSize(file.size())));

        // Path (truncated if too long)
        String displayPath = file.path();
        if (displayPath.length() > 50) {
            displayPath = "..." + displayPath.substring(displayPath.length() - 47);
        }
        detailsPanel.child(ScreenUIComponents.createInfoRow("Path:", displayPath));

        // Enhanced description
        if (description.isPresent()) {
            var desc = description.get();
            detailsPanel.child(ScreenUIComponents.createInfoRow("Description:", desc.description()));

            if (desc.isImportant()) {
                detailsPanel.child(ScreenUIComponents.createWarningCard(
                        "Important File",
                        "This file contains critical settings that will significantly affect your experience."
                ));
            }
        } else if (file.description() != null && !file.description().isEmpty()) {
            detailsPanel.child(ScreenUIComponents.createInfoRow("Description:", file.description()));
        }
    }

    // ===== File Loading & Tree Building =====

    private void loadFiles() {
        fileTreeContainer.clearChildren();
        fileTreeContainer.child(ScreenUIComponents.createEmptyState("Loading files..."));

        CompletableFuture<List<SelectableFile>> loadFuture = mode == Mode.CONFIG_APPLY
                ? SelectiveConfigApplyService.scanConfigFiles(sourceConfig)
                : SelectiveBackupRestoreService.scanBackupFiles(sourceBackup);

        loadFuture.thenAccept(files -> MinecraftClient.getInstance().execute(() -> {
            this.allFiles = files;
            buildFileTree();
        })).exceptionally(throwable -> {
            MinecraftClient.getInstance().execute(() -> {
                LOGGER.error("Failed to load files", throwable);
                fileTreeContainer.clearChildren();
                fileTreeContainer.child(ScreenUIComponents.createErrorCard(
                        "Load Error",
                        "Failed to scan files: " + throwable.getMessage()
                ));
            });
            return null;
        });
    }

    private void buildFileTree() {
        fileTreeContainer.clearChildren();
        treeNodeRows.clear();

        if (allFiles.isEmpty()) {
            fileTreeContainer.child(ScreenUIComponents.createEmptyState("No files found"));
            return;
        }

        // Build tree structure from flat file list
        virtualRoot = java.nio.file.Paths.get("");
        rootNode = new FileTreeNode(virtualRoot, "root", true);
        rootNode.setExpanded(true);
        rootNode.setChildrenLoaded(true);

        // Create a set of all directory paths for quick lookup
        Set<String> directoryPaths = new HashSet<>();
        for (SelectableFile file : allFiles) {
            String path = file.path();
            String[] parts = path.split("[/\\\\]");

            // Add all parent directories
            StringBuilder currentPath = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (currentPath.length() > 0) {
                    currentPath.append("/");
                }
                currentPath.append(parts[i]);
                directoryPaths.add(currentPath.toString());
            }
        }

        // Build tree from files
        for (SelectableFile file : allFiles) {
            addFileToTree(file, directoryPaths);
        }

        // Render root's children
        for (FileTreeNode child : rootNode.getChildren()) {
            addTreeNodeToUI(child, 0);
        }

        updateSelectionInfo();
    }

    private void addFileToTree(SelectableFile file, Set<String> directoryPaths) {
        String[] pathParts = file.path().split("[/\\\\]");
        FileTreeNode currentNode = rootNode;

        for (int i = 0; i < pathParts.length; i++) {
            String part = pathParts[i];

            // Build the full path up to this point
            StringBuilder pathBuilder = new StringBuilder();
            for (int j = 0; j <= i; j++) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append("/");
                }
                pathBuilder.append(pathParts[j]);
            }
            String fullPath = pathBuilder.toString();

            // Determine if this is a directory
            boolean isDirectory = directoryPaths.contains(fullPath);

            FileTreeNode childNode = findChild(currentNode, part);
            if (childNode == null) {
                Path nodePath = currentNode.getPath().resolve(part);
                childNode = new FileTreeNode(nodePath, part, isDirectory);
                childNode.setChildrenLoaded(true);
                currentNode.addChild(childNode);
            }

            currentNode = childNode;
        }
    }

    private FileTreeNode findChild(FileTreeNode parent, String name) {
        for (FileTreeNode child : parent.getChildren()) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Adds a single tree node to the UI and recursively adds its children if expanded.
     * Used primarily during initial tree build.
     */
    private void addTreeNodeToUI(FileTreeNode node, int depth) {
        if (node.isHidden()) return;

        boolean isSelected = selectedPaths.contains(node.getPath().toString());

        FlowLayout nodeRow = FileTreeUIHelper.createTreeNodeRow(
                node,
                depth,
                isSelected,
                () -> toggleNodeExpansion(node),
                (n, checked) -> toggleSelection(n, checked),
                this::showNodeDetails
        );

        treeNodeRows.put(node, nodeRow);
        fileTreeContainer.child(nodeRow);

        if (node.isDirectory() && node.isExpanded()) {
            for (FileTreeNode child : node.getChildren()) {
                addTreeNodeToUI(child, depth + 1);
            }
        }
    }

    private void showNodeDetails(FileTreeNode node) {
        if (node.isDirectory()) {
            int fileCount = FileTreeUIHelper.countFiles(node);
            long size = FileTreeUIHelper.calculateSize(node);

            detailsPanel.clearChildren();
            detailsPanel.child(Components.label(Text.literal("📁 " + node.getName())
                            .setStyle(Style.EMPTY.withBold(true)))
                    .color(color(ACCENT_SECONDARY)));

            detailsPanel.child(ScreenUIComponents.createInfoRow("Type:", "Directory"));
            detailsPanel.child(ScreenUIComponents.createInfoRow("Files:", String.valueOf(fileCount)));
            detailsPanel.child(ScreenUIComponents.createInfoRow("Total Size:",
                    ScreenUIComponents.formatSize(size)));
        } else {
            String path = node.getPath().toString();
            allFiles.stream()
                    .filter(f -> f.path().equals(path))
                    .findFirst()
                    .ifPresent(this::showFileDetails);
        }
    }

    // ===== Tree Updates & Expansion Handling =====

    private void toggleNodeExpansion(FileTreeNode node) {
        node.setExpanded(!node.isExpanded());
        updateNodeExpansion(node);
    }

    private void updateNodeExpansion(FileTreeNode node) {
        FlowLayout nodeRow = treeNodeRows.get(node);
        if (nodeRow == null) return;

        // Find index of this node row in the container
        int nodeIndex = -1;
        var children = fileTreeContainer.children();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == nodeRow) {
                nodeIndex = i;
                break;
            }
        }

        if (nodeIndex == -1) return;

        updateExpandButton(nodeRow, node.isExpanded());

        if (node.isExpanded()) {
            int depth = calculateDepth(nodeRow);
            int insertIndex = nodeIndex + 1;
            for (FileTreeNode child : node.getChildren()) {
                // Use createNodeRow which returns number of added rows to correctly position siblings
                int rowsAdded = createNodeRow(child, depth + 1, insertIndex);
                insertIndex += rowsAdded;
            }
        } else {
            removeChildrenFromTree(node);
        }
    }

    private void updateExpandButton(FlowLayout row, boolean expanded) {
        if (!row.children().isEmpty() && row.children().get(0) instanceof ButtonComponent btn) {
            btn.setMessage(Text.literal(expanded ? "▼" : "▶"));
        }
    }

    private int calculateDepth(FlowLayout row) {
        Insets padding = row.padding().get();
        return padding.left() / 16;
    }

    private int createNodeRow(FileTreeNode node, int depth, int insertIndex) {
        if (node.isHidden()) return 0;

        boolean isSelected = selectedPaths.contains(node.getPath().toString());

        FlowLayout nodeRow = FileTreeUIHelper.createTreeNodeRow(
                node,
                depth,
                isSelected,
                () -> toggleNodeExpansion(node),
                this::toggleSelection,
                this::showNodeDetails
        );

        treeNodeRows.put(node, nodeRow);

        // Insert row at specific index
        List<Component> children = new ArrayList<>(fileTreeContainer.children());
        if (insertIndex <= children.size()) {
            children.add(insertIndex, nodeRow);
        } else {
            children.add(nodeRow);
        }

        fileTreeContainer.clearChildren();
        children.forEach(fileTreeContainer::child);

        int rowsAdded = 1;

        // Recursively add expanded children
        if (node.isDirectory() && node.isExpanded()) {
            int childIndex = insertIndex + 1;
            for (FileTreeNode child : node.getChildren()) {
                int childRows = createNodeRow(child, depth + 1, childIndex);
                childIndex += childRows;
                rowsAdded += childRows;
            }
        }

        return rowsAdded;
    }

    private void removeChildrenFromTree(FileTreeNode node) {
        for (FileTreeNode child : node.getChildren()) {
            FlowLayout childRow = treeNodeRows.remove(child);
            if (childRow != null) {
                fileTreeContainer.removeChild(childRow);
            }
            if (child.isDirectory()) {
                removeChildrenFromTree(child);
            }
        }
    }

    // ===== Selection Logic =====

    private void toggleSelection(FileTreeNode node, boolean selected) {
        FileTreeUIHelper.selectNodeAndChildren(node, selected, selectedPaths);
        updateSelectionInfo();
        updateNodeVisualsRecursive(node);
    }

    private void updateNodeVisualsRecursive(FileTreeNode node) {
        FlowLayout row = treeNodeRows.get(node);
        if (row != null) {
            boolean isSelected = selectedPaths.contains(node.getPath().toString());
            for (Component child : row.children()) {
                if (child instanceof CheckboxComponent checkbox) {
                    checkbox.checked(isSelected);
                } else if (child instanceof LabelComponent label) {
                    // Heuristic to update only the text label, not the icon
                    if (label.text().getString().equals(node.getName())) {
                        label.color(color(isSelected ? ACCENT_SECONDARY : TEXT_PRIMARY));
                    }
                }
            }
        }

        if (node.isDirectory()) {
            for (FileTreeNode child : node.getChildren()) {
                updateNodeVisualsRecursive(child);
            }
        }
    }

    private void updateTreeVisuals() {
        for (Map.Entry<FileTreeNode, FlowLayout> entry : treeNodeRows.entrySet()) {
            FileTreeNode node = entry.getKey();
            FlowLayout row = entry.getValue();
            boolean isSelected = selectedPaths.contains(node.getPath().toString());

            for (Component child : row.children()) {
                if (child instanceof CheckboxComponent checkbox) {
                    checkbox.checked(isSelected);
                } else if (child instanceof LabelComponent label) {
                    if (label.text().getString().equals(node.getName())) {
                        label.color(color(isSelected ? ACCENT_SECONDARY : TEXT_PRIMARY));
                    }
                }
            }
        }
        updateSelectionInfo();
    }

    private void selectAll() {
        for (SelectableFile file : allFiles) {
            selectedPaths.add(file.path());
        }
        updateTreeVisuals();
    }

    private void deselectAll() {
        selectedPaths.clear();
        updateTreeVisuals();
    }

    private void selectByType(SelectableFile.FileType type) {
        selectedPaths.clear(); // Clear logic first
        for (SelectableFile file : allFiles) {
            if (file.type() == type) {
                selectedPaths.add(file.path());
            }
        }
        updateTreeVisuals();
    }

    private void updateSelectionInfo() {
        int count = selectedPaths.size();
        long totalSize = allFiles.stream()
                .filter(f -> selectedPaths.contains(f.path()))
                .mapToLong(SelectableFile::size)
                .sum();

        String sizeText = ScreenUIComponents.formatSize(totalSize);
        selectionInfoLabel.text(Text.literal(
                count + " file" + (count != 1 ? "s" : "") + " selected\n" + sizeText));

        applyButton.active(count > 0);
    }

    // ===== Application/Restoration =====

    private void showConfirmationDialog() {
        String action = mode == Mode.CONFIG_APPLY ? "Apply" : "Restore";
        String sourceName;
        if (mode == Mode.CONFIG_APPLY) {
            assert sourceConfig != null;
            sourceName = sourceConfig.getDisplayName();
        } else {
            assert sourceBackup != null;
            sourceName = sourceBackup.getDisplayName();
        }

        FlowLayout dialog = ScreenUIComponents.createWarningDialog(
                action + " Selected Files?",
                null,
                500
        );

        dialog.child(Components.label(
                Text.literal("You have selected " + selectedPaths.size() + " file" +
                        (selectedPaths.size() != 1 ? "s" : "") + " to " + action.toLowerCase() + " from:")
        ).color(color(TEXT_PRIMARY)));

        dialog.child(Components.label(
                Text.literal(sourceName).setStyle(Style.EMPTY.withBold(true))
        ).color(color(ACCENT_SECONDARY)).margins(Insets.bottom(8)));

        dialog.child(ScreenUIComponents.createWarningCard(
                "⚠ Game Will Close",
                "The game will close and apply the selected files on next startup.  " +
                        "Configuration files cannot be hot-reloaded while the game is running."
        ));

        dialog.child(ScreenUIComponents.createWarningCard(
                "Auto-Backup",
                "An automatic backup will be created before applying changes.  " +
                        "You can restore it later if needed."
        ));

        dialog.child(ScreenUIComponents.createButtonRow(
                ScreenUIComponents.createButton(action + " & Close Game", btn -> {
                    closeTopOverlay();
                    performApplication();
                }, 140, 20),
                ScreenUIComponents.createButton("Cancel", btn -> closeTopOverlay(), 90, 20)
        ));

        showOverlay(dialog, false);
    }

    private void performApplication() {
        FlowLayout progressDialog = ScreenUIComponents.createDialog(
                "Scheduling Files",
                "Preparing to " + (mode == Mode.CONFIG_APPLY ? "apply" : "restore") + " files on next startup...",
                350
        );

        showOverlay(progressDialog, false);

        CompletableFuture<Boolean> applyFuture = mode == Mode.CONFIG_APPLY
                ? SelectiveConfigApplyService.scheduleSelectiveConfigApplication(sourceConfig, selectedPaths)
                : SelectiveBackupRestoreService.scheduleSelectiveRestore(sourceBackup, selectedPaths);

        applyFuture.thenAccept(success -> MinecraftClient.getInstance().execute(() -> {
            closeTopOverlay();
            if (success) {
                if (MinecraftClient.getInstance().player != null) {
                    String action = mode == Mode.CONFIG_APPLY ? "Applying" : "Restoring";
                    MinecraftClient.getInstance().player.sendMessage(
                            Text.literal(action + " " + selectedPaths.size() + " selected files - Restarting..."),
                            false
                    );
                }
            } else {
                showErrorDialog();
            }
        })).exceptionally(throwable -> {
            MinecraftClient.getInstance().execute(() -> {
                closeTopOverlay();
                LOGGER.error("Failed to schedule files", throwable);
                showErrorDialog();
            });
            return null;
        });
    }

    private void showErrorDialog() {
        String action = mode == Mode.CONFIG_APPLY ? "apply" : "restore";

        FlowLayout dialog = ScreenUIComponents.createDialog(
                "Error",
                "Failed to " + action + " selected files. Check logs for details.",
                350
        );
        dialog.surface(Surface.flat(DARK_PANEL_BACKGROUND).and(Surface.outline(ERROR_BORDER)));

        dialog.child(ScreenUIComponents.createButton("OK",
                        btn -> closeTopOverlay(),
                        80, 20)
                .horizontalSizing(Sizing.content()));

        showOverlay(dialog, false);
    }
}