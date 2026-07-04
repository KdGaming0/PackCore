package com.github.kd_gaming1.packcore;

import com.github.kd_gaming1.packcore.command.PackCoreCommands;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.gui.screen.PackCoreTitleScreen;
import com.github.kd_gaming1.packcore.gui.screen.SBETitleScreen;
import com.github.kd_gaming1.packcore.gui.screen.WelcomeWizardScreen;
import com.github.kd_gaming1.packcore.gui.wizard.WizardStep;
import com.github.kd_gaming1.packcore.gui.wizard.WizardSteps;
import com.github.kd_gaming1.packcore.gui.wizard.WizardVersionStore;
import com.github.kd_gaming1.packcore.migration.ConfigMigrationRunner;
import com.github.kd_gaming1.packcore.playtime.PlaytimeTracker;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.util.CaxtonFontDetector;
import com.github.kd_gaming1.packcore.util.RamWarningHelper;
import com.github.kd_gaming1.packcore.util.diagnostics.DiagnosticsCollector;
import com.github.kd_gaming1.packcore.warning.CaxtonShaderConflictWarner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class PackCore implements ClientModInitializer {

    public static final String MOD_ID = "packcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Path PACKCORE_DIR =
            FabricLoader.getInstance().getGameDir().resolve(MOD_ID);

    private static final Identifier CAXTON_PROBE_RELOAD_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "caxton_probe");

    private static boolean clientFullyStarted = false;
    private static boolean replacingTitleScreen = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PackCore] Initialized\n{}", DiagnosticsCollector.buildFullReport());

        RamWarningHelper.init();
        UpdateChecker.checkAsync();
        CaxtonShaderConflictWarner.init();

        registerCommands();
        registerTitleScreenSwapping();
        registerWorldJoinHandlers();
        registerLifecycleHandlers();
        registerCaxtonProbeReloadListener();
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> PackCoreCommands.register(dispatcher));
    }

    /**
     * Subsequent returns to TitleScreen (e.g. disconnect) are handled here.
     * The initial startup swap happens in {@code CLIENT_STARTED} to avoid
     * racing the loading overlay.
     */
    private static void registerTitleScreenSwapping() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!isVanillaTitleScreen(screen)) return;
            RamWarningHelper.onMainMenu();
            if (!clientFullyStarted) return;
            if (PackCoreConfig.menuStyle != PackCoreConfig.MenuStyle.MINIMAL) {
                scheduleConfiguredTitleScreen(client, screen);
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!isVanillaTitleScreen(screen)) return;
            // Only decorate once the user has been through the wizard at least once.
            if (!WizardVersionStore.fileExists()) return;
            if (PackCoreConfig.menuStyle != PackCoreConfig.MenuStyle.MINIMAL) return;
            PackCoreTitleScreen.decorateExisting((TitleScreen) screen, scaledWidth, scaledHeight);
        });
    }

    private static void registerWorldJoinHandlers() {
        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> client.execute(RamWarningHelper::onWorldJoin));
    }

    private static void registerLifecycleHandlers() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            PlaytimeTracker.onSessionStart();
            clientFullyStarted = true;

            // All mods' configs are initialized by now — safe to force one-shot cross-mod config changes.
            ConfigMigrationRunner.run();

            if (isVanillaTitleScreen(client.screen)
                    && PackCoreConfig.menuStyle != PackCoreConfig.MenuStyle.MINIMAL) {
                applyConfiguredTitleScreen(client, client.screen);
            }

            CaxtonFontDetector.recompute();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PlaytimeTracker.onSessionEnd());
    }

    private static void registerCaxtonProbeReloadListener() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return CAXTON_PROBE_RELOAD_ID;
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager mgr) {
                        CaxtonFontDetector.recompute();
                    }
                }
        );
    }

    private static boolean isVanillaTitleScreen(Screen screen) {
        return screen instanceof TitleScreen && !(screen instanceof PackCoreTitleScreen);
    }

    private static void scheduleConfiguredTitleScreen(Minecraft client, Screen screen) {
        if (replacingTitleScreen) return;
        replacingTitleScreen = true;
        client.execute(() -> {
            replacingTitleScreen = false;
            // Bail if the user has already navigated elsewhere by the time
            // the deferred runnable executes.
            if (client.screen != screen) return;
            applyConfiguredTitleScreen(client, screen);
        });
    }

    private static void applyConfiguredTitleScreen(Minecraft client, Screen screen) {
        // The wizard opens for any page the user has not yet applied at its current version. An empty
        // store — a brand-new user, or anyone upgrading from before page tracking existed — shows the
        // full wizard (with intro); otherwise only the new or changed pages are shown.
        WizardVersionStore store = WizardVersionStore.load();
        List<WizardStep> pending = WizardSteps.pending(store);
        if (!pending.isEmpty()) {
            client.setScreen(store.isEmpty()
                    ? WelcomeWizardScreen.full(screen)
                    : WelcomeWizardScreen.forSteps(screen, pending.stream().map(WizardStep::id).toList()));
            return;
        }

        switch (PackCoreConfig.menuStyle) {
            case MODERN -> client.setScreen(new SBETitleScreen());
            case MODERN_MINIMAL -> client.setScreen(new SBETitleScreen(false));
            case MINIMAL -> client.setScreen(new PackCoreTitleScreen());
        }
    }
}
