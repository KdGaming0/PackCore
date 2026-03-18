package com.github.kd_gaming1.packcore.gui.wizard;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Bottom navigation bar for the Welcome Wizard.
 */
public class WizardButtonBar extends EmptyComponent {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int BTN_WIDTH = 90;
    private static final int BTN_HEIGHT = 18;
    private static final int ICON_SIZE = 20;
    private static final int BTN_GAP = 6;

    private static final WidgetSprites CONTINUE_BUTTON = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/continue_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_continue_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_continue_gray_button")
    );

    private static final WidgetSprites PREVIOUS_BUTTON = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/previous_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_previous_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_previous_gray_button")
    );

    private static final WidgetSprites DISCORD_BUTTON = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/discord_icon"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/discord_icon"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/discord_icon")
    );

    private static final WidgetSprites GITHUB_BUTTON = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/github_icon"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/github_icon"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/github_icon")
    );

    private static final WidgetSprites MODRINTH_BUTTON = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/modrinth_icon"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/modrinth_icon"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/modrinth_icon")
    );

    private final WizardNavigator navigator;
    private Runnable onFinish;
    private Runnable onSkipFinish;

    private final ButtonWidget backButton;
    private final ButtonWidget skipButton;
    private final ButtonWidget continueButton;
    private final ButtonWidget finishButton;

    public WizardButtonBar(WizardNavigator navigator, int width, int height) {
        super(0, 0, width, height);
        this.navigator = navigator;

        int btnY = (height - BTN_HEIGHT) / 2;

        ButtonWidget discord = new CustomButtonWidget(
                BTN_GAP, btnY, ICON_SIZE, ICON_SIZE,
                Component.literal(""),
                DISCORD_BUTTON,
                btn -> openUrlSafely(ModpackMetadata.getInstance().getDiscordUrl())
        );

        ButtonWidget modrinth = new CustomButtonWidget(
                BTN_GAP * 2 + ICON_SIZE, btnY, ICON_SIZE, ICON_SIZE,
                Component.literal(""),
                MODRINTH_BUTTON,
                btn -> openUrlSafely(ModpackMetadata.getInstance().getWebsiteUrl())
        );

        ButtonWidget github = new CustomButtonWidget(
                BTN_GAP * 3 + ICON_SIZE + ICON_SIZE, btnY, ICON_SIZE, ICON_SIZE,
                Component.literal(""),
                GITHUB_BUTTON,
                btn -> openUrlSafely(ModpackMetadata.getInstance().getIssueTrackerUrl())
        );

        continueButton = new CustomButtonWidget(
                width - BTN_WIDTH - BTN_GAP, btnY, BTN_WIDTH, BTN_HEIGHT,
                Component.translatable("gui.packcore.wizard.button.continue"),
                CONTINUE_BUTTON,
                btn -> navigator.nextPage()
        );

        backButton = new CustomButtonWidget(
                width - BTN_WIDTH * 2 - BTN_GAP * 2, btnY, BTN_WIDTH, BTN_HEIGHT,
                Component.translatable("gui.packcore.wizard.button.back"),
                PREVIOUS_BUTTON,
                btn -> navigator.previousPage()
        );

        // Skip is only shown on the last page as a "skip applying" escape hatch.
        // It marks the wizard complete without applying settings.
        skipButton = new CustomButtonWidget(
                width - BTN_WIDTH * 2 - BTN_GAP * 2, btnY, BTN_WIDTH, BTN_HEIGHT,
                Component.translatable("gui.packcore.wizard.button.skip"),
                GuiHelper.BLANK_BUTTON_SPRITES,
                btn -> { if (onSkipFinish != null) onSkipFinish.run(); }
        );

        finishButton = new CustomButtonWidget(
                width - BTN_WIDTH - BTN_GAP, btnY, BTN_WIDTH, BTN_HEIGHT,
                Component.translatable("gui.packcore.wizard.button.finish"),
                GuiHelper.BLANK_BUTTON_SPRITES,
                btn -> { if (onFinish != null) onFinish.run(); }
        );
        finishButton.active = false;
        finishButton.setTooltip(Tooltip.create(
                Component.translatable("gui.packcore.wizard.button.finish.locked_tooltip")));

        this.addWidget(discord);
        this.addWidget(modrinth);
        this.addWidget(github);
        this.addWidget(skipButton);
        this.addWidget(backButton);
        this.addWidget(continueButton);
        this.addWidget(finishButton);

        refresh();
    }

    /** Syncs button visibility, position, and active state with the current navigator position. */
    public void refresh() {
        boolean hasBack = navigator.hasPrevious();
        boolean isLastPage = navigator.isOnLastPage();

        backButton.visible = hasBack;
        backButton.active = hasBack;

        continueButton.visible = !isLastPage;
        continueButton.active = !isLastPage;

        skipButton.visible = isLastPage;
        skipButton.active = isLastPage;

        finishButton.visible = isLastPage;

        if (isLastPage) {
            skipButton.setX(getTotalX() + getWidth() - BTN_WIDTH * 3 - BTN_GAP * 3);
            backButton.setX(getTotalX() + getWidth() - BTN_WIDTH * 2 - BTN_GAP * 2);
        } else {
            backButton.setX(getTotalX() + getWidth() - BTN_WIDTH * 2 - BTN_GAP * 2);
        }

        continueButton.setFocused(false);
        backButton.setFocused(false);
    }

    /**
     * Enables or disables the Finish button.
     * Called by the screen once ConfirmApplyPage reports a successful applying.
     */
    public void setFinishEnabled(boolean enabled) {
        finishButton.active = enabled;
        finishButton.setTooltip(enabled ? null : Tooltip.create(
                Component.translatable("gui.packcore.wizard.button.finish.locked_tooltip")));
    }

    /** Registers a callback invoked when the user presses Finish. */
    public void setOnFinish(Runnable callback) {
        this.onFinish = callback;
    }

    /** Registers a callback invoked when the user presses Skip on the last page. */
    public void setOnSkipFinish(Runnable callback) {
        this.onSkipFinish = callback;
    }

    private static void openUrlSafely(String url) {
        if (url == null || url.isBlank()) {
            LOGGER.warn("Attempted to open an empty or null URL, skipping.");
            return;
        }
        try {
            Util.getPlatform().openUri(url);
        } catch (Exception e) {
            LOGGER.warn("Couldn't open uri '{}' ", url, e);
        }
    }
}
