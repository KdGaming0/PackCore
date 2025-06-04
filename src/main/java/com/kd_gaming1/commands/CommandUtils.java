package com.kd_gaming1.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Supplier;

/**
 * Utility methods for command classes.
 */
public final class CommandUtils {
    private CommandUtils() {}

    /**
     * Send multiple lines of feedback to the source, preserving order.
     */
    public static void sendFeedbackLines(ServerCommandSource source, List<Text> lines) {
        for (Text line : lines) {
            source.sendFeedback((Supplier<Text>) () -> line, false);
        }
    }
}

