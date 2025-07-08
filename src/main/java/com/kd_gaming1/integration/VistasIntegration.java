package com.kd_gaming1.integration;

import com.terraformersmc.vistas.api.VistasApi;
import com.terraformersmc.vistas.panorama.Cubemap;
import com.terraformersmc.vistas.panorama.Panorama;
import com.terraformersmc.vistas.panorama.VisualControl;
import com.terraformersmc.vistas.panorama.RotationControl;
import com.terraformersmc.vistas.title.VistasTitle;
import com.kd_gaming1.PackCore;
import com.kd_gaming1.config.PackCoreConfig;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VistasIntegration implements VistasApi {

    public static final Identifier CUSTOM_PANORAMA_ID = Identifier.of(PackCore.MOD_ID, "custom_panorama");

    @Override
    public void appendPanoramas(Map<Identifier, Panorama> panoramas) {
        // Register your custom panorama
        Identifier cubemapPath = Identifier.of(PackCore.MOD_ID, "textures/gui/title/background/panorama");

        // Create custom visual control for better quality
        VisualControl customVisualControl = new VisualControl(
                Optional.of(85.0D),    // FOV - keep default
                Optional.of(4.0D),     // Width - increased for better quality
                Optional.of(4.0D),     // Height - increased for better quality
                Optional.of(4.0D),     // Depth - increased for better quality
                Optional.of(0.0D),     // AddedX - center position
                Optional.of(0.0D),     // AddedY - center position
                Optional.of(0.0D),     // AddedZ - center position
                Optional.of(255.0D),   // ColorR - full red
                Optional.of(255.0D),   // ColorG - full green
                Optional.of(255.0D),   // ColorB - full blue
                Optional.of(255.0D)    // ColorA - full alpha (no transparency)
        );

        // Create custom rotation control for smoother rotation
        RotationControl customRotationControl = new RotationControl(
                Optional.of(false),    // frozen - allow rotation
                Optional.of(false),    // woozy - disable woozy effect for cleaner look
                Optional.of(0.0D),     // addedPitch - no additional pitch
                Optional.of(0.0D),     // addedYaw - no additional yaw
                Optional.of(0.0D),     // addedRoll - no additional roll
                Optional.of(1.0D)      // speedMultiplier - normal speed
        );

        Cubemap customCubemap = new Cubemap(
                Optional.of(cubemapPath),
                Optional.of(customRotationControl),
                Optional.of(customVisualControl)
        );

        // Create the panorama with your custom cubemap
        Panorama customPanorama = new Panorama(
                Optional.of(1), // Weight for random selection
                Optional.empty(), // Use default menu music
                Optional.empty(), // Use default splash text
                Optional.empty(), // Use default logo control
                Optional.of(List.of(customCubemap))
        );

        panoramas.put(CUSTOM_PANORAMA_ID, customPanorama);

        // Apply configuration when panoramas are registered
        applyConfiguration();
    }

    public static void applyConfiguration() {
        if (PackCoreConfig.enableCustomPanorama) {
            // Force set the current panorama to your custom one
            Panorama customPanorama = VistasTitle.PANORAMAS.get(CUSTOM_PANORAMA_ID);
            if (customPanorama != null) {
                VistasTitle.CURRENT.setValue(customPanorama);
            }
        }
    }
}