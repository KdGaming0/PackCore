package com.github.kd_gaming1.packcore.gui.wizard;

import java.util.*;

public class WizardState {

    private final Map<String, String> selections = new HashMap<>();
    private final Set<String> selectedResourcePacks = new HashSet<>();
    private final Map<String, Object> customData = new HashMap<>();

    private boolean settingsApplied = false;

    // --- Selections ---

    public void setSelection(String key, String value) {
        selections.put(key, value);
    }

    public String getSelection(String key) {
        return selections.get(key);
    }

    public boolean hasSelection(String key) {
        return selections.containsKey(key);
    }

    // --- Resource Packs ---

    public Set<String> getSelectedResourcePacks() {
        return Collections.unmodifiableSet(selectedResourcePacks);
    }

    public void addResourcePack(String packId) {
        selectedResourcePacks.add(packId);
    }

    public void removeResourcePack(String packId) {
        selectedResourcePacks.remove(packId);
    }

    public void clearResourcePacks() {
        selectedResourcePacks.clear();
    }

    // --- Custom Data ---

    public void setCustomData(String key, Object value) {
        customData.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getCustomData(String key, Class<T> type) {
        Object value = customData.get(key);
        return type.isInstance(value) ? (T) value : null;
    }

    public boolean hasCustomData(String key) {
        return customData.containsKey(key);
    }

    public void removeCustomData(String key) {
        customData.remove(key);
    }

    // --- Lifecycle ---

    public boolean isComplete(Collection<String> requiredKeys) {
        return selections.keySet().containsAll(requiredKeys);
    }

    public boolean isSettingsApplied() {
        return settingsApplied;
    }

    public void setSettingsApplied(boolean applied) {
        this.settingsApplied = applied;
    }

    public void reset() {
        selections.clear();
        selectedResourcePacks.clear();
        customData.clear();
        settingsApplied = false;
    }

    @Override
    public String toString() {
        return "WizardState{" +
                "selections=" + selections +
                ", resourcePacks=" + selectedResourcePacks +
                ", settingsApplied=" + settingsApplied +
                '}';
    }
}