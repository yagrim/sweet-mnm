package org.mnm.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.ApiConnector;
import org.mnm.api.Session;
import org.mnm.api.TokenSupplier;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.tools.JwtParser;

import static org.mnm.config.Client.Status.NEEDS_UPDATE;
import static org.mnm.config.Client.Status.NOT_INSTALLED;

public class LoginService {

    private final Logger logger = LoggerFactory.getLogger(LoginService.class);

    private final ConfigDb configDb;
    private final ApiConnector apiConnector;

    public LoginService(ConfigDb configDb, ApiConnector apiConnector) {
        this.configDb = configDb;
        this.apiConnector = apiConnector;
    }

    /**
     * Returns all slugs available for the account.
     */
    public List<String> login(String username, String password,
                              Path workDir, TokenSupplier tokenSupplier) {

        final Session session = Session.login(username, password, apiConnector, tokenSupplier);
        return updateClientsAndTokens(session, workDir);
    }

    /**
     * Creates or updates a client and token for each slug available in the session.
     */
    public List<String> updateClientsAndTokens(Session session, Path workDir) {
        final List<String> slugs = session.getSlugs();
        for (String slug : slugs) {
            updateClientAndToken(session, slug, configDb.getClient(slug), workDir, null);
        }
        return slugs;
    }

    // status only used when REPAIRING or INSTALLING
    public Client updateClientAndToken(Session session, String slug, Client client, Path workDir, Client.Status status) {
        final String version = session.getGameVersion(slug).version();
        if (client == null) {
            // Installing works, since no installation has been completed yet
            Client newClient = new Client(slug, version, NOT_INSTALLED, workDir);
            configDb.addClient(newClient);
            configDb.addToken(new Token(slug, session.getToken()));
            return newClient;
        } else {
            if (status != null && status.isInProgress() && status != client.status()) {
                configDb.updateClientStatus(slug, status);
            } else if (!version.equals(client.version())) {
                configDb.updateClient(slug, version, NEEDS_UPDATE, client.path());
            }
            refreshSessionToken(client, session);
        }
        return client;
    }

    private void refreshSessionToken(Client client, Session session) {
        final List<Token> tokens = configDb.getTokens(client.slug());
        final Optional<Token> expiredToken = findToken(tokens, true);

        Token tokenToUpdate;
        if (expiredToken.isPresent()) {
            tokenToUpdate = expiredToken.get();
            configDb.updateToken(tokenToUpdate.id(), session.getToken());
            logger.debug("Updated expired token: {}, {}", tokenToUpdate.id(), tokenToUpdate.slug());
        } else {
            if (tokens.size() > 1) {
                logger.debug("WARNING: Found multiple valid tokens: {}", tokens.size());
            }
            if (tokens.isEmpty()) {
                // This is a protection for inconsistency scenarios, in theory it should not happen, but data is not transactional
                configDb.addToken(new Token(client.slug(), session.getToken()));
            } else {
                tokenToUpdate = tokens.get(0);
                if (!session.getToken().equals(tokenToUpdate.token())) {
                    configDb.updateToken(tokenToUpdate.id(), session.getToken());
                    logger.debug("Replaced valid token: {}, {}", tokenToUpdate.id(), tokenToUpdate.slug());
                }
            }
        }
    }

    private static Optional<Token> findToken(List<Token> tokens, boolean isExpired) {
        return tokens.stream()
            .filter(s -> JwtParser.parse(s.token()).isExpired() == isExpired)
            .findFirst();
    }

}
