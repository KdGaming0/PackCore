package com.github.kd_gaming1.packcore.command;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CommandHelper {
    // Send a command that can be clicked to copy
    public static void sendCopyCommand(FabricClientCommandSource source, String message, String command) {
        MutableText commandText = Text.literal("  ").append(Text.literal(message))
                .styled(style -> style
                        .withClickEvent(new ClickEvent.SuggestCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal("Click to copy command").formatted(Formatting.YELLOW))));
        source.sendFeedback(commandText);
    }
}
