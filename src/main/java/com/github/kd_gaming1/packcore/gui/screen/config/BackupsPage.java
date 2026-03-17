package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.github.kd_gaming1.packcore.configpack.BackupEntry;
import com.github.kd_gaming1.packcore.configpack.BackupManager;
import com.github.kd_gaming1.packcore.gui.component.RestoreConfirmOverlay;
import com.github.kd_gaming1.packcore.gui.util.GuiColors;
import com.github.kd_gaming1.packcore.gui.util.GuiHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BackupsPage extends BaseConfigPage {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/BackupsPage");

    private static final Executor IO_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private static final int PADDING = 10;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 120;
    private static final int CARD_GAP = 6;
    private static final int CARD_PADDING = 8;
    private static final int RESTORE_BTN_WIDTH = 70;

    private RestoreConfirmOverlay restoreOverlay;
    private ScrollContainerWidget backupListScroll;
    private CustomButtonWidget createBackupButton;

    public BackupsPage(int width, int height) {
        super(width, height);
    }

    @Override
    public void onEnter() {
        clearComponents();

        var font = Minecraft.getInstance().font;

        addComponent(new TextComponent(PADDING, PADDING,
                Component.translatable("gui.packcore.backups.heading"), GuiColors.NAME_DEFAULT));

        int createBtnX = (getWidth() - BUTTON_WIDTH) / 2;
        int createBtnY = getHeight() - BUTTON_HEIGHT - PADDING;
        createBackupButton = new CustomButtonWidget(createBtnX, createBtnY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.backups.button.create"),
                GuiHelper.BLANK_BUTTON_SPRITES,
                btn -> createBackupAsync());
        addWidget(createBackupButton);

        restoreOverlay = new RestoreConfirmOverlay(getWidth(), getHeight());
        restoreOverlay.setOnClose(() -> {
            if (backupListScroll != null) backupListScroll.active = true;
            if (createBackupButton != null) createBackupButton.visible = true;
        });

        buildBackupList(PADDING + font.lineHeight + PADDING);
        addComponent(restoreOverlay);
    }

    private void buildBackupList(int startY) {
        List<BackupEntry> backups;
        try {
            backups = BackupManager.listBackups();
        } catch (IOException e) {
            LOGGER.error("Failed to list backups: {}", e.getMessage());
            addComponent(new TextComponent(PADDING, startY,
                    Component.literal("Error reading backups."), GuiColors.ERROR));
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
                    Component.translatable("gui.packcore.backups.empty"), GuiColors.TEXT_HINT));
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
                    GuiHelper.BLANK_BUTTON_SPRITES,
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
        addComponent(scrollWrapper);
    }

    /** Disables the button immediately, runs the backup on a background thread, then refreshes the page. */
    private void createBackupAsync() {
        createBackupButton.active = false;

        CompletableFuture.runAsync(
                () -> {
                    try {
                        BackupManager.createBackup(FabricLoader.getInstance().getGameDir());
                    } catch (IOException e) {
                        LOGGER.error("Failed to create backup: {}", e.getMessage());
                    }
                },
                IO_EXECUTOR
        ).thenRun(() -> Minecraft.getInstance().execute(() -> {
            onEnter();
            updateParentPosition(getParentX(), getParentY(), getWidth(), getHeight());
        }));
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

            graphics.fill(x, y, x + w, y + h, GuiColors.ROW_BACKGROUND);
            GuiHelper.drawBorder(graphics, x, y, w, h, GuiColors.BORDER_IDLE);

            var font = Minecraft.getInstance().font;
            int textY = y + (h - font.lineHeight) / 2;

            graphics.drawString(font, backup.zipPath().getFileName().toString(),
                    x + CARD_PADDING, textY, GuiColors.NAME_DEFAULT, false);

            String timestamp = backup.displayName();
            int timestampX = x + w - RESTORE_BTN_WIDTH - CARD_PADDING - font.width(timestamp) - 8;
            graphics.drawString(font, timestamp, timestampX, textY, GuiColors.TEXT_SECONDARY, false);
        }
    }
}