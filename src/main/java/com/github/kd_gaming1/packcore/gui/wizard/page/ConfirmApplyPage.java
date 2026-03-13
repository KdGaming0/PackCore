package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.integration.ItemBackgroundManager;
import com.github.kd_gaming1.packcore.integration.PerformanceProfileService;
import com.github.kd_gaming1.packcore.integration.ResourcePackManager;
import com.github.kd_gaming1.packcore.integration.ScamScreenerConfigurator;
import com.github.kd_gaming1.packcore.integration.StorageDesignManager;
import com.github.kd_gaming1.packcore.integration.TabDesignManager;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/** Final wizard step — review selections and apply them. */
public class ConfirmApplyPage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ConfirmApplyPage");

    private static final Component PAGE_TITLE = Component.translatable("gui.packcore.wizard.page.confirm.title");

    private static final int PADDING = 16;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 6;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int SCROLL_BAR_WIDTH = 8;

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

    private record SummaryEntry(String selectionKey, String statusKey, String label, String translationPrefix) {}

    private static final List<SummaryEntry> SUMMARY_ENTRIES = List.of(
            new SummaryEntry(MainMenuDesignPage.STATE_KEY, MainMenuDesignPage.STATE_KEY, "Main Menu Design", "gui.packcore.wizard.menu_design."),
            new SummaryEntry(PerformancePage.STATE_KEY, PerformancePage.STATE_KEY, "Performance Profile", "gui.packcore.wizard.performance."),
            new SummaryEntry(TabDesignPage.STATE_KEY, TabDesignPage.STATE_KEY, "Tab Design", "gui.packcore.wizard.tab_design."),
            new SummaryEntry(ItemBackgroundPage.STATE_KEY, ItemBackgroundPage.STATE_KEY, "Item Background", "gui.packcore.wizard.item_background."),
            new SummaryEntry(StorageDesignPage.STATE_KEY, StorageDesignPage.STATE_KEY, "Storage Design", "gui.packcore.wizard.storage_design."),
            new SummaryEntry(ScamScreenerPage.ALERT_LEVEL_KEY, ScamScreenerPage.ALERT_LEVEL_KEY, "ScamScreener Alerts", "gui.packcore.wizard.scamscreener.minimum_risk.")
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

    public void setOnApplySucceeded(Runnable callback) {
        onApplySucceeded = callback;
    }

    public ConfirmApplyPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() { return PAGE_TITLE; }
    @Override public boolean validate() { return true; }
    @Override public void onExit() {}

    @Override
    public void onEnter() {
        clearComponents();
        applyButton = null;
        rowStatuses.clear();
        rowErrors.clear();
        summaryRows.clear();
        packRows.clear();
        scamPingRows.clear();
        globalErrorMessage = null;

        var font = Minecraft.getInstance().font;
        int rowWidth = getWidth() - PADDING * 2 - SCROLL_BAR_WIDTH;
        boolean scamLoaded = FabricLoader.getInstance().isModLoaded("scamscreener");

        addComponent(new TextComponent(PADDING, PADDING,
                Component.translatable("gui.packcore.wizard.confirm.title"), GuiColors.NAME_DEFAULT));

        int buttonY = getHeight() - PADDING - BUTTON_HEIGHT;
        boolean showWarning = requiresWorldJoin();
        int warningY = buttonY - (showWarning ? font.lineHeight + BUTTON_GAP : 0);

        if (showWarning) {
            addComponent(new TextComponent(PADDING, warningY,
                    Component.translatable("gui.packcore.wizard.confirm.world_join_required"), GuiColors.WARNING));
        }

        int scrollTop = PADDING + font.lineHeight + PADDING;
        int scrollHeight = (showWarning ? warningY - BUTTON_GAP : buttonY - BUTTON_GAP) - scrollTop;

        EmptyComponent rowContainer = new EmptyComponent(0, 0, rowWidth, 0);
        int currentY = 0;

        for (SummaryEntry entry : SUMMARY_ENTRIES) {
            if (!scamLoaded && entry.selectionKey().equals(ScamScreenerPage.ALERT_LEVEL_KEY)) continue;

            String selectedId = state.getSelection(entry.selectionKey());
            boolean skipped = selectedId == null;

            Component valueText = skipped
                    ? Component.literal("Skipped")
                    : entry.selectionKey().equals(ScamScreenerPage.ALERT_LEVEL_KEY)
                    ? ScamScreenerPage.labelForAlertLevel(selectedId)
                    : Component.translatable(entry.translationPrefix() + selectedId + ".name");

            SummaryRowComponent row = new SummaryRowComponent(
                    0, currentY, rowWidth, ROW_HEIGHT,
                    entry.statusKey(), entry.label(), valueText,
                    skipped ? COLOR_VALUE_SKIPPED : COLOR_VALUE_SELECTED, false);
            summaryRows.add(row);
            rowContainer.addComponent(row);
            currentY += ROW_HEIGHT + ROW_GAP;
        }

        if (scamLoaded) {
            Set<String> pingOptions = state.getMultiSelection(ScamScreenerPage.PING_OPTIONS_KEY);
            SummaryRowComponent pingHeader = new SummaryRowComponent(
                    0, currentY, rowWidth, ROW_HEIGHT, SCAM_PINGS_KEY,
                    "ScamScreener Pings",
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

        Set<String> selectedPacks = state.getSelectedResourcePacks();
        SummaryRowComponent packHeader = new SummaryRowComponent(
                0, currentY, rowWidth, ROW_HEIGHT, RESOURCE_PACKS_KEY,
                "Resource Packs",
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

        rowContainer.setHeight(currentY);
        addComponent(GuiHelper.scrollWrapped(PADDING, scrollTop, getWidth() - PADDING * 2, scrollHeight,
                scroll -> scroll.addComponent(rowContainer)));

        applyButton = new CustomButtonWidget(
                (getWidth() - BUTTON_WIDTH) / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.wizard.confirm.apply_all_configs"),
                APPLY_BUTTON_SPRITES, btn -> applyAll());
        addWidget(applyButton);
    }

    private void applyAll() {
        LOGGER.info("Applying wizard selections...");
        globalErrorMessage = null;
        boolean anyError = false;

        anyError |= runStep(MainMenuDesignPage.STATE_KEY, () -> applyMainMenuDesign(state.getSelection(MainMenuDesignPage.STATE_KEY)));
        anyError |= runStep(PerformancePage.STATE_KEY, () -> applyPerformanceProfile(state.getSelection(PerformancePage.STATE_KEY)));
        anyError |= runStep(TabDesignPage.STATE_KEY, () -> applyTabDesign(state.getSelection(TabDesignPage.STATE_KEY)));
        anyError |= runStep(ItemBackgroundPage.STATE_KEY, () -> applyItemBackground(state.getSelection(ItemBackgroundPage.STATE_KEY)));
        anyError |= runStep(StorageDesignPage.STATE_KEY, () -> applyStorageDesign(state.getSelection(StorageDesignPage.STATE_KEY)));

        if (FabricLoader.getInstance().isModLoaded("scamscreener")) {
            anyError |= runStep(ScamScreenerPage.ALERT_LEVEL_KEY, () -> applyScamScreener(
                    state.getSelection(ScamScreenerPage.ALERT_LEVEL_KEY),
                    state.getMultiSelection(ScamScreenerPage.PING_OPTIONS_KEY)));
        }

        anyError |= runStep(RESOURCE_PACKS_KEY, () -> applyResourcePacks(state.getSelectedResourcePacks()));

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
        for (SummaryRowComponent row : scamPingRows) {
            row.setStatus(scamStatus, scamError);
        }
        RowStatus packStatus = rowStatuses.get(RESOURCE_PACKS_KEY);
        String packError = rowErrors.get(RESOURCE_PACKS_KEY);
        for (SummaryRowComponent row : packRows) {
            row.setStatus(packStatus, packError);
        }
    }

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
            case "max_fps" -> PerformanceProfileService.PerformanceProfile.PERFORMANCE;
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
        if (!ItemBackgroundManager.apply(background)) throw new RuntimeException("Failed to apply item background: " + selectedId);
    }

    private void applyStorageDesign(String selectedId) {
        if (selectedId == null) return;
        StorageDesignManager.StorageDesign design = switch (selectedId) {
            case "overlay" -> StorageDesignManager.StorageDesign.OVERLAY;
            case "vanilla" -> StorageDesignManager.StorageDesign.VANILLA;
            default -> throw new RuntimeException("Unknown storage design ID: " + selectedId);
        };
        if (!StorageDesignManager.apply(design)) throw new RuntimeException("Failed to apply storage design: " + selectedId);
    }

    private void applyScamScreener(String selectedId, Set<String> pingOptions) {
        if (selectedId == null && pingOptions.isEmpty()) return;
        String riskLevel = selectedId != null ? selectedId : ScamScreenerConfigurator.defaultSettings().minimumRiskLevel();
        if (!ScamScreenerConfigurator.apply(riskLevel,
                pingOptions.contains("risk_warning"),
                pingOptions.contains("blacklist_warning"))) {
            throw new RuntimeException("Failed to update ScamScreener settings");
        }
    }

    private void applyResourcePacks(Set<String> packIds) {
        if (packIds.isEmpty()) return;
        ResourcePackManager.apply(packIds);
    }

    private boolean requiresWorldJoin() {
        return (state.getSelection(TabDesignPage.STATE_KEY) != null && FabricLoader.getInstance().isModLoaded("skyhanni"))
                || (state.getSelection(StorageDesignPage.STATE_KEY) != null && FabricLoader.getInstance().isModLoaded("firmament"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        if (globalErrorMessage == null) return;
        var font = Minecraft.getInstance().font;
        int errorY = getTotalY() + getHeight() - PADDING - BUTTON_HEIGHT + BUTTON_HEIGHT + 4;
        graphics.drawCenteredString(font, globalErrorMessage, getTotalX() + getWidth() / 2, errorY, GuiColors.ERROR);
    }

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
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
            int x = getTotalX();
            int y = getTotalY();
            int w = getWidth();
            int h = getHeight();
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