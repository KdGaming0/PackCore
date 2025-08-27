package com.github.kd_gaming1.packcore.gui.basic;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.UpdateAvailablePopup;
import com.github.kd_gaming1.packcore.gui.toast.UpdateNotificationToast;
import com.github.kd_gaming1.packcore.utils.ModpackInfo;
import com.github.kd_gaming1.packcore.utils.UpdateCacheManager;
import com.github.kd_gaming1.packcore.utils.UpdateCheckResult;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.core.*;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class BasicTitleScreen extends BaseOwoScreen<FlowLayout> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Button dimensions
    private static final int MAIN_BUTTON_WIDTH = 200;
    private static final int MAIN_BUTTON_HEIGHT = 20;
    private static final int ICON_BUTTON_SIZE = 20;
    private static final int BOTTOM_BUTTON_WIDTH = 98;

    // Title logo dimensions
    private static final int TITLE_WIDTH = 400;
    private static final int TITLE_HEIGHT = 63;
    private static final int TITLE_Y_OFFSET = 30;

    private static final int ORIGINAL_TITLE_WIDTH = 1325;
    private static final int ORIGINAL_TITLE_HEIGHT = 209;


    // Update notification fields
    private static final boolean updateNotificationEnabled = PackCoreConfig.showUpdateNotificationsOnTitleScreen & PackCoreConfig.enableUpdateNotifications;
    private boolean updateAvailable;
    private String currentVersion;
    private String newVersion;
    private String changelog;
    private String modrinthUrl;
    private String modrinthName;

    // Panorama background
    private final Identifier base = Identifier.of("packcore", "textures/gui/title/background/panorama");
    private final CubeMapRenderer cubeMap = new CubeMapRenderer(base);
    private final RotatingCubeMapRenderer rotating = new RotatingCubeMapRenderer(cubeMap);

    // Temp variables for toasts
    private static long lastToastTime = 0;
    private static final long TOAST_COOLDOWN_MS = 10_000; // 10 seconds

    // Popup state
    private boolean isPopupOpen = false;

    private FlowLayout buttonContainer;

    @Override
    protected void init() {
        preloadPanoramaTextures();
        checkForUpdates();

        long now = System.currentTimeMillis();
        if (updateAvailable & updateNotificationEnabled && (now - lastToastTime > TOAST_COOLDOWN_MS)) {
            MinecraftClient.getInstance().getToastManager().add(
                    new UpdateNotificationToast(currentVersion, newVersion, modrinthName)
            );
            lastToastTime = now;
        }
        super.init();
    }

    private void preloadPanoramaTextures() {
        var tm = MinecraftClient.getInstance().getTextureManager();
        for (int i = 0; i < 6; i++) {
            Identifier face = Identifier.of(base.getNamespace(), base.getPath() + "_" + i + ".png");
            tm.getTexture(face);
        }
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        // Set the background image
        rootComponent.surface(Surface.panorama(rotating, true));

        // Title container with fixed positioning at top
        FlowLayout titleContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .positioning(Positioning.absolute(0, 0))
                .margins(Insets.top(TITLE_Y_OFFSET));

        titleContainer.child(
                Components.texture(
                        Identifier.of("packcore", "textures/gui/title/skyblock_enhanced.png"),
                        0, 0,
                        ORIGINAL_TITLE_WIDTH, ORIGINAL_TITLE_HEIGHT,
                        ORIGINAL_TITLE_WIDTH, ORIGINAL_TITLE_HEIGHT
                ).sizing(Sizing.fixed(TITLE_WIDTH), Sizing.fixed(TITLE_HEIGHT))
        );

        // Create and store button container reference
        buttonContainer = createMainButtonContainer();
        populateButtonContainer();

        // Add both containers as separate children of root
        rootComponent.child(titleContainer);
        rootComponent.child(buttonContainer)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        if (updateAvailable && !isPopupOpen) {
            rootComponent.child(createUpdateAvailableButton().positioning(Positioning.relative(98, 98)));
        }

        // Version label bottom-right (keep as absolute)
        rootComponent.child(createVersionLabel());
    }


    private FlowLayout createMainButtonContainer() {
        return (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of(10, 10, 10, 10))
                .margins(Insets.top(38));
    }

    private void populateButtonContainer() {
        buttonContainer.clearChildren();

        buttonContainer.child(createMainButton("menu.singleplayer", () -> new SelectWorldScreen(this)));
        buttonContainer.child(createMainButton("menu.multiplayer", () -> new MultiplayerScreen(this)));

        if (PackCoreConfig.replaceRealmsButtonWithQuickServerJoinButton) {
            buttonContainer.child(createHypixelButton());
        } else {
            buttonContainer.child(createMainButton("menu.online", () -> new RealmsMainScreen(this)));
        }

        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            buttonContainer.child(createModsButton());
        }

        buttonContainer.child(Components.box(Sizing.fixed(0), Sizing.fixed(12)).color(Color.ofArgb(0xAA)));
        buttonContainer.child(createBottomRow());
    }

    private ButtonComponent createMainButton(String translationKey, ScreenSupplier screenSupplier) {
        return (ButtonComponent) Components.button(Text.translatable(translationKey),
                        button -> MinecraftClient.getInstance().setScreen(screenSupplier.get()))
                .sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(MAIN_BUTTON_HEIGHT));
    }

    private ButtonComponent createHypixelButton() {
        return (ButtonComponent) Components.button(Text.literal("Join Hypixel"), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ServerInfo serverInfo = new ServerInfo("Hypixel", PackCoreConfig.serverAddressForQuickJoinButton, ServerInfo.ServerType.OTHER);
            ConnectScreen.connect(this, client, ServerAddress.parse(PackCoreConfig.serverAddressForQuickJoinButton), serverInfo, false, null);
        }).sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(MAIN_BUTTON_HEIGHT));
    }

    private ButtonComponent createModsButton() {
        return (ButtonComponent) Components.button(Text.literal("Mods"), button -> {
            try {
                Class<?> modMenuClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
                Screen modsScreen = (Screen) modMenuClass
                        .getConstructor(Screen.class)
                        .newInstance(client.currentScreen);
                client.setScreen(modsScreen);
            } catch (Exception e) {
                LOGGER.error("Failed to open ModMenu screen", e);
            }
        }).sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(MAIN_BUTTON_HEIGHT));
    }

    private FlowLayout createBottomRow() {
        FlowLayout bottomRow = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        bottomRow.child(createLanguageButton());
        bottomRow.child(createOptionsButton());
        bottomRow.child(createQuitButton());
        bottomRow.child(createAccessibilityButton());

        return bottomRow;
    }

    public static ButtonComponent.Renderer iconButtonRenderer(Identifier iconTexture, int u, int v, int iconWidth, int iconHeight) {
        return (context, button, delta) -> {
            ButtonComponent.Renderer.VANILLA.draw(context, button, delta);
            int iconX = button.getX() + (button.getWidth() - iconWidth) / 2;
            int iconY = button.getY() + (button.getHeight() - iconHeight) / 2;
            context.drawTexture(
                    RenderLayer::getGuiTextured,
                    iconTexture,
                    iconX, iconY,
                    u, v,
                    iconWidth, iconHeight,
                    iconWidth, iconHeight
            );
        };
    }

    private ButtonComponent createLanguageButton() {
        ButtonComponent button = Components.button(Text.empty(), b ->
                MinecraftClient.getInstance().setScreen(new LanguageOptionsScreen(
                        this,
                        MinecraftClient.getInstance().options,
                        MinecraftClient.getInstance().getLanguageManager())));

        button.sizing(Sizing.fixed(ICON_BUTTON_SIZE), Sizing.fixed(ICON_BUTTON_SIZE));
        button.renderer(iconButtonRenderer(
                Identifier.of("minecraft", "textures/gui/sprites/icon/language.png"),
                0, 0, 16, 16
        ));
        return button;
    }

    private ButtonComponent createOptionsButton() {
        return (ButtonComponent) Components.button(Text.translatable("menu.options"),
                        button -> MinecraftClient.getInstance().setScreen(
                                new OptionsScreen(this, MinecraftClient.getInstance().options)))
                .sizing(Sizing.fixed(BOTTOM_BUTTON_WIDTH), Sizing.fixed(MAIN_BUTTON_HEIGHT));
    }

    private ButtonComponent createQuitButton() {
        return (ButtonComponent) Components.button(Text.translatable("menu.quit"),
                        button -> MinecraftClient.getInstance().scheduleStop())
                .sizing(Sizing.fixed(BOTTOM_BUTTON_WIDTH), Sizing.fixed(MAIN_BUTTON_HEIGHT));
    }

    private ButtonComponent createAccessibilityButton() {
        ButtonComponent button = Components.button(Text.empty(), b ->
                MinecraftClient.getInstance().setScreen(
                        new AccessibilityOptionsScreen(this, MinecraftClient.getInstance().options)));

        button.sizing(Sizing.fixed(ICON_BUTTON_SIZE), Sizing.fixed(ICON_BUTTON_SIZE));
        button.renderer(iconButtonRenderer(
                Identifier.of("minecraft", "textures/gui/sprites/icon/accessibility.png"),
                0, 0, 16, 16
        ));
        return button;
    }

    private OverlayContainer<FlowLayout> currentPopup = null;

    private void showUpdatePopup() {
        // Set popup state
        isPopupOpen = true;

        // Hide buttons by clearing the container
        buttonContainer.clearChildren();

        // Create the popup
        currentPopup = UpdateAvailablePopup.createUpdatePopup(
                currentVersion,
                newVersion,
                changelog,
                modrinthUrl,
                this::closeUpdatePopup
        );

        // Add popup to root component
        this.uiAdapter.rootComponent.child(currentPopup);
    }

    private void closeUpdatePopup() {
        if (currentPopup != null) {
            this.uiAdapter.rootComponent.removeChild(currentPopup);
            currentPopup = null;
            isPopupOpen = false;
        }
        // Restore buttons
        populateButtonContainer();
    }

    private ButtonComponent createUpdateAvailableButton() {
        ButtonComponent button = Components.button(
                Text.literal("🔔 Update Available!"),
                b -> showUpdatePopup()
        );
        button.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH - 50), Sizing.fixed(MAIN_BUTTON_HEIGHT))
                .tooltip(Text.literal("Click to view update details"));
        return button;
    }

    private LabelComponent createVersionLabel() {
        String versionString = "Minecraft " + SharedConstants.getGameVersion().getName();
        return (LabelComponent) Components.label(Text.literal(versionString))
                .positioning(Positioning.relative(1, 98));
    }

    public void checkForUpdates() {
        UpdateCacheManager updateManager = PackCore.getUpdateManager();
        ModpackInfo info = PackCore.getModpackInfo();

        if (updateManager == null || info == null) {
            LOGGER.error("Update system not initialized properly");
            return;
        }

        // Check if the configuration is valid
        if (!info.isConfigurationValid()) {
            LOGGER.warn("Skipping update check - configuration not properly set up: {}",
                    info.getValidationError());
            return;
        }

        UpdateCheckResult result = updateManager.checkForUpdates(info);

        if (!result.isSuccess()) {
            LOGGER.error("Update check failed: {}", result.getErrorMessage());
            return;
        }

        this.updateAvailable = result.isUpdateAvailable();
        this.currentVersion = info.getVersion();
        this.newVersion = result.getVersionNumber();
        this.changelog = result.getChangelog();
        this.modrinthUrl = result.getModrinthUrl();
        this.modrinthName = info.getName();

        // Debug logging
        // LOGGER.info("Update check completed - Available: {}, Current: {}, Latest: {}", updateAvailable, currentVersion, newVersion);
        // LOGGER.info("Modrinth URL: {}", modrinthUrl);
    }

    @FunctionalInterface
    private interface ScreenSupplier {
        net.minecraft.client.gui.screen.Screen get();
    }
}