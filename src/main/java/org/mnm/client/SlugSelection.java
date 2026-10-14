package org.mnm.client;

import java.util.List;

public class SlugSelection {

    public static final String DEFAULT_SLUG = "mnm";

    private SlugSelection() {
    }

    public static String select(List<String> slugs, String preferredSlug) {
        if (preferredSlug != null && slugs.contains(preferredSlug)) {
            return preferredSlug;
        }
        if (slugs.contains(DEFAULT_SLUG)) {
            return DEFAULT_SLUG;
        }
        return slugs.isEmpty() ? null : slugs.get(0);
    }
}
