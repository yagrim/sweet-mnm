package org.mnm.config;

import java.nio.file.Path;
import java.util.function.Supplier;

public class ConfigDbSettingsStore implements SettingsStore {

    private final Supplier<Path> configDbLocator;

    public ConfigDbSettingsStore(Supplier<Path> configDbLocator) {
        this.configDbLocator = configDbLocator;
    }

    @Override
    public String get(String key) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            return configDb.getSettings(key);
        }
    }

    @Override
    public void put(String key, String value) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            configDb.putSettings(key, value);
        }
    }
}
