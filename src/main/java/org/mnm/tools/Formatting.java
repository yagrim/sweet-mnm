package org.mnm.tools;

import java.util.stream.Stream;

public class Formatting {

    public static int width(String header, Stream<String> values) {
        return Math.max(header.length(), values.mapToInt(String::length).max().orElse(0));
    }

}
