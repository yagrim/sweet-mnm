package org.mnm.cli;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ParsedArguments {

    private final Map<String, String> argsMap;

    public ParsedArguments(Map<String, String> argsMap) {
        this.argsMap = new HashMap<>(argsMap);
    }

    public String get(String key) {
        return argsMap.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return argsMap.getOrDefault(key, defaultValue);
    }


    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(argsMap.get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(argsMap.getOrDefault(key, "false"));
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(argsMap);
    }
}