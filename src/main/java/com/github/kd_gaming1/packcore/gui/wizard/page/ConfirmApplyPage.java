package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.integration.ItemBackgroundManager;
import com.github.kd_gaming1.packcore.integration.PerformanceProfileService;
import com.github.kd_gaming1.packcore.integration.ResourcePackManager;
import com.github.kd_gaming1.packcore.integration.TabDesignManager;
import com.github.kd_gaming1.packcore.integration.StorageDesignManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * The final wizard step — shows a summary of all selections and applies them.
 * <p>
 * Each apply method is a stub. Replace the LOGGER.info body with the real
 * implementation one at a time; the rest of the page doesn't need to change.
 */
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

    private static final int COLOR_HEADING = 0xFFCCCCCC;
    private static final int COLOR_ROW_BACKGROUND = 0x22FFFFFF;
    private static final int COLOR_BORDER_IDLE = 0x44FFFFFF;
    private static final int COLOR_BORDER_SUCCESS = 0xFF4CAF50;
    private static final int COLOR_BORDER_ERROR = 0xFFFF5555;
    private static final int COLOR_LABEL = 0xFFCCCCCC;
    private static final int COLOR_VALUE_SELECTED = 0xFF2196F3;
    private static final int COLOR_VALUE_SKIPPED = 0xFF555555;
    private static final int COLOR_STATUS_ERROR = 0xFFFF5555;
    private static final int COLOR_WARNING = 0xFFFFAA00;
    private static final int COLOR_PACK_NAME = 0xFF777777;

    private static final WidgetSprites APPLY_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_blank_gray_button")
    );

    private record SummaryEntry(String stateKey, String label, String translationPrefix) {}

    private static final List<SummaryEntry> SUMMARY_ENTRIES = List.of(
            new SummaryEntry(MainMenuDesignPage.STATE_KEY, "Main Menu Design",    "gui.packcore.wizard.menu_design."),
            new SummaryEntry(PerformancePage.STATE_KEY,    "Performance Profile", "gui.packcore.wizard.performance."),
            new SummaryEntry(TabDesignPage.STATE_KEY,      "Tab Design",          "gui.packcore.wizard.tab_design."),
            new SummaryEntry(ItemBackgroundPage.STATE_KEY, "Item Background",     "gui.packcore.wizard.item_background."),
            new SummaryEntry(StorageDesignPage.STATE_KEY,  "Storage Design",      "gui.packcore.wizard.storage_design.")
    );

    private static final String RESOURCE_PACKS_KEY = "resourcePacks";

    private enum RowStatus { SUCCESS, ERROR }

    /** Per-row apply result. Null = not yet applied. */
    private final Map<String, RowStatus> rowStatuses = new HashMap<>();
    private final Map<String, String> rowErrors = new HashMap<>();

    // Hold references so we can push status updates into them after applying
    private final List<SummaryRowComponent> summaryRows = new ArrayList<>();
    private final List<SummaryRowComponent> packRows = new ArrayList<>();

    private CustomButtonWidget applyButton = null;
    private String globalErrorMessage = null;
    private Runnable onApplySucceeded = null;

    /** Called by the screen after wiring to unlock the Finish button on success. */
    public void setOnApplySucceeded(Runnable callback) {
        this.onApplySucceeded = callback;
    }

    public ConfirmApplyPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() { return PAGE_TITLE; }
    @Override public boolean validate() { return true; }
    @Override public void onExit() { }

    @Override
    public void onEnter() {
        this.clearComponents();
        applyButton = null;
        rowStatuses.clear();
        rowErrors.clear();
        summaryRows.clear();
        packRows.clear();
        globalErrorMessage = null;

        var font = Minecraft.getInstance().font;
        int availableWidth = getWidth() - PADDING * 2;
        int rowWidth = availableWidth - SCROLL_BAR_WIDTH;

        // Heading
        int headingY = PADDING;
        int headingHeight = font.lineHeight + PADDING;
        addComponent(new TextComponent(PADDING, headingY, Component.translatable("gui.packcore.wizard.confirm.title"), COLOR_HEADING));

        // Work upward from the bottom to place button and warning
        int buttonY = getHeight() - PADDING - BUTTON_HEIGHT;

        boolean showWarning = requiresWorldJoin();
        int warningHeight = showWarning ? font.lineHeight + BUTTON_GAP : 0;
        int warningY = buttonY - warningHeight;

        if (showWarning) {
            addComponent(new TextComponent(PADDING, warningY, Component.translatable("gui.packcore.wizard.confirm.world_join_required"), COLOR_WARNING));
        }

        // Scroll area fills between heading and warning/button
        int scrollTop = headingY + headingHeight;
        int scrollBottom = showWarning ? warningY - BUTTON_GAP : buttonY - BUTTON_GAP;
        int scrollHeight = scrollBottom - scrollTop;

        // Build row container
        EmptyComponent rowContainer = new EmptyComponent(0, 0, rowWidth, 0);
        int currentY = 0;

        for (SummaryEntry entry : SUMMARY_ENTRIES) {
            String selectedId = state.getSelection(entry.stateKey());
            boolean skipped = selectedId == null;

            Component valueText = skipped
                    ? Component.literal("Skipped")
                    : Component.translatable(entry.translationPrefix() + selectedId + ".name");
            int valueColor = skipped ? COLOR_VALUE_SKIPPED : COLOR_VALUE_SELECTED;

            SummaryRowComponent row = new SummaryRowComponent(
                    0, currentY, rowWidth, ROW_HEIGHT,
                    entry.stateKey(), entry.label(), valueText, valueColor, false
            );
            summaryRows.add(row);
            rowContainer.addComponent(row);
            currentY += ROW_HEIGHT + ROW_GAP;
        }

        Set<String> selectedPacks = state.getSelectedResourcePacks();

        SummaryRowComponent packHeaderRow = new SummaryRowComponent(
                0, currentY, rowWidth, ROW_HEIGHT,
                RESOURCE_PACKS_KEY,
                "Resource Packs",
                selectedPacks.isEmpty()
                        ? Component.literal("None selected")
                        : Component.literal(selectedPacks.size() + " selected"),
                selectedPacks.isEmpty() ? COLOR_VALUE_SKIPPED : COLOR_VALUE_SELECTED,
                false
        );
        packRows.add(packHeaderRow);
        rowContainer.addComponent(packHeaderRow);
        currentY += ROW_HEIGHT + ROW_GAP;

        for (String packId : selectedPacks) {
            SummaryRowComponent packRow = new SummaryRowComponent(
                    0, currentY, rowWidth, ROW_HEIGHT,
                    RESOURCE_PACKS_KEY + ":" + packId,
                    "",
                    Component.literal(packId),
                    COLOR_PACK_NAME,
                    true
            );
            packRows.add(packRow);
            rowContainer.addComponent(packRow);
            currentY += ROW_HEIGHT + ROW_GAP;
        }

        rowContainer.setHeight(currentY);

        addComponent(GuiHelper.scrollWrapped(PADDING, scrollTop, availableWidth, scrollHeight,
                scroll -> scroll.addComponent(rowContainer)));

        // Apply button
        applyButton = new CustomButtonWidget(
                (getWidth() - BUTTON_WIDTH) / 2, buttonY,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.wizard.confirm.apply_all_configs"),
                APPLY_BUTTON_SPRITES,
                btn -> applyAll()
        );
        addWidget(applyButton);
        applyButton.active = true;

    }

    // --- Apply logic ---

    private void applyAll() {
        LOGGER.info("Applying wizard selections...");
        globalErrorMessage = null;
        boolean anyError = false;

        anyError |= runStep(MainMenuDesignPage.STATE_KEY,
                () -> applyMainMenuDesign(state.getSelection(MainMenuDesignPage.STATE_KEY)));

        anyError |= runStep(PerformancePage.STATE_KEY,
                () -> applyPerformanceProfile(state.getSelection(PerformancePage.STATE_KEY)));

        anyError |= runStep(TabDesignPage.STATE_KEY,
                () -> applyTabDesign(state.getSelection(TabDesignPage.STATE_KEY)));

        anyError |= runStep(ItemBackgroundPage.STATE_KEY,
                () -> applyItemBackground(state.getSelection(ItemBackgroundPage.STATE_KEY)));

        anyError |= runStep(StorageDesignPage.STATE_KEY,
                () -> applyStorageDesign(state.getSelection(StorageDesignPage.STATE_KEY)));

        anyError |= runStep(RESOURCE_PACKS_KEY,
                () -> applyResourcePacks(state.getSelectedResourcePacks()));

        if (!anyError) {
            applyButton.active = false;
            if (onApplySucceeded != null) onApplySucceeded.run();
            LOGGER.info("All settings applied successfully.");
        } else {
            globalErrorMessage = "Some settings failed to apply — see highlighted rows and check logs.";
            LOGGER.warn("One or more settings failed to apply.");
        }

        refreshRowStatuses();
    }

    /**
     * Runs a single applied step, recording success or failure against the given key.
     */
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

    /** Pushes the current rowStatuses into each row component so borders update. */
    private void refreshRowStatuses() {
        for (SummaryRowComponent row : summaryRows) {
            row.setStatus(rowStatuses.get(row.getKey()), rowErrors.get(row.getKey()));
        }
        // All pack rows share the same status as the resource pack step
        RowStatus packStatus = rowStatuses.get(RESOURCE_PACKS_KEY);
        String packError = rowErrors.get(RESOURCE_PACKS_KEY);
        for (SummaryRowComponent row : packRows) {
            row.setStatus(packStatus, packError);
        }
    }

    // --- Stubs — replace each body with the real implementation ---

    private void applyMainMenuDesign(String selectedId) {
        if (selectedId == null) { LOGGER.info("mainMenuDesign: skipped"); return; }

        PackCoreConfig.menuStyle = switch (selectedId) {
            case "modern"         -> PackCoreConfig.MenuStyle.MODERN;
            case "modern_minimal" -> PackCoreConfig.MenuStyle.MODERN_MINIMAL;
            case "minimal"        -> PackCoreConfig.MenuStyle.MINIMAL;
            default -> throw new RuntimeException("Unknown menu design ID: " + selectedId);
        };
        MidnightConfig.write(MOD_ID);
        LOGGER.info("Applied menu design: {}", selectedId);
    }

    private void applyPerformanceProfile(String selectedId) {
        if (selectedId == null) {
            LOGGER.info("Performance profile: skipped");
            return;
        }

        // Map UI option IDs → PerformanceProfile enum IDs
        PerformanceProfileService.PerformanceProfile profile = switch (selectedId) {
            case "max_fps" -> PerformanceProfileService.PerformanceProfile.PERFORMANCE;
            case "balanced" -> PerformanceProfileService.PerformanceProfile.BALANCED;
            case "quality" -> PerformanceProfileService.PerformanceProfile.QUALITY;
            case "quality_performance_shaders" -> PerformanceProfileService.PerformanceProfile.SHADERS_PERFORMANCE;
            case "quality_quality_shaders" -> PerformanceProfileService.PerformanceProfile.SHADERS_QUALITY;
            default -> throw new RuntimeException("Unknown profile ID from UI: " + selectedId);
        };

        LOGGER.info("Applying performance profile: {} -> {}", selectedId, profile.getDisplayName());

        boolean success = PerformanceProfileService.applyAll(profile);
        if (!success) {
            throw new RuntimeException("One or more integrations failed for profile: " + profile.getDisplayName());
        }
    }

    private void applyTabDesign(String selectedId) {
        if (selectedId == null) {
            LOGGER.info("Tab design: skipped");
            return;
        }

        TabDesignManager.TabDesign design = switch (selectedId) {
            case "compact" -> TabDesignManager.TabDesign.COMPACT;
            case "fancy" -> TabDesignManager.TabDesign.FANCY;
            default -> throw new RuntimeException("Unknown tab design ID: " + selectedId);
        };

        boolean success = TabDesignManager.apply(design);
        if (!success) {
            throw new RuntimeException("Failed to apply tab design: " + selectedId);
        }
    }

    private void applyItemBackground(String selectedId) {
        if (selectedId == null) {
            LOGGER.info("Item background_old: skipped");
            return;
        }

        ItemBackgroundManager.ItemBackground background = switch (selectedId) {
            case "none" -> ItemBackgroundManager.ItemBackground.NONE;
            case "circle" -> ItemBackgroundManager.ItemBackground.CIRCLE;
            case "square" -> ItemBackgroundManager.ItemBackground.SQUARE;
            default -> throw new RuntimeException("Unknown item background_old ID: " + selectedId);
        };

        boolean success = ItemBackgroundManager.apply(background);
        if (!success) {
            throw new RuntimeException("Failed to apply item background_old: " + selectedId);
        }
    }

    private void applyStorageDesign(String selectedId) {
        if (selectedId == null) {
            LOGGER.info("Storage design: skipped");
            return;
        }

        StorageDesignManager.StorageDesign design = switch (selectedId) {
            case "overlay" -> StorageDesignManager.StorageDesign.OVERLAY;
            case "vanilla" -> StorageDesignManager.StorageDesign.VANILLA;
            default -> throw new RuntimeException("Unknown storage design ID: " + selectedId);
        };

        boolean success = StorageDesignManager.apply(design);
        if (!success) {
            throw new RuntimeException("Failed to apply storage design: " + selectedId);
        }
    }

    private void applyResourcePacks(Set<String> packIds) {
        if (packIds.isEmpty()) {
            LOGGER.info("Resource packs: none selected");
            return;
        }

        LOGGER.info("Resource packs: applying {} packs: {}", packIds.size(), packIds);
        ResourcePackManager.apply(packIds);
    }

    private boolean requiresWorldJoin() {
        String tabDesign = state.getSelection(TabDesignPage.STATE_KEY);
        String storageDesign = state.getSelection(StorageDesignPage.STATE_KEY);

        boolean skyhanniPending = tabDesign != null && FabricLoader.getInstance().isModLoaded("skyhanni");
        boolean firmamentPending = storageDesign != null && FabricLoader.getInstance().isModLoaded("firmament");

        return skyhanniPending || firmamentPending;
    }

    // --- Draw the global error message below the button ---

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        if (globalErrorMessage == null) return;

        var font = Minecraft.getInstance().font;
        int statusY = getTotalY() + getHeight() - PADDING - BUTTON_HEIGHT + BUTTON_HEIGHT + 4;
        int centerX = getTotalX() + getWidth() / 2;

        graphics.drawCenteredString(font, globalErrorMessage, centerX, statusY, COLOR_STATUS_ERROR);
    }

    // --- Inner component for one summary row ---

    private static class SummaryRowComponent extends EmptyComponent {

        private final String key;
        private final String label;
        private final Component value;
        private final int valueColor;
        private final boolean isSubRow;

        private RowStatus status = null;
        private String errorMessage = null;

        SummaryRowComponent(int x, int y, int width, int height,
                            String key, String label, Component value, int valueColor, boolean isSubRow) {
            super(x, y, width, height);
            this.key = key;
            this.label = label;
            this.value = value;
            this.valueColor = valueColor;
            this.isSubRow = isSubRow;
        }

        String getKey() { return key; }

        void setStatus(RowStatus newStatus, String error) {
            this.status = newStatus;
            this.errorMessage = error;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
            int x = getTotalX();
            int y = getTotalY();
            int w = getWidth();
            int h = getHeight();

            int leftInset = isSubRow ? 20 : 0;

            int borderColor = status == RowStatus.SUCCESS ? COLOR_BORDER_SUCCESS
                    : status == RowStatus.ERROR ? COLOR_BORDER_ERROR
                    : COLOR_BORDER_IDLE;

            graphics.fill(x + leftInset, y, x + w, y + h, COLOR_ROW_BACKGROUND);
            GuiHelper.drawBorder(graphics, x + leftInset, y, w - leftInset, h, borderColor);

            var font = Minecraft.getInstance().font;
            int textY = y + (h - font.lineHeight) / 2;

            if (!label.isEmpty()) {
                graphics.drawString(font, label, x + leftInset + 8, textY, COLOR_LABEL, false);
            }

            // On error, show the error message in place of the value (non-sub-rows only)
            if (status == RowStatus.ERROR && errorMessage != null && !isSubRow) {
                String errText = "Error: " + errorMessage;
                graphics.drawString(font, errText, x + w - font.width(errText) - 8, textY, COLOR_BORDER_ERROR, false);
            } else {
                String valueStr = value.getString();
                graphics.drawString(font, valueStr, x + w - font.width(valueStr) - 8, textY, valueColor, false);
            }
        }
    }
}