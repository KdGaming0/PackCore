package com.github.kd_gaming1.packcore.integration.skyblocker;

import com.github.kd_gaming1.packcore.PackCore;

import java.lang.reflect.Method;

public class DungeonMap {

    public static void disableDungeonMap() {
        try {
            Class<?> configManager = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
            Method updateMethod = configManager.getDeclaredMethod("update", java.util.function.Consumer.class);

            java.util.function.Consumer<Object> consumer = DungeonMap::updateDungeonMapConfig;

            updateMethod.invoke(null, consumer);
            PackCore.LOGGER.info("Disabled Skyblocker Dungeon Map");

        } catch (ClassNotFoundException e) {
            PackCore.LOGGER.info("Skyblocker not present");
        } catch (Exception e) {
            PackCore.LOGGER.warn("Could not update Skyblocker Dungeon Map config", e);
        }
    }

    private static void updateDungeonMapConfig(Object config) {
        try {
            Object dungeons = config.getClass().getField("dungeons").get(config);
            Object dungeonMap = dungeons.getClass().getField("dungeonMap").get(dungeons);

            dungeonMap.getClass().getField("enableMap").setBoolean(dungeonMap, false);
        } catch (Exception e) {
            PackCore.LOGGER.warn("Failed to update Dungeon Map config", e);
        }
    }
}
