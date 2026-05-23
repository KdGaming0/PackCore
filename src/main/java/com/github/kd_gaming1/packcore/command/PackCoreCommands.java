package com.github.kd_gaming1.packcore.command;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.util.diagnostics.DiagnosticsCollector;
import com.github.kd_gaming1.packcore.gui.screen.WelcomeWizardScreen;
import com.github.kd_gaming1.packcore.gui.screen.config.ConfigScreen;
import com.github.kd_gaming1.packcore.gui.wizard.page.CaxtonFontPage;
import com.github.kd_gaming1.packcore.integration.DungeonRoutesManager;
import com.github.kd_gaming1.packcore.integration.ItemBackgroundManager;
import com.github.kd_gaming1.packcore.integration.PerformanceProfileService;
import com.github.kd_gaming1.packcore.integration.ResourcePackManager;
import com.github.kd_gaming1.packcore.integration.StorageDesignManager;
import com.github.kd_gaming1.packcore.integration.TabDesignManager;
import com.github.kd_gaming1.packcore.update.UpdateCache;
import com.github.kd_gaming1.packcore.update.UpdateChecker;
import com.github.kd_gaming1.packcore.warning.CaxtonShaderConflictWarner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Arrays;
import java.util.Set;

import com.mojang.brigadier.context.CommandContext;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

//? if >=26.1 {
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
//?} else {
/*import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
*///?}

public class PackCoreCommands {

    private PackCoreCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("packcore")
                .executes(PackCoreCommands::executeOpenConfig)
                .then(literal("config")
                        .executes(PackCoreCommands::executeOpenConfig))
                .then(literal("update")
                        .then(literal("check").executes(ctx -> {
                            checkUpdate(ctx.getSource());
                            return 1;
                        }))
                        .then(literal("reset").executes(ctx -> {
                            resetUpdateCache(ctx.getSource());
                            return 1;
                        })))
                .then(literal("performance-profile")
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
                .then(literal("dungeonroutes")
                        .then(literal("skyblocker").executes(ctx -> {
                            applyDungeonRoutes(ctx.getSource(), DungeonRoutesManager.DungeonRoutesMode.SKYBLOCKER_WAYPOINTS);
                            return 1;
                        }))
                        .then(literal("secretroutesmod").executes(ctx -> {
                            applyDungeonRoutes(ctx.getSource(), DungeonRoutesManager.DungeonRoutesMode.SECRET_ROUTES_MOD);
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
                .then(literal("font")
                        .then(argument("font", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    Arrays.asList("none", "opensans", "inter", "thickinter")
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    applyCaxtonFont(ctx.getSource(), StringArgumentType.getString(ctx, "font"));
                                    return 1;
                                })))
                .then(literal("diagnose").executes(ctx -> {
                    sendDiagnostics(ctx.getSource());
                    return 1;
                }))
                .then(literal("crashtest").executes(ctx -> {
                    triggerTestCrash();
                    return 1;
                }))
                .then(literal("ignore-shader-warning").executes(ctx -> {
                    CaxtonShaderConflictWarner.ignoreWarning();
                    ctx.getSource().sendFeedback(
                            Component.literal("Shader/Font warnings ignored for this session")
                                    .withStyle(ChatFormatting.GREEN)
                    );
                    return 1;
                }))
                .then(literal("enable-shader-warning").executes(ctx -> {
                    CaxtonShaderConflictWarner.enableWarnings();
                    ctx.getSource().sendFeedback(
                            Component.literal("Shader/Font warnings re-enabled")
                                    .withStyle(ChatFormatting.GREEN)
                    );
                    return 1;
                })));

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
            send(source, "Storage design applied: " + name + ".");
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

    private static void applyDungeonRoutes(
            FabricClientCommandSource source, DungeonRoutesManager.DungeonRoutesMode mode) {
        String name = mode.name().toLowerCase().replace("_", " ");
        send(source, "Applying dungeon routes: " + name + "...");
        if (DungeonRoutesManager.apply(mode)) {
            send(source, "Dungeon routes applied: " + name);
        } else {
            sendError(source, "Failed to apply dungeon routes: " + name
                    + ". Check that Skyblocker and/or Secret Routes Mod are loaded. See logs for details.");
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

    private static void applyCaxtonFont(FabricClientCommandSource source, String fontId) {
        String normalized = fontId == null ? "" : fontId.trim().toLowerCase();

        Set<String> caxtonPackIds = CaxtonFontPage.FontOption.all().stream()
                .map(CaxtonFontPage.FontOption::packId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> selectedPackIds = switch (normalized) {
            case "none" -> Set.of();
            case "opensans", "open_sans" -> Set.of("caxton:opensans");
            case "inter" -> Set.of("caxton:inter");
            case "thickinter", "thick_inter" -> Set.of("file/ThickInter.zip");
            default -> {
                sendError(source, "Unknown Caxton font: \"" + fontId + "\". Valid options: none, opensans, inter, thickinter");
                yield null;
            }
        };

        if (selectedPackIds == null) return;

        send(source, "Applying Caxton font: " + normalized + "...");
        ResourcePackManager.apply(selectedPackIds, caxtonPackIds);
        send(source, "Caxton font command issued: " + normalized + ".");
    }

    /**
     * Opens the configuration menu.
     * Uses client.send() to delay opening until after the chat closes.
     */
    private static int executeOpenConfig(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            sendError(ctx);
            return 0;
        }

        client.schedule(() -> {
            try {
                client.setScreen(MidnightConfig.getScreen(client.screen, PackCore.MOD_ID));
            } catch (Exception e) {
                PackCore.LOGGER.error("Failed to open config menu", e);
            }
        });

        sendSuccess(ctx);
        return 1;
    }

    // ---------------------------------------------------------------------------
    // Feedback helpers
    // ---------------------------------------------------------------------------

    private static void send(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal("[PackCore] " + message));
    }

    private static void sendSuccess(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Component.literal("[PackCore] Opening configuration menu...").withStyle(ChatFormatting.GREEN));
    }

    private static void sendError(FabricClientCommandSource source, String message) {
        source.sendError(Component.literal("[PackCore] " + message));
    }

    private static void sendError(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendError(Component.literal("[PackCore] You must be in-game to open the config menu.").withStyle(ChatFormatting.RED));
    }
}
