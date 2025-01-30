package com.github.kdgaming0.packcore.command;

import com.github.kdgaming0.packcore.PackCore;
import com.github.kdgaming0.packcore.screen.ConfigGui;
import com.github.kdgaming0.packcore.screen.ConfigManagementScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;// Create a command class
import net.minecraft.entity.player.EntityPlayer;

public class CommandOpenGui extends CommandBase {
    @Override
    public String getCommandName() {
        return "packcore";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/packcore";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (sender instanceof EntityPlayer) {
            PackCore.screenToOpenNextTick = new ConfigManagementScreen();
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
