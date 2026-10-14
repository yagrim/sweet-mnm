package org.mnm.config;

public class WinePrefix {

    private static final String SUFFIX = "_prefix";

    private WinePrefix() {
    }

    public static String name(String slug) {
        return slug.replace(' ', '_') + SUFFIX;
    }
}
