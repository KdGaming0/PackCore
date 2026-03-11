package com.github.kd_gaming1.packcore.gui.screen;

import com.daqem.uilib.gui.AbstractScreen;
import com.daqem.uilib.gui.component.sprite.SpriteComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.daqem.uilib.gui.widget.CustomButtonWidget;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.component.OverlayComponent;
import com.github.kd_gaming1.packcore.gui.screen.config.ConfigScreen;
import com.github.kd_gaming1.packcore.gui.util.ImageBackground;
import com.github.kd_gaming1.packcore.gui.util.SpriteHelper;
import com.github.kd_gaming1.packcore.metadata.ModpackMetadata;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.update.UpdateStatus;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

/**
 * Custom title screen for the modpack with branded UI and quick-access buttons.
 */
public class SBETitleScreen extends AbstractScreen {

    // Logo dimensions and positioning
    private static final int LOGO_ORIGINAL_WIDTH = 1476;
    private static final int LOGO_ORIGINAL_HEIGHT = 157;
    private static final double LOGO_SCALE = 0.8;
    private static final int LOGO_Y_POSITION = 20;

    // Main menu button dimensions
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;
    private static final int LOGO_TO_BUTTONS_GAP = 40;

    // Icon button dimensions
    private static final int ICON_BUTTON_SIZE = 20;

    // Screen margins and spacing
    private static final int SCREEN_MARGIN = 5;
    private static final int VERSION_TEXT_HEIGHT = 15;
    private static final int SOCIAL_TO_VERSION_GAP = 5;

    // Instance fields
    private OverlayComponent changelogOverlay;
    private ButtonWidget joinHypixelButton;
    private ButtonWidget singleplayerButton;
    private ButtonWidget multiplayerButton;
    private ButtonWidget modmenuButton;
    private ButtonWidget optionsButton;
    private ButtonWidget quitButton;

    public SBETitleScreen() {
        super(Component.literal("Title Screen"));
        this.setBackground(new ImageBackground(
                Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/title_menu_background.png"),
                1920,
                1080,
                ImageBackground.BackgroundMode.STRETCH
        ));
    }

    @Override
    protected void init() {
        // === LOGO ===
        SpriteComponent titleSprite = createLogo();

        // === MAIN MENU BUTTONS ===
        int buttonX = this.width / 2 - BUTTON_WIDTH / 2;
        int buttonStartY = calculateButtonStartY();

        joinHypixelButton = createJoinHypixelButton(buttonX, buttonStartY);
        singleplayerButton = createSingleplayerButton(buttonX, buttonStartY + (BUTTON_HEIGHT + BUTTON_SPACING));
        multiplayerButton = createMultiplayerButton(buttonX, buttonStartY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2);
        modmenuButton = createModMenuButton(buttonX, buttonStartY + (BUTTON_HEIGHT + BUTTON_SPACING) * 3);
        optionsButton = createOptionsButton(buttonX, buttonStartY + (BUTTON_HEIGHT + BUTTON_SPACING) * 4);
        quitButton = createQuitButton(buttonX, buttonStartY + (BUTTON_HEIGHT + BUTTON_SPACING) * 5);

        // === SOCIAL BUTTONS (Bottom-left, stacked vertically) ===
        int socialButtonX = SCREEN_MARGIN;
        int versionY = this.height - VERSION_TEXT_HEIGHT;
        int githubY = versionY - SOCIAL_TO_VERSION_GAP - ICON_BUTTON_SIZE;
        int modrinthY = githubY - BUTTON_SPACING - ICON_BUTTON_SIZE;
        int discordY = modrinthY - BUTTON_SPACING - ICON_BUTTON_SIZE;

        ButtonWidget discordButton = createDiscordButton(socialButtonX, discordY);
        ButtonWidget modrinthButton = createModrinthButton(socialButtonX, modrinthY);
        ButtonWidget githubButton = createGithubButton(socialButtonX, githubY);

        // === VERSION TEXT ===
        UpdateStatus updateStatus = UpdateChecker.getCachedStatus();

        Component versionText = buildVersionText(updateStatus);

        TextComponent modpackVersion = new TextComponent(
                SCREEN_MARGIN, versionY,
                versionText,
                0xFFFFFFFF
        );

        // === CHANGELOG OVERLAY ===
        int overlayW = Math.min((int) (this.width * 0.80), 800);
        int overlayH = Math.min((int) (this.height * 0.75), 500);
        int overlayX = (this.width - overlayW) / 2;
        int overlayY = (this.height - overlayH) / 2 + 25;

        String changelogMarkdown = updateStatus.changelog();

        changelogOverlay = new OverlayComponent(
                overlayX, overlayY, overlayW, overlayH,
                Component.translatable("gui.packcore.overlay.changelog.title", updateStatus.latestVersion()),
                changelogMarkdown
        );
        changelogOverlay.setOnClose(() -> setMenuButtonsVisible(true));

        // === CORNER BUTTONS ===
        ButtonWidget modpackConfigButton = createModpackConfigButton();
        ButtonWidget modpackUpdateButton = createModpackUpdateButton(changelogOverlay);

        // === ADD ALL COMPONENTS AND WIDGETS ===
        this.addComponent(titleSprite);
        this.addComponent(modpackVersion);

        this.addWidget(joinHypixelButton);
        this.addWidget(singleplayerButton);
        this.addWidget(multiplayerButton);
        this.addWidget(modmenuButton);
        this.addWidget(optionsButton);
        this.addWidget(quitButton);

        this.addWidget(discordButton);
        this.addWidget(modrinthButton);
        this.addWidget(githubButton);

        this.addWidget(modpackUpdateButton);
        this.addWidget(modpackConfigButton);

        this.addComponent(changelogOverlay);

        super.init();
    }

    // === LOGO CREATION ===

    private SpriteComponent createLogo() {
        return SpriteHelper.createScaledCenteredSprite(
                this.width,
                this.height,
                LOGO_ORIGINAL_WIDTH,
                LOGO_ORIGINAL_HEIGHT,
                LOGO_SCALE,
                LOGO_Y_POSITION,
                Identifier.fromNamespaceAndPath(MOD_ID, "title/title")
        );
    }

    private int calculateButtonStartY() {
        SpriteHelper.SpriteDimensions logoDims = SpriteHelper.scaleAndCenter(
                this.width, this.height,
                LOGO_ORIGINAL_WIDTH, LOGO_ORIGINAL_HEIGHT,
                LOGO_SCALE, LOGO_Y_POSITION
        );
        return logoDims.y() + logoDims.height() + LOGO_TO_BUTTONS_GAP;
    }

    // === MAIN MENU BUTTONS ===

    private ButtonWidget createJoinHypixelButton(int x, int y) {
        return new CustomButtonWidget(
                x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.button.join_hypixel"),
                MAIN_BUTTON_SPRITES,
                btn -> {
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
        );
    }

    private ButtonWidget createSingleplayerButton(int x, int y) {
        return new CustomButtonWidget(
                x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("menu.singleplayer"),
                MAIN_BUTTON_SPRITES,
                btn -> Minecraft.getInstance().setScreen(new SelectWorldScreen(this))
        );
    }

    private ButtonWidget createMultiplayerButton(int x, int y) {
        return new CustomButtonWidget(
                x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("menu.multiplayer"),
                MAIN_BUTTON_SPRITES,
                btn -> Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(this))
        );
    }

    private ButtonWidget createModMenuButton(int x, int y) {
        return new CustomButtonWidget(
                x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.packcore.button.modmenu"),
                MAIN_BUTTON_SPRITES,
                btn -> Minecraft.getInstance().setScreen(ModMenuApi.createModsScreen(this))
        );
    }

    private ButtonWidget createOptionsButton(int x, int y) {
        return new CustomButtonWidget(
                x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("menu.options"),
                MAIN_BUTTON_SPRITES,
                btn -> Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options))
        );
    }

    private ButtonWidget createQuitButton(int x, int y) {
        return new CustomButtonWidget(
                x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("menu.quit"),
                MAIN_BUTTON_SPRITES,
                btn -> Minecraft.getInstance().stop()
        );
    }

    // === CORNER BUTTONS ===

    private ButtonWidget createModpackUpdateButton(OverlayComponent changelogOverlay) {
        UpdateStatus status = UpdateChecker.getCachedStatus();
        ButtonWidget button = new CustomButtonWidget(
                this.width - ICON_BUTTON_SIZE - SCREEN_MARGIN,
                SCREEN_MARGIN,
                ICON_BUTTON_SIZE,
                ICON_BUTTON_SIZE,
                Component.empty(),
                // Tint the icon green when up to date, yellow when update available
                status.isUpdateAvailable()
                        ? createIconSprites("menu/update_icon_available")
                        : createIconSprites("menu/update_icon"),
                btn -> {
                    changelogOverlay.toggle();
                    setMenuButtonsVisible(!changelogOverlay.isShown());
                }
        );
        button.setTooltip(Tooltip.create(
                status.isUpdateAvailable()
                        ? Component.translatable("gui.packcore.tooltip.update_available", status.latestVersion())
                        : Component.translatable("gui.packcore.tooltip.changelog")
        ));
        return button;
    }

    private ButtonWidget createModpackConfigButton() {
        ButtonWidget button = new CustomButtonWidget(
                this.width - ICON_BUTTON_SIZE - SCREEN_MARGIN,
                this.height - ICON_BUTTON_SIZE - SCREEN_MARGIN,
                ICON_BUTTON_SIZE,
                ICON_BUTTON_SIZE,
                Component.empty(),
                createIconSprites("menu/settings_icon"),
                btn -> Minecraft.getInstance().setScreen(new ConfigScreen())
        );
        button.setTooltip(Tooltip.create(Component.translatable("gui.packcore.tooltip.modpack_config")));
        return button;
    }

    // === SOCIAL BUTTONS ===

    private ButtonWidget createDiscordButton(int x, int y) {
        ButtonWidget button = new CustomButtonWidget(
                x, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE,
                Component.empty(),
                createIconSprites("menu/discord_icon"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getDiscordUrl())
        );
        button.setTooltip(Tooltip.create(Component.translatable("gui.packcore.tooltip.discord")));
        return button;
    }

    private ButtonWidget createModrinthButton(int x, int y) {
        ButtonWidget button = new CustomButtonWidget(
                x, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE,
                Component.empty(),
                createIconSprites("menu/modrinth_icon"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getWebsiteUrl())
        );
        button.setTooltip(Tooltip.create(Component.translatable("gui.packcore.tooltip.modrinth")));
        return button;
    }

    private ButtonWidget createGithubButton(int x, int y) {
        ButtonWidget button = new CustomButtonWidget(
                x, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE,
                Component.empty(),
                createIconSprites("menu/github_icon"),
                btn -> Util.getPlatform().openUri(ModpackMetadata.getInstance().getIssueTrackerUrl())
        );
        button.setTooltip(Tooltip.create(Component.translatable("gui.packcore.tooltip.github")));
        return button;
    }

    private static Component buildVersionText(UpdateStatus status) {
        String installed = status.installedVersion() != null
                ? status.installedVersion()
                : ModpackMetadata.getInstance().getModpackVersion();

        if (status.isUpdateAvailable()) {
            return Component.literal("v" + installed + " → ")
                    .append(Component.literal("v" + status.latestVersion())
                            .withStyle(s -> s.withColor(0xFFFFFFFF))); // green
        }

        return Component.literal("v" + installed);
    }

    private void setMenuButtonsVisible(boolean visible) {
        joinHypixelButton.visible = visible;
        joinHypixelButton.active = visible;
        singleplayerButton.visible = visible;
        singleplayerButton.active = visible;
        multiplayerButton.visible = visible;
        multiplayerButton.active = visible;
        modmenuButton.visible = visible;
        modmenuButton.active = visible;
        optionsButton.visible = visible;
        optionsButton.active = visible;
        quitButton.visible = visible;
        quitButton.active = visible;
    }

    // === SPRITE HELPERS ===

    private static final WidgetSprites MAIN_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/blank_red_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/disabled_red_button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "menu/buttons/hover_red_button")
    );

    private WidgetSprites createIconSprites(String basePath) {
        Identifier icon = Identifier.fromNamespaceAndPath(MOD_ID, basePath);
        return new WidgetSprites(icon, icon, icon);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE && changelogOverlay.isShown()) {
            changelogOverlay.setShown(false);
            return true;
        }
        return super.keyPressed(keyEvent);
    }
}