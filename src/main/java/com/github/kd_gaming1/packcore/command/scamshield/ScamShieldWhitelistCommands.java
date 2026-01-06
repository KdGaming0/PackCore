package com.github.kd_gaming1.packcore.command.scamshield;

import com.github.kd_gaming1.packcore.scamshield.ScamShieldWhitelist;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Commands for managing the ScamShield whitelist.
 */
public class ScamShieldWhitelistCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("whitelist")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("Available options: add, remove, list, clear")
                            .formatted(Formatting.YELLOW));
                    return 0;
                })
                .then(ClientCommandManager.literal("add")
                        .executes(context -> {
                            context.getSource().sendFeedback(Text.literal("/scamshield whitelist add <player>")
                                    .formatted(Formatting.YELLOW));
                            return 0;
                        })
                        .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                .executes(ScamShieldWhitelistCommands::whitelistAdd)
                        )
                )
                .then(ClientCommandManager.literal("remove")
                        .executes(context -> {
                            context.getSource().sendFeedback(Text.literal("/scamshield whitelist remove <player>")
                                    .formatted(Formatting.YELLOW));
                            return 0;
                        })
                        .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                .executes(ScamShieldWhitelistCommands::whitelistRemove)
                        )
                )
                .then(ClientCommandManager.literal("list")
                        .executes(ScamShieldWhitelistCommands::whitelistList)
                )
                .then(ClientCommandManager.literal("clear")
                        .executes(ScamShieldWhitelistCommands::whitelistClear)
                );
    }

    private static int whitelistAdd(CommandContext<FabricClientCommandSource> context) {
        String player = StringArgumentType.getString(context, "player");
        FabricClientCommandSource source = context.getSource();

        boolean added = ScamShieldWhitelist.getInstance().add(player);

        if (added) {
            source.sendFeedback(
                    Text.literal("§a[ScamShield] ✓ Added §f" + player + "§a to whitelist")
            );
            return 1;
        } else {
            source.sendError(
                    Text.literal("§c[ScamShield] Player already whitelisted")
            );
            return 0;
        }
    }

    private static int whitelistRemove(CommandContext<FabricClientCommandSource> context) {
        String player = StringArgumentType.getString(context, "player");
        FabricClientCommandSource source = context.getSource();

        boolean removed = ScamShieldWhitelist.getInstance().remove(player);

        if (removed) {
            source.sendFeedback(
                    Text.literal("§a[ScamShield] ✓ Removed §f" + player + "§a from whitelist")
            );
            return 1;
        } else {
            source.sendError(
                    Text.literal("§c[ScamShield] Player not in whitelist")
            );
            return 0;
        }
    }

    private static int whitelistList(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        var whitelist = ScamShieldWhitelist.getInstance().getWhitelistedPlayers();

        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        source.sendFeedback(Text.literal("§e[ScamShield Whitelist]"));
        source.sendFeedback(Text.literal(""));

        if (whitelist.isEmpty()) {
            source.sendFeedback(Text.literal("§7No whitelisted players"));
        } else {
            source.sendFeedback(
                    Text.literal("§7Whitelisted Players: §f" + whitelist.size())
            );
            source.sendFeedback(Text.literal(""));
            whitelist.forEach(player -> {
                source.sendFeedback(Text.literal("§7  • §f" + player));
            });
        }

        source.sendFeedback(Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return 1;
    }

    private static int whitelistClear(CommandContext<FabricClientCommandSource> context) {
        ScamShieldWhitelist.getInstance().clear();
        context.getSource().sendFeedback(
                Text.literal("§a[ScamShield] ✓ Whitelist cleared")
        );
        return 1;
    }
}