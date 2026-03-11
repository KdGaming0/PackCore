package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.sprite.SpriteComponent;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class ConfigScreenHeader extends AbstractComponent {

    private static final int LOGO_ORIGINAL_SIZE = 640;
    private static final int LOGO_PADDING_X = 10;
    private static final int LOGO_PADDING_Y = 6;

    private static final int COLOR_LABEL = 0xFFCCCCCC;
    private static final int COLOR_ACCENT = 0xFF2196F3;
    private static final int COLOR_VALUE = 0xFFAAAAAA;

    private static final int CLOSE_BUTTON_SIZE = 16;
    private static final int CLOSE_BUTTON_MARGIN = 8;
    private static final int TEXT_PADDING_RIGHT = CLOSE_BUTTON_SIZE + CLOSE_BUTTON_MARGIN * 2;

    private static final WidgetSprites CLOSE_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/x"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/x"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/xhover")
    );

    public ConfigScreenHeader(int x, int y, int width, int height, Runnable onClose) {
        super(x, y, width, height);

        int maxLogoHeight = height - LOGO_PADDING_Y * 2;
        int scaledLogoWidth = (LOGO_ORIGINAL_SIZE * maxLogoHeight) / LOGO_ORIGINAL_SIZE;
        addComponent(new SpriteComponent(
                LOGO_PADDING_X, LOGO_PADDING_Y,
                scaledLogoWidth, maxLogoHeight,
                Identifier.fromNamespaceAndPath(MOD_ID, "assets/sbe_logo")
        ));

        int closeBtnX = width - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN;
        int closeBtnY = (height - CLOSE_BUTTON_SIZE) / 2;
        addWidget(new CustomButtonWidget(
                closeBtnX, closeBtnY,
                CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE,
                Component.empty(),
                CLOSE_SPRITES,
                b -> onClose.run()
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        int x = getTotalX();
        int y = getTotalY();
        int w = getWidth();
        int h = getHeight();

        var font = Minecraft.getInstance().font;

        String packName = resolvePackName();
        String version = PackCoreConfig.lastAppliedVersion.isBlank() ? "—" : "v" + PackCoreConfig.lastAppliedVersion;
        String modpackVersion = "v" + ModpackMetadata.getInstance().getModpackVersion();
        String labelText = "Active Config";
        String detailText = "  ·  " + version + "  ·  Modpack " + modpackVersion;

        int totalTextHeight = font.lineHeight * 2 + 2;
        int firstLineY = y + (h - totalTextHeight) / 2;
        int secondLineY = firstLineY + font.lineHeight + 2;
        int rightEdge = x + w - TEXT_PADDING_RIGHT;

        graphics.drawString(font, labelText, rightEdge - font.width(labelText), firstLineY, COLOR_LABEL, false);

        int packNameX = rightEdge - font.width(packName + detailText);
        graphics.drawString(font, packName, packNameX, secondLineY, COLOR_ACCENT, false);
        graphics.drawString(font, detailText, packNameX + font.width(packName), secondLineY, COLOR_VALUE, false);
    }

    private static String resolvePackName() {
        String file = PackCoreConfig.lastAppliedPackFile;
        if (file == null || file.isBlank()) return "None";
        return file.endsWith(".zip") ? file.substring(0, file.length() - 4) : file;
    }
}