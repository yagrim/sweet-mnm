package org.mnm.gui;

import java.util.List;

/**
 * Information required to initialize the GUI: available slugs, the status of the selected one and new launcher release.
 *
 * @param slugs        slugs of the clients in the config database
 * @param clientStatus status of the selected client, without client when there are no clients
 * @param newRelease   version of a newer launcher release, null when there is no newer release
 */
record GuiState(List<String> slugs, ClientStatus clientStatus, String newRelease) {

    static Builder builder() {
        return new Builder();
    }

    String selectedSlug() {
        return clientStatus.client() == null ? null : clientStatus.client().slug();
    }

    static class Builder {

        private List<String> slugs;
        private ClientStatus clientStatus;
        private String newRelease;

        private Builder() {
        }

        Builder slugs(List<String> slugs) {
            this.slugs = slugs;
            return this;
        }

        Builder clientStatus(ClientStatus clientStatus) {
            this.clientStatus = clientStatus;
            return this;
        }

        Builder newRelease(String newRelease) {
            this.newRelease = newRelease;
            return this;
        }

        GuiState build() {
            return new GuiState(slugs, clientStatus, newRelease);
        }
    }
}
