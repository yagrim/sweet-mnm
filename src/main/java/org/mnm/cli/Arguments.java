package org.mnm.cli;

import java.util.HashMap;
import java.util.Map;

public class Arguments {

    private final Map<String, String> argsMap;

    public Arguments(Map<String, String> argsMap) {
        this.argsMap = new HashMap<>(argsMap);
    }

    public String get(String key) {
        String value = argsMap.get(key);
        if (isABoolean(value)) {
            throw new IllegalStateException("Boolean flag cannot be read as string");
        }
        return value;
    }

    public String getOrDefault(String key, String defaultValue) {
        if (isABoolean(argsMap.get(key))) {
            return defaultValue;
        }
        return argsMap.getOrDefault(key, defaultValue);
    }

    private static boolean isABoolean(String value) {
        return "true".equals(value) || "false".equals(value);
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

}