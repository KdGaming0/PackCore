package com.github.kd_gaming1.packcore.command;

import com.github.kd_gaming1.packcore.util.diagnostics.DiagnosticsCollector;
import com.github.kd_gaming1.packcore.gui.screen.WelcomeWizardScreen;
import com.github.kd_gaming1.packcore.gui.screen.config.ConfigScreen;
import com.github.kd_gaming1.packcore.integration.ItemBackgroundManager;
import com.github.kd_gaming1.packcore.integration.ModernUIConfigurator;
import com.github.kd_gaming1.packcore.integration.PerformanceProfileService;
import com.github.kd_gaming1.packcore.integration.StorageDesignManager;
import com.github.kd_gaming1.packcore.integration.TabDesignManager;
import com.github.kd_gaming1.packcore.update.UpdateCache;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Arrays;
import java.util.Set;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class PackCoreCommands {

    private PackCoreCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        var root = literal("packcore")
                .then(literal("update")
                        .then(literal("check").executes(ctx -> {
                            checkUpdate(ctx.getSource());
                            return 1;
                        }))
                        .then(literal("reset").executes(ctx -> {
                            resetUpdateCache(ctx.getSource());
                            return 1;
                        })))
                .then(literal("performance")
                        .then(argument("profile", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    Arrays.stream(PerformanceProfileService.PerformanceProfile.values())
                                            .map(PerformanceProfileService.PerformanceProfile::id)
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    applyPerformanceProfile(
                                            ctx.getSource(), StringArgumentType.getString(ctx, "profile"));
                                    return 1;
                                })))
                .then(literal("tabdesign")
                        .then(literal("compact").executes(ctx -> {
                            applyTabDesign(ctx.getSource(), TabDesignManager.TabDesign.COMPACT);
                            return 1;
                        }))
                        .then(literal("fancy").executes(ctx -> {
                            applyTabDesign(ctx.getSource(), TabDesignManager.TabDesign.FANCY);
                            return 1;
                        })))
                .then(literal("itembg")
                        .then(literal("none").executes(ctx -> {
                            applyItemBackground(ctx.getSource(), ItemBackgroundManager.ItemBackground.NONE);
                            return 1;
                        }))
                        .then(literal("circle").executes(ctx -> {
                            applyItemBackground(ctx.getSource(), ItemBackgroundManager.ItemBackground.CIRCLE);
                            return 1;
                        }))
                        .then(literal("square").executes(ctx -> {
                            applyItemBackground(ctx.getSource(), ItemBackgroundManager.ItemBackground.SQUARE);
                            return 1;
                        })))
                .then(literal("storagedesign")
                        .then(literal("overlay").executes(ctx -> {
                            applyStorageDesign(ctx.getSource(), StorageDesignManager.StorageDesign.OVERLAY);
                            return 1;
                        }))
                        .then(literal("vanilla").executes(ctx -> {
                            applyStorageDesign(ctx.getSource(), StorageDesignManager.StorageDesign.VANILLA);
                            return 1;
                        })))
                .then(literal("wizard").executes(ctx -> {
                    Minecraft.getInstance().execute(() ->
                            Minecraft.getInstance().setScreen(
                                    new WelcomeWizardScreen(Minecraft.getInstance().screen)));
                    return 1;
                }))
                .then(literal("modpack_config").executes(ctx -> {
                    Minecraft.getInstance().execute(() ->
                            Minecraft.getInstance().setScreen(new ConfigScreen()));
                    return 1;
                }))
                .then(literal("diagnose").executes(ctx -> {
                    sendDiagnostics(ctx.getSource());
                    return 1;
                }))
                .then(literal("crashtest").executes(ctx -> {
                    triggerTestCrash();
                    return 1;
                }));

        // Modern UI subcommands -- only registered when the mod is present
        if (FabricLoader.getInstance().isModLoaded("modernui")) {
            root.then(literal("modernui")
                    .then(literal("font")
                            .then(literal("vanilla").executes(ctx -> {
                                applyModernUIFont(ctx.getSource(), false);
                                return 1;
                            }))
                            .then(literal("custom").executes(ctx -> {
                                applyModernUIFont(ctx.getSource(), true);
                                return 1;
                            })))
                    .then(literal("tooltip")
                            .then(literal("on").executes(ctx -> {
                                applyModernUIToggle(ctx.getSource(), "fancyTooltip", true);
                                return 1;
                            }))
                            .then(literal("off").executes(ctx -> {
                                applyModernUIToggle(ctx.getSource(), "fancyTooltip", false);
                                return 1;
                            })))
                    .then(literal("ding")
                            .then(literal("on").executes(ctx -> {
                                applyModernUIToggle(ctx.getSource(), "dingSound", true);
                                return 1;
                            }))
                            .then(literal("off").executes(ctx -> {
                                applyModernUIToggle(ctx.getSource(), "dingSound", false);
                                return 1;
                            }))));
        }

        dispatcher.register(root);
    }

    // ---------------------------------------------------------------------------
    // Diagnostics
    // ---------------------------------------------------------------------------

    private static void sendDiagnostics(FabricClientCommandSource source) {
        String report = DiagnosticsCollector.buildCompactReport();

        for (String line : report.split("\n")) {
            source.sendFeedback(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }

        source.sendFeedback(
                Component.literal("[PackCore] ")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(
                                Component.literal(" [ Click to copy ] ")
                                        .withStyle(
                                                Style.EMPTY
                                                        .withColor(ChatFormatting.AQUA)
                                                        .withUnderlined(true)
                                                        .withClickEvent(
                                                                new ClickEvent.CopyToClipboard(report)))));
    }

    private static void triggerTestCrash() {
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().delayCrash(
                        CrashReport.forThrowable(
                                new Throwable("PackCore crash report test"),
                                "PackCore crashtest command")));
    }

    // ---------------------------------------------------------------------------
    // Handlers
    // ---------------------------------------------------------------------------

    private static void checkUpdate(FabricClientCommandSource source) {
        send(source, "Checking for updates...");
        UpdateChecker.checkAsync().thenAccept(status ->
                Minecraft.getInstance().execute(() -> {
                    switch (status.state()) {
                        case UP_TO_DATE ->
                                send(source, "You are up to date! Version: " + status.installedVersion());
                        case UPDATE_AVAILABLE -> {
                            send(source, "Update available!");
                            send(source, "Installed: " + status.installedVersion());
                            send(source, "Latest: " + status.latestVersion());
                            if (status.changelog() != null && !status.changelog().isBlank()) {
                                send(source, "Changelog:");
                                send(source, status.changelog());
                            }
                        }
                        case UNKNOWN -> sendError(source, "Could not determine update status.");
                    }
                }));
    }

    private static void resetUpdateCache(FabricClientCommandSource source) {
        UpdateCache.invalidate();
        send(source, "Update cache cleared. Next check will fetch from Modrinth.");
    }

    private static void applyStorageDesign(
            FabricClientCommandSource source, StorageDesignManager.StorageDesign design) {
        String name = design.name().toLowerCase();
        send(source, "Applying storage design: " + name + "...");
        if (StorageDesignManager.apply(design)) {
            send(source, "Storage design applied: " + name
                    + ". If not in a world yet, the change will take effect on next world join.");
        } else {
            sendError(source, "Failed to apply storage design: " + name
                    + ". Firmament may not be loaded -- check logs.");
        }
    }

    private static void applyItemBackground(
            FabricClientCommandSource source, ItemBackgroundManager.ItemBackground background) {
        String name = background.name().toLowerCase();
        send(source, "Applying item background: " + name + "...");
        if (ItemBackgroundManager.apply(background)) {
            send(source, "Item background applied: " + name);
        } else {
            sendError(source, "Failed to apply item background: " + name
                    + ". Skyblocker may not be loaded -- check logs.");
        }
    }

    private static void applyTabDesign(
            FabricClientCommandSource source, TabDesignManager.TabDesign design) {
        String name = design.name().toLowerCase();
        send(source, "Applying tab design: " + name + "...");
        if (TabDesignManager.apply(design)) {
            send(source, "Tab design applied: " + name);
        } else {
            sendError(source, "Failed to apply tab design: " + name + ". Check logs for details.");
        }
    }

    private static void applyPerformanceProfile(FabricClientCommandSource source, String id) {
        PerformanceProfileService.PerformanceProfile profile =
                Arrays.stream(PerformanceProfileService.PerformanceProfile.values())
                        .filter(p -> p.id().equals(id))
                        .findFirst()
                        .orElse(null);

        if (profile == null) {
            String valid = Arrays.stream(PerformanceProfileService.PerformanceProfile.values())
                    .map(PerformanceProfileService.PerformanceProfile::id)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            sendError(source, "Unknown performance profile: \"" + id + "\". Valid options: " + valid);
            return;
        }

        send(source, "Applying performance profile: " + profile.getDisplayName() + "...");
        if (PerformanceProfileService.applyAll(profile)) {
            send(source, "Performance profile applied: " + profile.getDisplayName());
        } else {
            sendError(source, "One or more integrations failed for profile: "
                    + profile.getDisplayName() + ". Check logs for details.");
        }
    }

    private static void applyModernUIFont(FabricClientCommandSource source, boolean custom) {
        String label = custom ? "custom" : "vanilla";
        send(source, "Switching Modern UI font to " + label + "...");
        try {
            Set<String> features = buildCurrentFeatures();
            if (custom) features.add("customFont"); else features.remove("customFont");
            ModernUIConfigurator.apply(features);
            send(source, "Font set to " + label + ".");
        } catch (Exception e) {
            sendError(source, "Failed to switch font: " + e.getMessage());
        }
    }

    private static void applyModernUIToggle(FabricClientCommandSource source, String feature, boolean enable) {
        send(source, "Setting Modern UI " + feature + " to " + enable + "...");
        try {
            Set<String> features = buildCurrentFeatures();
            if (enable) features.add(feature); else features.remove(feature);
            ModernUIConfigurator.apply(features);
            send(source, feature + " set to " + enable + ".");
        } catch (Exception e) {
            sendError(source, "Failed to update " + feature + ": " + e.getMessage());
        }
    }

    /**
     * Builds a mutable feature set reflecting the current live config state, so a single-feature
     * command doesn't inadvertently reset the other features.
     */
    private static Set<String> buildCurrentFeatures() {
        Set<String> features = new java.util.HashSet<>();
        if (ModernUIConfigurator.isCustomFontEnabled()) features.add("customFont");
        if (ModernUIConfigurator.isTooltipEnabled()) features.add("fancyTooltip");
        if (ModernUIConfigurator.isDingEnabled()) features.add("dingSound");
        return features;
    }

    // ---------------------------------------------------------------------------
    // Feedback helpers
    // ---------------------------------------------------------------------------

    private static void send(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal("[PackCore] " + message));
    }

    private static void sendError(FabricClientCommandSource source, String message) {
        source.sendError(Component.literal("[PackCore] " + message));
    }
}