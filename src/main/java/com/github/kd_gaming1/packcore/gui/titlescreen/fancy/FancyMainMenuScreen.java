package com.github.kd_gaming1.packcore.gui.titlescreen.fancy;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.util.UiSurfaces;
import com.github.kd_gaming1.packcore.gui.configscreen.ModpackConfigMenuScreen;
import com.github.kd_gaming1.packcore.gui.help.guide.BaseGuidePage;
import com.github.kd_gaming1.packcore.util.modpack.ModpackInfo;
import com.github.kd_gaming1.packcore.util.api.UpdateCacheManager;
import com.github.kd_gaming1.packcore.util.api.UpdateCheckResult;
import com.github.kd_gaming1.packcore.util.notification.UpdateNotificationManager;
import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.lavendermd.feature.*;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;

public class FancyMainMenuScreen extends BaseOwoScreen<FlowLayout> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private final Identifier backgroundTexture = Identifier.of(MOD_ID, "textures/gui/title/main_menu_background.png");

    private static final ModpackInfo info = PackCore.getModpackInfo();

    private String ChangeLogInfoText;
    private static final boolean updateNotificationEnabled =
            info != null &&
                    PackCoreConfig.showUpdateNotificationsOnTitleScreen &&
                    PackCoreConfig.enableUpdateNotifications;
    private boolean updateAvailable;
    private String currentVersion;
    private String newVersion;
    private String changelog;
    private String modrinthUrl;
    private String modrinthName;

    private boolean showChangelog = false;
    private FlowLayout mainButtonLayout;
    private FlowLayout changelogLayout;

    // Cached Markdown processor
    private static final MarkdownProcessor<ParentComponent> MARKDOWN_PROCESSOR =
            new MarkdownProcessor<>(
                    OwoUICompiler::new,
                    new BasicFormattingFeature(),
                    new ColorFeature(),
                    new LinkFeature(),
                    new ListFeature(),
                    new BlockQuoteFeature(),
                    new ImageFeature()
            );

    private static final Map<String, ParentComponent> COMPONENT_CACHE = new ConcurrentHashMap<>();

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(UiSurfaces.stretched(backgroundTexture, 1920, 1082));
        rootComponent.child(createMainButtonAndTitle()).horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.child(createSocialButtons().positioning(Positioning.relative(0, 100)));
        rootComponent.child(createSeeWhatIsNewButtons().positioning(Positioning.relative(100, 0)));
        rootComponent.child(createModpackButtons().positioning(Positioning.relative(100, 100)));

        // Create changelog layout but don't add it initially
        changelogLayout = (FlowLayout) createChangelogFiled().positioning(Positioning.relative(50, 75));
    }

    @Override
    public void init() {
        UpdateCheckResult result = checkForUpdates();

        if (result.isSuccess() && result.isUpdateAvailable() && updateNotificationEnabled) {
            if (UpdateNotificationManager.shouldShowMainMenuToast(result.getVersionNumber())) {
                UpdateNotificationManager.showMainMenuToast(currentVersion, newVersion, modrinthName);
            }
        }

        if (!result.isSuccess()) {
            LOGGER.warn("Update check failed, but don't spam user: {}", result.getErrorMessage());
        }

        super.init();
    }

    private FlowLayout createMainButtonAndTitle() {
        FlowLayout buttonAndTitle = (FlowLayout) Containers.verticalFlow(Sizing.fixed(320), Sizing.fill(100))
                .gap(4)
                .padding(Insets.of(4))
                .margins(Insets.of(4, 4, 4, 4));

        TextureComponent title = (TextureComponent) Components.texture(
                        Identifier.of(MOD_ID, "textures/gui/title/title.png"),
                        0, 0, 1476, 157, 1476, 157
                )
                .margins(Insets.top(8))
                .horizontalSizing(Sizing.fixed(312))
                .verticalSizing(Sizing.fixed(34));

        mainButtonLayout = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(8))
                .margins(Insets.top(12));

        // Add all buttons to layout
        mainButtonLayout
                .child(createHypixelButton())
                .child(createSingleplayerButton())
                .child(createMultiplayerButton())
                .child(createModsButton())
                .child(createOptionsButton())
                .child(createQuitButton());

        buttonAndTitle.child(title);
        buttonAndTitle.child(mainButtonLayout);

        return buttonAndTitle;
    }

    private FlowLayout createSocialButtons() {
        FlowLayout buttonLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(6)
                .horizontalAlignment(HorizontalAlignment.LEFT)
                .padding(Insets.of(4));


        buttonLayout
                .child(createDiscordButton())
                .child(createModrinthButton())
                .child(createGitHubButton())
                .child(createInfo());

        return buttonLayout;
    }

    private FlowLayout createSeeWhatIsNewButtons() {
        FlowLayout buttonLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(6)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .padding(Insets.of(4));

        buttonLayout
                .child(createSeeWhatIsNewButton());

        return buttonLayout;
    }

    private FlowLayout createModpackButtons() {
        FlowLayout buttonLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(6)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .padding(Insets.of(4));

        buttonLayout
                .child(createModpackButton())
                .child(createHelpUpdateButton());

        return buttonLayout;
    }

    private FlowLayout createChangelogFiled() {
        FlowLayout mainLayout = (FlowLayout) Containers.verticalFlow(Sizing.fill(65), Sizing.fill(75))
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(4))
                .surface(UiSurfaces.stretched(Identifier.of(MOD_ID, "textures/gui/menu/info_box.png"), 1142, 934))
                .margins(Insets.of(4, 4, 4, 4));

        FlowLayout changelogInfo = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2)
                .padding(Insets.of(6, 0, 8, 8))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        if (currentVersion.equals(newVersion)) {
            ChangeLogInfoText = "You are up to date! See change log for current version below:";
        } else if (compareVersions(currentVersion, newVersion) < 0) {
            ChangeLogInfoText = "A new version is available! See what's new below:";
        } else {
            ChangeLogInfoText = "You are using a newer or unknown version.";
        }

        LabelComponent changelogLabel = Components.label(Text.literal(ChangeLogInfoText).styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))).shadow(false);

        FlowLayout divider = (FlowLayout) Containers.horizontalFlow(Sizing.fill(98), Sizing.fill(8))
                .surface(UiSurfaces.scaledContain(Identifier.of(MOD_ID, "textures/gui/menu/divider.png"), 2401, 96));

        var markdownComponent = COMPONENT_CACHE.computeIfAbsent(
                changelog += """
                        
                        ---
                        
                        Need help **updating**? Click the button below.
                        """,
                MARKDOWN_PROCESSOR::process
        );
        markdownComponent.horizontalSizing(Sizing.fill(98));
        markdownComponent.padding(Insets.of(0, 4, 4, 4));


        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(Sizing.fill(98), Sizing.expand(), (FlowLayout) markdownComponent);
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.margins(Insets.bottom(10));

        changelogInfo.child(changelogLabel);
        changelogInfo.child(divider);
        mainLayout.child(changelogInfo);
        mainLayout.child(scrollContainer);

        return mainLayout;
    }

    private ButtonComponent createHypixelButton() {
        return (ButtonComponent) Components.button(
                        Text.literal("Join Hypixel").styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))),
                        button -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            ServerInfo serverInfo = new ServerInfo("Hypixel", PackCoreConfig.serverAddressForQuickJoinButton, ServerInfo.ServerType.OTHER);
                            ConnectScreen.connect(this, client, ServerAddress.parse(PackCoreConfig.serverAddressForQuickJoinButton), serverInfo, false, null);
                        }
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/blank_button.png"), 0, 0, 200, 66))
                .horizontalSizing(Sizing.fixed(200))
                .verticalSizing(Sizing.fixed(22));
    }

    private ButtonComponent createSingleplayerButton() {
        return (ButtonComponent) Components.button(
                        Text.literal("SINGLEPLAYER").styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))),
                        button -> MinecraftClient.getInstance().setScreen(new SelectWorldScreen(this))
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/blank_button.png"), 0, 0, 200, 66))
                .horizontalSizing(Sizing.fixed(200))
                .verticalSizing(Sizing.fixed(22));
    }

    private ButtonComponent createMultiplayerButton() {
        return (ButtonComponent) Components.button(
                        Text.literal("Multiplayer").styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))),
                        button -> MinecraftClient.getInstance().setScreen(new MultiplayerScreen(this))
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/blank_button.png"), 0, 0, 200, 66))
                .horizontalSizing(Sizing.fixed(200))
                .verticalSizing(Sizing.fixed(22));
    }

    private ButtonComponent createModsButton() {
        return (ButtonComponent) Components.button(
                        Text.literal("MODS").styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))),
                        button -> {
                            try {
                                Class<?> modMenuClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
                                MinecraftClient client = MinecraftClient.getInstance();
                                Screen modsScreen = (Screen) modMenuClass
                                        .getConstructor(Screen.class)
                                        .newInstance(client.currentScreen);
                                client.setScreen(modsScreen);
                            } catch (Exception e) {
                                LOGGER.error("Failed to open ModMenu screen", e);
                            }
                        }
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/blank_button.png"), 0, 0, 200, 66))
                .horizontalSizing(Sizing.fixed(200))
                .verticalSizing(Sizing.fixed(22));
    }

    private ButtonComponent createOptionsButton() {
        return (ButtonComponent) Components.button(
                        Text.literal("OPTIONS").styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))),
                        button -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            client.setScreen(new OptionsScreen(this, client.options));
                        }
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/blank_button.png"), 0, 0, 200, 66))
                .horizontalSizing(Sizing.fixed(200))
                .verticalSizing(Sizing.fixed(22));
    }

    private ButtonComponent createQuitButton() {
        return (ButtonComponent) Components.button(
                        Text.literal("QUIT").styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte"))),
                        button -> MinecraftClient.getInstance().scheduleStop()
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/blank_button.png"), 0, 0, 200, 66))
                .horizontalSizing(Sizing.fixed(200))
                .verticalSizing(Sizing.fixed(22));
    }

    private ButtonComponent createDiscordButton() {
        return (ButtonComponent) Components.button(
                        Text.empty(),
                        button -> {
                            Util.getOperatingSystem().open(info.getDiscord());
                        }
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/discord_icon.png"), 0, 0, 22, 22))
                .horizontalSizing(Sizing.fixed(22))
                .verticalSizing(Sizing.fixed(22))
                .tooltip(Text.literal("Join our Discord server"));
    }

    private ButtonComponent createModrinthButton() {
        return (ButtonComponent) Components.button(
                        Text.empty(),
                        button -> {
                            Util.getOperatingSystem().open(info.getWebsite());
                        }
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/modrinth_icon.png"), 0, 0, 22, 22))
                .horizontalSizing(Sizing.fixed(22))
                .verticalSizing(Sizing.fixed(22))
                .tooltip(Text.literal("Visit the modrinth page"));
    }

    private ButtonComponent createGitHubButton() {
        return (ButtonComponent) Components.button(
                        Text.empty(), button -> {
                            Util.getOperatingSystem().open(info.getIssueTracker());
                        }
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/github_icon.png"), 0, 0, 22, 22))
                .horizontalSizing(Sizing.fixed(22))
                .verticalSizing(Sizing.fixed(22))
                .tooltip(Text.literal("Report an issue"));
    }

    private FlowLayout createInfo() {
        FlowLayout mainLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        //#if MC >= 1.21.8
        //$$ LabelComponent versionLabel = Components.label(Text.literal("Pack Version: " + currentVersion).styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))).color(Color.BLACK);
        //#else
        LabelComponent versionLabel = Components.label(Text.literal("Pack Version: " + currentVersion).styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))).color(Color.ofArgb(0x030100));
        //#endif
        mainLayout.child(versionLabel);
        if (updateAvailable) {
            //#if MC >= 1.21.8
            //$$ LabelComponent updateAvailableLabel = Components.label(Text.literal("Update Available: " + newVersion).styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))).color(Color.BLACK);
            //#else
            LabelComponent updateAvailableLabel = Components.label(Text.literal("Update Available: " + newVersion).styled(s -> s.withFont(Identifier.of(MOD_ID, "gallaeciaforte")))).color(Color.ofArgb(0x030100));
            //#endif
            mainLayout.child(updateAvailableLabel);
        }

        return mainLayout;
    }

    private ButtonComponent createSeeWhatIsNewButton() {
        return (ButtonComponent) Components.button(
                        Text.empty(),
                        button -> toggleChangelog()
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/update_icon.png"), 0, 0, 22, 22))
                .horizontalSizing(Sizing.fixed(22))
                .verticalSizing(Sizing.fixed(22))
                .tooltip(Text.literal("See what's new"));
    }

    private ButtonComponent createHelpUpdateButton() {
        return (ButtonComponent) Components.button(
                        Text.empty(), button -> {
                            this.client.setScreen(new BaseGuidePage());
                        }
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/guide_icon.png"), 0, 0, 22, 22))
                .horizontalSizing(Sizing.fixed(22))
                .verticalSizing(Sizing.fixed(22))
                .tooltip(Text.literal("See Guides on how to use the modpack"));
    }

    private ButtonComponent createModpackButton() {
        return (ButtonComponent) Components.button(
                        Text.empty(), button -> {
                            this.client.setScreen(new ModpackConfigMenuScreen());
                        }
                )
                .renderer(ButtonComponent.Renderer.texture(Identifier.of(MOD_ID, "textures/gui/menu/settings_icon.png"), 0, 0, 22, 22))
                .horizontalSizing(Sizing.fixed(22))
                .verticalSizing(Sizing.fixed(22))
                .tooltip(Text.literal("Modpack Settings import/export your config"));
    }


    private void toggleChangelog() {
        showChangelog = !showChangelog;

        if (showChangelog) {
            // Hide main buttons and show changelog
            mainButtonLayout.remove();
            this.uiAdapter.rootComponent.child(changelogLayout);
        } else {
            // Hide changelog and show main buttons
            changelogLayout.remove();
            // Get the first child (which is the buttonAndTitle FlowLayout) and add mainButtonLayout back
            FlowLayout buttonAndTitle = (FlowLayout) this.uiAdapter.rootComponent.children().getFirst();
            buttonAndTitle.child(mainButtonLayout);
        }
    }

    public UpdateCheckResult checkForUpdates() {
        UpdateCacheManager updateManager = PackCore.getUpdateManager();
        ModpackInfo info = PackCore.getModpackInfo();

        if (updateManager == null || info == null) {
            LOGGER.error("Update system not initialized properly");
            return UpdateCheckResult.error("Update system not initialized properly");
        }

        // Check if the configuration is valid
        if (info.isConfigurationValid()) {
            this.updateAvailable = false;
            this.currentVersion = "";
            this.newVersion = "";
            this.changelog = "";
            this.modrinthUrl = "";
            this.modrinthName = "";
            LOGGER.warn("Skipping update check - configuration not properly set up: {}",
                    info.getValidationError());
            return UpdateCheckResult.error("Configuration not properly set up: " + info.getValidationError());
        }

        UpdateCheckResult result = updateManager.checkForUpdates(info);

        if (!result.isSuccess()) {
            LOGGER.error("Update check failed: {}", result.getErrorMessage());
            return result; // Return the error result
        }

        // Update instance variables
        this.updateAvailable = result.isUpdateAvailable();
        this.currentVersion = info.getVersion();
        this.newVersion = result.getVersionNumber();
        this.changelog = result.getChangelog();
        this.modrinthUrl = result.getModrinthUrl();
        this.modrinthName = info.getName();

        return result; // Return the successful result
    }

    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.replaceAll("[^0-9.]", "").split("\\.");
        String[] parts2 = v2.replaceAll("[^0-9.]", "").split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (p1 != p2) {
                return p1 - p2;
            }
        }

        return 0;
    }

}