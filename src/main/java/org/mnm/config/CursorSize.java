package org.mnm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Wine scales the mouse cursor with the "LogPixels" (DPI) value of the prefix.
// 96 DPI is 100%. The cursor size does not increase above 200% (192 DPI).
public enum CursorSize {

    DEFAULT("Default size", null),
    SCALE_100("100%", 96),
    SCALE_150("150%", 144),
    SCALE_200("200%", 192);

    private static final Logger logger = LoggerFactory.getLogger(CursorSize.class);

    private static final String CURSOR_SIZE_KEY = "linux.wine.cursor-size";

    private final String label;
    private final Integer logPixels;

    CursorSize(String label, Integer logPixels) {
        this.label = label;
        this.logPixels = logPixels;
    }

    public Integer logPixels() {
        return logPixels;
    }

    public static CursorSize read(SettingsStore settingsStore) {
        String value = settingsStore.get(CURSOR_SIZE_KEY);
        if (value == null) {
            return DEFAULT;
        }
        try {
            return CursorSize.valueOf(value);
        } catch (IllegalArgumentException e) {
            logger.debug("Invalid cursor size found in db {}", value);
            return DEFAULT;
        }
    }

    public static void save(SettingsStore settingsStore, CursorSize cursorSize) {
        settingsStore.put(CURSOR_SIZE_KEY, cursorSize.name());
    }

    @Override
    public String toString() {
        return label;
    }
}
