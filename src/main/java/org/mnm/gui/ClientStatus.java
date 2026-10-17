package org.mnm.gui;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import static org.mnm.config.Environment.getWorkDir;

// We extract the information we need from token, we do not store the actual token for security
public record ClientStatus(Client client, boolean validToken, Instant expiresAt) {

    private static final Logger logger = LoggerFactory.getLogger(ClientStatus.class);

    /**
     * 1. Compares the versions of all clients with the ones from the API, using a single API call.
     * Outdated clients are marked as {@link Client.Status#NEEDS_UPDATE}.
     * 2. Adds new slugs returned by the API to the db as {@link Client.Status#NOT_INSTALLED}.
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
        final Map<String, Client> dbClients = configDb.getClients().stream()
            .collect(Collectors.toMap(Client::slug, Function.identity()));
        for (String slug : session.getSlugs()) {
            final String apiVersion = session.getGameVersion(slug).version();
            final Client client = dbClients.get(slug);
            if (client == null) {
                // New slugs can be added to the account after login, show them without a new login
                logger.debug("Adding new slug to db: {}", slug);
                configDb.addClient(new Client(slug, apiVersion, NOT_INSTALLED, getWorkDir()));
                configDb.addToken(new Token(slug, validToken.get().token()));
            } else if (client.status() != NOT_INSTALLED && !apiVersion.equals(client.version())) {
                // Not installed clients keep showing 'Install'
                configDb.updateClientStatus(slug, NEEDS_UPDATE);
            }
        }
    }

    /**
     * Reads the status of a client from the database, without API calls.
     * The client is kept when its token is missing or expired.
     */
    static ClientStatus read(ConfigDb configDb, String slug) {
        Client client = configDb.getClient(slug);
        if (client == null) {
            return new ClientStatus(null, false, null);
        }
        List<Token> tokens = configDb.getTokens(slug);
        if (!tokens.isEmpty()) {
            JwtParser.JwtClaims tokenClaims = JwtParser.parse(tokens.get(0).token());
            if (!tokenClaims.isExpired()) {
                return new ClientStatus(client, true, tokenClaims.expirationTime());
            }
        }
        return new ClientStatus(client, false, null);
    }

    public boolean statusIs(Client.Status status) {
        return client != null && client.status() == status;
    }

}
