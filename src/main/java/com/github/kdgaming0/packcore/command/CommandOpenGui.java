package com.github.kdgaming0.packcore.command;

import com.github.kdgaming0.packcore.screen.JavaTestGui;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;// Create a command class
import net.minecraft.entity.player.EntityPlayer;

public class CommandOpenGui extends CommandBase {
    @Override
    public String getCommandName() {
        return "opengui";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/opengui";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (sender instanceof EntityPlayer) {
            Minecraft.getMinecraft().displayGuiScreen(new JavaTestGui());
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
