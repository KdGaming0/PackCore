package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.integration.DungeonRoutesManager;
import com.github.kd_gaming1.packcore.integration.ItemBackgroundManager;
import com.github.kd_gaming1.packcore.integration.PerformanceProfileService;
import com.github.kd_gaming1.packcore.integration.ResourcePackManager;
import com.github.kd_gaming1.packcore.integration.ScamScreenerConfigurator;
import com.github.kd_gaming1.packcore.integration.StorageDesignManager;
import com.github.kd_gaming1.packcore.integration.TabDesignManager;
import com.github.kd_gaming1.scaleme.config.ScaleMeConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/** Final wizard step — review selections and apply them. */
public class ConfirmApplyPage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ConfirmApplyPage");

    private static final Component PAGE_TITLE =
            Component.translatable("gui.packcore.wizard.page.confirm.title");
    private static final Component MINI_PAGE_TITLE =
            Component.translatable("gui.packcore.wizard.page.dungeon_routes.title");

    private static final int PADDING = 16;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 6;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int SCROLL_BAR_WIDTH = 8;
    private static final int WARNING_LINE_GAP = 2;

    private static final int COLOR_VALUE_SELECTED = GuiColors.ACCENT;
    private static final int COLOR_VALUE_SKIPPED = 0xFF555555;
    private static final int COLOR_PACK_SUBROW = 0xFF777777;

    private static final WidgetSprites APPLY_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_blank_gray_button")
    );

    private static final String RESOURCE_PACKS_KEY = "resourcePacks";
    private static final String SCAM_PINGS_KEY = "scamScreenerPings";
    private static final String CAXTON_FONT_KEY = CaxtonFontPage.STATE_KEY;

    private record SummaryEntry(String selectionKey, String statusKey, String label, String translationPrefix) {}

    private static final List<SummaryEntry> SUMMARY_ENTRIES = List.of(
            new SummaryEntry(MainMenuDesignPage.STATE_KEY, MainMenuDesignPage.STATE_KEY, "Main Menu Design", "gui.packcore.wizard.menu_design."),
            new SummaryEntry(PerformancePage.STATE_KEY, PerformancePage.STATE_KEY, "Performance Profile", "gui.packcore.wizard.performance."),
            new SummaryEntry(TabDesignPage.STATE_KEY, TabDesignPage.STATE_KEY, "Tab Design", "gui.packcore.wizard.tab_design."),
            new SummaryEntry(ItemBackgroundPage.STATE_KEY, ItemBackgroundPage.STATE_KEY, "Item Background", "gui.packcore.wizard.item_background."),
            new SummaryEntry(StorageDesignPage.STATE_KEY, StorageDesignPage.STATE_KEY, "Storage Design", "gui.packcore.wizard.storage_design."),
            new SummaryEntry(SwordBlockPage.STATE_KEY, SwordBlockPage.STATE_KEY, "Sword Block", "gui.packcore.wizard.sword_block."),
            new SummaryEntry(DungeonRoutesPage.STATE_KEY, DungeonRoutesPage.STATE_KEY, "Dungeon Routes", "gui.packcore.wizard.dungeon_routes."),
            new SummaryEntry(ScamScreenerPage.ALERT_LEVEL_KEY, ScamScreenerPage.ALERT_LEVEL_KEY, "ScamScreener Alerts", "gui.packcore.wizard.scamscreener.minimum_risk.")
    );

    private static final List<SummaryEntry> MINI_SUMMARY_ENTRIES = List.of(
            new SummaryEntry(DungeonRoutesPage.STATE_KEY, DungeonRoutesPage.STATE_KEY, "Dungeon Routes", "gui.packcore.wizard.dungeon_routes.")
    );

    private enum RowStatus { SUCCESS, ERROR }

    private final Map<String, RowStatus> rowStatuses = new HashMap<>();
    private final Map<String, String> rowErrors = new HashMap<>();
    private final List<SummaryRowComponent> summaryRows = new ArrayList<>();
    private final List<SummaryRowComponent> packRows = new ArrayList<>();
    private final List<SummaryRowComponent> scamPingRows = new ArrayList<>();

    private CustomButtonWidget applyButton;
    private String globalErrorMessage;
    private Runnable onApplySucceeded;
    private boolean applyCompleted;
    private boolean miniWizardMode = false;

    public void setOnApplySucceeded(Runnable callback) { onApplySucceeded = callback; }
    public boolean isApplyCompleted() { return applyCompleted; }

    /** When enabled, only shows the Dungeon Routes row and applies only that setting. */
    public void setMiniWizardMode(boolean mini) { this.miniWizardMode = mini; }

    public ConfirmApplyPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() {
        return miniWizardMode ? MINI_PAGE_TITLE : PAGE_TITLE;
    }
    @Override public boolean validate() { return true; }

    @Override
    public void onExit() {
        applyCompleted = false;
        rowStatuses.clear();
        rowErrors.clear();
        globalErrorMessage = null;
    }

    @Override
    public void onEnter() {
        clearComponents();
        applyButton = null;
        summaryRows.clear();
        packRows.clear();
        scamPingRows.clear();
        globalErrorMessage = null;

        if (!applyCompleted) {
            rowStatuses.clear();
            rowErrors.clear();
        }

        var font = Minecraft.getInstance().font;
        int rowWidth = getWidth() - PADDING * 2 - SCROLL_BAR_WIDTH;
        boolean scamLoaded = FabricLoader.getInstance().isModLoaded("scamscreener");

        addComponent(new TextComponent(PADDING, PADDING,
                Component.translatable("gui.packcore.wizard.confirm.title"), GuiColors.NAME_DEFAULT));

        int buttonY = getHeight() - PADDING - BUTTON_HEIGHT;
        int scrollTop = PADDING + font.lineHeight + PADDING;
        int scrollHeight = buttonY - BUTTON_GAP - scrollTop;

        // ── Summary rows ──
        EmptyComponent rowContainer = new EmptyComponent(0, 0, rowWidth, 0);
        int currentY = 0;

        List<SummaryEntry> entriesToShow = miniWizardMode ? MINI_SUMMARY_ENTRIES : SUMMARY_ENTRIES;

        for (SummaryEntry entry : entriesToShow) {
            if (!scamLoaded && entry.selectionKey().equals(ScamScreenerPage.ALERT_LEVEL_KEY)) continue;

            String selectedId = state.getSelection(entry.selectionKey());
            boolean skipped = selectedId == null;
            Component valueText = skipped
                    ? Component.literal("Skipped")
                    : entry.selectionKey().equals(ScamScreenerPage.ALERT_LEVEL_KEY)
                      ? ScamScreenerPage.labelForAlertLevel(selectedId)
                      : Component.translatable(entry.translationPrefix() + selectedId + ".name");

            SummaryRowComponent row = new SummaryRowComponent(
                    0, currentY, rowWidth, ROW_HEIGHT, entry.statusKey(), entry.label(), valueText,
                    skipped ? COLOR_VALUE_SKIPPED : COLOR_VALUE_SELECTED, false);
            summaryRows.add(row);
            rowContainer.addComponent(row);
            currentY += ROW_HEIGHT + ROW_GAP;
        }

        // ── Caxton font row ──
        if (!miniWizardMode && FabricLoader.getInstance().isModLoaded("caxton")) {
            String caxtonId = state.getSelection(CAXTON_FONT_KEY);

            if (caxtonId == null) {
                caxtonId = CaxtonFontPage.FontOption.NONE_ID;
                state.setSelection(CAXTON_FONT_KEY, caxtonId);
            }

            Component caxtonValue = CaxtonFontPage.FontOption.NONE_ID.equals(caxtonId)
                    ? Component.translatable("gui.packcore.wizard.caxton_font.none.name")
                    : Component.translatable("gui.packcore.wizard.caxton_font." + caxtonId + ".name");

            SummaryRowComponent caxtonRow = new SummaryRowComponent(
                    0, currentY, rowWidth, ROW_HEIGHT, CAXTON_FONT_KEY,
                    "Font", caxtonValue,
                    CaxtonFontPage.FontOption.NONE_ID.equals(caxtonId) ? COLOR_VALUE_SKIPPED : COLOR_VALUE_SELECTED, false);
            summaryRows.add(caxtonRow);
            rowContainer.addComponent(caxtonRow);
            currentY += ROW_HEIGHT + ROW_GAP;
        }

        // ── ScamScreener pings ──
        if (!miniWizardMode && scamLoaded) {
            Set<String> pingOptions = state.getMultiSelection(ScamScreenerPage.PING_OPTIONS_KEY);
            SummaryRowComponent pingHeader = new SummaryRowComponent(
                    0, currentY, rowWidth, ROW_HEIGHT, SCAM_PINGS_KEY, "ScamScreener Pings",
                    pingOptions.isEmpty() ? Component.literal("None selected") : Component.literal(pingOptions.size() + " selected"),
                    pingOptions.isEmpty() ? COLOR_VALUE_SKIPPED : COLOR_VALUE_SELECTED, false);
            scamPingRows.add(pingHeader);
            rowContainer.addComponent(pingHeader);
            currentY += ROW_HEIGHT + ROW_GAP;

            for (String optionId : pingOptions.stream().sorted(Comparator.naturalOrder()).toList()) {
                SummaryRowComponent pingRow = new SummaryRowComponent(
                        0, currentY, rowWidth, ROW_HEIGHT, SCAM_PINGS_KEY,
                        "", ScamScreenerPage.labelForPingOption(optionId), COLOR_PACK_SUBROW, true);
                scamPingRows.add(pingRow);
                rowContainer.addComponent(pingRow);
                currentY += ROW_HEIGHT + ROW_GAP;
            }
        }

        // ── Resource packs ──
        if (!miniWizardMode) {
            Set<String> selectedPacks = state.getSelectedResourcePacks();
            SummaryRowComponent packHeader = new SummaryRowComponent(
                    0, currentY, rowWidth, ROW_HEIGHT, RESOURCE_PACKS_KEY, "Resource Packs",
                    selectedPacks.isEmpty() ? Component.literal("None selected") : Component.literal(selectedPacks.size() + " selected"),
                    selectedPacks.isEmpty() ? COLOR_VALUE_SKIPPED : COLOR_VALUE_SELECTED, false);
            packRows.add(packHeader);
            rowContainer.addComponent(packHeader);
            currentY += ROW_HEIGHT + ROW_GAP;

            for (String packId : selectedPacks) {
                SummaryRowComponent packRow = new SummaryRowComponent(
                        0, currentY, rowWidth, ROW_HEIGHT, RESOURCE_PACKS_KEY + ":" + packId,
                        "", Component.literal(packId), COLOR_PACK_SUBROW, true);
                packRows.add(packRow);
                rowContainer.addComponent(packRow);
                currentY += ROW_HEIGHT + ROW_GAP;
            }
        }

        rowContainer.setHeight(currentY);
        addComponent(GuiHelper.scrollWrapped(PADDING, scrollTop, getWidth() - PADDING * 2, scrollHeight,
                scroll -> scroll.addComponent(rowContainer)));

        applyButton = new CustomButtonWidget(
                (getWidth() - BUTTON_WIDTH) / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.wizard.confirm.apply_all_configs"),
                APPLY_BUTTON_SPRITES, btn -> applyAll());
        addWidget(applyButton);

        if (applyCompleted) {
            applyButton.active = false;
            refreshRowStatuses();
        }
    }

    // ── Apply ─────────────────────────────────────────────────────────────────

    private void applyAll() {
        LOGGER.info("Applying wizard selections...");
        globalErrorMessage = null;
        boolean anyError = false;

        if (miniWizardMode) {
            // Mini-wizard: only apply dungeon routes
            anyError |= runStep(DungeonRoutesPage.STATE_KEY, () -> applyDungeonRoutes(state.getSelection(DungeonRoutesPage.STATE_KEY)));
        } else {
            // Full wizard: apply everything
            anyError |= runStep(MainMenuDesignPage.STATE_KEY, () -> applyMainMenuDesign(state.getSelection(MainMenuDesignPage.STATE_KEY)));
            anyError |= runStep(PerformancePage.STATE_KEY, () -> applyPerformanceProfile(state.getSelection(PerformancePage.STATE_KEY)));
            anyError |= runStep(TabDesignPage.STATE_KEY, () -> applyTabDesign(state.getSelection(TabDesignPage.STATE_KEY)));
            anyError |= runStep(ItemBackgroundPage.STATE_KEY, () -> applyItemBackground(state.getSelection(ItemBackgroundPage.STATE_KEY)));
            anyError |= runStep(StorageDesignPage.STATE_KEY, () -> applyStorageDesign(state.getSelection(StorageDesignPage.STATE_KEY)));
            anyError |= runStep(DungeonRoutesPage.STATE_KEY, () -> applyDungeonRoutes(state.getSelection(DungeonRoutesPage.STATE_KEY)));

            if (FabricLoader.getInstance().isModLoaded("scaleme")) {
                anyError |= runStep(SwordBlockPage.STATE_KEY, () -> applySwordBlock(state.getSelection(SwordBlockPage.STATE_KEY)));
            }
            if (FabricLoader.getInstance().isModLoaded("scamscreener")) {
                anyError |= runStep(ScamScreenerPage.ALERT_LEVEL_KEY, () -> applyScamScreener(
                        state.getSelection(ScamScreenerPage.ALERT_LEVEL_KEY),
                        state.getMultiSelection(ScamScreenerPage.PING_OPTIONS_KEY)));
            }

            if (FabricLoader.getInstance().isModLoaded("caxton")) {
                anyError |= runStep(CAXTON_FONT_KEY, () -> applyCaxtonFont(state.getSelection(CAXTON_FONT_KEY)));
            }

            Set<String> caxtonPackIds = CaxtonFontPage.FontOption.all().stream()
                    .map(CaxtonFontPage.FontOption::packId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());

            anyError |= runStep(RESOURCE_PACKS_KEY, () -> ResourcePackManager.apply(state.getSelectedResourcePacks(), caxtonPackIds));
        }

        applyCompleted = !anyError;
        if (!anyError) {
            applyButton.active = false;
            if (onApplySucceeded != null) onApplySucceeded.run();
        } else {
            globalErrorMessage = "Some settings failed to apply — see highlighted rows and check logs.";
        }

        refreshRowStatuses();
    }

    private boolean runStep(String key, Runnable step) {
        try {
            step.run();
            rowStatuses.put(key, RowStatus.SUCCESS);
            return false;
        } catch (Exception e) {
            rowStatuses.put(key, RowStatus.ERROR);
            rowErrors.put(key, e.getMessage());
            LOGGER.error("Failed to apply \"{}\": {}", key, e.getMessage(), e);
            return true;
        }
    }

    private void refreshRowStatuses() {
        for (SummaryRowComponent row : summaryRows) {
            row.setStatus(rowStatuses.get(row.getKey()), rowErrors.get(row.getKey()));
        }

        RowStatus scamStatus = rowStatuses.get(ScamScreenerPage.ALERT_LEVEL_KEY);
        String scamError = rowErrors.get(ScamScreenerPage.ALERT_LEVEL_KEY);
        for (SummaryRowComponent row : scamPingRows) row.setStatus(scamStatus, scamError);

        RowStatus packStatus = rowStatuses.get(RESOURCE_PACKS_KEY);
        String packError = rowErrors.get(RESOURCE_PACKS_KEY);
        for (SummaryRowComponent row : packRows) row.setStatus(packStatus, packError);
    }

    // ── Appliers ──────────────────────────────────────────────────────────────

    private void applyMainMenuDesign(String selectedId) {
        if (selectedId == null) return;
        PackCoreConfig.menuStyle = switch (selectedId) {
            case "modern" -> PackCoreConfig.MenuStyle.MODERN;
            case "modern_minimal" -> PackCoreConfig.MenuStyle.MODERN_MINIMAL;
            case "minimal" -> PackCoreConfig.MenuStyle.MINIMAL;
            default -> throw new RuntimeException("Unknown menu design ID: " + selectedId);
        };
        MidnightConfig.write(MOD_ID);
    }

    private void applyPerformanceProfile(String selectedId) {
        if (selectedId == null) return;
        PerformanceProfileService.PerformanceProfile profile = switch (selectedId) {
            case "maxfps" -> PerformanceProfileService.PerformanceProfile.PERFORMANCE;
            case "balanced" -> PerformanceProfileService.PerformanceProfile.BALANCED;
            case "quality" -> PerformanceProfileService.PerformanceProfile.QUALITY;
            case "quality_performance_shaders" -> PerformanceProfileService.PerformanceProfile.SHADERS_PERFORMANCE;
            case "quality_quality_shaders" -> PerformanceProfileService.PerformanceProfile.SHADERS_QUALITY;
            default -> throw new RuntimeException("Unknown profile ID: " + selectedId);
        };
        if (!PerformanceProfileService.applyAll(profile)) {
            throw new RuntimeException("One or more integrations failed for profile: " + profile.getDisplayName());
        }
    }

    private void applyTabDesign(String selectedId) {
        if (selectedId == null) return;
        TabDesignManager.TabDesign design = switch (selectedId) {
            case "compact" -> TabDesignManager.TabDesign.COMPACT;
            case "fancy" -> TabDesignManager.TabDesign.FANCY;
            default -> throw new RuntimeException("Unknown tab design ID: " + selectedId);
        };
        if (!TabDesignManager.apply(design)) throw new RuntimeException("Failed to apply tab design: " + selectedId);
    }

    private void applyItemBackground(String selectedId) {
        if (selectedId == null) return;
        ItemBackgroundManager.ItemBackground background = switch (selectedId) {
            case "none" -> ItemBackgroundManager.ItemBackground.NONE;
            case "circle" -> ItemBackgroundManager.ItemBackground.CIRCLE;
            case "square" -> ItemBackgroundManager.ItemBackground.SQUARE;
            default -> throw new RuntimeException("Unknown item background ID: " + selectedId);
        };
        if (!ItemBackgroundManager.apply(background))
            throw new RuntimeException("Failed to apply item background: " + selectedId);
    }

    private void applyStorageDesign(String selectedId) {
        if (selectedId == null) return;
        StorageDesignManager.StorageDesign design = switch (selectedId) {
            case "overlay" -> StorageDesignManager.StorageDesign.OVERLAY;
            case "vanilla" -> StorageDesignManager.StorageDesign.VANILLA;
            default -> throw new RuntimeException("Unknown storage design ID: " + selectedId);
        };
        if (!StorageDesignManager.apply(design))
            throw new RuntimeException("Failed to apply storage design: " + selectedId);
    }

    private void applyDungeonRoutes(String selectedId) {
        if (selectedId == null) return;
        DungeonRoutesManager.DungeonRoutesMode mode = switch (selectedId) {
            case "skyblocker_waypoints" -> DungeonRoutesManager.DungeonRoutesMode.SKYBLOCKER_WAYPOINTS;
            case "secret_routes_mod" -> DungeonRoutesManager.DungeonRoutesMode.SECRET_ROUTES_MOD;
            default -> throw new RuntimeException("Unknown dungeon routes ID: " + selectedId);
        };
        if (!DungeonRoutesManager.apply(mode))
            throw new RuntimeException("Failed to apply dungeon routes mode: " + selectedId);
    }

    private void applySwordBlock(String selectedId) {
        if (selectedId == null) return;
        ScaleMeConfig.enableSwordBlock = selectedId.equals("enabled");
        MidnightConfig.write("scaleme");
    }

    private void applyScamScreener(String selectedId, Set<String> pingOptions) {
        if (selectedId == null && pingOptions.isEmpty()) return;
        String riskLevel = selectedId != null
                ? selectedId
                : ScamScreenerConfigurator.defaultSettings().minimumRiskLevel();
        if (!ScamScreenerConfigurator.apply(riskLevel,
                pingOptions.contains("risk_warning"),
                pingOptions.contains("blacklist_warning"))) {
            throw new RuntimeException("Failed to update ScamScreener settings");
        }
    }

    /**
     * If the user chose a Caxton font pack, adds its pack ID into the wizard's resource pack
     * selection so it is applied together with any other chosen packs by the resource pack step.
     */
    private void applyCaxtonFont(String selectedId) {
        // Always clear any existing Caxton packs from selection first
        CaxtonFontPage.FontOption.all().forEach(opt -> {
            if (opt.packId() != null) state.removeResourcePack(opt.packId());
        });

        if (selectedId == null || CaxtonFontPage.FontOption.NONE_ID.equals(selectedId)) {
            return;
        }

        CaxtonFontPage.FontOption.all().stream()
                .filter(opt -> opt.id().equals(selectedId))
                .findFirst()
                .ifPresent(opt -> {
                    if (opt.packId() != null) {
                        state.addResourcePack(opt.packId());
                    }
                });
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick, int parentWidth, int parentHeight) {
        if (globalErrorMessage == null) return;

        var font = Minecraft.getInstance().font;
        int buttonY = getTotalY() + getHeight() - PADDING - BUTTON_HEIGHT;
        int messageY = buttonY - font.lineHeight - 4;

        graphics.drawCenteredString(font, globalErrorMessage,
                getTotalX() + getWidth() / 2, messageY, GuiColors.ERROR);
    }

    // ── SummaryRowComponent ───────────────────────────────────────────────────

    private static class SummaryRowComponent extends EmptyComponent {

        private final String key;
        private final String label;
        private final Component value;
        private final int valueColor;
        private final boolean isSubRow;

        private RowStatus status;
        private String cachedRightText;
        private int cachedRightColor;
        private int cachedRightWidth;

        SummaryRowComponent(int x, int y, int width, int height,
                            String key, String label, Component value, int valueColor, boolean isSubRow) {
            super(x, y, width, height);
            this.key = key;
            this.label = label;
            this.value = value;
            this.valueColor = valueColor;
            this.isSubRow = isSubRow;
            cacheRightSide(value.getString(), valueColor);
        }

        String getKey() { return key; }

        void setStatus(RowStatus newStatus, String error) {
            status = newStatus;
            if (newStatus == RowStatus.ERROR && error != null && !isSubRow) {
                cacheRightSide("Error: " + error, GuiColors.ERROR);
            } else {
                cacheRightSide(value.getString(), valueColor);
            }
        }

        private void cacheRightSide(String text, int color) {
            cachedRightText = text;
            cachedRightColor = color;
            cachedRightWidth = Minecraft.getInstance().font.width(text);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                           float partialTick, int parentWidth, int parentHeight) {
            int x = getTotalX(), y = getTotalY(), w = getWidth(), h = getHeight();
            int leftInset = isSubRow ? 20 : 0;

            int borderColor = status == RowStatus.SUCCESS ? GuiColors.SUCCESS
                    : status == RowStatus.ERROR ? GuiColors.ERROR
                      : GuiColors.BORDER_IDLE;

            graphics.fill(x + leftInset, y, x + w, y + h, GuiColors.ROW_BACKGROUND);
            GuiHelper.drawBorder(graphics, x + leftInset, y, w - leftInset, h, borderColor);

            var font = Minecraft.getInstance().font;
            int textY = y + (h - font.lineHeight) / 2;
            if (!label.isEmpty()) {
                graphics.drawString(font, label, x + leftInset + 8, textY, GuiColors.NAME_DEFAULT, false);
            }
            graphics.drawString(font, cachedRightText, x + w - cachedRightWidth - 8, textY, cachedRightColor, false);
        }
    }
}
