package org.mnm.cli;

import java.util.HashMap;
import java.util.Map;

public class ArgumentsParser {

    private ArgumentsParser() {
    }

    public static Arguments parse(String[] args) {
        Map<String, String> argsMap = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String key = args[i];

            if (key.startsWith("--")) {
                key = key.substring(2);

                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    argsMap.put(key, args[i + 1]);
                    i++;
                } else {
                    argsMap.put(key, "true");
                }
            }
        }

        return new Arguments(argsMap);
    }
}