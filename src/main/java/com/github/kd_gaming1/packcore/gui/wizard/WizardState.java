package com.github.kd_gaming1.packcore.gui.wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WizardState {

    private final Map<String, String> selections = new HashMap<>();
    private final Map<String, Set<String>> multiSelections = new HashMap<>();

    /** Selected resource packs in priority order, highest-priority (top of the UI) first. */
    private final LinkedHashSet<String> selectedResourcePacks = new LinkedHashSet<>();

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

    // Resource Packs — order matters: the list is priority-ordered, highest first.
    public Set<String> getSelectedResourcePacks() {
        return Collections.unmodifiableSet(selectedResourcePacks);
    }

    /** Selected packs in priority order, highest-priority first. */
    public List<String> getResourcePackOrder() {
        return List.copyOf(selectedResourcePacks);
    }

    /** Adds a pack at the bottom (lowest priority) so it cannot silently override existing picks. */
    public void addResourcePack(String packId) {
        selectedResourcePacks.add(packId);
    }

    public void removeResourcePack(String packId) {
        selectedResourcePacks.remove(packId);
    }

    /** Moves a pack one step towards the top (higher priority). No-op if absent or already first. */
    public void moveResourcePackUp(String packId) {
        swapResourcePack(packId, -1);
    }

    /** Moves a pack one step towards the bottom (lower priority). No-op if absent or already last. */
    public void moveResourcePackDown(String packId) {
        swapResourcePack(packId, +1);
    }

    private void swapResourcePack(String packId, int delta) {
        List<String> order = new ArrayList<>(selectedResourcePacks);
        int index = order.indexOf(packId);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= order.size()) return;

        Collections.swap(order, index, target);
        selectedResourcePacks.clear();
        selectedResourcePacks.addAll(order);
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
