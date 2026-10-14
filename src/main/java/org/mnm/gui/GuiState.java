package org.mnm.gui;

import java.util.List;

/**
 * Information required to initialize the GUI: available slugs and the status of the selected one.
 *
 * @param slugs        slugs of the clients in the config database
 * @param clientStatus status of the selected client, without client when there are no clients
 */
record GuiState(List<String> slugs, ClientStatus clientStatus) {

    String selectedSlug() {
        return clientStatus.client() == null ? null : clientStatus.client().slug();
    }
}
