package org.mnm.gui;

import org.mnm.config.SettingsStore;

import java.util.HashMap;
import java.util.Map;

record InMemorySettingsStore(Map<String, String> values) implements SettingsStore {

    InMemorySettingsStore(Map<String, String> values) {
        this.values = new HashMap<>(values);
    }

    @Override
    public String get(String key) {
        return values.get(key);
    }

    @Override
    public void put(String key, String value) {
        values.put(key, value);
    }
}
