package org.mnm.gui;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.TestUtils;
import org.mnm.api.ApiConnection.GameVersion;
import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.TestUtils.expiredToken;
import static org.mnm.config.Client.Status.NEEDS_UPDATE;
import static org.mnm.config.Client.Status.NOT_INSTALLED;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.config.Environment.API_BASE_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ClientStatusTest {

    private static final String CLIENT_VERSION = "1.0.0";
    private static final String TEST_SLUG = "my-slug";
    private static final String OTHER_SLUG = "other-slug";

    @Test
    void shouldReadClientStatusWhenClientIsMissing(@TempDir Path tempDir) {
        try (ConfigDb configDb = ConfigDb.open(tempDir.resolve("config.db"));
             MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {

            ClientStatus clientStatus = ClientStatus.read(configDb, TEST_SLUG);

            assertEmptyClientStatus(clientStatus);
            sessionMock.verifyNoInteractions();
        }
    }

    @Test
    void shouldReadClientStatusWhenTokenIsInvalid(@TempDir Path tempDir) {
        shouldReadClientStatusWithoutValidToken(expiredToken(), tempDir);
    }

    @Test
    void shouldReadClientStatusWhenTokenIsMissing(@TempDir Path tempDir) {
        shouldReadClientStatusWithoutValidToken(null, tempDir);
    }

    private static void shouldReadClientStatusWithoutValidToken(String token, Path tempDir) {
        try (ConfigDb configDb = ConfigDb.open(tempDir.resolve("config.db"))) {
            configDb.addClient(new Client(TEST_SLUG, CLIENT_VERSION, UPDATED, tempDir));
            if (token != null) {
                configDb.addToken(new Token(TEST_SLUG, token));
            }

            ClientStatus clientStatus = ClientStatus.read(configDb, TEST_SLUG);

            // Client is kept, so its slug is still available
            assertThat(clientStatus.client()).isEqualTo(new Client(TEST_SLUG, CLIENT_VERSION, UPDATED, tempDir));
            assertThat(clientStatus.validToken()).isFalse();
            assertThat(clientStatus.expiresAt()).isNull();
        }
    }

    @Test
    void shouldReadClientStatusWithoutApiCalls(@TempDir Path tempDir) {
        final Instant expiresAt = Instant.now().plus(5, MINUTES).truncatedTo(SECONDS);
        final String validToken = TestUtils.testToken(expiresAt);

        try (ConfigDb configDb = ConfigDb.open(tempDir.resolve("config.db"));
             MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            configDb.addClient(new Client(TEST_SLUG, CLIENT_VERSION, NEEDS_UPDATE, tempDir));
            configDb.addToken(new Token(TEST_SLUG, validToken));

            ClientStatus clientStatus = ClientStatus.read(configDb, TEST_SLUG);

            assertThat(clientStatus.client()).isEqualTo(new Client(TEST_SLUG, CLIENT_VERSION, NEEDS_UPDATE, tempDir));
            assertThat(clientStatus.validToken()).isTrue();
            assertThat(clientStatus.expiresAt()).isEqualTo(expiresAt);
            sessionMock.verifyNoInteractions();
        }
    }

    @Test
    void shouldNotCallApiWhenThereIsNoValidToken(@TempDir Path tempDir) {
        try (ConfigDb configDb = ConfigDb.open(tempDir.resolve("config.db"));
             MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            configDb.addClient(new Client(TEST_SLUG, CLIENT_VERSION, UPDATED, tempDir));
            configDb.addToken(new Token(TEST_SLUG, expiredToken()));

            ClientStatus.updateVersions(configDb, API_BASE_URL);

            sessionMock.verifyNoInteractions();
            assertThat(configDb.getClient(TEST_SLUG).status()).isEqualTo(UPDATED);
        }
    }

    @Test
    void shouldUpdateVersionsOfAllClientsWithOneApiCall(@TempDir Path tempDir) {
        final String validToken = TestUtils.testToken(Instant.now().plus(5, MINUTES));
        final String notInstalledSlug = "not-installed-slug";

        try (ConfigDb configDb = ConfigDb.open(tempDir.resolve("config.db"));
             MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            configDb.addClient(new Client(TEST_SLUG, CLIENT_VERSION, UPDATED, tempDir));
            configDb.addClient(new Client(OTHER_SLUG, CLIENT_VERSION, UPDATED, tempDir));
            configDb.addClient(new Client(notInstalledSlug, CLIENT_VERSION, NOT_INSTALLED, tempDir));
            configDb.addToken(new Token(TEST_SLUG, validToken));
            configDb.addToken(new Token(OTHER_SLUG, validToken));
            configDb.addToken(new Token(notInstalledSlug, validToken));

            Session session = Mockito.mock(Session.class);
            when(session.getSlugs()).thenReturn(List.of(TEST_SLUG, OTHER_SLUG, notInstalledSlug));
            when(session.getGameVersion(TEST_SLUG)).thenReturn(new GameVersion(TEST_SLUG, CLIENT_VERSION, null, null));
            when(session.getGameVersion(OTHER_SLUG)).thenReturn(new GameVersion(OTHER_SLUG, "2.0.0", null, null));
            when(session.getGameVersion(notInstalledSlug)).thenReturn(new GameVersion(notInstalledSlug, "2.0.0", null, null));
            sessionMock.when(() -> Session.login(anyString(), any())).thenReturn(session);

            ClientStatus.updateVersions(configDb, API_BASE_URL);

            sessionMock.verify(() -> Session.login(eq(validToken), any()), Mockito.times(1));
            assertThat(configDb.getClient(TEST_SLUG).status()).isEqualTo(UPDATED);
            assertThat(configDb.getClient(OTHER_SLUG).status()).isEqualTo(NEEDS_UPDATE);
            assertThat(configDb.getClient(notInstalledSlug).status()).isEqualTo(NOT_INSTALLED);
        }
    }

    private static void assertEmptyClientStatus(ClientStatus clientStatus) {
        assertThat(clientStatus.client()).isNull();
        assertThat(clientStatus.validToken()).isFalse();
        assertThat(clientStatus.expiresAt()).isNull();
    }
}
