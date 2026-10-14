package org.mnm.gui;

import java.util.List;

/**
 * Information required to initialize the GUI: available slugs and the status of the selected one.
 *
 * @param slugs        slugs of the clients in the config database
 * @param clientStatus status of the selected client, without client when there are no clients
 */
record GuiState(List<String> slugs, ClientStatus clientStatus) {

    // Selected when the preferred slug is not available
    static final String FALLBACK_SLUG = "mnm";

    String selectedSlug() {
        return clientStatus.client() == null ? null : clientStatus.client().slug();
    }

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
