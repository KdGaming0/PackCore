package com.github.kd_gaming1.packcore.gui.screen;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.screen.config.ConfigScreen;
import com.github.kd_gaming1.packcore.gui.util.ToastHelper;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.update.UpdateStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Vanilla title screen extended with PackCore-specific buttons.
 * Extends TitleScreen so other mods hooking ScreenEvents still see it.
 */
public class PackCoreTitleScreen extends TitleScreen {

    private static final int ICON_SIZE = 20;
    private static final int MARGIN = 5;
    private static final int ICON_SPACING = 4;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_STRIDE = 24;

    private static boolean updateToastShown = false;

    @Override
    protected void init() {
        super.init();

        UpdateStatus status = UpdateChecker.getCachedStatus();

        if (!updateToastShown) {
            if (status.isUpdateAvailable()) {
                ToastHelper.showUpdateAvailable(status.latestVersion());
            }
            updateToastShown = true;
        }

        // Join Hypixel — one row above vanilla singleplayer
        int hypixelY = this.height / 4 + 48 - (BUTTON_STRIDE * 2);
        addRenderableWidget(Button.builder(
                Component.translatable("gui.packcore.button.join_hypixel"),
                btn -> connectToHypixel()
        ).bounds(this.width / 2 - 100, hypixelY, 200, BUTTON_HEIGHT).build());

        int vanillaTwoLinesY = this.height - 10 - this.font.lineHeight - 2;
        int yourVersionY = vanillaTwoLinesY - this.font.lineHeight - 2;

        int githubY   = yourVersionY - MARGIN - ICON_SIZE;
        int modrinthY = githubY - ICON_SPACING - ICON_SIZE;
        int discordY  = modrinthY - ICON_SPACING - ICON_SIZE;

        addIconButton(MARGIN, discordY, "menu/discord_icon",
                Component.translatable("gui.packcore.tooltip.discord"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getDiscordUrl()));

        addIconButton(MARGIN, modrinthY, "menu/modrinth_icon",
                Component.translatable("gui.packcore.tooltip.modrinth"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getWebsiteUrl()));

        addIconButton(MARGIN, githubY, "menu/github_icon",
                Component.translatable("gui.packcore.tooltip.github"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getIssueTrackerUrl()));

        // Config — bottom-right
        int settingsY = this.height - ICON_SIZE - MARGIN - (this.font.lineHeight * 2) - 4;
        addIconButton(this.width - ICON_SIZE - MARGIN, settingsY, "menu/settings_icon",
                Component.translatable("gui.packcore.tooltip.modpack_config"),
                btn -> Minecraft.getInstance().setScreen(new ConfigScreen()));

        // Changelog/update
        boolean hasUpdate = status.isUpdateAvailable();
        String updateIcon = hasUpdate ? "menu/update_icon_available" : "menu/update_icon";
        Component updateTooltip = hasUpdate
                ? Component.translatable("gui.packcore.tooltip.update_available", status.latestVersion())
                : Component.translatable("gui.packcore.tooltip.changelog");

        addIconButton(this.width - ICON_SIZE - MARGIN, MARGIN, updateIcon, updateTooltip,
                btn -> Minecraft.getInstance().setScreen(new ChangelogScreen(this, status)));
    }

    @Override
    public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int yourVersionY = (this.height - MARGIN - ICON_SIZE) + (ICON_SIZE - this.font.lineHeight) / 2;
        graphics.drawString(this.font, buildVersionText(UpdateChecker.getCachedStatus()), MARGIN, yourVersionY, 0xFFFFFFFF, false);
    }

    private void connectToHypixel() {
        ServerData serverData = new ServerData(
                "Hypixel",
                PackCoreConfig.serverAddressForQuickJoinButton,
                ServerData.Type.OTHER
        );
        ConnectScreen.startConnecting(
                this,
                Minecraft.getInstance(),
                ServerAddress.parseString(PackCoreConfig.serverAddressForQuickJoinButton),
                serverData,
                false,
                null
        );
    }

    private void addIconButton(int x, int y, String spritePath, Component tooltip, Button.OnPress onPress) {
        Identifier icon = Identifier.fromNamespaceAndPath(MOD_ID, spritePath);
        WidgetSprites sprites = new WidgetSprites(icon, icon, icon);
        ImageButton button = new ImageButton(x, y, ICON_SIZE, ICON_SIZE, sprites, onPress);
        button.setTooltip(Tooltip.create(tooltip));
        addRenderableWidget(button);
    }

    private static Component buildVersionText(UpdateStatus status) {
        String installed = status.installedVersion() != null
                ? status.installedVersion()
                : ModpackMetadata.getInstance().getModpackVersion();

        if (status.isUpdateAvailable()) {
            return Component.literal("v" + installed + " → v" + status.latestVersion());
        }
        return Component.literal("v" + installed);
    }
}