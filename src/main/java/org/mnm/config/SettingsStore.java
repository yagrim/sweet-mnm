package org.mnm.config;

import org.mnm.tools.StringUtils;

public interface SettingsStore {

    String DEBUG_KEY = "debug";
    String IN_MEMORY_HASHING_KEY = "in-memory-hashing";

    String MANGOHUD_KEY = "linux.mangohud";

    String UMU_GAMEID = "linux.umu.gameid";
    String DEFAULT_UMU_GAMEID = "mnm";
    String UMU_PROTONPATH = "linux.umu.protonpath";
    String DEFAULT_UMU_PROTONPATH = "GE-Proton";

    String UMU_USE_CLIENT_AS_PREFIX = "linux.umu.use-client-as-prefix";
    String UMU_WINEPREFIX = "linux.umu.wineprefix";


    String get(String key);

    default String get(String key, String defaultValue) {
        String value = get(key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

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
