package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.github.kd_gaming1.packcore.configpack.ConfigPackExtractor;
import com.github.kd_gaming1.packcore.configpack.ConfigPackScanner;
import com.github.kd_gaming1.packcore.gui.component.ConfigPackCard;
import com.github.kd_gaming1.packcore.gui.component.FileTreeBuilder;
import com.github.kd_gaming1.packcore.gui.component.FileTreeComponent;
import com.github.kd_gaming1.packcore.gui.component.FileTreeNode;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ImportPage extends BaseConfigPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ImportPage");

    private static final Path IMPORTS_DIR = PackCore.PACKCORE_DIR.resolve("imports");

    private static final int PANEL_GAP = 12;
    private static final int PADDING = 10;
    private static final int BUTTON_HEIGHT = 18;
    private static final int OPEN_BTN_WIDTH = 140;
    private static final int ACTION_BTN_WIDTH = 110;
    private static final int BUTTON_GAP = 8;
    private static final int LABEL_GAP = 6;

    private static final int COLOR_LABEL = 0xFFCCCCCC;
    private static final int COLOR_HINT = 0xFF666666;
    private static final int COLOR_ERROR = 0xFFFF5555;

    private ConfigPackEntry selectedImport;
    private EmptyComponent leftPanel;
    private int panelWidth;
    private double rightPanelScrollAmount = 0;

    public ImportPage(int width, int height) {
        super(width, height);
    }

    @Override
    public void onEnter() {
        this.clearComponents();

        ensureImportsDirExists();

        panelWidth = (getWidth() - PANEL_GAP) / 2;

        leftPanel = new EmptyComponent(0, 0, panelWidth, getHeight());
        EmptyComponent rightPanel = new EmptyComponent(panelWidth + PANEL_GAP, 0, panelWidth, getHeight());

        buildLeftPanel();
        buildRightPanel(rightPanel);

        this.addComponent(leftPanel);
        this.addComponent(rightPanel);
    }

    private void buildLeftPanel() {
        leftPanel.clearComponents();

        var font = Minecraft.getInstance().font;
        int panelHeight = leftPanel.getHeight();
        int currentY = PADDING;

        leftPanel.addComponent(new TextComponent(PADDING, currentY,
                Component.translatable("gui.packcore.import.files.heading"), COLOR_LABEL));
        currentY += font.lineHeight + LABEL_GAP;

        if (selectedImport == null) {
            leftPanel.addComponent(new TextComponent(PADDING, currentY,
                    Component.translatable("gui.packcore.import.files.hint"), COLOR_HINT));
            return;
        }

        int buttonsAreaHeight = BUTTON_HEIGHT + PADDING * 2;
        int treeHeight = panelHeight - currentY - buttonsAreaHeight;

        FileTreeNode treeRoot;
        try {
            treeRoot = FileTreeBuilder.fromZip(selectedImport.zipPath());
        } catch (IOException e) {
            LOGGER.error("Failed to read import zip: {}", e.getMessage());
            leftPanel.addComponent(new TextComponent(PADDING, currentY,
                    Component.literal("Error reading import."), COLOR_ERROR));
            return;
        }

        FileTreeComponent fileTree = new FileTreeComponent(PADDING, currentY, panelWidth - PADDING * 2, treeHeight, treeRoot);
        leftPanel.addComponent(fileTree);

        int buttonsY = panelHeight - PADDING - BUTTON_HEIGHT;
        int totalBtnWidth = ACTION_BTN_WIDTH * 2 + BUTTON_GAP;
        int buttonsStartX = (panelWidth - totalBtnWidth) / 2;

        CustomButtonWidget applySelectedBtn = new CustomButtonWidget(
                buttonsStartX, buttonsY, ACTION_BTN_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.config.button.apply_selected"),
                GuiHelper.BLANK_BUTTON_SPRITES,
                btn -> applyFiles(fileTree.getSelectedPaths()));
        applySelectedBtn.active = false;
        applySelectedBtn.setTooltip(Tooltip.create(
                Component.translatable("gui.packcore.config.tooltip.apply_selected")));

        CustomButtonWidget applyAllBtn = new CustomButtonWidget(
                buttonsStartX + ACTION_BTN_WIDTH + BUTTON_GAP, buttonsY, ACTION_BTN_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.config.button.apply_all"),
                GuiHelper.BLANK_BUTTON_SPRITES,
                btn -> applyAll());
        applyAllBtn.setTooltip(Tooltip.create(
                Component.translatable("gui.packcore.config.tooltip.apply_all")));

        fileTree.setOnSelectionChanged(() ->
                applySelectedBtn.active = !fileTree.getSelectedPaths().isEmpty());

        leftPanel.addWidget(applySelectedBtn);
        leftPanel.addWidget(applyAllBtn);
    }

    private void buildRightPanel(EmptyComponent panel) {
        panel.clearComponents();

        var font = Minecraft.getInstance().font;
        int currentY = PADDING;

        panel.addComponent(new TextComponent(PADDING, currentY,
                Component.translatable("gui.packcore.import.list.heading"), COLOR_LABEL));
        currentY += font.lineHeight + LABEL_GAP;

        List<ConfigPackEntry> imports = scanImports();
        int buttonAreaHeight = BUTTON_HEIGHT + PADDING * 2;
        int listHeight = getHeight() - currentY - buttonAreaHeight;

        ScrollContainerWidget scroll = new ScrollContainerWidget(panelWidth - PADDING * 3, listHeight);
        EmptyComponent container = new EmptyComponent(0, 0, panelWidth - PADDING * 2, 0);

        int y = 0;
        if (imports.isEmpty()) {
            container.addComponent(new TextComponent(0, 0,
                    Component.translatable("gui.packcore.import.list.empty"), COLOR_HINT));
            container.addComponent(new MultiLineTextComponent(0, font.lineHeight + 4, container.getWidth(),
                    Component.translatable("gui.packcore.import.list.empty.hint"), COLOR_HINT));
            y = font.lineHeight * 2 + 4;
        } else {
            for (ConfigPackEntry entry : imports) {
                boolean isActive = selectedImport != null && selectedImport.zipPath().equals(entry.zipPath());
                ConfigPackCard card = new ConfigPackCard(
                        0, y, panelWidth - PADDING * 4, entry, isActive,
                        null,
                        clicked -> {
                            rightPanelScrollAmount = scroll.scrollAmount();
                            selectedImport = clicked;
                            buildLeftPanel();
                            buildRightPanel(panel);
                            leftPanel.updateParentPosition(
                                    leftPanel.getParentX(), leftPanel.getParentY(),
                                    getWidth(), getHeight());
                            panel.updateParentPosition(
                                    panel.getParentX(), panel.getParentY(),
                                    getWidth(), getHeight());
                        }
                );
                container.addComponent(card);
                y += card.getHeight() + 6;
            }
        }

        container.setHeight(y);
        scroll.addComponent(container);
        scroll.setScrollAmount(rightPanelScrollAmount);

        EmptyComponent scrollWrapper = new EmptyComponent(PADDING, currentY, panelWidth - PADDING * 2, listHeight);
        scrollWrapper.addWidget(scroll);
        panel.addComponent(scrollWrapper);

        int openBtnX = PADDING + (panelWidth - PADDING * 2 - OPEN_BTN_WIDTH) / 2;
        int openBtnY = currentY + listHeight + PADDING;
        panel.addWidget(new CustomButtonWidget(openBtnX, openBtnY, OPEN_BTN_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.import.button.open_folder"),
                GuiHelper.BLANK_BUTTON_SPRITES,
                btn -> openImportsFolder()));
    }

    private void applyFiles(List<String> paths) {
        if (selectedImport == null || paths.isEmpty()) return;
        try {
            ConfigPackExtractor.extractSelective(selectedImport.zipPath(), PackCore.PACKCORE_DIR,
                    ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING, paths);
            saveAppliedState();
        } catch (IOException e) {
            LOGGER.error("Failed to apply imported files: {}", e.getMessage());
        }
    }

    private void applyAll() {
        if (selectedImport == null) return;
        try {
            ConfigPackExtractor.extractAll(selectedImport.zipPath(), PackCore.PACKCORE_DIR,
                    ConfigPackExtractor.OverwriteMode.REPLACE_EXISTING);
            saveAppliedState();
        } catch (IOException e) {
            LOGGER.error("Failed to apply all imported files: {}", e.getMessage());
        }
    }

    private void saveAppliedState() {
        if (selectedImport.config().has("version")) {
            PackCoreConfig.lastAppliedVersion = selectedImport.config().get("version").getAsString();
        }
        PackCoreConfig.lastAppliedPackFile = selectedImport.zipPath().getFileName().toString();
        MidnightConfig.write("packcore");
    }

    private void openImportsFolder() {
        try {
            Files.createDirectories(IMPORTS_DIR);
            Util.getPlatform().openUri(IMPORTS_DIR.toUri());
        } catch (IOException e) {
            LOGGER.error("Failed to open imports folder: {}", e.getMessage());
        }
    }

    private void ensureImportsDirExists() {
        try { Files.createDirectories(IMPORTS_DIR); }
        catch (IOException ignored) {}
    }

    private List<ConfigPackEntry> scanImports() {
        try { return new ConfigPackScanner().scanFolder(IMPORTS_DIR); }
        catch (IOException e) {
            LOGGER.error("Failed to scan imports: {}", e.getMessage());
            return List.of();
        }
    }
}