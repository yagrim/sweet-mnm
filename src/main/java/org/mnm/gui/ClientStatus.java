package org.mnm.gui;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.tools.JwtParser;

import static org.mnm.gui.MainGui.DEFAULT_SLUG;

// We extract the information we need from token, we do not store the actual token for security
record ClientStatus(Client client, boolean clientUptoDate,
                    boolean validToken, Instant expiresAt) {

    static ClientStatus getClientStatus(Path dbFile, String apiEndpoint) {
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            Client client = configDb.getClient(DEFAULT_SLUG);
            if (client != null) {
                List<Token> tokens = configDb.getTokens(DEFAULT_SLUG);
                if (!tokens.isEmpty()) {
                    Token token = tokens.get(0);
                    JwtParser.JwtClaims tokenClaims = JwtParser.parse(token.token());
                    if (!tokenClaims.isExpired()) {
                        Session session = Session.login(token.token(), apiEndpoint);
                        boolean upToDateClient = session.getVersion().equals(client.version());
                        return new ClientStatus(client, upToDateClient, true, tokenClaims.expirationTime());
                    } else {
                        return new ClientStatus(null, false, false, null);
                    }
                } else {
                    // We assume client is not valid, but it could be up-to-date, but we can't query API without a token
                    return new ClientStatus(null, false, false, null);
                }
            }
            return new ClientStatus(null, false, false, null);
        }
    }
    
    public boolean statusIs(Client.Status status) {
        return client != null && client.status() == status;
    }
}
