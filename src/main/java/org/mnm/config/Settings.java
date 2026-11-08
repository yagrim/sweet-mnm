package org.mnm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mnm.config.SettingsStore.OPTIONS_FONT_SCALING_KEY;

public class Settings {

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);

    public static final float DEFAULT_FONT_SCALING = 1f;
    public static final float MAX_FONT_SCALING = 4f;

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
