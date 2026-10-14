package org.mnm.gui;

import java.util.List;

/**
 * Information required to initialize the GUI: available slugs, the selected one and its status.
 *
 * @param slugs        slugs of the clients in the config database
 * @param selectedSlug selected slug, null when there are no clients
 * @param clientStatus status of the selected client
 */
record GuiState(List<String> slugs, String selectedSlug, ClientStatus clientStatus) {

    // Selected when the preferred slug is not available
    static final String FALLBACK_SLUG = "mnm";

    static String selectSlug(List<String> slugs, String preferredSlug) {
        if (preferredSlug != null && slugs.contains(preferredSlug)) {
            return preferredSlug;
        }
        if (slugs.contains(FALLBACK_SLUG)) {
            return FALLBACK_SLUG;
        }
        return slugs.isEmpty() ? null : slugs.get(0);
    }
}
