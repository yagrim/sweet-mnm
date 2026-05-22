package org.mnm.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Arguments {

    private final Map<String, Optional<String>> argsMap;

    private Arguments(Map<String, Optional<String>> argsMap) {
        Objects.requireNonNull(argsMap);
        this.argsMap = new HashMap<>(argsMap);
    }

    public static Arguments parse(String... args) {
        final Map<String, Optional<String>> argsMap = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String key = args[i];

            if (key.startsWith("--")) {
                key = key.substring(2);

                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    argsMap.put(key, Optional.of(args[i + 1]));
                    i++;
                } else {
                    argsMap.put(key, Optional.empty());
                }
            }
        }

        return new Arguments(argsMap);
    }

    public String get(String key) {
        if (!argsMap.containsKey(key)) {
            return null;
        }
        return argsMap.get(key).orElse(null);
    }

    public String getOrDefault(String key, String defaultValue) {
        if (!argsMap.containsKey(key)) {
            return defaultValue;
        }
        return argsMap.get(key).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(argsMap.get(key).get());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key) {
        if (!argsMap.containsKey(key)) {
            return false;
        }
        return argsMap.get(key)
            .map(v -> v.equals("true"))
            .orElse(true);
    }

    public boolean isHelp() {
        return getBoolean("help");
    }

}
