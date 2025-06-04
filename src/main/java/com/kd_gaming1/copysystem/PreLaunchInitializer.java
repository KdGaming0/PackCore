package com.kd_gaming1.copysystem;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.FabricLoader;
import com.kd_gaming1.copysystem.ZipSelectionDialog;
import java.io.File;

public class PreLaunchInitializer implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        // Get Minecraft root directory
        File minecraftRoot = FabricLoader.getInstance().getGameDir().toFile();

        // Show the dialog and wait for user selection
        ZipSelectionDialog.showDialog(minecraftRoot);

        System.out.println("Configuration selection completed, continuing with Minecraft startup...");
    }
}