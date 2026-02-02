package com.github.kd_gaming1.packcore.ui.screen.title;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.ui.surface.effects.TextureSurfaces;
import com.github.kd_gaming1.packcore.ui.screen.configmanager.ConfigManagerScreen;
import com.github.kd_gaming1.packcore.ui.help.guide.GuideListScreen;
import com.github.kd_gaming1.packcore.modpack.ModpackInfo;
import com.github.kd_gaming1.packcore.util.update.modrinth.UpdateCache;
import com.github.kd_gaming1.packcore.util.update.UpdateResult;
import com.github.kd_gaming1.packcore.notification.UpdateNotifier;
import com.github.kd_gaming1.packcore.ui.toast.PackCoreToast;
import com.terraformersmc.modmenu.api.ModMenuApi;
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
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.kd_gaming1.packcore.PackCore.MOD_ID;
import static com.github.kd_gaming1.packcore.ui.theme.UITheme.*;

/**
 * Improved styled title screen with better component organization
 */
public class SBEStyledTitleScreen extends BaseOwoScreen<FlowLayout> {

    private final Identifier backgroundTexture = Identifier.of(MOD_ID, "textures/gui/title/main_menu_background.png");
    private static final ModpackInfo info = PackCore.getModpackInfo();

    private static long lastToastTime = 0;
    private static final long TOAST_COOLDOWN_MS = 5 * 60 * 1000;

    private static long lastRamWarningTime = 0;
    private static final long RAM_WARNING_COOLDOWN_MS = 5 * 60 * 1000;

    // Update state
    private final boolean updateNotificationEnabled =
            info != null &&
                    PackCoreConfig.showUpdateNotificationsOnTitleScreen &&
                    PackCoreConfig.enableUpdateNotifications;
    private boolean updateAvailable;
    private String currentVersion;
    private String newVersion;
    private String changelog;
    private String modrinthName;

    // UI state
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
    protected void build(FlowLayout rootUIComponent) {
        rootUIComponent.surface(TextureSurfaces.stretched(backgroundTexture, 1920, 1082));

        // Main components
        rootUIComponent.child(createMainButtonAndTitle()).horizontalAlignment(HorizontalAlignment.CENTER);
        rootUIComponent.child(createSocialButtons().positioning(Positioning.relative(0, 100)));
        rootUIComponent.child(createSeeWhatIsNewButtons().positioning(Positioning.relative(100, 0)));
        rootUIComponent.child(createModpackButtons().positioning(Positioning.relative(100, 100)));

        // Create changelog layout but don't add it initially
        changelogLayout = createChangelogPanel();
    }

    @Override
    public void init() {
        UpdateResult result = checkForUpdates();

        if (result.isSuccess() && result.isUpdateAvailable() && updateNotificationEnabled) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastToastTime > TOAST_COOLDOWN_MS && UpdateNotifier.shouldShowMainMenuToast(result.getVersionNumber())) {
                PackCoreToast.showUpdateAvailable(currentVersion, newVersion, modrinthName);
                lastToastTime = currentTime;
            }
        }

        if (!result.isSuccess()) {
            PackCore.LOGGER.warn("Update check failed: {}", result.getErrorMessage());
        }

        if (PackCoreConfig.showLowMemoryWarning) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRamWarningTime > RAM_WARNING_COOLDOWN_MS) {
                checkAndWarnLowMemory();
            }
        }

        super.init();
    }

    /**
     * Check allocated RAM and show warning if less than 3GB
     */
    private void checkAndWarnLowMemory() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        double maxMemoryGB = maxMemory / (1024.0 * 1024.0 * 1024.0);

        if (maxMemoryGB < PackCoreConfig.minimumRamGB) {
            PackCoreToast.showWarning(
                    "Low Memory Allocation",
                    String.format("Only %.1f GB allocated! Recommend %.0fGB+ for optimal performance.", maxMemoryGB, (double) PackCoreConfig.minimumRamGB)
            );
            lastRamWarningTime = System.currentTimeMillis();
        }
    }

    /**
     * Create main button area and title
     */
    private FlowLayout createMainButtonAndTitle() {
        FlowLayout buttonAndTitle = (FlowLayout) Containers.verticalFlow(Sizing.fixed(320), Sizing.fill(100))
                .gap(4)
                .padding(Insets.of(4))
                .margins(Insets.of(4, 4, 4, 4));

        // Title texture
        TextureComponent title = (TextureComponent) Components.texture(
                        Identifier.of(MOD_ID, "textures/gui/title/title.png"),
                        0, 0, 1476, 157, 1476, 157
                )
                .margins(Insets.top(8))
                .horizontalSizing(Sizing.fixed(312))
                .verticalSizing(Sizing.fixed(34));

        // Button layout
        mainButtonLayout = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(8))
                .margins(Insets.top(12));

        // Add all buttons
        ButtonComponent joinHypixel = createButton("Join Hypixel", this::joinHypixel);
        ButtonComponent openSingleplayer = createButton("SINGLEPLAYER", this::openSingleplayer);
        ButtonComponent openMultiplayer = createButton("Multiplayer", this::openMultiplayer);
        ButtonComponent openMods = createButton("MODS", this::openMods);
        ButtonComponent openOptions = createButton("OPTIONS", this::openOptions);

        mainButtonLayout
                .child(joinHypixel)
                .child(openSingleplayer)
                .child(openMultiplayer)
                .child(openMods)
                .child(openOptions)
                .child(createButton("QUIT", button -> MinecraftClient.getInstance().scheduleStop()));


        buttonAndTitle.child(title);
        buttonAndTitle.child(mainButtonLayout);

        return buttonAndTitle;
    }

    /**
     * Create a standard button
     */
    private ButtonComponent createButton(String text, ButtonComponent.PressAction action) {
        return (ButtonComponent) Components.button(
                        Text.literal(text)
                                .styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte")))),
                        action::onPress
                )
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/menu/blank_button.png"), 0, 0, 200, 66))
                .horizontalSizing(Sizing.fixed(200))
                .verticalSizing(Sizing.fixed(22));
    }


    /**
     * Create an icon button
     */
    private ButtonComponent createIconButton(String texture, String tooltip, Runnable action) {
        return (ButtonComponent) Components.button(
                        Text.empty(),
                        button -> action.run()
                )
                .renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, texture), 0, 0, 22, 22))
                .horizontalSizing(Sizing.fixed(22))
                .verticalSizing(Sizing.fixed(22))
                .tooltip(Text.literal(tooltip));
    }

    /**
     * Create social buttons panel
     */
    private FlowLayout createSocialButtons() {
        FlowLayout buttonLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(6)
                .horizontalAlignment(HorizontalAlignment.LEFT)
                .padding(Insets.of(4));

        buttonLayout
                .child(createIconButton("textures/gui/menu/discord_icon.png",
                        "Join our Discord server",
                        () -> Util.getOperatingSystem().open(info.getDiscord())))
                .child(createIconButton("textures/gui/menu/modrinth_icon.png",
                        "Visit the modrinth page",
                        () -> Util.getOperatingSystem().open(info.getWebsite())))
                .child(createIconButton("textures/gui/menu/github_icon.png",
                        "Report an issue",
                        () -> Util.getOperatingSystem().open(info.getIssueTracker())))
                .child(createVersionInfo());

        return buttonLayout;
    }

    /**
     * Create version info display
     */
    private FlowLayout createVersionInfo() {
        FlowLayout mainLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        LabelComponent versionLabel = Components.label(
                Text.literal("Pack Version: " + currentVersion)
                        .styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte"))))
        ).color(Color.ofArgb(TEXT_DARK));

        mainLayout.child(versionLabel);

        if (updateAvailable) {
            LabelComponent updateAvailableLabel = Components.label(
                    Text.literal("Update Available: " + newVersion)
                            .styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte"))))
            ).color(Color.ofArgb(TEXT_DARK));
            mainLayout.child(updateAvailableLabel);
        }

        return mainLayout;
    }

    /**
     * Create see what's new button
     */
    private FlowLayout createSeeWhatIsNewButtons() {
        FlowLayout buttonLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(6)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .padding(Insets.of(4));

        buttonLayout.child(createIconButton("textures/gui/menu/update_icon.png",
                "See what's new",
                this::toggleChangelog));

        return buttonLayout;
    }

    /**
     * Create modpack buttons
     */
    private FlowLayout createModpackButtons() {
        FlowLayout buttonLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(6)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .padding(Insets.of(4));

        buttonLayout
                .child(createIconButton("textures/gui/menu/settings_icon.png",
                        "Modpack Settings import/export your config",
                        () -> {
                            assert this.client != null;
                            this.client.setScreen(new ConfigManagerScreen());
                        }))
                .child(createIconButton("textures/gui/menu/guide_icon.png",
                        "See Guides on how to use the modpack",
                        () -> {
                            assert this.client != null;
                            this.client.setScreen(new GuideListScreen());
                        }));

        return buttonLayout;
    }

    /**
     * Create changelog panel with help button
     */
    private FlowLayout createChangelogPanel() {
        FlowLayout mainLayout = (FlowLayout) Containers.verticalFlow(Sizing.fill(65), Sizing.fill(75))
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(4))
                .surface(TextureSurfaces.stretched(
                        Identifier.of(MOD_ID, "textures/gui/menu/info_box.png"), 1142, 934))
                .margins(Insets.of(4, 4, 4, 4))
                .positioning(Positioning.relative(50, 75));

        // Header section
        FlowLayout changelogInfo = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(2)
                .padding(Insets.of(6, 0, 8, 8))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        // Determine status text
        String changeLogInfoText;
        if (Objects.equals(currentVersion, newVersion)) {
            changeLogInfoText = "You are up to date! See change log for current version below:";
        } else if (compareVersions(currentVersion, newVersion) < 0) {
            changeLogInfoText = "A new version is available! See what's new below:";
        } else {
            changeLogInfoText = "You are using a newer or unknown version.";
        }

        LabelComponent changelogLabel = Components.label(
                Text.literal(changeLogInfoText)
                        .styled(s -> s.withFont(new StyleSpriteSource.Font(Identifier.of(MOD_ID, "gallaeciaforte"))))
        ).shadow(false);

        // Divider
        FlowLayout divider = (FlowLayout) Containers.horizontalFlow(Sizing.fill(98), Sizing.fill(8))
                .surface(TextureSurfaces.scaledContain(
                        Identifier.of(MOD_ID, "textures/gui/menu/divider.png"), 2401, 96));

        changelogInfo.child(changelogLabel);
        changelogInfo.child(divider);
        mainLayout.child(changelogInfo);

        // Changelog content with markdown
        String changelogContent = changelog != null ? changelog : "No changelog available.";

        // Process markdown
        var markdownUIComponent = COMPONENT_CACHE.computeIfAbsent(
                changelogContent,
                MARKDOWN_PROCESSOR::process
        );
        markdownUIComponent.horizontalSizing(Sizing.fill(98));
        markdownUIComponent.padding(Insets.of(0, 4, 4, 4));

        // Scrollable content
        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(98),
                Sizing.expand(),
                (FlowLayout) markdownUIComponent
        );
        scrollContainer.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollContainer.margins(Insets.bottom(10));

        mainLayout.child(scrollContainer);

        // Help button section
        FlowLayout buttonSection = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(8));

        ButtonComponent helpButton = (ButtonComponent) Components.button(
                        Text.literal("📚 Open Update Guide"),
                        button -> {
                            // Open the guide screen and close the changelog
                            assert this.client != null;
                            this.client.setScreen(new GuideListScreen(this));
                            toggleChangelog(); // Hide changelog when opening guides
                        }
                ).renderer(ButtonComponent.Renderer.texture(
                        Identifier.of(MOD_ID, "textures/gui/wizard/button.png"), 0, 0, 180, 60))
                .horizontalSizing(Sizing.fixed(180))
                .verticalSizing(Sizing.fixed(20));

        buttonSection.child(helpButton);
        mainLayout.child(buttonSection);

        return mainLayout;
    }

    /**
     * Toggle changelog visibility
     */
    private void toggleChangelog() {
        showChangelog = !showChangelog;

        if (showChangelog) {
            // Hide main buttons and show changelog
            mainButtonLayout.remove();
            this.uiAdapter.rootComponent.child(changelogLayout);
        } else {
            // Hide changelog and show main buttons
            changelogLayout.remove();
            FlowLayout buttonAndTitle = (FlowLayout) this.uiAdapter.rootComponent.children().getFirst();
            buttonAndTitle.child(mainButtonLayout);
        }
    }

    // ===== Button Actions =====

    private void joinHypixel(ButtonWidget button) {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo serverInfo = new ServerInfo("Hypixel",
                PackCoreConfig.serverAddressForQuickJoinButton,
                ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, client,
                ServerAddress.parse(PackCoreConfig.serverAddressForQuickJoinButton),
                serverInfo, false, null);
    }

    private void openSingleplayer(ButtonWidget button) {
        MinecraftClient.getInstance().setScreen(new SelectWorldScreen(this));
    }

    private void openMultiplayer(ButtonWidget button) {
        MinecraftClient.getInstance().setScreen(new MultiplayerScreen(this));
    }

    private void openMods(ButtonWidget button) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen current = client.currentScreen;

        try {
            Screen modsScreen = ModMenuApi.createModsScreen(current);
            client.setScreen(modsScreen);
        } catch (Throwable t) {
            PackCore.LOGGER.error("Failed to open Mod Menu screen", t);
            PackCoreToast.showError("Mod Menu Error", "Could not open Mod Menu");
        }
    }

    private void openOptions(ButtonWidget button) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new OptionsScreen(this, client.options));
    }

    // ===== Update Check Logic =====

    public UpdateResult checkForUpdates() {
        UpdateCache updateManager = PackCore.getUpdateManager();
        ModpackInfo info = PackCore.getModpackInfo();

        if (updateManager == null || info == null) {
            PackCore.LOGGER.error("Update system not initialized properly");
            // Initialize defaults to prevent NPEs in UI
            this.currentVersion = "Unknown";
            this.newVersion = "Unknown";
            this.modrinthName = "Modpack";
            this.changelog = "Update system error";
            return UpdateResult.error("Update system not initialized properly");
        }

        // Always initialize local state from info first to ensure variables are not null
        this.currentVersion = info.getVersion() != null ? info.getVersion() : "Unknown";
        this.modrinthName = info.getName();
        // Default newVersion to currentVersion so equality checks in build() don't fail
        this.newVersion = this.currentVersion;

        // Check if the configuration is valid
        if (info.isConfigurationValid()) {
            this.updateAvailable = false;
            this.changelog = "Configuration invalid: " + info.getValidationError();
            PackCore.LOGGER.warn("Skipping update check - configuration not properly set up: {}",
                    info.getValidationError());
            return UpdateResult.error("Configuration not properly set up: " + info.getValidationError());
        }

        UpdateResult result = updateManager.checkForUpdates(info);

        if (!result.isSuccess()) {
            PackCore.LOGGER.error("Update check failed: {}", result.getErrorMessage());
            // Set error info for the UI
            this.updateAvailable = false;
            this.changelog = "Failed to check for updates.\n\nError: " + result.getErrorMessage();
            return result;
        }

        // Update instance variables with remote data
        this.updateAvailable = result.isUpdateAvailable();
        this.newVersion = result.getVersionNumber();
        this.changelog = result.getChangelog();

        return result;
    }

    /**
     * Compare version strings
     */
    public static int compareVersions(String v1, String v2) {
        // Treat nulls explicitly
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        String[] parts1 = v1.replaceAll("[^0-9.]", "").split("\\.");
        String[] parts2 = v2.replaceAll("[^0-9.]", "").split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int p1 = 0;
            int p2 = 0;

            if (i < parts1.length && !parts1[i].isEmpty()) {
                try { p1 = Integer.parseInt(parts1[i]); } catch (NumberFormatException ignored) { p1 = 0; }
            }

            if (i < parts2.length && !parts2[i].isEmpty()) {
                try { p2 = Integer.parseInt(parts2[i]); } catch (NumberFormatException ignored) { p2 = 0; }
            }

            if (p1 != p2) return p1 - p2;
        }

        return 0;
    }
}