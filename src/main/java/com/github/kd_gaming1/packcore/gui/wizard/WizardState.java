package com.github.kd_gaming1.packcore.gui.wizard;

import java.util.*;

public class WizardState {

    private final Map<String, String> selections = new HashMap<>();
    private final Set<String> selectedResourcePacks = new HashSet<>();

    // Selections
    public void setSelection(String key, String value) {
        selections.put(key, value);
    }

    public String getSelection(String key) {
        return selections.get(key);
    }

    // Resource Packs
    public Set<String> getSelectedResourcePacks() {
        return Collections.unmodifiableSet(selectedResourcePacks);
    }

    public void addResourcePack(String packId) {
        selectedResourcePacks.add(packId);
    }

    public void removeResourcePack(String packId) {
        selectedResourcePacks.remove(packId);
    }

    @Override
    public String toString() {
        return "WizardState{" +
                "selections=" + selections +
                ", resourcePacks=" + selectedResourcePacks +
                '}';
    }
}