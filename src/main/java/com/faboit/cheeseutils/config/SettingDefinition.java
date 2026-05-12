package com.faboit.cheeseutils.config;

import org.bukkit.Material;

import java.util.List;

public record SettingDefinition(String id, int slot, Material material, String name, String defaultState, List<State> states) {
    public State byId(String stateId) {
        return states.stream().filter(s -> s.id().equalsIgnoreCase(stateId)).findFirst().orElse(states.getFirst());
    }

    public State next(String stateId) {
        for (int i = 0; i < states.size(); i++) {
            State current = states.get(i);
            if (current.id().equalsIgnoreCase(stateId)) {
                return states.get((i + 1) % states.size());
            }
        }
        return states.getFirst();
    }

    public record State(String id, List<String> lore, List<String> commands, String executor, String sound) {
    }
}
