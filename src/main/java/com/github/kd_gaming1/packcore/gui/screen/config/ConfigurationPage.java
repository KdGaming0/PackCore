package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.github.kd_gaming1.packcore.configpack.ConfigPackScanner;
import com.github.kd_gaming1.packcore.gui.component.*;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class ConfigurationPage extends BaseConfigPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ConfigurationPage");

    private static final int PANEL_GAP = 12;
    private static final int PADDING = 10;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_GAP = 8;
    private static final int LABEL_GAP = 6;
    private static final int SUB_TAB_HEIGHT = 22;

    private static final int COLOR_LABEL = 0xFFCCCCCC;
    private static final int COLOR_HINT = 0xFF666666;
    private static final int COLOR_ERROR = 0xFFFF5555;

    private ConfigPackEntry selectedPreset;
    private EmptyComponent leftPanel;
    private int panelWidth;
    private double rightPanelScrollAmount = 0;

    private enum PresetsSource { OFFICIAL, MY_EXPORTS }
    private PresetsSource presetsSource = PresetsSource.OFFICIAL;

    // Cached per page-open session; invalidated on enter and on source switch
    private List<ConfigPackEntry> cachedPacks = null;
    private PresetsSource cachedPacksSource = null;

    public ConfigurationPage(int width, int height) {
        super(width, height);
    }

    @Override
    public void onEnter() {
        this.clearComponents();
        cachedPacks = null;
        cachedPacksSource = null;

        panelWidth = (getWidth() - PANEL_GAP) / 2;
        int panelHeight = getHeight();

        leftPanel = new EmptyComponent(0, 0, panelWidth, panelHeight);
        EmptyComponent rightPanel = new EmptyComponent(panelWidth + PANEL_GAP, 0, panelWidth, panelHeight);

        buildLeftPanel();
        buildRightPanel(rightPanel);

        this.addComponent(leftPanel);
        this.addComponent(rightPanel);
    }

    private void buildLeftPanel() {
        leftPanel.clearComponents();
        var font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight;
        int currentY = PADDING;

        leftPanel.addComponent(new TextComponent(PADDING, currentY,
                Component.translatable("gui.packcore.config.files.heading"), COLOR_LABEL));
        currentY += lineHeight + LABEL_GAP;

        if (selectedPreset == null) {
            leftPanel.addComponent(new MultiLineTextComponent(PADDING, currentY, (getWidth() - PANEL_GAP) / 2,
                    Component.translatable("gui.packcore.config.files.hint"), COLOR_HINT));
            return;
        }

        int buttonsAreaHeight = BUTTON_HEIGHT + PADDING * 2;
        int treeHeight = getHeight() - currentY - buttonsAreaHeight;

        FileTreeNode treeRoot;
        try {
            treeRoot = FileTreeBuilder.fromZip(selectedPreset.zipPath());
        } catch (IOException e) {
            LOGGER.error("Failed to read zip for file tree: {}", e.getMessage());
            leftPanel.addComponent(new TextComponent(PADDING, currentY,
                    Component.literal("Error reading preset."), COLOR_ERROR));
            return;
        }

        FileTreeComponent fileTree = new FileTreeComponent(PADDING, currentY, panelWidth - PADDING * 2, treeHeight, treeRoot);
        leftPanel.addComponent(fileTree);

        int panelHeight = leftPanel.getHeight();
        int buttonsY = panelHeight - PADDING - BUTTON_HEIGHT;
        int totalBtnWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int buttonsStartX = (panelWidth - totalBtnWidth) / 2;

        CustomButtonWidget applySelectedBtn = new CustomButtonWidget(
                buttonsStartX, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.config.button.apply_selected"),
                GuiHelper.BLANK_BUTTON_SPRITES,
                btn -> applyFiles(fileTree.getSelectedPaths()));
        applySelectedBtn.active = false;
        applySelectedBtn.setTooltip(Tooltip.create(
                Component.translatable("gui.packcore.config.tooltip.apply_selected")));

        CustomButtonWidget applyAllBtn = new CustomButtonWidget(
                buttonsStartX + BUTTON_WIDTH + BUTTON_GAP, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT,
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
        int tabWidth = (panelWidth - PADDING * 4) / 2;

        // Sub-tab bar
        panel.addComponent(new AbstractComponent(PADDING, PADDING, panelWidth - PADDING * 2, SUB_TAB_HEIGHT) {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt, int pw, int ph) {
                int bx = getTotalX(), by = getTotalY(), bh = getHeight();
                for (int i = 0; i < 2; i++) {
                    boolean active = (i == 0) == (presetsSource == PresetsSource.OFFICIAL);
                    String label = i == 0
                            ? Component.translatable("gui.packcore.config.source.official").getString()
                            : Component.translatable("gui.packcore.config.source.my_exports").getString();
                    int tx = bx + i * tabWidth;
                    int color = active ? 0xFFFFFFFF : 0xFF888888;
                    g.drawString(font, label, tx + (tabWidth - font.width(label)) / 2,
                            by + (bh - font.lineHeight) / 2, color, false);
                    if (active)
                        g.fill(tx, by + bh - 2, tx + tabWidth, by + bh, 0xFF2196F3);
                }
            }
        });

        // Tab click hit-areas
        for (int i = 0; i < 2; i++) {
            final PresetsSource src = (i == 0) ? PresetsSource.OFFICIAL : PresetsSource.MY_EXPORTS;
            int btnX = PADDING + i * tabWidth;
            panel.addWidget(new ButtonWidget(btnX, PADDING, tabWidth, SUB_TAB_HEIGHT,
                    Component.empty(), b -> {
                if (presetsSource != src) {
                    presetsSource = src;
                    selectedPreset = null;
                    cachedPacks = null;
                    cachedPacksSource = null;
                    buildLeftPanel();
                    buildRightPanel(panel);
                    leftPanel.updateParentPosition(leftPanel.getParentX(), leftPanel.getParentY(), getWidth(), getHeight());
                    panel.updateParentPosition(panel.getParentX(), panel.getParentY(), getWidth(), getHeight());
                }
            }) {
                @Override protected void renderContents(@NonNull GuiGraphics graphics, int mx, int my, float pt) {}
            });
        }

        int listY = PADDING + SUB_TAB_HEIGHT + LABEL_GAP;
        int listHeight = getHeight() - listY - PADDING;

        List<ConfigPackEntry> packs = getScannedPacks();

        ScrollContainerWidget scroll = new ScrollContainerWidget(panelWidth - PADDING * 3, listHeight);
        EmptyComponent scrollWrapper = new EmptyComponent(PADDING, listY, panelWidth - PADDING * 2, listHeight);
        EmptyComponent container = new EmptyComponent(0, 0, panelWidth - PADDING * 2, 0);

        int y = 0;

        for (ConfigPackEntry pack : packs) {
            boolean isActive = selectedPreset != null && selectedPreset.zipPath().equals(pack.zipPath());
            ConfigPackCard card = new ConfigPackCard(
                    0, y, panelWidth - PADDING * 4, pack, isActive,
                    null,
                    clicked -> {
                        rightPanelScrollAmount = scroll.scrollAmount();
                        selectedPreset = clicked;
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

        container.setHeight(y);
        scroll.addComponent(container);
        scroll.setScrollAmount(rightPanelScrollAmount);
        scrollWrapper.addWidget(scroll);
        panel.addComponent(scrollWrapper);
    }

    /** Returns cached packs for the current source, scanning from disk only when the cache is stale. */
    private List<ConfigPackEntry> getScannedPacks() {
        if (cachedPacks == null || cachedPacksSource != presetsSource) {
            cachedPacks = scanPacks();
            cachedPacksSource = presetsSource;
        }
        return cachedPacks;
    }

    private void applyFiles(List<String> paths) {
        if (selectedPreset == null || paths.isEmpty()) return;
        PackCoreConfig.pendingConfigPack = selectedPreset.zipPath().getFileName().toString();
        PackCoreConfig.pendingConfigPackFiles = String.join("|", paths);
        MidnightConfig.write(MOD_ID);
        Minecraft.getInstance().stop();
    }

    private void applyAll() {
        if (selectedPreset == null) {
            LOGGER.warn("Apply All clicked without a selected preset (source={})", presetsSource);
            return;
        }

        String pendingFile = selectedPreset.zipPath().getFileName().toString();
        LOGGER.info("Scheduling config apply on restart: file='{}', source={}", pendingFile, presetsSource);

        PackCoreConfig.pendingConfigPack = pendingFile;
        MidnightConfig.write(MOD_ID);

        LOGGER.info("Pending config saved, stopping client to apply on next launch.");
        Minecraft.getInstance().stop();
    }

    private List<ConfigPackEntry> scanPacks() {
        Path dir = presetsSource == PresetsSource.OFFICIAL
                ? PackCore.PACKCORE_DIR.resolve("configs")
                : PackCore.PACKCORE_DIR.resolve("user_configs");
        try {
            return new ConfigPackScanner().scanFolder(dir);
        } catch (IOException e) {
            LOGGER.error("Failed to scan packs: {}", e.getMessage());
            return List.of();
        }
    }
}