package com.github.kd_gaming1.packcore.gui.configscreen;

import com.github.kd_gaming1.packcore.gui.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.configscreen.ui.UITheme;
import com.github.kd_gaming1.packcore.util.ConfigApplicationManager;
import com.github.kd_gaming1.packcore.util.ConfigFileUtils;
import com.github.kd_gaming1.packcore.util.ConfigMetadata;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ButtonComponent;
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

import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.PackCore.getModpackInfo;

public class ModpackConfigMenuScreen extends BaseOwoScreen<FlowLayout> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Identifier backgroundTexture;
    private ConfigFileUtils.ConfigFile selectedConfig = null;
    private FlowLayout selectedEntry = null;
    private FlowLayout infoPanel;
    private io.wispforest.owo.ui.container.OverlayContainer<FlowLayout> currentPopup = null;

    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        backgroundTexture = Identifier.of(MOD_ID, "textures/gui/wizard/welcome_bg.png");
        rootComponent.surface(UiSurfaces.stretched(backgroundTexture, 1920, 1082));
        rootComponent.padding(Insets.of(6,8,8,8));

        rootComponent.child(createHeader());

        FlowLayout contentArea = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        contentArea.gap(6);

        contentArea.child(createSidebar());

        infoPanel = createInfoPanel();
        contentArea.child(infoPanel);

        rootComponent.child(contentArea);
    }

    private FlowLayout createHeader() {
        FlowLayout header = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.padding(Insets.of(4));
        header.verticalAlignment(VerticalAlignment.CENTER);

        ButtonComponent backButton = (ButtonComponent) Components.button(Text.literal("Back"), button -> {
                    MinecraftClient.getInstance().setScreen(null); // closes to game
                })
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/previous.png"),
                        0, 0, 90, 57))
                .horizontalSizing(Sizing.fixed(90))
                .verticalSizing(Sizing.fixed(19));

        Identifier logoId = Identifier.of(MOD_ID, "textures/gui/assets/sbe_logo.png");
        TextureComponent logo = Components.texture(logoId, 0, 0, 40, 40, 40 ,40);

        Text titleText = Text.literal(getModpackInfo().getName()).styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")));
        LabelComponent titleLabel = Components.label(titleText).color(UITheme.color(UITheme.TEXT_WHITE));
        titleLabel.margins(Insets.of(4, 0, 4, 0));

        // Use ConfigFileUtils.getCurrentConfig() which now returns the unified ConfigMetadata
        ConfigMetadata currentMeta = ConfigFileUtils.getCurrentConfig();

        FlowLayout currentConfigSection = (FlowLayout) Containers.horizontalFlow(Sizing.expand(), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .verticalAlignment(VerticalAlignment.CENTER)
                .margins(Insets.horizontal(6));

        FlowLayout configInfo = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(2)
                .horizontalAlignment(HorizontalAlignment.RIGHT);

        LabelComponent currentLabel = Components.label(Text.literal("Current: " + currentMeta.getName())
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.ACCENT_GOLD));

        FlowLayout detailsRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        detailsRow.gap(8);

        LabelComponent versionLabel = Components.label(Text.literal("v" + currentMeta.getVersion()))
                .color(UITheme.color(UITheme.TEXT_SECONDARY));

        LabelComponent resolutionLabel = Components.label(Text.literal("(" + currentMeta.getTargetResolution() + ")"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY));

        detailsRow.child(versionLabel);
        detailsRow.child(resolutionLabel);

        configInfo.child(currentLabel);
        configInfo.child(detailsRow);
        currentConfigSection.child(configInfo);

        header.child(logo);
        header.child(titleLabel);
        header.child(currentConfigSection);
        header.child(backButton);
        header.margins(Insets.bottom(6));

        return header;
    }

    private FlowLayout createSidebar() {
        FlowLayout sidebar = Containers.verticalFlow(Sizing.fill(35), Sizing.expand());
        sidebar.gap(4);
        sidebar.surface(UiSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/menu/notif_box.png"), 607, 755));
        sidebar.padding(Insets.of(12));
        sidebar.horizontalAlignment(HorizontalAlignment.CENTER);

        FlowLayout infoSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .padding(Insets.of(2));

        LabelComponent infoLabel = (LabelComponent) Components.label(Text.literal("Export, Import or Apply Modpack Configurations"))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .horizontalSizing(Sizing.fill(100));

        infoSection.child(infoLabel);
        sidebar.child(infoSection);

        FlowLayout topSection = createOfficialConfigSection();
        FlowLayout bottomSection = createCustomConfigSection();

        FlowLayout buttonSection = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        ButtonComponent importButton = (ButtonComponent) Components.button(Text.literal("Import Configs"), button -> {
                    MinecraftClient.getInstance().setScreen(new ConfigImportScreen());
                })
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 57))
                .horizontalSizing(Sizing.fixed(90))
                .verticalSizing(Sizing.fixed(19));

        ButtonComponent exportButton = (ButtonComponent) Components.button(Text.literal("Export Configs"), button -> {
                    MinecraftClient.getInstance().setScreen(new ConfigExportScreen());
                })
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 90, 57))
                .horizontalSizing(Sizing.fixed(90))
                .verticalSizing(Sizing.fixed(19));

        sidebar.child(topSection);
        sidebar.child(bottomSection);
        buttonSection.child(importButton).child(exportButton);
        sidebar.child(buttonSection);
        return sidebar;
    }

    private FlowLayout createOfficialConfigSection() {
        FlowLayout topSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand(45))
                .gap(4)
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)))
                .padding(Insets.of(4))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        LabelComponent header = (LabelComponent) Components.label(Text.literal("Official Config Options")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .horizontalSizing(Sizing.fill(100));

        topSection.child(header);

        List<ConfigFileUtils.ConfigFile> officialConfigs = ConfigFileUtils.getOfficialConfigs();
        ScrollContainer<FlowLayout> officialScrollContainer = createConfigList(officialConfigs, true);

        topSection.child(officialScrollContainer);

        return topSection;
    }

    private FlowLayout createCustomConfigSection() {
        FlowLayout bottomSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.expand(50))
                .gap(4)
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)))
                .padding(Insets.of(4))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        LabelComponent header2 = (LabelComponent) Components.label(Text.literal("Custom/Community Config Options")
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .horizontalSizing(Sizing.fill(100));

        bottomSection.child(header2);

        List<ConfigFileUtils.ConfigFile> customConfigs = ConfigFileUtils.getCustomConfigs();
        ScrollContainer<FlowLayout> customScrollContainer = createConfigList(customConfigs, false);

        bottomSection.child(customScrollContainer);

        return bottomSection;
    }

    private ScrollContainer<FlowLayout> createConfigList(java.util.List<ConfigFileUtils.ConfigFile> configs, boolean isOfficial) {
        FlowLayout listContent = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2);

        if (configs.isEmpty()) {
            LabelComponent emptyLabel = Components.label(Text.literal("No configs found"))
                    .color(UITheme.color(UITheme.TEXT_SECONDARY));
            listContent.child(emptyLabel);
        } else {
            for (ConfigFileUtils.ConfigFile config : configs) {
                FlowLayout entry = createConfigEntry(config);
                listContent.child(entry);
            }
        }

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), listContent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.scrollStep(10);

        return scrollContainer;
    }

    private FlowLayout createConfigEntry(ConfigFileUtils.ConfigFile config) {
        FlowLayout entry = Containers.verticalFlow(Sizing.fill(95), Sizing.content());
        entry.gap(2);
        entry.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        entry.padding(Insets.of(6));
        entry.margins(Insets.of(1));

        LabelComponent nameLabel = Components.label(Text.literal(config.getDisplayName()))
                .color(UITheme.color(UITheme.TEXT_WHITE));

        String typeText = config.isOfficial() ? "Official" : "Custom";
        int typeColor = config.isOfficial() ? UITheme.STATUS_SUCCESS_BORDER : UITheme.STATUS_WARNING_BORDER;
        LabelComponent typeLabel = Components.label(Text.literal(typeText))
                .color(UITheme.color(typeColor));

        entry.child(nameLabel);
        entry.child(typeLabel);

        entry.mouseDown().subscribe((mouseX, mouseY, button) -> {
            selectConfig(config, entry);
            return true;
        });

        entry.mouseEnter().subscribe(() -> {
            if (selectedEntry != entry) {
                entry.surface(Surface.flat(UITheme.ENTRY_HOVER).and(Surface.outline(UITheme.ACCENT_GOLD)));
            }
        });

        entry.mouseLeave().subscribe(() -> {
            if (selectedEntry != entry) {
                entry.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
            }
        });

        return entry;
    }

    private void selectConfig(ConfigFileUtils.ConfigFile config, FlowLayout entry) {
        if (selectedEntry != null) {
            selectedEntry.surface(Surface.flat(UITheme.ENTRY_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        }

        selectedConfig = config;
        selectedEntry = entry;
        entry.surface(Surface.flat(UITheme.ENTRY_SELECTED).and(Surface.outline(UITheme.ACCENT_GOLD)));

        updateInfoPanel();
    }

    private void updateInfoPanel() {
        if (selectedConfig == null) return;

        infoPanel.clearChildren();

        ConfigMetadata meta = selectedConfig.getMetadata();

        FlowLayout header = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ACCENT_GOLD)))
                .padding(Insets.of(6))
                .verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent headerLabel = Components.label(Text.literal(selectedConfig.getDisplayName())
                        .setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.TEXT_WHITE));
        header.child(headerLabel);
        infoPanel.child(header);

        FlowLayout extractInfo = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2)
                .surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)))
                .padding(Insets.of(6));

        extractInfo.child(Components.label(Text.literal("Source: " + meta.getSource()))
                .color(UITheme.color(UITheme.TEXT_WHITE)));
        extractInfo.child(Components.label(Text.literal("Author: " + meta.getAuthor()))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));
        extractInfo.child(Components.label(Text.literal("Version: " + meta.getVersion()))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));
        if (meta.getCreatedDate() != null && !meta.getCreatedDate().isEmpty()) {
            extractInfo.child(Components.label(Text.literal("Created: " + meta.getCreatedDate()))
                    .color(UITheme.color(UITheme.TEXT_SECONDARY)));
        }

        infoPanel.child(extractInfo);

        FlowLayout contentWrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.expand());
        contentWrapper.surface(Surface.flat(UITheme.PANEL_BACKGROUND).and(Surface.outline(UITheme.ENTRY_BORDER)));
        contentWrapper.padding(Insets.of(2));

        FlowLayout scrollableContent = (FlowLayout) Containers.verticalFlow(Sizing.fill(98), Sizing.content())
                .gap(4)
                .padding(Insets.of(6));

        scrollableContent.child(Components.label(Text.literal("Description:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));
        scrollableContent.child(Components.label(Text.literal(meta.getDescription()))
                .color(UITheme.color(UITheme.TEXT_WHITE))
                .sizing(Sizing.fill(95), Sizing.content()));

        scrollableContent.child(Components.label(Text.literal("Technical Details:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                .color(UITheme.color(UITheme.ACCENT_GOLD)));
        scrollableContent.child(Components.label(Text.literal("Resolution: " + meta.getTargetResolution()))
                .color(UITheme.color(UITheme.TEXT_SECONDARY)));

        // Features
        if (meta.getFeatures() != null && !meta.getFeatures().isEmpty()) {
            scrollableContent.child(Components.label(Text.literal("Features:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));
            for (String feature : meta.getFeatures()) {
                scrollableContent.child(Components.label(Text.literal("• " + feature))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }
        }

        // Requirements
        if (meta.getRequirements() != null && !meta.getRequirements().isEmpty()) {
            scrollableContent.child(Components.label(Text.literal("Requirements:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));
            for (String req : meta.getRequirements()) {
                scrollableContent.child(Components.label(Text.literal("• " + req))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }
        }

        // Mods list
        if (meta.getMods() != null && !meta.getMods().isEmpty()) {
            scrollableContent.child(Components.label(Text.literal("Mods:").setStyle(Style.EMPTY.withBold(Boolean.TRUE)))
                    .color(UITheme.color(UITheme.ACCENT_GOLD)));
            for (String mod : meta.getMods()) {
                scrollableContent.child(Components.label(Text.literal("• " + mod))
                        .color(UITheme.color(UITheme.TEXT_WHITE)));
            }
        }

        ScrollContainer<FlowLayout> contentScrollContainer = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), scrollableContent);
        contentScrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        contentScrollContainer.scrollStep(15);

        contentWrapper.child(contentScrollContainer);
        infoPanel.child(contentWrapper);

        FlowLayout buttonPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .horizontalAlignment(HorizontalAlignment.CENTER);

        ButtonComponent applyButton = (ButtonComponent) Components.button(Text.literal("Apply Config"), button -> showConfirmationPopup())
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 100, 60))
                .horizontalSizing(Sizing.fixed(100))
                .verticalSizing(Sizing.fixed(20));

        buttonPanel.child(applyButton);
        infoPanel.child(buttonPanel);
    }

    private void showConfirmationPopup() {
        if (selectedConfig == null) return;

        currentPopup = ConfigConfirmationPopup.createConfirmationPopup(
                selectedConfig,
                this::onConfigApply,
                this::closeConfirmationPopup
        );

        currentPopup.zIndex(10);
        this.uiAdapter.rootComponent.child(currentPopup);
    }

    private void closeConfirmationPopup() {
        if (currentPopup != null) {
            this.uiAdapter.rootComponent.removeChild(currentPopup);
            currentPopup = null;
        }
    }

    private void onConfigApply() {
        if (selectedConfig == null) return;

        closeConfirmationPopup();

        try {
            ConfigApplicationManager.applyConfigOnRestart(selectedConfig);

            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Applying config: " + selectedConfig.getDisplayName() + " - Game will restart..."),
                        false
                );
            }

        } catch (Exception e) {
            LOGGER.error("Failed to apply config", e);

            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Failed to apply config: " + e.getMessage()).styled(style ->
                                style.withColor(net.minecraft.util.Formatting.RED)),
                        false
                );
            }
        }
    }

    private FlowLayout createInfoPanel() {
        FlowLayout infoPanel = (FlowLayout) Containers.verticalFlow(Sizing.expand(65), Sizing.expand())
                .gap(4)
                .surface(UiSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/menu/info_box.png"), 1142, 934))
                .padding(Insets.of(14))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent emptyLabel = Components.label(Text.literal("Select a config to view details"))
                .color(UITheme.color(UITheme.TEXT_SECONDARY));

        infoPanel.child(emptyLabel);

        return infoPanel;
    }
}