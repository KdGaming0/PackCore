package com.github.kdgaming0.packcore.copysystem;

/**
 * Data class representing information about a configuration file
 */
public class ConfigInfo {
    private final String name;
    private final long size;

    public ConfigInfo(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getDisplayName() {
        // Remove .zip extension for display
        if (name.toLowerCase().endsWith(".zip")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConfigInfo that = (ConfigInfo) obj;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
