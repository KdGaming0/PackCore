package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.configpack.BackupEntry;
import com.github.kd_gaming1.packcore.configpack.BackupManager;
import com.github.kd_gaming1.packcore.gui.component.RestoreConfirmOverlay;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class BackupsPage extends BaseConfigPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/BackupsPage");

    private static final int PADDING = 10;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 120;
    private static final int CARD_GAP = 6;
    private static final int CARD_PADDING = 8;
    private static final int RESTORE_BTN_WIDTH = 70;

    private static final int COLOR_LABEL = 0xFFCCCCCC;
    private static final int COLOR_HINT = 0xFF666666;
    private static final int COLOR_CARD_BG = 0x22FFFFFF;
    private static final int COLOR_CARD_BORDER = 0xFF333333;
    private static final int COLOR_TIMESTAMP = 0xFFAAAAAA;

    private static final WidgetSprites BLANK_BUTTON = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_blank_gray_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_blank_gray_button")
    );

    private RestoreConfirmOverlay restoreOverlay;
    private ScrollContainerWidget backupListScroll;
    private CustomButtonWidget createBackupButton;

    public BackupsPage(int width, int height) {
        super(width, height);
    }

    @Override
    public void onEnter() {
        this.clearComponents();

        var font = Minecraft.getInstance().font;

        this.addComponent(new TextComponent(PADDING, PADDING,
                Component.translatable("gui.packcore.backups.heading"), COLOR_LABEL));

        int createBtnX = (getWidth() - BUTTON_WIDTH) / 2;
        int createBtnY = getHeight() - BUTTON_HEIGHT - PADDING;
        createBackupButton = new CustomButtonWidget(createBtnX, createBtnY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.backups.button.create"),
                BLANK_BUTTON,
                btn -> createBackupAndRefresh());
        this.addWidget(createBackupButton);

        restoreOverlay = new RestoreConfirmOverlay(getWidth(), getHeight());
        restoreOverlay.setOnClose(() -> {
            if (backupListScroll != null) backupListScroll.active = true;
            if (createBackupButton != null) createBackupButton.visible = true;
        });

        int listStartY = PADDING + font.lineHeight + PADDING;
        buildBackupList(listStartY);

        // Add overlay last so it renders on top of everything else
        this.addComponent(restoreOverlay);
    }

    private void buildBackupList(int startY) {
        List<BackupEntry> backups;
        try {
            backups = BackupManager.listBackups();
        } catch (IOException e) {
            LOGGER.error("Failed to list backups: {}", e.getMessage());
            this.addComponent(new TextComponent(PADDING, startY,
                    Component.literal("Error reading backups."), 0xFFFF5555));
            return;
        }

        int listHeight = getHeight() - startY - BUTTON_HEIGHT - PADDING * 2;
        int listWidth = getWidth() - PADDING * 3;

        backupListScroll = new ScrollContainerWidget(listWidth, listHeight, CARD_GAP);
        EmptyComponent container = new EmptyComponent(0, 0, listWidth - 8, 0);

        var font = Minecraft.getInstance().font;
        int cardHeight = CARD_PADDING * 2 + font.lineHeight;
        int containerHeight = 0;

        if (backups.isEmpty()) {
            container.addComponent(new TextComponent(0, 0,
                    Component.translatable("gui.packcore.backups.empty"), COLOR_HINT));
            containerHeight = font.lineHeight;
        }

        for (int i = 0; i < backups.size(); i++) {
            BackupEntry backup = backups.get(i);
            int cardY = i * (cardHeight + CARD_GAP);

            container.addComponent(new BackupCard(0, cardY, listWidth - 8, cardHeight, backup));

            container.addWidget(new CustomButtonWidget(
                    listWidth - 8 - RESTORE_BTN_WIDTH - CARD_PADDING,
                    cardY + (cardHeight - BUTTON_HEIGHT) / 2,
                    RESTORE_BTN_WIDTH, BUTTON_HEIGHT,
                    Component.translatable("gui.packcore.backups.button.restore"),
                    BLANK_BUTTON,
                    btn -> {
                        if (backupListScroll != null) backupListScroll.active = false;
                        if (createBackupButton != null) createBackupButton.visible = false;
                        restoreOverlay.show(backup);
                    }));
                    containerHeight = cardY + cardHeight;
        }

        container.setHeight(containerHeight);
        backupListScroll.addComponent(container);

        EmptyComponent scrollWrapper = new EmptyComponent(PADDING, startY, listWidth, listHeight);
        scrollWrapper.addWidget(backupListScroll);
        this.addComponent(scrollWrapper);
    }

    private void createBackupAndRefresh() {
        try {
            BackupManager.createBackup(FabricLoader.getInstance().getGameDir());
            onEnter();
            this.updateParentPosition(getParentX(), getParentY(), getWidth(), getHeight());
        } catch (IOException e) {
            LOGGER.error("Failed to create backup: {}", e.getMessage());
        }
    }

    public boolean handleEsc() {
        if (restoreOverlay != null && restoreOverlay.isVisible()) {
            restoreOverlay.setVisible(false);
            return true;
        }
        return false;
    }

    private static class BackupCard extends AbstractComponent {

        private final BackupEntry backup;

        BackupCard(int x, int y, int width, int height, BackupEntry backup) {
            super(x, y, width, height);
            this.backup = backup;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
            int x = getTotalX();
            int y = getTotalY();
            int w = getWidth();
            int h = getHeight();

            graphics.fill(x, y, x + w, y + h, COLOR_CARD_BG);
            graphics.fill(x, y, x + w, y + 1, COLOR_CARD_BORDER);
            graphics.fill(x, y + h - 1, x + w, y + h, COLOR_CARD_BORDER);
            graphics.fill(x, y, x + 1, y + h, COLOR_CARD_BORDER);
            graphics.fill(x + w - 1, y, x + w, y + h, COLOR_CARD_BORDER);

            var font = Minecraft.getInstance().font;
            String name = backup.zipPath().getFileName().toString();
            int textY = y + (h - font.lineHeight) / 2;
            graphics.drawString(font, name, x + CARD_PADDING, textY, 0xFFCCCCCC, false);

            String time = backup.displayName();
            int timeX = x + w - RESTORE_BTN_WIDTH - CARD_PADDING - font.width(time) - 8;
            graphics.drawString(font, time, timeX, textY, COLOR_TIMESTAMP, false);
        }
    }
}
