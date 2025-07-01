package com.kd_gaming1.integration;

import com.terraformersmc.vistas.api.VistasApi;
import com.terraformersmc.vistas.panorama.Cubemap;
import com.terraformersmc.vistas.panorama.Panorama;
import com.terraformersmc.vistas.title.VistasTitle;
import com.kd_gaming1.PackCore;
import com.kd_gaming1.config.PackCoreConfig;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public class VistasIntegration implements VistasApi {

    public static final Identifier CUSTOM_PANORAMA_ID = Identifier.of(PackCore.MOD_ID, "custom_panorama");

    @Override
    public void appendPanoramas(Map<Identifier, Panorama> panoramas) {
        // Register your custom panorama
        Identifier cubemapPath = Identifier.of(PackCore.MOD_ID, "textures/gui/title/background/custom_panorama");

        Cubemap customCubemap = new Cubemap(
                java.util.Optional.of(cubemapPath),
                java.util.Optional.empty(), // Use default rotation
                java.util.Optional.empty()  // Use default visual settings
        );

        // Create the panorama with your custom cubemap
        Panorama customPanorama = new Panorama(
                java.util.Optional.of(1), // Weight for random selection
                java.util.Optional.empty(), // Use default menu music
                java.util.Optional.empty(), // Use default splash text
                java.util.Optional.empty(), // Use default logo control
                java.util.Optional.of(List.of(customCubemap))
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