package com.github.kd_gaming1.packcore.gui.screen.config;

import net.minecraft.network.chat.Component;

public enum ConfigTab {
    CONFIGURATION(0, "gui.packcore.config.tab.configuration"),
    EXPORT(1, "gui.packcore.config.tab.export"),
    BACKUPS(2, "gui.packcore.config.tab.backups"),
    IMPORT(3, "gui.packcore.config.tab.import");

    private final int index;
    private final String translationKey;

    ConfigTab(int index, String translationKey) {
        this.index = index;
        this.translationKey = translationKey;
    }

    public int index() { return index; }
    public Component label() { return Component.translatable(translationKey); }
    public static ConfigTab[] ordered() { return values(); }
}