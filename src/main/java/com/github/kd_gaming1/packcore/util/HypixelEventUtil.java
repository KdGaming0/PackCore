package com.github.kd_gaming1.packcore.util;

import com.github.kd_gaming1.packcore.PackCore;
import net.azureaaron.hmapi.events.HypixelPacketEvents;
import net.azureaaron.hmapi.network.packet.s2c.HypixelS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.concurrent.atomic.AtomicBoolean;

public final class HypixelEventUtil {
    private static final AtomicBoolean helloPacketReceived = new AtomicBoolean(false);

    // Call once on client init
    public static void init() {
        // Register HM-api HELLO event listener
        HypixelPacketEvents.HELLO.register((HypixelS2CPacket packet) -> {
            helloPacketReceived.set(true);
            PackCore.LOGGER.info("HELLO packet received — connected to Hypixel!");
        });

        // Reset on disconnect so state reflects current connection
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static boolean isHelloPacketReceived() {
        return helloPacketReceived.get();
    }

    public static void reset() {
        helloPacketReceived.set(false);
        PackCore.LOGGER.info("Hello packet state reset. Not connected to Hypixel.");
    }
}