package org.mnm.config;

public interface SettingsStore {

    String get(String key);

    void put(String key, String value);

    void delete(String key);

    default boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    default void putBoolean(String key, boolean value) {
        put(key, Boolean.toString(value));
    }

}
