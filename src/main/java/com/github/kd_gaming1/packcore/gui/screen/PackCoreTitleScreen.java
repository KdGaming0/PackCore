package com.github.kd_gaming1.packcore.gui.screen;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.screen.config.ConfigScreen;
import com.github.kd_gaming1.packcore.gui.util.ToastHelper;
import com.github.kd_gaming1.packcore.integration.HypixelQuickJoin;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.update.UpdateStatus;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

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
    private static final String JOIN_HYPIXEL_LABEL =
            Component.translatable("gui.packcore.button.join_hypixel").getString();

    private static boolean updateToastShown = false;
    private static final Set<TitleScreen> VERSION_HOOKED_SCREENS =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    protected void init() {
        super.init();

        UpdateStatus status = UpdateChecker.getCachedStatus();
        showUpdateToastIfNeeded();

        // Join Hypixel — one row above vanilla singleplayer
        int hypixelY = this.height / 4 + 48 - (BUTTON_STRIDE * 2);
        addRenderableWidget(Button.builder(
                Component.translatable("gui.packcore.button.join_hypixel"),
                btn -> connectToHypixel()
        ).bounds(this.width / 2 - 100, hypixelY, 200, BUTTON_HEIGHT).build());

        int vanillaTwoLinesY = this.height - 10 - this.font.lineHeight - 2;
        int yourVersionY = vanillaTwoLinesY - this.font.lineHeight - 2;

        // Social icon stack (bottom-up)
        int githubY   = yourVersionY - MARGIN - ICON_SIZE;
        int modrinthY = githubY   - ICON_SPACING - ICON_SIZE;
        int discordY  = modrinthY - ICON_SPACING - ICON_SIZE;
        int fluxerY   = discordY  - ICON_SPACING - ICON_SIZE;

        addIconButton(MARGIN, githubY, "menu/github_icon",
                Component.translatable("gui.packcore.tooltip.github"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getIssueTrackerUrl()));

        addIconButton(MARGIN, modrinthY, "menu/modrinth_icon",
                Component.translatable("gui.packcore.tooltip.modrinth"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getWebsiteUrl()));

        addIconButton(MARGIN, discordY, "menu/discord_icon",
                Component.translatable("gui.packcore.tooltip.discord"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getDiscordUrl()));

        String fluxerUrl = ModpackMetadata.getInstance().getFluxerUrl();
        if (!fluxerUrl.isBlank()) {
            addIconButton(MARGIN, fluxerY, "menu/fluxericon",
                    Component.translatable("gui.packcore.tooltip.fluxer"),
                    btn -> Util.getPlatform().openUri(fluxerUrl));
        }

        // Config — bottom-right
        int settingsY = this.height - ICON_SIZE - MARGIN - (this.font.lineHeight * 2) - 4;
        addIconButton(this.width - ICON_SIZE - MARGIN, settingsY, "menu/settings_icon",
                Component.translatable("gui.packcore.tooltip.modpack_config"),
                btn -> Minecraft.getInstance().setScreen(new ConfigScreen()));

        // Changelog/update — top-right
        boolean hasUpdate = status.isUpdateAvailable();
        String updateIcon = hasUpdate ? "menu/update_icon_available" : "menu/update_icon";
        Component updateTooltip = hasUpdate
                ? Component.translatable("gui.packcore.tooltip.update_available", status.latestVersion())
                : Component.translatable("gui.packcore.tooltip.changelog");

        addIconButton(this.width - ICON_SIZE - MARGIN, MARGIN, updateIcon, updateTooltip,
                btn -> Minecraft.getInstance().setScreen(new ChangelogScreen(this, status)));
    }

    public static void decorateExisting(TitleScreen screen, int scaledWidth, int scaledHeight) {
        showUpdateToastIfNeeded();
        Screens.getWidgets(screen).removeIf(button ->
                button instanceof PackCoreDecoratedWidget
                        || JOIN_HYPIXEL_LABEL.equals(button.getMessage().getString())
        );

        int hypixelY = scaledHeight / 4 + 48 - (BUTTON_STRIDE * 2);
        Screens.getWidgets(screen).add(Button.builder(
                Component.translatable("gui.packcore.button.join_hypixel"),
                btn -> connectToHypixel(screen)
        ).bounds(scaledWidth / 2 - 100, hypixelY, 200, BUTTON_HEIGHT).build());

        int vanillaTwoLinesY = scaledHeight - 10 - Minecraft.getInstance().font.lineHeight - 2;
        int yourVersionY = vanillaTwoLinesY - Minecraft.getInstance().font.lineHeight - 2;

        // Social icon stack (bottom-up)
        int githubY   = yourVersionY - MARGIN - ICON_SIZE;
        int modrinthY = githubY   - ICON_SPACING - ICON_SIZE;
        int discordY  = modrinthY - ICON_SPACING - ICON_SIZE;
        int fluxerY   = discordY  - ICON_SPACING - ICON_SIZE;

        Screens.getWidgets(screen).add(createDecoratedIconButton(
                MARGIN, githubY, "menu/github_icon",
                Component.translatable("gui.packcore.tooltip.github"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getIssueTrackerUrl())
        ));

        Screens.getWidgets(screen).add(createDecoratedIconButton(
                MARGIN, modrinthY, "menu/modrinth_icon",
                Component.translatable("gui.packcore.tooltip.modrinth"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getWebsiteUrl())
        ));

        Screens.getWidgets(screen).add(createDecoratedIconButton(
                MARGIN, discordY, "menu/discord_icon",
                Component.translatable("gui.packcore.tooltip.discord"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getDiscordUrl())
        ));

        String fluxerUrl = ModpackMetadata.getInstance().getFluxerUrl();
        if (!fluxerUrl.isBlank()) {
            Screens.getWidgets(screen).add(createDecoratedIconButton(
                    MARGIN, fluxerY, "menu/fluxericon",
                    Component.translatable("gui.packcore.tooltip.fluxer"),
                    btn -> Util.getPlatform().openUri(fluxerUrl)
            ));
        }

        int settingsY = scaledHeight - ICON_SIZE - MARGIN - (Minecraft.getInstance().font.lineHeight * 2) - 4;
        Screens.getWidgets(screen).add(createDecoratedIconButton(
                scaledWidth - ICON_SIZE - MARGIN, settingsY, "menu/settings_icon",
                Component.translatable("gui.packcore.tooltip.modpack_config"),
                btn -> Minecraft.getInstance().setScreen(new ConfigScreen())
        ));

        UpdateStatus status = UpdateChecker.getCachedStatus();
        boolean hasUpdate = status.isUpdateAvailable();
        String updateIcon = hasUpdate ? "menu/update_icon_available" : "menu/update_icon";
        Component updateTooltip = hasUpdate
                ? Component.translatable("gui.packcore.tooltip.update_available", status.latestVersion())
                : Component.translatable("gui.packcore.tooltip.changelog");

        Screens.getWidgets(screen).add(createDecoratedIconButton(
                scaledWidth - ICON_SIZE - MARGIN, MARGIN, updateIcon, updateTooltip,
                btn -> Minecraft.getInstance().setScreen(new ChangelogScreen(screen, status))
        ));

        registerVersionHook(screen);
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        //?} else {
     /*@Override
    public void render(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    *///?}
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int yourVersionY = (this.height - MARGIN - ICON_SIZE) + (ICON_SIZE - this.font.lineHeight) / 2;
        //? if >=26.1 {
        graphics.text(this.font, buildVersionText(UpdateChecker.getCachedStatus()),
            //?} else {
         /*graphics.drawString(this.font, buildVersionText(UpdateChecker.getCachedStatus()),
        *///?}
                MARGIN, yourVersionY, 0xFFFFFFFF, false);
    }

    private void connectToHypixel() {
        connectToHypixel(this);
    }

    private static void connectToHypixel(TitleScreen screen) {
        ServerData serverData = HypixelQuickJoin.resolveServerData();
        ConnectScreen.startConnecting(
                screen,
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

    private static PackCoreImageButton createDecoratedIconButton(
            int x, int y, String spritePath, Component tooltip, Button.OnPress onPress) {
        Identifier icon = Identifier.fromNamespaceAndPath(MOD_ID, spritePath);
        WidgetSprites sprites = new WidgetSprites(icon, icon, icon);
        PackCoreImageButton button = new PackCoreImageButton(x, y, ICON_SIZE, ICON_SIZE, sprites, onPress);
        button.setTooltip(Tooltip.create(tooltip));
        return button;
    }

    private static void registerVersionHook(TitleScreen screen) {
        if (!VERSION_HOOKED_SCREENS.add(screen)) return;

            //? if >=26.1 {
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> {
            //?} else {
                /*ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> {
            *///?}
            Minecraft client = Minecraft.getInstance();
            int height = client.getWindow().getGuiScaledHeight();
            int yourVersionY = (height - MARGIN - ICON_SIZE) + (ICON_SIZE - client.font.lineHeight) / 2;
                //? if >=26.1 {
                graphics.text(client.font, buildVersionText(UpdateChecker.getCachedStatus()), MARGIN, yourVersionY, 0xFFFFFFFF, false);
                //?} else {
                /*graphics.drawString(client.font, buildVersionText(UpdateChecker.getCachedStatus()), MARGIN, yourVersionY, 0xFFFFFFFF, false);
                  *///?}
        });
    }

    private static void showUpdateToastIfNeeded() {
        if (updateToastShown) return;
        UpdateStatus status = UpdateChecker.getCachedStatus();
        if (status.isUpdateAvailable()) {
            boolean isBeta = status.latestVersion() != null
                    && status.latestVersion().contains("-beta.");
            ToastHelper.showUpdateAvailable(status.latestVersion(), isBeta);
        }
        updateToastShown = true;
    }

    private static Component buildVersionText(UpdateStatus status) {
        String installed = status.installedVersion() != null
                ? status.installedVersion()
                : ModpackMetadata.getInstance().getModpackVersion();

        if (status.isUpdateAvailable()) {
            String latest = status.latestVersion();
            boolean isBeta = latest != null && latest.contains("-beta.");
            String arrow = "v" + installed + " → v" + latest;
            return isBeta
                    ? Component.literal(arrow + " (beta)")
                    : Component.literal(arrow);
        }
        return Component.literal("v" + installed);
    }

    private interface PackCoreDecoratedWidget {}

    private static final class PackCoreImageButton extends ImageButton implements PackCoreDecoratedWidget {
        private PackCoreImageButton(int x, int y, int width, int height, WidgetSprites sprites, OnPress onPress) {
            super(x, y, width, height, sprites, onPress);
        }
    }
}