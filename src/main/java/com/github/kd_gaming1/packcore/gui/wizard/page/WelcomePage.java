package com.github.kd_gaming1.packcore.gui.wizard.page;

import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.configpack.ConfigPackEntry;
import com.github.kd_gaming1.packcore.configpack.ConfigPackScanner;
import com.github.kd_gaming1.packcore.gui.component.*;
import com.github.kd_gaming1.packcore.gui.wizard.BaseWizardPage;
import com.github.kd_gaming1.packcore.gui.wizard.WizardNavigator;
import com.github.kd_gaming1.packcore.gui.wizard.WizardState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Step 0 — Welcome page.
 */
public class WelcomePage extends BaseWizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/WelcomePage");

    private static final Component TITLE = Component.translatable("gui.packcore.wizard.page.welcome.title");
    private static final Component SUBTITLE = Component.translatable("gui.packcore.wizard.page.welcome.subtitle");

    private static final int PADDING = 16;
    private static final int COLUMN_GAP = 14;
    private static final int LABEL_GAP = 5;
    private static final int CARD_GAP = 8;
    private static final int SCROLL_BAR_ROOM = 8;
    private static final int DIVIDER_GAP = 10;
    private static final int HINT_GAP = 6;

    private static final int COLOR_LABEL_PRIMARY = 0xFFCCCCCC;
    private static final int COLOR_HINT = 0xFF777777;
    private static final int COLOR_DIVIDER = 0x44FFFFFF;

    private static final String FALLBACK_MARKDOWN = "*No welcome message found.*";
    private static final Path MARKDOWN_PATH = PackCore.PACKCORE_DIR.resolve("markdown").resolve("welcome.md");

    private ConfigPackEntry activePack;
    private ConfigSwitchOverlay overlay;

    private ScrollContainerWidget leftScroll;
    private ScrollContainerWidget rightScroll;

    private MarkdownComponent markdownComp;

    public WelcomePage(WizardState state, WizardNavigator navigator, int width, int height) {
        super(state, navigator, width, height);
    }

    @Override public Component getTitle() { return TITLE; }
    @Override public boolean validate() { return true; }
    @Override public void onExit() { }

    @Override
    public void onEnter() {
        this.clearComponents();

        int innerWidth = getWidth() - (PADDING * 2);
        int innerHeight = getHeight() - (PADDING * 2);
        int columnWidth = (innerWidth - COLUMN_GAP) / 2;

        List<ConfigPackEntry> packs = scanAvailablePacks();
        this.activePack = findActivePack(packs);

        EmptyComponent leftColumn = new EmptyComponent(PADDING, PADDING, columnWidth, innerHeight);
        EmptyComponent rightColumn = new EmptyComponent(PADDING + columnWidth + COLUMN_GAP, PADDING, columnWidth, innerHeight);

        buildLeftColumn(leftColumn, columnWidth, innerHeight);
        buildRightColumn(rightColumn, columnWidth, innerHeight, packs);

        this.addComponent(leftColumn);
        this.addComponent(rightColumn);

        overlay = new ConfigSwitchOverlay(getWidth(), getHeight());
        overlay.setOnClose(() -> {
            if (leftScroll != null) leftScroll.active = true;
            if (rightScroll != null) rightScroll.active = true;
        });
        this.addComponent(overlay);
    }

    private void buildLeftColumn(EmptyComponent column, int columnWidth, int columnHeight) {
        markdownComp = new MarkdownComponent(         // <-- field, not local var
                0, 0, columnWidth - SCROLL_BAR_ROOM - (PADDING / 2), loadMarkdown()
        );
        leftScroll = new ScrollContainerWidget(columnWidth, columnHeight);
        leftScroll.addComponent(markdownComp);

        EmptyComponent scrollWrapper = new EmptyComponent(0, 0, columnWidth, columnHeight);
        scrollWrapper.addWidget(leftScroll);
        column.addComponent(scrollWrapper);
    }

    private void buildRightColumn(EmptyComponent column, int columnWidth, int columnHeight, List<ConfigPackEntry> packs) {
        var font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight;
        int currentY = 0;

        ConfigStatusCard statusCard = new ConfigStatusCard(0, currentY, columnWidth, activePack);
        column.addComponent(statusCard);
        currentY += statusCard.getHeight() + DIVIDER_GAP;

        final int dividerY = currentY;
        column.addComponent(new EmptyComponent(0, dividerY, columnWidth, 1) {
            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
                graphics.fill(getTotalX(), getTotalY(), getTotalX() + getWidth(), getTotalY() + 1, COLOR_DIVIDER);
            }
        });
        currentY += 1 + DIVIDER_GAP;

        column.addComponent(new TextComponent(0, currentY,
                Component.translatable("gui.packcore.wizard.card.configs.heading"), COLOR_LABEL_PRIMARY));
        currentY += lineHeight + LABEL_GAP;

        column.addComponent(new TextComponent(0, currentY,
                Component.translatable("gui.packcore.wizard.card.configs.hint"), COLOR_HINT));
        currentY += lineHeight + HINT_GAP;

        int scrollHeight = columnHeight - currentY;
        if (scrollHeight <= 0) return;

        if (packs.isEmpty()) {
            column.addComponent(new TextComponent(0, currentY,
                    Component.literal("No configs found in the configs folder."), COLOR_HINT));
            return;
        }

        rightScroll = new ScrollContainerWidget(columnWidth, scrollHeight);
        EmptyComponent listContainer = new EmptyComponent(0, 0, columnWidth - SCROLL_BAR_ROOM, 0);

        int cardY = 0;
        for (ConfigPackEntry pack : packs) {
            boolean isActivePack = isSamePack(activePack, pack);

            ConfigPackCard card = new ConfigPackCard(
                    0, cardY,
                    columnWidth - SCROLL_BAR_ROOM,
                    pack, isActivePack,
                    clickedPack -> {
                        if (isSamePack(activePack, clickedPack)) return;
                        overlay.show(activePack, clickedPack);
                        if (leftScroll != null) leftScroll.active = false;
                        if (rightScroll != null) rightScroll.active = false;
                    }
            );

            listContainer.addComponent(card);
            cardY += card.getHeight() + CARD_GAP;
        }

        listContainer.setHeight(cardY);
        rightScroll.addComponent(listContainer);

        EmptyComponent scrollWrapper = new EmptyComponent(0, currentY, columnWidth, scrollHeight);
        scrollWrapper.addWidget(rightScroll);
        column.addComponent(scrollWrapper);
    }

    /** Returns true if both packs are non-null and refer to the same zip file. */
    private static boolean isSamePack(ConfigPackEntry a, ConfigPackEntry b) {
        return a != null && b != null
                && a.zipPath().getFileName().toString().equals(b.zipPath().getFileName().toString());
    }

    private static ConfigPackEntry findActivePack(List<ConfigPackEntry> packs) {
        String appliedFile = PackCoreConfig.lastAppliedPackFile;
        if (appliedFile == null || appliedFile.isBlank()) return null;

        return packs.stream()
                .filter(pack -> pack.zipPath().getFileName().toString().equals(appliedFile))
                .findFirst()
                .orElse(null);
    }

    private static List<ConfigPackEntry> scanAvailablePacks() {
        try {
            return new ConfigPackScanner().scanFolder(PackCore.PACKCORE_DIR.resolve("configs"));
        } catch (IOException e) {
            LOGGER.error("Failed to scan config packs: {}", e.getMessage());
            return List.of();
        }
    }

    private static String loadMarkdown() {
        if (!Files.exists(MARKDOWN_PATH)) {
            LOGGER.warn("welcome.md not found at {}", MARKDOWN_PATH);
            return FALLBACK_MARKDOWN;
        }
        try {
            return Files.readString(MARKDOWN_PATH);
        } catch (IOException e) {
            LOGGER.error("Failed to read welcome.md: {}", e.getMessage());
            return FALLBACK_MARKDOWN;
        }
    }
}