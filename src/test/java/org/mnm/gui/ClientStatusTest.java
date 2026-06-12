package org.mnm.gui;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.TestUtils;
import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.io.CleanupMode.NEVER;
import static org.mnm.TestUtils.expiredToken;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.config.Environment.API_BASE_URL;
import static org.mnm.gui.ClientStatus.getClientStatus;
import static org.mnm.gui.MainGui.DEFAULT_SLUG;
import static org.mockito.Mockito.when;

class ClientStatusTest {

    private static final String CLIENT_VERSION = "1.0.0";


    @Test
    void shouldCalculateClientStatusClientIsMissing(@TempDir(cleanup = NEVER) Path tempDir) {
        Path dbFile = tempDir.resolve("config.db");

        ClientStatus clientStatus = getClientStatus(dbFile, API_BASE_URL);

        assertEmptyClientStatus(clientStatus);
    }

    @Test
    void shouldCalculateClientStatusWhenTokenIsInvalid(@TempDir(cleanup = NEVER) Path tempDir) {
        shouldCalculateClientStatus(expiredToken(), tempDir);
    }

    @Test
    void shouldCalculateClientStatusWhenTokenIsMissing(@TempDir(cleanup = NEVER) Path tempDir) {
        shouldCalculateClientStatus(null, tempDir);
    }

    private static void shouldCalculateClientStatus(String token, Path tempDir) {
        Path dbFile = tempDir.resolve("config.db");
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(new Client(DEFAULT_SLUG, CLIENT_VERSION, UPDATED, tempDir));
            if (token != null) {
                configDb.addToken(new Token(DEFAULT_SLUG, token));
            }
        }

        ClientStatus clientStatus = getClientStatus(dbFile, API_BASE_URL);

        assertEmptyClientStatus(clientStatus);
    }

    @Test
    void shouldCalculateClientStatusWhenTokenIsValidAndClientIsUpToDate(@TempDir(cleanup = NEVER) Path tempDir) {
        shouldCalculateClientStatusWhenTokenIsValid(CLIENT_VERSION, tempDir);
    }

    @Test
    void shouldCalculateClientStatusWhenTokenIsValidAndClientIsOutdated(@TempDir(cleanup = NEVER) Path tempDir) {
        shouldCalculateClientStatusWhenTokenIsValid("1.2.3", tempDir);
    }

    private static void shouldCalculateClientStatusWhenTokenIsValid(String serverVersion, Path tempDir) {
        final Instant expiresAt = Instant.now().plus(5, MINUTES).truncatedTo(SECONDS);
        final String validToken = TestUtils.testToken(expiresAt);

        Path dbFile = tempDir.resolve("config.db");
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(new Client(DEFAULT_SLUG, CLIENT_VERSION, UPDATED, tempDir));
            configDb.addToken(new Token(DEFAULT_SLUG, validToken));
        }

        Session session = Mockito.mock(Session.class);
        when(session.getVersion()).thenReturn(serverVersion);

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(validToken, API_BASE_URL)).thenReturn(session);

            ClientStatus clientStatus = getClientStatus(dbFile, API_BASE_URL);

            assertThat(clientStatus.client()).isEqualTo(new Client(DEFAULT_SLUG, CLIENT_VERSION, UPDATED, tempDir));
            assertThat(clientStatus.clientUptoDate()).isEqualTo(serverVersion.equals(CLIENT_VERSION));
            assertThat(clientStatus.validToken()).isTrue();
            assertThat(clientStatus.expiresAt()).isEqualTo(expiresAt);
        }
    }

    private static void assertEmptyClientStatus(ClientStatus clientStatus) {
        assertThat(clientStatus.client()).isNull();
        assertThat(clientStatus.clientUptoDate()).isFalse();
        assertThat(clientStatus.validToken()).isFalse();
        assertThat(clientStatus.expiresAt()).isNull();
    }
}
