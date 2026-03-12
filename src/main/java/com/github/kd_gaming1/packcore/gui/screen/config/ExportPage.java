package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.daqem.uilib.gui.widget.EditBoxWidget;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.daqem.uilib.util.ValidationErrors;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.configpack.ConfigPackBuilder;
import com.github.kd_gaming1.packcore.configpack.ConfigPackMeta;
import com.github.kd_gaming1.packcore.gui.component.FileTreeBuilder;
import com.github.kd_gaming1.packcore.gui.component.FileTreeComponent;
import com.github.kd_gaming1.packcore.gui.component.FileTreeNode;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import com.github.kd_gaming1.packcore.util.ScreenResolution;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class ExportPage extends BaseConfigPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ExportPage");

    private static final Path EXPORTS_DIR = PackCore.PACKCORE_DIR.resolve("user_configs");

    private static final int PANEL_GAP = 12;
    private static final int PADDING = 10;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_GAP = 6;
    private static final int LABEL_GAP = 3;
    private static final int BUTTON_WIDTH = 120;
    private static final int OPEN_BTN_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 18;

    private static final int COLOR_LABEL = 0xFFCCCCCC;
    private static final int COLOR_SECTION = 0xFF888888;

    private static final Set<String> HIDDEN_PATHS = Set.of(
            "logs", "crash-reports", "screenshots", "saves", "packcore",
            "replay_recordings", "debug", ".fabric", "resourcepacks", "shaderpacks"
    );

    private EditBoxWidget nameField;
    private EditBoxWidget versionField;
    private EditBoxWidget authorField;
    private EditBoxWidget descriptionField;
    private EditBoxWidget resolutionField;
    private FileTreeComponent fileTree;
    private CustomButtonWidget exportBtn;

    public ExportPage(int width, int height) {
        super(width, height);
    }

    @Override
    public void onEnter() {
        this.clearComponents();

        int panelWidth = (getWidth() - PANEL_GAP) / 2;

        EmptyComponent leftPanel = new EmptyComponent(0, 0, panelWidth, getHeight());
        EmptyComponent rightPanel = new EmptyComponent(panelWidth + PANEL_GAP, 0, panelWidth, getHeight());

        buildLeftPanel(leftPanel, panelWidth);
        buildRightPanel(rightPanel, panelWidth);

        this.addComponent(leftPanel);
        this.addComponent(rightPanel);
    }

    private void buildLeftPanel(EmptyComponent panel, int width) {
        var font = Minecraft.getInstance().font;
        int fieldWidth = width - PADDING * 2;
        int fieldStride = font.lineHeight + LABEL_GAP + FIELD_HEIGHT + FIELD_GAP;

        panel.addComponent(new TextComponent(PADDING, PADDING,
                Component.translatable("gui.packcore.export.metadata.heading"), COLOR_LABEL));

        int scrollY = PADDING + font.lineHeight + FIELD_GAP;
        int scrollHeight = getHeight() - scrollY - BUTTON_HEIGHT - PADDING * 2;

        EmptyComponent container = new EmptyComponent(0, 0, width, 0);
        int currentY = PADDING;

        nameField = addValidatedField(container, fieldWidth, currentY,
                "gui.packcore.export.field.name", "", input -> {
                    if (input.isBlank()) return List.of(ValidationErrors.minLength(1));
                    return List.of();
                });
        currentY += fieldStride;

        versionField = addValidatedField(container, fieldWidth, currentY,
                "gui.packcore.export.field.version", "1.0.0", input -> {
                    if (!input.matches("\\d+\\.\\d+(\\.\\d+)?(-.*)?"))
                        return List.of(ValidationErrors.pattern("e.g. 1.0.0"));
                    return List.of();
                });
        currentY += fieldStride;

        authorField = addPlainField(container, fieldWidth, currentY,
                "gui.packcore.export.field.author", ModpackMetadata.getInstance().getAuthor());
        currentY += fieldStride;

        descriptionField = addPlainField(container, fieldWidth, currentY,
                "gui.packcore.export.field.description", "");
        currentY += fieldStride;

        ScreenResolution.ScreenSize screenSize = ScreenResolution.detect();
        resolutionField = addValidatedField(container, fieldWidth, currentY,
                "gui.packcore.export.field.resolution", screenSize.width() + "x" + screenSize.height(), input -> {
                    if (!input.matches("\\d+[x×]\\d+"))
                        return List.of(ValidationErrors.pattern("e.g. 1920x1080"));
                    return List.of();
                });
        currentY += fieldStride;

        container.setHeight(currentY + PADDING);

        ScrollContainerWidget scroll = new ScrollContainerWidget(width - PADDING * 2, scrollHeight);
        scroll.addComponent(container);
        EmptyComponent scrollWrapper = new EmptyComponent(PADDING, scrollY, width - PADDING, scrollHeight);
        scrollWrapper.addWidget(scroll);
        panel.addComponent(scrollWrapper);

        int exportBtnX = (fieldWidth - BUTTON_WIDTH) / 2;
        int exportBtnY = scrollY + scrollHeight + PADDING;
        exportBtn = new CustomButtonWidget(exportBtnX, exportBtnY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.export.button.export"),
                GuiHelper.BLANK_BUTTON_SPRITES,
                btn -> doExport());
        exportBtn.active = false;
        panel.addWidget(exportBtn);
    }

    private void buildRightPanel(EmptyComponent panel, int width) {
        var font = Minecraft.getInstance().font;
        int currentY = PADDING;

        panel.addComponent(new TextComponent(PADDING, currentY,
                Component.translatable("gui.packcore.export.files.heading"), COLOR_LABEL));
        currentY += font.lineHeight + FIELD_GAP;

        int treeHeight = getHeight() - currentY - BUTTON_HEIGHT - PADDING * 2;

        FileTreeNode root;
        try {
            root = FileTreeBuilder.fromDirectory(FabricLoader.getInstance().getGameDir(), HIDDEN_PATHS);
        } catch (IOException e) {
            LOGGER.error("Failed to build directory tree: {}", e.getMessage());
            panel.addComponent(new TextComponent(PADDING, currentY,
                    Component.literal("Error reading game folder."), 0xFFFF5555));
            return;
        }

        fileTree = new FileTreeComponent(PADDING, currentY, width - PADDING * 3, treeHeight, root);
        panel.addComponent(fileTree);

        int openBtnX = PADDING + (width - PADDING * 2 - OPEN_BTN_WIDTH) / 2;
        int openBtnY = currentY + treeHeight + PADDING;
        panel.addWidget(new CustomButtonWidget(openBtnX, openBtnY, OPEN_BTN_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.export.button.open_folder"),
                GuiHelper.BLANK_BUTTON_SPRITES,
                btn -> openExportsFolder()));
    }

    private EditBoxWidget addValidatedField(EmptyComponent panel, int fieldWidth, int y,
                                            String labelKey, String defaultValue,
                                            Function<String, List<Component>> validator) {
        var font = Minecraft.getInstance().font;
        panel.addComponent(new TextComponent(PADDING, y, Component.translatable(labelKey), COLOR_SECTION));

        EditBoxWidget field = new EditBoxWidget(font, PADDING, y + font.lineHeight + LABEL_GAP,
                fieldWidth - PADDING * 2, FIELD_HEIGHT, Component.translatable(labelKey)) {
            @Override
            public List<Component> validateInput(String input) {
                List<Component> errors = new ArrayList<>(validator.apply(input));
                updateExportButton();
                return errors;
            }
        };
        field.setValue(defaultValue);
        panel.addWidget(field);
        return field;
    }

    private EditBoxWidget addPlainField(EmptyComponent panel, int fieldWidth, int y,
                                        String labelKey, String defaultValue) {
        var font = Minecraft.getInstance().font;
        panel.addComponent(new TextComponent(PADDING, y, Component.translatable(labelKey), COLOR_SECTION));

        EditBoxWidget field = new EditBoxWidget(font, PADDING, y + font.lineHeight + LABEL_GAP,
                fieldWidth - PADDING * 2, FIELD_HEIGHT, Component.translatable(labelKey));
        field.setValue(defaultValue);
        panel.addWidget(field);
        return field;
    }

    private void updateExportButton() {
        if (exportBtn == null) return;

        boolean nameOk = nameField != null && !nameField.getValue().isBlank();
        boolean versionOk = versionField != null && versionField.getValue().matches("\\d+\\.\\d+(\\.\\d+)?(-.*)?");
        boolean resOk = resolutionField != null && resolutionField.getValue().matches("\\d+[x×]\\d+");
        boolean filesSelected = fileTree != null && !fileTree.getSelectedPaths().isEmpty();

        exportBtn.active = nameOk && versionOk && resOk && filesSelected;

        if (exportBtn.active) {
            exportBtn.setTooltip(null);
            return;
        }

        List<Component> reasons = new ArrayList<>();
        if (!nameOk) reasons.add(Component.translatable("gui.packcore.export.tooltip.missing_name"));
        if (!versionOk) reasons.add(Component.translatable("gui.packcore.export.tooltip.missing_version"));
        if (!resOk) reasons.add(Component.translatable("gui.packcore.export.tooltip.missing_resolution"));
        if (!filesSelected) reasons.add(Component.translatable("gui.packcore.export.tooltip.missing_files"));

        var tooltipText = Component.empty();
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) tooltipText.append("\n");
            tooltipText.append(reasons.get(i));
        }

        exportBtn.setTooltip(Tooltip.create(tooltipText));
    }


    private void doExport() {
        if (nameField == null || nameField.getValue().isBlank()) return;

        String name = nameField.getValue().trim();
        String version = versionField != null ? versionField.getValue().trim() : "1.0.0";
        String author = authorField != null ? authorField.getValue().trim() : "";
        String description = descriptionField != null ? descriptionField.getValue().trim() : "";
        String resolution = resolutionField != null ? resolutionField.getValue().trim() : "1920x1080";

        int targetWidth = 1920;
        int targetHeight = 1080;
        try {
            String[] parts = resolution.split("[x×]");
            if (parts.length == 2) {
                targetWidth = Integer.parseInt(parts[0].trim());
                targetHeight = Integer.parseInt(parts[1].trim());
            }
        } catch (NumberFormatException ignored) {}

        ConfigPackMeta meta = ConfigPackMeta.builder(version, targetWidth, targetHeight)
                .name(name)
                .description(description.isEmpty() ? null : description)
                .author(author.isEmpty() ? null : author)
                .build();

        List<String> selectedPaths = fileTree != null ? fileTree.getSelectedPaths() : List.of();
        if (selectedPaths.isEmpty()) {
            LOGGER.warn("No files selected for export.");
            return;
        }

        String zipName = name.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".zip";
        Path gameDir = FabricLoader.getInstance().getGameDir();

        try {
            ConfigPackBuilder.zipFiles(gameDir, selectedPaths, zipName, meta);
            LOGGER.info("Exported config pack: {}", zipName);
        } catch (IOException e) {
            LOGGER.error("Export failed: {}", e.getMessage());
        }
    }

    private void openExportsFolder() {
        try {
            Files.createDirectories(EXPORTS_DIR);
            Util.getPlatform().openUri(EXPORTS_DIR.toUri());
        } catch (IOException e) {
            LOGGER.error("Failed to open exports folder: {}", e.getMessage());
        }
    }
}
