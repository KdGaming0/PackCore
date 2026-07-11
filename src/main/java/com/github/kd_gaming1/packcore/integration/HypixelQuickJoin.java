package com.github.kd_gaming1.packcore.integration;

import com.github.kd_gaming1.packcore.config.PackCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

/**
 * Resolves the {@link ServerData} used by PackCore's Hypixel quick-join buttons.
 *
 * <p>The quick-join buttons used to construct a brand-new {@link ServerData} on every click,
 * which meant the resource-pack "don't ask again" choice ({@link ServerData.ServerPackStatus})
 * could never persist: Minecraft only loads/saves that status via a matching entry in
 * {@code servers.dat}, and a throwaway object is never in that file. This meant players were
 * shown the "This server requires a resource pack" prompt on every single quick-join, even after
 * clicking Proceed.
 *
 * <p>This class resolves (or creates, on first use) a real, saved server-list entry for Hypixel
 * instead, so quick-joining behaves exactly like clicking a normal saved server: vanilla's own
 * {@code PackConfirmScreen} logic persists the player's Proceed/Disconnect choice to that entry,
 * and it is honored on every subsequent join without further changes needed here.
 */
public final class HypixelQuickJoin {

    private static final String NAME = "Hypixel";

    private HypixelQuickJoin() {
    }

    /**
     * Returns the persisted "Hypixel" server-list entry, creating and saving one on first use.
     * Subsequent calls return the same on-disk entry, carrying whatever {@link
     * ServerData.ServerPackStatus} the player previously chose.
     */
    public static ServerData resolveServerData() {
        String address = PackCoreConfig.serverAddressForQuickJoinButton;

        ServerList serverList = new ServerList(Minecraft.getInstance());
        serverList.load();

        ServerData existing = serverList.get(address);
        if (existing != null) {
            return existing;
        }

        ServerData created = new ServerData(NAME, address, ServerData.Type.OTHER);
        serverList.add(created, false);
        serverList.save();
        return created;
    }
}