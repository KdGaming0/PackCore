package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.gui.component.MarkdownComponent;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import com.github.kd_gaming1.packcore.gui.util.ToastHelper;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class SupportWelcomePage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/SupportWelcomePage");

    private static final Component TITLE = Component.translatable("gui.packcore.wizard.page.support_welcome.title");

    private static final String KOFI_URL = "https://ko-fi.com/kdgaming1";
    private static final String HYTALE_URL = "https://store.hytale.com/";
    private static final String BISECT_URL = "https://www.bisecthosting.com/SBE";
    private static final String PROTON_VPN_URL = "https://pr.tn/ref/ED4AR2XY";

    private static final String HYTALE_CODE = "KD1";
    private static final String BISECT_CODE = "SBE";

    private static final int PADDING = 16;
    private static final int COLUMN_GAP = 14;
    private static final int SCROLL_BAR_ROOM = 8;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;

    private static final WidgetSprites MAIN_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/blank_red_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_red_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_red_button")
    );

    private static final String FALLBACK_MARKDOWN = "**Thanks for installing SkyBlock Enhanced.**\n\n"
            + "This project represents close to 1,000 hours of work — making mods, checking for compatibility, "
            + "and creating default configs so you can jump straight in and play.\n\n"
            + "I've also spent hundreds of hours providing support to users through our Discord server.\n\n"
            + "If you'd like to help keep this going, the buttons on the right have a few ways to show your support.";
    private static final Path MARKDOWN_PATH = PackCore.PACKCORE_DIR.resolve("markdown").resolve("welcome.md");

    public SupportWelcomePage(WizardState state, WizardNavigator navigator, int width, int height) {
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
        this.clearComponents();

        int innerWidth = getWidth() - (PADDING * 2);
        int innerHeight = getHeight() - (PADDING * 2);
        int columnWidth = (innerWidth - COLUMN_GAP) / 2;

        EmptyComponent leftColumn = new EmptyComponent(PADDING, PADDING, columnWidth, innerHeight);
        EmptyComponent rightColumn = new EmptyComponent(PADDING + columnWidth + COLUMN_GAP, PADDING, columnWidth, innerHeight);

        buildLeftColumn(leftColumn, columnWidth, innerHeight);
        buildRightColumn(rightColumn, columnWidth, innerHeight);

        this.addComponent(leftColumn);
        this.addComponent(rightColumn);
    }

    private void buildLeftColumn(EmptyComponent column, int columnWidth, int columnHeight) {
        MarkdownComponent markdownComp = new MarkdownComponent(
                0, 0, columnWidth - SCROLL_BAR_ROOM - (PADDING / 2),
                GuiHelper.loadMarkdown(MARKDOWN_PATH, FALLBACK_MARKDOWN, LOGGER)
        );
        ScrollContainerWidget scroll = new ScrollContainerWidget(columnWidth, columnHeight);
        scroll.addComponent(markdownComp);

        EmptyComponent scrollWrapper = new EmptyComponent(0, 0, columnWidth, columnHeight);
        scrollWrapper.addWidget(scroll);
        column.addComponent(scrollWrapper);
    }

    private void buildRightColumn(EmptyComponent column, int columnWidth, int columnHeight) {
        int currentY = 0;
        int buttonWidth = columnWidth;

        currentY = addButton(column, currentY, buttonWidth,
                "gui.packcore.wizard.button.ko_fi",
                "gui.packcore.wizard.button.ko_fi.tooltip",
                () -> openUrl(KOFI_URL));
        currentY += BUTTON_GAP;
        currentY = addButton(column, currentY, buttonWidth,
                "gui.packcore.wizard.button.hytale",
                "gui.packcore.wizard.button.hytale.tooltip",
                () -> {
                    openUrl(HYTALE_URL);
                    copyToClipboard(HYTALE_CODE);
                });
        currentY += BUTTON_GAP;
        currentY = addButton(column, currentY, buttonWidth,
                "gui.packcore.wizard.button.bisect_hosting",
                "gui.packcore.wizard.button.bisect_hosting.tooltip",
                () -> {
                    openUrl(BISECT_URL);
                    copyToClipboard(BISECT_CODE);
                });
        currentY += BUTTON_GAP;
        currentY = addButton(column, currentY, buttonWidth,
                "gui.packcore.wizard.button.proton_vpn",
                "gui.packcore.wizard.button.proton_vpn.tooltip",
                () -> openUrl(PROTON_VPN_URL));

        String discordUrl = ModpackMetadata.getInstance().getDiscordUrl();
        if (discordUrl != null && !discordUrl.isBlank()) {
            currentY += BUTTON_GAP * 2;
            addButton(column, currentY, buttonWidth,
                    "gui.packcore.wizard.button.get_help",
                    "gui.packcore.wizard.button.get_help.tooltip",
                    () -> openUrl(discordUrl));
        }
    }

    private int addButton(EmptyComponent column, int y, int width, String labelKey, String tooltipKey, Runnable action) {
        ButtonWidget button = new CustomButtonWidget(
                0, y, width, BUTTON_HEIGHT,
                Component.translatable(labelKey),
                MAIN_BUTTON_SPRITES,
                btn -> action.run()
        );
        button.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        column.addWidget(button);
        return y + BUTTON_HEIGHT;
    }

    private static void openUrl(String url) {
        if (url == null || url.isBlank()) {
            LOGGER.warn("Attempted to open an empty or null URL, skipping.");
            return;
        }
        try {
            Util.getPlatform().openUri(url);
        } catch (Exception e) {
            LOGGER.warn("Couldn't open uri '{}'", url, e);
        }
    }

    private static void copyToClipboard(String code) {
        try {
            Minecraft.getInstance().keyboardHandler.setClipboard(code);
            ToastHelper.show(
                    Component.translatable("gui.packcore.toast.code_copied.title"),
                    Component.translatable("gui.packcore.toast.code_copied.message", code)
            );
        } catch (Exception e) {
            LOGGER.warn("Failed to copy code '{}' to clipboard", code, e);
        }
    }
}
