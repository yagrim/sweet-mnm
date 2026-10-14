package org.mnm.gui;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.Session;
import org.mnm.client.Factories;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.tools.JwtParser;

import static org.mnm.config.Client.Status.NEEDS_UPDATE;
import static org.mnm.config.Client.Status.NOT_INSTALLED;

// We extract the information we need from token, we do not store the actual token for security
public record ClientStatus(Client client, boolean validToken, Instant expiresAt) {

    private static final Logger logger = LoggerFactory.getLogger(ClientStatus.class);

    /**
     * Compares the versions of all clients with the ones from the API, using a single API call.
     * Outdated clients are marked as {@link Client.Status#NEEDS_UPDATE}.
     */
    static void updateVersions(ConfigDb configDb, String apiEndpoint) {
        final Optional<Token> validToken = configDb.getTokens().stream()
            .filter(token -> !JwtParser.parse(token.token()).isExpired())
            .findFirst();
        if (validToken.isEmpty()) {
            logger.debug("No valid token found: skipping version check");
            return;
        }

        final Session session = Session.login(validToken.get().token(), Factories.apiConnector(apiEndpoint));
        final List<String> slugs = session.getSlugs();
        for (Client client : configDb.getClients()) {
            // Not installed clients keep showing 'Install'
            if (slugs.contains(client.slug()) && client.status() != NOT_INSTALLED) {
                boolean upToDateClient = session.getGameVersion(client.slug()).version().equals(client.version());
                if (!upToDateClient) {
                    configDb.updateClientStatus(client.slug(), NEEDS_UPDATE);
                }
            }
        }
    }

    /**
     * Reads the status of a client from the database, without API calls.
     */
    static ClientStatus read(ConfigDb configDb, String slug) {
        Client client = configDb.getClient(slug);
        if (client != null) {
            List<Token> tokens = configDb.getTokens(slug);
            if (!tokens.isEmpty()) {
                JwtParser.JwtClaims tokenClaims = JwtParser.parse(tokens.get(0).token());
                if (!tokenClaims.isExpired()) {
                    return new ClientStatus(client, true, tokenClaims.expirationTime());
                }
            }
        }
        return new ClientStatus(null, false, null);
    }

    public boolean statusIs(Client.Status status) {
        return client != null && client.status() == status;
    }

}
