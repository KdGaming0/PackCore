package com.github.kd_gaming1.packcore.gui.wizard;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class WizardState {

    private final Map<String, String> selections = new HashMap<>();
    private final Map<String, Set<String>> multiSelections = new HashMap<>();
    private final Set<String> selectedResourcePacks = new HashSet<>();

    public boolean migratedFromV3 = false;

    // Selections
    public void setSelection(String key, String value) {
        selections.put(key, value);
    }

    public String getSelection(String key) {
        return selections.get(key);
    }

    // Generic multi-select state
    public Set<String> getMultiSelection(String key) {
        return Collections.unmodifiableSet(multiSelections.getOrDefault(key, Set.of()));
    }

    public void addMultiSelection(String key, String value) {
        multiSelections.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(value);
    }

    public void removeMultiSelection(String key, String value) {
        Set<String> values = multiSelections.get(key);
        if (values == null) return;

        values.remove(value);
        if (values.isEmpty()) {
            multiSelections.remove(key);
        }
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
                ", multiSelections=" + multiSelections +
                ", resourcePacks=" + selectedResourcePacks +
                '}';
    }
}
