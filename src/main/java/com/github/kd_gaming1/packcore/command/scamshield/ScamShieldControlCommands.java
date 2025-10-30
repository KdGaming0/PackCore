package com.github.kd_gaming1.packcore.command.scamshield;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import com.github.kd_gaming1.packcore.scamshield.detector.types.ScamType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Commands for controlling ScamShield system state.
 */
public class ScamShieldControlCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> registerToggle() {
        return ClientCommandManager.literal("toggle").executes(ScamShieldControlCommands::toggleScamShield);
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> registerReload() {
        return ClientCommandManager.literal("reload").executes(ScamShieldControlCommands::reloadPatterns);
    }

    private static int toggleScamShield(CommandContext<FabricClientCommandSource> context) {
        PackCoreConfig.enableScamShield = !PackCoreConfig.enableScamShield;
        PackCoreConfig.write(PackCore.MOD_ID);

        String status = PackCoreConfig.enableScamShield ? "§aenabled" : "§cdisabled";
        context.getSource().sendFeedback(
                Text.literal("§e[ScamShield] §7System " + status)
        );
        return 1;
    }

    private static int reloadPatterns(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(
                Text.literal("§e[ScamShield] §7Reloading pattern files and scanning for new scam types...")
        );

        try {
            int loadedCount = PackCore.getScamDetector().reloadScamTypes();

            context.getSource().sendFeedback(
                    Text.literal("§a[ScamShield] ✓ Reload complete!")
            );
            context.getSource().sendFeedback(
                    Text.literal("§7Loaded §f" + loadedCount + "§7 scam detectors")
            );
            context.getSource().sendFeedback(
                    Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            );

            // Show loaded scam types
            List<ScamType> scamTypes = PackCore.getScamDetector().getScamTypes();
            context.getSource().sendFeedback(
                    Text.literal("§e[Active Scam Detectors]")
            );

            for (ScamType scamType : scamTypes) {
                String status = scamType.isEnabled() ? "§a✓" : "§c✗";
                context.getSource().sendFeedback(
                        Text.literal("§7  " + status + " §f" + scamType.getDisplayName())
                );
            }

            context.getSource().sendFeedback(
                    Text.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            );
            context.getSource().sendFeedback(
                    Text.literal("§7Tip: Drop new §escamtype-*.json§7 files in the folder and reload!")
            );

            return 1;
        } catch (Exception e) {
            context.getSource().sendError(
                    Text.literal("§c[ScamShield] Failed to reload: " + e.getMessage())
            );
            return 0;
        }
    }
}