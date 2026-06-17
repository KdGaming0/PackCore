package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.SummaryRow;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.gui.wizard.WizardVersionStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Final wizard step — review the selected settings and apply them.
 *
 * <p>Page-agnostic: it renders {@link WizardStep#summaryRows} for every step in the current run and,
 * on Apply, calls {@link WizardStep#apply} for each, colouring each row by its step's result.
 */
public class ConfirmApplyPage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ConfirmApplyPage");

    private static final Component PAGE_TITLE =
            Component.translatable("gui.packcore.wizard.page.confirm.title");

    private static final int PADDING = 16;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 6;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int SCROLL_BAR_WIDTH = 8;

    private static final int COLOR_VALUE_SELECTED = GuiColors.ACCENT;
    private static final int COLOR_VALUE_SKIPPED = 0xFF555555;
    private static final int COLOR_SUBROW = 0xFF777777;

    private static final WidgetSprites APPLY_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_blank_gray_button")
    );

    private enum RowStatus { SUCCESS, ERROR }

    private final List<WizardStep> steps;
    private final Map<String, RowStatus> rowStatuses = new HashMap<>();
    private final Map<String, String> rowErrors = new HashMap<>();
    private final List<SummaryRowComponent> rows = new ArrayList<>();

    private CustomButtonWidget applyButton;
    private String globalErrorMessage;
    private Runnable onApplySucceeded;
    private boolean applyCompleted;

    public ConfirmApplyPage(WizardState state, WizardNavigator navigator, int width, int height, List<WizardStep> steps) {
        super(state, navigator, width, height);
        this.steps = steps;
    }

    public void setOnApplySucceeded(Runnable callback) { onApplySucceeded = callback; }
    public boolean isApplyCompleted() { return applyCompleted; }

    @Override public Component getTitle() { return PAGE_TITLE; }
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
        rows.clear();
        globalErrorMessage = null;

        if (!applyCompleted) {
            rowStatuses.clear();
            rowErrors.clear();
        }

        var font = Minecraft.getInstance().font;
        int rowWidth = getWidth() - PADDING * 2 - SCROLL_BAR_WIDTH;

        addComponent(new TextComponent(PADDING, PADDING,
                Component.translatable("gui.packcore.wizard.confirm.title"), GuiColors.NAME_DEFAULT));

        int buttonY = getHeight() - PADDING - BUTTON_HEIGHT;
        int scrollTop = PADDING + font.lineHeight + PADDING;
        int scrollHeight = buttonY - BUTTON_GAP - scrollTop;

        EmptyComponent rowContainer = new EmptyComponent(0, 0, rowWidth, 0);
        int currentY = 0;
        for (WizardStep step : steps) {
            for (SummaryRow row : step.summaryRows(state)) {
                int color = row.subRow() ? COLOR_SUBROW
                        : row.skipped() ? COLOR_VALUE_SKIPPED
                        : COLOR_VALUE_SELECTED;
                SummaryRowComponent component = new SummaryRowComponent(
                        0, currentY, rowWidth, ROW_HEIGHT,
                        row.stepId(), row.label(), row.value(), color, row.subRow());
                rows.add(component);
                rowContainer.addComponent(component);
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

        List<WizardStep> applied = new ArrayList<>();
        for (WizardStep step : steps) {
            if (runStep(step)) {
                anyError = true;
            } else {
                applied.add(step);
            }
        }

        // Record each successfully-applied page at its current version so it won't reopen next launch.
        if (!applied.isEmpty()) {
            WizardVersionStore.load().markApplied(applied);
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

    private boolean runStep(WizardStep step) {
        try {
            step.apply(state);
            rowStatuses.put(step.id(), RowStatus.SUCCESS);
            return false;
        } catch (Exception e) {
            rowStatuses.put(step.id(), RowStatus.ERROR);
            rowErrors.put(step.id(), e.getMessage());
            LOGGER.error("Failed to apply \"{}\": {}", step.id(), e.getMessage(), e);
            return true;
        }
    }

    private void refreshRowStatuses() {
        for (SummaryRowComponent row : rows) {
            row.setStatus(rowStatuses.get(row.getKey()), rowErrors.get(row.getKey()));
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        if (globalErrorMessage == null) return;

        var font = Minecraft.getInstance().font;
        int buttonY = getTotalY() + getHeight() - PADDING - BUTTON_HEIGHT;
        int messageY = buttonY - font.lineHeight - 4;

        graphics.centeredText(font, globalErrorMessage,
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
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
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
                graphics.text(font, label, x + leftInset + 8, textY, GuiColors.NAME_DEFAULT, false);
            }
            graphics.text(font, cachedRightText, x + w - cachedRightWidth - 8, textY, cachedRightColor, false);
        }
    }
}
