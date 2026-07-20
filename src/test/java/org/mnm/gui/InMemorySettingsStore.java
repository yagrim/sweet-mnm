package org.mnm.gui;

import java.util.HashMap;
import java.util.Map;

import org.mnm.config.SettingsStore;

record InMemorySettingsStore(Map<String, String> values) implements SettingsStore {

    static final String STORE_CREDENTIALS_KEY = "user.store-credentials";
    static final String EMAIL_KEY = "user.email";
    static final String PASSWORD_KEY = "user.password";

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

    @Override
    public void delete(String key) {
        values.remove(key);
    }
}
