package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.PackCore;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket;

public class HypixelEventUtil {
    private static boolean helloPacketReceived = false;

    public static void init() {
        HypixelModAPI.getInstance().createHandler(ClientboundHelloPacket.class, packet -> {
            helloPacketReceived = true;
            PackCore.LOGGER.info("ClientboundHelloPacket received! You are now connected to Hypixel!");
        });
    }

    public static boolean isHelloPacketReceived() {
        return helloPacketReceived;
    }

    public static void reset() {
        helloPacketReceived = false;
        PackCore.LOGGER.info("Hello packet state reset.");
    }
}
