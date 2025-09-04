package com.github.kd_gaming1.packcore.gui.configscreen;

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
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;

public class ConfigExportScreen extends BaseOwoScreen<FlowLayout> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Theme constants
    protected static final int OVERLAY_DARK = 0x80_000000;
    protected static final int PANEL_BACKGROUND = 0xC0_1A1A1A;
    protected static final int ACCENT_GOLD = 0xFF_FFD700;
    protected static final int TEXT_WHITE = 0xFFFFFF;
    protected static final int TEXT_SECONDARY = 0xB9BBBE;
    protected static final int STATUS_SUCCESS_BG = 0xC0_2D5016;
    protected static final int STATUS_SUCCESS_BORDER = 0xFF_52C41A;
    protected static final int STATUS_WARNING_BG = 0xC0_5C3317;
    protected static final int STATUS_WARNING_BORDER = 0xFF_FAAD14;
    protected static final int STATUS_ERROR_BG = 0xC0_5C1717;
    protected static final int STATUS_ERROR_BORDER = 0xFF_FF4D4F;
    protected static final int ENTRY_BACKGROUND = 0xC0_2A2A2A;
    protected static final int ENTRY_HOVER = 0xC0_3A3A3A;
    protected static final int ENTRY_SELECTED = 0xC0_4A4A4A;
    protected static final int ENTRY_BORDER = 0xFF_555555;

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
    private TextAreaComponent descriptionArea; // expanding area
    private TextBoxComponent versionField;
    private TextBoxComponent authorField;

    // Resolution dropdown
    private ButtonComponent resolutionButton;
    private List<String> resolutionPresets = new ArrayList<>(List.of("1920x1080", "1600x900", "1280x720"));
    private String selectedResolution = "1920x1080";
    private OverlayContainer<FlowLayout> resolutionOverlay = null;

    // Features / requirements
    private TextAreaComponent featuresArea;
    private TextAreaComponent requirementsArea;

    // Mods auto-detection & toggles
    private final Map<String, Boolean> modsSelected = new LinkedHashMap<>();
    private FlowLayout modsListContainer;

    public ConfigExportScreen() { }

    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        exportManager = new ConfigExportManager();

        int backgroundWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int backgroundHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        backgroundTexture = Identifier.of(MOD_ID, "textures/gui/wizard/test_temp.png");

        rootComponent.surface(Surface.tiled(backgroundTexture, backgroundWidth, backgroundHeight));
        rootComponent.padding(Insets.of(16));

        rootComponent.child(createHeader());

        FlowLayout contentArea = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        contentArea.gap(8);

        FlowLayout mainPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(75), Sizing.expand())
                .margins(Insets.right(1));

        treePanelContainer = createTreePanel();
        metadataPanel = createMetadataPanel();

        // start with tree view
        mainPanel.child(treePanelContainer);

        contentArea.child(createSidebar());
        contentArea.child(mainPanel);

        rootComponent.child(contentArea);

        // Build initial tree and scan mods
        rootNode = exportManager.buildFileTree();
        populateFileTree();
        scanAndPopulateMods();
    }

    private FlowLayout createHeader() {
        FlowLayout header = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        header.padding(Insets.of(8));
        header.verticalAlignment(VerticalAlignment.CENTER);

        Identifier logoId = Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png");
        TextureComponent logo = Components.texture(logoId, 0, 0, 48, 48, 48, 48);

        Text titleText = Text.literal("Export Configs - " + getModpackInfo().getName())
                .setStyle(Style.EMPTY.withBold(Boolean.TRUE));
        LabelComponent titleLabel = Components.label(titleText).color(Color.ofRgb(TEXT_WHITE));
        titleLabel.margins(Insets.of(8, 0, 0, 0));

        // Back button
        ButtonComponent backButton = (ButtonComponent) Components.button(Text.literal("← Back"), button -> {
                    MinecraftClient.getInstance().setScreen(new ModpackConfigMenuScreen());
                })
                .renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ACCENT_GOLD, ENTRY_BORDER))
                .sizing(Sizing.fixed(80), Sizing.fixed(25));

        header.child(logo);
        header.child(titleLabel);
        header.child(Containers.horizontalFlow(Sizing.expand(), Sizing.content())); // spacer
        header.child(backButton);
        header.margins(Insets.bottom(8));

        return header;
    }

    private FlowLayout createSidebar() {
        FlowLayout sidebar = Containers.verticalFlow(Sizing.fill(25), Sizing.expand());
        sidebar.gap(6);
        sidebar.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        sidebar.padding(Insets.of(8));

        FlowLayout infoSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        infoSection.gap(4);
        infoSection.surface(Surface.flat(0xC0_2A3A2A).and(Surface.outline(0xFF_4A7C59)));
        infoSection.padding(Insets.of(8));

        LabelComponent infoLabel = (LabelComponent) Components.label(Text.literal("Select folders and files from your game directory to export as a config. " +
                        "Choose what to include, then fill in the metadata details."))
                .color(Color.ofRgb(TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content());

        infoSection.child(infoLabel);
        sidebar.child(infoSection);

        // Preset buttons
        FlowLayout presetSection = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        presetSection.gap(4);
        presetSection.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
        presetSection.padding(Insets.of(8));

        LabelComponent presetLabel = Components.label(Text.literal("Quick Presets")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(ACCENT_GOLD));
        presetSection.child(presetLabel);

        ButtonComponent modConfigButton = (ButtonComponent) Components.button(Text.literal("Mod Configs Only"),
                        button -> applyPreset("mod_only"))
                .renderer(ButtonComponent.Renderer.flat(STATUS_WARNING_BG, STATUS_WARNING_BORDER, ENTRY_BORDER))
                .sizing(Sizing.fill(100), Sizing.fixed(22));

        ButtonComponent mcConfigButton = (ButtonComponent) Components.button(Text.literal("MC Configs Only"),
                        button -> applyPreset("mc_only"))
                .renderer(ButtonComponent.Renderer.flat(STATUS_WARNING_BG, STATUS_WARNING_BORDER, ENTRY_BORDER))
                .sizing(Sizing.fill(100), Sizing.fixed(22));

        ButtonComponent bothConfigButton = (ButtonComponent) Components.button(Text.literal("Both Configs"),
                        button -> applyPreset("both"))
                .renderer(ButtonComponent.Renderer.flat(STATUS_SUCCESS_BG, STATUS_SUCCESS_BORDER, ENTRY_BORDER))
                .sizing(Sizing.fill(100), Sizing.fixed(22));

        ButtonComponent clearButton = (ButtonComponent) Components.button(Text.literal("Clear All"),
                        button -> applyPreset("clear"))
                .renderer(ButtonComponent.Renderer.flat(STATUS_ERROR_BG, STATUS_ERROR_BORDER, ENTRY_BORDER))
                .sizing(Sizing.fill(100), Sizing.fixed(22));

        presetSection.child(modConfigButton);
        presetSection.child(mcConfigButton);
        presetSection.child(bothConfigButton);
        presetSection.child(clearButton);

        sidebar.child(presetSection);

        // Selection info
        FlowLayout selectionInfo = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        selectionInfo.gap(4);
        selectionInfo.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ENTRY_BORDER)));
        selectionInfo.padding(Insets.of(8));

        LabelComponent selectionLabel = Components.label(Text.literal("Selection Info")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(ACCENT_GOLD));
        selectionInfo.child(selectionLabel);

        selectionCountLabel = Components.label(Text.literal("Selected: 0 items"))
                .color(Color.ofRgb(TEXT_SECONDARY));
        selectionInfo.child(selectionCountLabel);

        selectionSizeLabel = Components.label(Text.literal("Estimated size: 0 MB"))
                .color(Color.ofRgb(TEXT_SECONDARY));
        selectionInfo.child(selectionSizeLabel);

        sidebar.child(selectionInfo);

        sidebar.child(Containers.verticalFlow(Sizing.fill(100), Sizing.expand()));

        nextButton = (ButtonComponent) Components.button(Text.literal("Next: Add Metadata →"),
                        button -> showMetadataPanel())
                .renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ENTRY_BORDER, ENTRY_BORDER))
                .sizing(Sizing.fill(100), Sizing.fixed(30));

        updateNextButton();
        sidebar.child(nextButton);

        return sidebar;
    }

    private FlowLayout createTreePanel() {
        FlowLayout treePanel = Containers.verticalFlow(Sizing.fill(100), Sizing.expand());
        treePanel.gap(4);
        treePanel.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        treePanel.padding(Insets.of(8));

        LabelComponent header = Components.label(Text.literal("Game Directory Structure")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(TEXT_WHITE));
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
        panel.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
        panel.padding(Insets.of(8));

        LabelComponent header = Components.label(Text.literal("Config Metadata")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(Color.ofRgb(TEXT_WHITE));
        panel.child(header);

        FlowLayout formContainer = Containers.verticalFlow(Sizing.fill(98), Sizing.content());
        formContainer.gap(8);

        // Name
        nameField = Components.textBox(Sizing.fill(70), "My Custom Config");
        formContainer.child(createFieldRow("Name*:", nameField));

        // Description - expanding text area
        descriptionArea = Components.textArea(Sizing.fill(70), Sizing.content(), "A custom configuration...");
        descriptionArea.maxLines(12); // grows up to 12 lines
        descriptionArea.displayCharCount(false);
        formContainer.child(createFieldRow("Description*:", descriptionArea));

        // Version / Author
        versionField = Components.textBox(Sizing.fill(70), "1.0.0");
        formContainer.child(createFieldRow("Version:", versionField));

        authorField = Components.textBox(Sizing.fill(70), MinecraftClient.getInstance().getSession().getUsername());
        formContainer.child(createFieldRow("Author:", authorField));

        // Resolution: button that opens overlay of presets + custom input
        resolutionButton = (ButtonComponent) Components.button(Text.literal(selectedResolution), b -> {
            showResolutionDropdown(b);
        }).sizing(Sizing.fill(70), Sizing.fixed(20));
        formContainer.child(createFieldRow("Target Resolution:", resolutionButton));

        // Features and Requirements (multi-line)
        featuresArea = Components.textArea(Sizing.fill(70), Sizing.fixed(60), "Better FPS\nCustom UI\nBalanced progression");
        featuresArea.maxLines(6);
        formContainer.child(createFieldRow("Features (one per line):", featuresArea));

        requirementsArea = Components.textArea(Sizing.fill(70), Sizing.fixed(60), "Minecraft 1.20.2\nFabric API");
        requirementsArea.maxLines(6);
        formContainer.child(createFieldRow("Requirements (one per line):", requirementsArea));

        // Mods (auto-detected)
        LabelComponent modsLabel = Components.label(Text.literal("Mods (auto-detected):"))
                .color(Color.ofRgb(TEXT_WHITE));
        FlowLayout modsRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        modsRow.gap(8);
        modsRow.child(modsLabel);

        modsListContainer = Containers.verticalFlow(Sizing.fill(70), Sizing.fixed(90));
        modsListContainer.gap(2);
        modsListContainer.surface(Surface.flat(0xC0_222222).and(Surface.outline(0xFF333333)));
        modsListContainer.padding(Insets.of(4));
        modsListContainer.child(Components.label(Text.literal("Scanning mods folder...")).color(Color.ofRgb(TEXT_SECONDARY)));

        formContainer.child(modsRow);
        formContainer.child(modsListContainer);

        formContainer.child(Components.label(Text.literal(" ")).sizing(Sizing.fill(100), Sizing.fixed(20)));

        ScrollContainer<FlowLayout> formScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), formContainer);
        formScroll.scrollbar(ScrollContainer.Scrollbar.vanilla());
        formScroll.scrollStep(10);

        panel.child(formScroll);

        // Buttons
        FlowLayout buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonRow.gap(8);
        buttonRow.horizontalAlignment(HorizontalAlignment.CENTER);

        ButtonComponent backToSelectionButton = (ButtonComponent) Components.button(Text.literal("← Back to Selection"),
                        button -> hideMetadataPanel())
                .renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ENTRY_BORDER, ENTRY_BORDER))
                .sizing(Sizing.fixed(140), Sizing.fixed(25));

        ButtonComponent exportButton = (ButtonComponent) Components.button(Text.literal("Export Config"),
                        button -> exportConfig())
                .renderer(ButtonComponent.Renderer.flat(STATUS_SUCCESS_BG, STATUS_SUCCESS_BORDER, ENTRY_BORDER))
                .sizing(Sizing.fixed(120), Sizing.fixed(25));

        buttonRow.child(backToSelectionButton);
        buttonRow.child(exportButton);
        panel.child(buttonRow);

        return panel;
    }

    private FlowLayout createFieldRow(String labelText, io.wispforest.owo.ui.core.Component field) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(8);
        row.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent label = Components.label(Text.literal(labelText)).color(Color.ofRgb(TEXT_WHITE));
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
                    .renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ACCENT_GOLD, ENTRY_BORDER))
                    .sizing(Sizing.fixed(16), Sizing.fixed(16));
            nodeEntry.child(expandButton);
        } else {
            nodeEntry.child(Components.label(Text.literal("  ")).sizing(Sizing.fixed(16), Sizing.fixed(16)));
        }

        boolean isSelected = selectedPaths.contains(node.getPath());
        String checkboxText = isSelected ? "☑" : "☐";
        int checkboxColor = isSelected ? STATUS_SUCCESS_BORDER : ENTRY_BORDER;
        ButtonComponent checkbox = (ButtonComponent) Components.button(Text.literal(checkboxText),
                        button -> toggleSelection(node))
                .renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, checkboxColor, ENTRY_BORDER))
                .sizing(Sizing.fixed(20), Sizing.fixed(16));
        nodeEntry.child(checkbox);

        String icon = node.isDirectory() ? "📁" : "📄";
        int nameColor = selectedPaths.contains(node.getPath()) ? ACCENT_GOLD : TEXT_WHITE;
        LabelComponent nameLabel = Components.label(Text.literal(icon + " " + node.getName()))
                .color(Color.ofRgb(nameColor));
        nodeEntry.child(nameLabel);

        nodeEntry.mouseEnter().subscribe(() -> {
            if (!selectedPaths.contains(node.getPath())) {
                nodeEntry.surface(Surface.flat(ENTRY_HOVER));
            }
        });

        nodeEntry.mouseLeave().subscribe(() -> {
            if (!selectedPaths.contains(node.getPath())) {
                nodeEntry.surface(Surface.BLANK);
            }
        });

        if (selectedPaths.contains(node.getPath())) {
            nodeEntry.surface(Surface.flat(ENTRY_SELECTED));
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
        boolean shouldEnable = hasSelection && !showingMetadata;

        if (shouldEnable) {
            nextButton.renderer(ButtonComponent.Renderer.flat(STATUS_SUCCESS_BG, STATUS_SUCCESS_BORDER, ENTRY_BORDER));
            nextButton.active = true;
        } else {
            nextButton.renderer(ButtonComponent.Renderer.flat(ENTRY_BACKGROUND, ENTRY_BORDER, ENTRY_BORDER));
            nextButton.active = false;
        }
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
        list.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
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
        // position overlay centered-ish; it's acceptable — you can tune Positioning if needed
        resolutionOverlay.positioning(Positioning.relative(50, 40));
        resolutionOverlay.zIndex(50);
        resolutionOverlay.child(list);
        this.uiAdapter.rootComponent.child(resolutionOverlay);
    }

    private void showAddCustomResolutionPopup() {
        FlowLayout popup = Containers.verticalFlow(Sizing.fill(60), Sizing.content());
        popup.gap(6);
        popup.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(ACCENT_GOLD)));
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
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();
            String version = versionField.getText().trim();
            String author = authorField.getText().trim();
            String resolution = selectedResolution;

            if (name.isEmpty() || description.isEmpty()) {
                showTemporaryMessage("Name and Description are required fields!", STATUS_ERROR_BG, STATUS_ERROR_BORDER);
                return;
            }

            List<String> features = Arrays.stream(featuresArea.getText().split("[\\r\\n,]+"))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

            List<String> requirements = Arrays.stream(requirementsArea.getText().split("[\\r\\n,]+"))
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

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        int backgroundWidth = client.getWindow().getScaledWidth();
        int backgroundHeight = client.getWindow().getScaledHeight();
        this.uiAdapter.rootComponent.surface(Surface.tiled(backgroundTexture, backgroundWidth, backgroundHeight));
    }
}