package com.self.help.graph.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BiDirectionalDictionary {
    // String to ID for Ingestion
    private Map<String, Integer> valueToId = new HashMap<>();

    // ID to String for UI Hydration (Index is the ID)
    private List<String> idToValue = new ArrayList<>();

    public int getOrCreateId(String value) {
        return valueToId.computeIfAbsent(value, k -> {
            int id = idToValue.size();
            idToValue.add(k);
            return id;
        });
    }

    public String getValue(int id) {
        return idToValue.get(id);
    }

    public int size() {
        return idToValue.size();
    }

    public int getIdIfExists(String name) {
        return valueToId.getOrDefault(name, -1);
    }
}