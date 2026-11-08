package org.mnm.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.config.SettingsStore;

public class CacheHandler {

    private static final Logger logger = LoggerFactory.getLogger(CacheHandler.class);

    private static final String AUTO_CLEAR_KEY = "cache.auto-clear";
    private static final String RETENTION_DAYS_KEY = "cache.retention-days";

    private static final boolean DEFAULT_AUTO_CLEAR = true;
    private static final int DEFAULT_RETENTION_DAYS = 10;

    public static final List<Integer> RETENTION_DAYS = List.of(5, 10, 15, 30);

    private final SettingsStore settingsStore;

    public CacheHandler(SettingsStore settingsStore) {
        this.settingsStore = settingsStore;
    }

    // Returns true when the options were not set and the defaults were written.
    // Returns false when the options were already set.
    public boolean initDefaults() {
        if (settingsStore.get(AUTO_CLEAR_KEY) != null) {
            return false;
        }
        settingsStore.putBoolean(AUTO_CLEAR_KEY, DEFAULT_AUTO_CLEAR);
        if (settingsStore.get(RETENTION_DAYS_KEY) == null) {
            settingsStore.putInt(RETENTION_DAYS_KEY, DEFAULT_RETENTION_DAYS);
        }
        logger.debug("Cache auto-clear options not found. Default options set.");
        return true;
    }

    public boolean isAutoClear() {
        return settingsStore.getBoolean(AUTO_CLEAR_KEY, DEFAULT_AUTO_CLEAR);
    }

    public void saveAutoClear(boolean value) {
        settingsStore.putBoolean(AUTO_CLEAR_KEY, value);
    }

    public int getRetentionDays() {
        try {
            int candidate = settingsStore.getInt(RETENTION_DAYS_KEY, DEFAULT_RETENTION_DAYS);
            if (RETENTION_DAYS.contains(candidate)) {
                return candidate;
            }
            logger.debug("Invalid cache retention days found in db {}", candidate);
        } catch (NumberFormatException e) {
            logger.debug("Invalid cache retention days found in db: {}", e.getMessage());
        }
        return DEFAULT_RETENTION_DAYS;
    }

    public void saveRetentionDays(int days) {
        settingsStore.putInt(RETENTION_DAYS_KEY, days);
    }
}
