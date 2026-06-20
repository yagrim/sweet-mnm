package org.mnm.gui;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.tools.JwtParser;

import static org.mnm.config.Client.Status.NEEDS_UPDATE;

// We extract the information we need from token, we do not store the actual token for security
public record ClientStatus(Client client, boolean validToken, Instant expiresAt) {

    static ClientStatus getClientStatus(String slug, Path dbFile, String apiEndpoint) {
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            Client client = configDb.getClient(slug);
            if (client != null) {
                List<Token> tokens = configDb.getTokens(slug);
                if (!tokens.isEmpty()) {
                    Token token = tokens.get(0);
                    JwtParser.JwtClaims tokenClaims = JwtParser.parse(token.token());
                    if (!tokenClaims.isExpired()) {
                        Session session = Session.login(token.token(), apiEndpoint);
                        boolean upToDateClient = session.getVersion().equals(client.version());
                        if (!upToDateClient) {
                            configDb.updateClientStatus(slug, NEEDS_UPDATE);
                            client = new Client(client.slug(), client.version(), NEEDS_UPDATE, client.path());
                        }
                        return new ClientStatus(client, true, tokenClaims.expirationTime());
                    }
                }
            }
        }
        return new ClientStatus(null, false, null);
    }

    public boolean statusIs(Client.Status status) {
        return client != null && client.status() == status;
    }

}
