package com.github.kd_gaming1.packcore.gui.wizard;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
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

    private static final WidgetSprites BLANK_BUTTON = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_blank_gray_button")
    );

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

        skipButton = new CustomButtonWidget(
                width - BTN_WIDTH * 2 - BTN_GAP * 2, btnY, BTN_WIDTH, BTN_HEIGHT,
                Component.translatable("gui.packcore.wizard.button.skip"),
                BLANK_BUTTON,
                btn -> { if (navigator.hasNext()) navigator.nextPage(); }
        );

        finishButton = new CustomButtonWidget(
                width - BTN_WIDTH - BTN_GAP, btnY, BTN_WIDTH, BTN_HEIGHT,
                Component.translatable("gui.packcore.wizard.button.finish"),
                BLANK_BUTTON,
                btn -> { if (onFinish != null) onFinish.run(); }
        );

        this.addWidget(discord);
        this.addWidget(modrinth);
        this.addWidget(github);
        this.addWidget(skipButton);
        this.addWidget(backButton);
        this.addWidget(continueButton);
        this.addWidget(finishButton);

        refresh();
    }

    /** Syncs button visibility and active state with the current navigator position. */
    public void refresh() {
        boolean hasBack = navigator.hasPrevious();
        boolean isLastPage = navigator.isOnLastPage();
        boolean canSkip = navigator.canSkip() && !isLastPage;

        backButton.visible = hasBack;
        backButton.active = hasBack;

        continueButton.visible = !isLastPage;
        continueButton.active = !isLastPage;

        // Shift skip left when back is hidden to keep it visually consistent
        int skipX = hasBack
                ? getWidth() - BTN_WIDTH * 3 - BTN_GAP * 3
                : getWidth() - BTN_WIDTH * 2 - BTN_GAP * 2;
        skipButton.setX(getTotalX() + skipX);

        skipButton.visible = canSkip;
        skipButton.active = canSkip;

        finishButton.visible = isLastPage;
        finishButton.active = isLastPage;
    }

    /** Registers a callback invoked when the user presses Finish. */
    public void setOnFinish(Runnable callback) {
        this.onFinish = callback;
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