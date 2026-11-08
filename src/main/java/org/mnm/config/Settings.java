package org.mnm.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mnm.config.SettingsStore.CACHE_AUTO_CLEAR_KEY;
import static org.mnm.config.SettingsStore.CACHE_RETENTION_DAYS_KEY;
import static org.mnm.config.SettingsStore.OPTIONS_FONT_SCALING_KEY;

public class Settings {

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);

    public static final float DEFAULT_FONT_SCALING = 1f;
    public static final float MAX_FONT_SCALING = 4f;

    public static final boolean DEFAULT_CACHE_AUTO_CLEAR = true;
    public static final List<Integer> CACHE_RETENTION_DAYS = List.of(5, 10, 15, 30);
    public static final int DEFAULT_CACHE_RETENTION_DAYS = 10;

    /**
     * Writes the default cache auto-clear options when they are not set.
     *
     * @param settingsStore the store that keeps the options
     * @return {@code true} when the options were not set and the defaults were written,
     *         {@code false} when the options were already set
     */
    public static boolean initCacheAutoClearDefaults(SettingsStore settingsStore) {
        if (settingsStore.get(CACHE_AUTO_CLEAR_KEY) != null) {
            return false;
        }
        settingsStore.putBoolean(CACHE_AUTO_CLEAR_KEY, DEFAULT_CACHE_AUTO_CLEAR);
        if (settingsStore.get(CACHE_RETENTION_DAYS_KEY) == null) {
            settingsStore.putInt(CACHE_RETENTION_DAYS_KEY, DEFAULT_CACHE_RETENTION_DAYS);
        }
        logger.debug("Cache auto-clear options not found. Default options set.");
        return true;
    }

    public static int readCacheRetentionDays(SettingsStore settingsStore) {
        try {
            int candidate = settingsStore.getInt(CACHE_RETENTION_DAYS_KEY, DEFAULT_CACHE_RETENTION_DAYS);
            if (CACHE_RETENTION_DAYS.contains(candidate)) {
                return candidate;
            }
            logger.debug("Invalid cache retention days found in db {}", candidate);
        } catch (NumberFormatException e) {
            logger.debug("Invalid cache retention days found in db: {}", e.getMessage());
        }
        return DEFAULT_CACHE_RETENTION_DAYS;
    }

    public static float readUiScaling(SettingsStore settingsStore) {
        Float candidate = null;
        try {
            candidate = settingsStore.getFloat(OPTIONS_FONT_SCALING_KEY, DEFAULT_FONT_SCALING);
            return candidate >= DEFAULT_FONT_SCALING && candidate <= MAX_FONT_SCALING ? candidate : DEFAULT_FONT_SCALING;
        } catch (NumberFormatException e) {
            if (candidate != null) {
                logger.debug("Invalid font-scaling found in db {}", candidate);
            }
            return DEFAULT_FONT_SCALING;
        }
    }

}
