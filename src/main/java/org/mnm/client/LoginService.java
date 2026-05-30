package org.mnm.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.tools.JwtParser;

import static org.mnm.config.Client.Status.*;

public class LoginService {

    private final Logger logger = LoggerFactory.getLogger(LoginService.class);

    private final ConfigDb configDb;

    public LoginService(ConfigDb configDb) {
        this.configDb = configDb;
    }

    public String login(String username, String password,
                        Path workDir, String apiBaseUrl) {

        final Session session = Session.login(username, password, apiBaseUrl);
        final Client client = configDb.getClient(session.getSlug());

        return storeToken(session, client, workDir, INSTALLING).slug();
    }

    public Client storeToken(Session session, Client client, Path workDir, Client.Status status) {
        if (client == null) {
            // Installing works, since no installation has been completed yet
            Client client1 = new Client(session.getSlug(), session.getVersion(), INSTALLING, workDir);
            configDb.addClient(client1);
            configDb.addToken(new Token(session.getSlug(), session.getToken()));
            return client1;
        } else {
            configDb.updateClientStatus(session.getSlug(), status);
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
            logger.debug("Updating expired token: {}", tokenToUpdate.id());
            configDb.updateToken(tokenToUpdate.id(), session.getToken());
        } else {
            // TODO test
            if (tokens.isEmpty()) {
                // This is a protection for inconsistency scenarios, in theory it should not happen, but data is not transactional
                configDb.addToken(new Token(session.getSlug(), session.getToken()));
            } else {
                tokenToUpdate = tokens.get(0);
                logger.debug("Refreshing token: {}", tokenToUpdate.id());
                configDb.updateToken(tokenToUpdate.id(), session.getToken());
            }
        }
    }

    private static Optional<Token> findToken(List<Token> tokens, boolean isExpired) {
        return tokens.stream()
            .filter(s -> JwtParser.parse(s.token()).isExpired() == isExpired)
            .findFirst();
    }

}
