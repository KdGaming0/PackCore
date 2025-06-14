package com.github.kdgaming0.packcore.command;

import com.github.kdgaming0.packcore.config.ModConfig;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

/**
 * Command to configure the dialog window settings.
 * Usage: /packcore dialog <true|false>
 */
public class DialogConfigCommand extends CommandBase {

    // Since ModConfig is not available, we'll use a simple static field
    // You should replace this with your actual config system
    private static boolean promptSetDefaultConfig = true;

    @Override
    public String getCommandName() {
        return "packcore-dialog";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/packcore dialog [true|false]";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true; // Required for client commands
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            showDialogStatus(sender);
        } else if (args.length == 1) {
            try {
                boolean enabled = Boolean.parseBoolean(args[0]);
                executeDialogConfig(sender, enabled);
            } catch (Exception e) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Invalid argument. Use 'true' or 'false'."));
            }
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /packcore dialog [true|false]"));
        }
    }

    private void executeDialogConfig(ICommandSender sender, boolean enabled) {
        // Update the config
        setPromptSetDefaultConfig(enabled);

        // Send feedback
        String status = enabled ? EnumChatFormatting.GREEN + "enabled" : EnumChatFormatting.RED + "disabled";
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Dialog window has been " + status + EnumChatFormatting.GOLD + "."));

        if (enabled) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "The selection dialog will now appear on next startup if multiple config files are found."));
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "The selection dialog will be skipped and auto-extract single configs."));
        }
    }

    private void showDialogStatus(ICommandSender sender) {
        boolean currentStatus = getPromptSetDefaultConfig();

        String status = currentStatus ? EnumChatFormatting.GREEN + "enabled" : EnumChatFormatting.RED + "disabled";
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Dialog window is currently " + status + EnumChatFormatting.GOLD + "."));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Use " + EnumChatFormatting.WHITE + "/packcore dialog <true|false> " + EnumChatFormatting.GRAY + "to change this setting."));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return Arrays.asList();
    }

    // Simple config methods - use actual config system
    public static void setPromptSetDefaultConfig(boolean value) {
        promptSetDefaultConfig = value;
        ModConfig.setPromptSetDefaultConfig(value);
    }

    public static boolean getPromptSetDefaultConfig() {
        return ModConfig.getPromptSetDefaultConfig();
    }
}