package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.text.TextAlign;
import com.daqem.uilib.gui.component.text.multiline.MultiLineTextComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Wizard page shown to users upgrading from pre-5.0, offering an optional full config reapply.
 */
public class ConfigPackReapplyPage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/ConfigPackReapplyPage");

    private static final Component TITLE =
            Component.translatable("gui.packcore.wizard.page.config_reapply.title");

    private static final int PADDING = 16;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 12;
    private static final int TEXT_GAP = 10;

    private static final WidgetSprites PRIMARY_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/blank_red_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_red_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_red_button")
    );

    private static final WidgetSprites SECONDARY_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_blank_gray_button")
    );

    public ConfigPackReapplyPage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public void onExit() {
    }

    @Override
    public void onEnter() {
        this.clear();

        int contentWidth = Math.min(420, getWidth() - PADDING * 2);
        int centerX = getWidth() / 2; // With TextAlign.CENTER, the insertion point is the exact middle
        int currentY = PADDING;

        // Add dynamically wrapped and centered text components
        currentY = addCenteredWrappedText(
                Component.translatable("gui.packcore.wizard.page.config_reapply.line1"),
                centerX, currentY, contentWidth, GuiColors.TEXT_PRIMARY
        );

        currentY = addCenteredWrappedText(
                Component.translatable("gui.packcore.wizard.page.config_reapply.line2"),
                centerX, currentY, contentWidth, GuiColors.TEXT_SECONDARY
        );

        currentY = addCenteredWrappedText(
                Component.translatable("gui.packcore.wizard.page.config_reapply.question"),
                centerX, currentY, contentWidth, GuiColors.TEXT_PRIMARY
        );

        currentY = addCenteredWrappedText(
                Component.translatable("gui.packcore.wizard.page.config_reapply.recommendation"),
                centerX, currentY, contentWidth, GuiColors.TEXT_SECONDARY
        );

        // Extra gap before buttons
        currentY += BUTTON_GAP;

        // Buttons
        int buttonsX = (getWidth() - BUTTON_WIDTH) / 2;

        ButtonWidget reapplyButton = new CustomButtonWidget(
                buttonsX, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.wizard.button.config_reapply.reapply"),
                PRIMARY_BUTTON_SPRITES,
                btn -> queueReapplyAndRestart()
        );
        addWidget(reapplyButton);
        currentY += BUTTON_HEIGHT + BUTTON_GAP;

        ButtonWidget keepButton = new CustomButtonWidget(
                buttonsX, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.wizard.button.config_reapply.keep"),
                SECONDARY_BUTTON_SPRITES,
                btn -> navigator.nextPage()
        );
        addWidget(keepButton);
        currentY += BUTTON_HEIGHT + BUTTON_GAP;

        // Footer
        addCenteredWrappedText(
                Component.translatable("gui.packcore.wizard.page.config_reapply.footer"),
                centerX, currentY, contentWidth, GuiColors.TEXT_HINT
        );
    }

    /**
     * Helper to create, align, and position a MultiLineTextComponent.
     * Returns the dynamic Y coordinate for the next element based on the generated text height.
     */
    private int addCenteredWrappedText(Component text, int centerX, int currentY, int maxWidth, int color) {
        MultiLineTextComponent textComp = new MultiLineTextComponent(centerX, currentY, maxWidth, text, color);
        textComp.setTextAlign(TextAlign.CENTER);
        addComponent(textComp);

        // Use the component's dynamic height to space the next item correctly
        return currentY + textComp.getHeight() + TEXT_GAP;
    }

    private void queueReapplyAndRestart() {
        String packFile = PackCoreConfig.lastAppliedPackFile;
        if (packFile == null || packFile.isBlank()) {
            LOGGER.warn("Cannot queue config reapply: no lastAppliedPackFile recorded.");
            return;
        }

        PackCoreConfig.pendingConfigPack = packFile;
        PackCoreConfig.pendingConfigPackFiles = "";
        MidnightConfig.write(MOD_ID);
        Minecraft.getInstance().stop();
    }
}