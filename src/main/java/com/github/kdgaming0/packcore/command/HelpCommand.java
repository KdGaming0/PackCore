package com.github.kdgaming0.packcore.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

/**
 * Help command for PackCore.
 * Usage: /packcore or /packcore help
 */
public class HelpCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "packcore";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/packcore [help|archive|dialog]";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true; // Required for client commands
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            executeHelp(sender);
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("archive")) {
            // Delegate to ArchiveCommand
            String[] archiveArgs = new String[args.length - 1];
            System.arraycopy(args, 1, archiveArgs, 0, args.length - 1);
            new ArchiveCommand().processCommand(sender, archiveArgs);
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("dialog")) {
            // Delegate to DialogConfigCommand
            String[] dialogArgs = new String[args.length - 1];
            System.arraycopy(args, 1, dialogArgs, 0, args.length - 1);
            new DialogConfigCommand().processCommand(sender, dialogArgs);
        } else {
            executeHelp(sender);
        }
    }

    private void executeHelp(ICommandSender sender) {
        String[] helpLines = {
                EnumChatFormatting.GOLD + "=== PackCore Commands ===",
                EnumChatFormatting.YELLOW + "/packcore " + EnumChatFormatting.GRAY + "- Show this help",
                EnumChatFormatting.YELLOW + "/packcore archive <target> [filename]",
                "  " + EnumChatFormatting.GRAY + "Archive presets:",
                "  " + EnumChatFormatting.WHITE + "- vanilla-configs " + EnumChatFormatting.GRAY + "(options.txt, servers.dat)",
                "  " + EnumChatFormatting.WHITE + "- mod-configs " + EnumChatFormatting.GRAY + "(config folder only)",
                "  " + EnumChatFormatting.WHITE + "- all-configs " + EnumChatFormatting.GRAY + "(vanilla + mod configs)",
                "  " + EnumChatFormatting.GRAY + "Or specify any folder name",
                "",
                EnumChatFormatting.YELLOW + "/packcore dialog [true|false]",
                "  " + EnumChatFormatting.GRAY + "Enable/disable the config selection dialog",
                "",
                EnumChatFormatting.GRAY + "Archives are saved to: " + EnumChatFormatting.WHITE + "Skyblock Enhanced/CustomConfigs/"
        };

        for (String line : helpLines) {
            sender.addChatMessage(new ChatComponentText(line));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "help", "archive", "dialog");
        }
        return Arrays.asList();
    }
}