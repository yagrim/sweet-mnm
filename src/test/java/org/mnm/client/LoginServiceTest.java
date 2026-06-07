package org.mnm.client;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.TestUtils.expiredToken;
import static org.mnm.TestUtils.validToken;
import static org.mnm.config.Client.Status.*;
import static org.mnm.config.Environment.API_BASE_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private static final String TEST_USERNAME = "user";
    private static final String TEST_PASSWORD = "pass";

    private static final String TEST_SLUG = "test-slug";
    private static final String TEST_VERSION = "publish-0.24.1.8-261bd93c2824d2dcc872027d036afb295cf4d9c1";

    private static final String VALID_TOKEN = validToken();
    private static final String EXPIRED_TOKEN = expiredToken();

    @Test
    void shouldLoginAndSaveClientAndToken(@TempDir Path tempDir) {
        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        when(configDb.getClient(TEST_SLUG)).thenReturn(null);

        Session session = Mockito.mock(Session.class);
        when(session.getSlug()).thenReturn(TEST_SLUG);
        when(session.getVersion()).thenReturn(TEST_VERSION);
        when(session.getToken()).thenReturn(VALID_TOKEN);

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(TEST_USERNAME, TEST_PASSWORD, API_BASE_URL)).thenReturn(session);

            final LoginService loginService = new LoginService(configDb);
            String slug = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir, API_BASE_URL);
            assertThat(slug).isEqualTo(TEST_SLUG);

            verify(configDb, Mockito.times(1))
                .addClient(eq(new Client(TEST_SLUG, TEST_VERSION, NOT_INSTALLED, tempDir)));
            verify(configDb, Mockito.times(1))
                .addToken(eq(new Token(TEST_SLUG, VALID_TOKEN)));
        }
    }

    @Test
    void shouldLoginExistingUpToDateClientWithoutToken(@TempDir Path tempDir) {
        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        when(configDb.getClient(TEST_SLUG)).thenReturn(new Client(TEST_SLUG, TEST_VERSION, UPDATED, tempDir));
        when(configDb.getTokens(TEST_SLUG)).thenReturn(Collections.emptyList());

        Session session = mockSession(VALID_TOKEN);

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(TEST_USERNAME, TEST_PASSWORD, API_BASE_URL)).thenReturn(session);

            final LoginService loginService = new LoginService(configDb);
            String slug = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir, API_BASE_URL);
            assertThat(slug).isEqualTo(TEST_SLUG);

            verify(configDb, Mockito.times(0)).addClient(any());
            verify(configDb, Mockito.times(0)).updateClientStatus(eq(TEST_SLUG), eq(UPDATED));
            verify(configDb, Mockito.times(1)).addToken(eq(new Token(TEST_SLUG, VALID_TOKEN)));
            verify(configDb, Mockito.times(0)).updateToken(Mockito.anyInt(), Mockito.anyString());
        }
    }

    @Test
    void shouldLoginExistingOutdatedClientWithoutToken(@TempDir Path tempDir) {
        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        when(configDb.getClient(TEST_SLUG)).thenReturn(new Client(TEST_SLUG, TEST_VERSION, UPDATED, tempDir));
        when(configDb.getTokens(TEST_SLUG)).thenReturn(Collections.emptyList());

        Session session = mockSession(VALID_TOKEN, "1.2.3");

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(TEST_USERNAME, TEST_PASSWORD, API_BASE_URL)).thenReturn(session);

            final LoginService loginService = new LoginService(configDb);
            String slug = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir, API_BASE_URL);
            assertThat(slug).isEqualTo(TEST_SLUG);

            verify(configDb, Mockito.times(0)).addClient(any());
            verify(configDb, Mockito.times(0)).updateClientStatus(eq(TEST_SLUG), any());
            verify(configDb, Mockito.times(1)).updateClient(eq(TEST_SLUG), eq("1.2.3"), eq(NEEDS_UPDATE), eq(tempDir));
            verify(configDb, Mockito.times(1)).addToken(eq(new Token(TEST_SLUG, VALID_TOKEN)));
            verify(configDb, Mockito.times(0)).updateToken(eq(1), eq(VALID_TOKEN));
        }
    }

    @Test
    void shouldLoginExistingClientAndReplaceExpiredToken(@TempDir Path tempDir) {
        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        when(configDb.getClient(TEST_SLUG)).thenReturn(new Client(TEST_SLUG, TEST_VERSION, UPDATED, tempDir));
        when(configDb.getTokens(TEST_SLUG)).thenReturn(List.of(new Token(1, TEST_SLUG, EXPIRED_TOKEN)));

        Session session = mockSession(VALID_TOKEN);

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(TEST_USERNAME, TEST_PASSWORD, API_BASE_URL)).thenReturn(session);

            final LoginService loginService = new LoginService(configDb);
            String slug = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir, API_BASE_URL);
            assertThat(slug).isEqualTo(TEST_SLUG);

            verify(configDb, Mockito.times(0)).addClient(any());
            // Don't update client data if it's up-to-date
            verify(configDb, Mockito.times(0)).updateClientStatus(eq(TEST_SLUG), any());
            verify(configDb, Mockito.times(0)).addToken(any());
            verify(configDb, Mockito.times(1)).updateToken(eq(1), eq(VALID_TOKEN));
        }
    }

    @Test
    void shouldLoginExistingClientAndReplacePreviousValidToken(@TempDir Path tempDir) {
        final String newValidToken = validToken();

        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        when(configDb.getClient(TEST_SLUG)).thenReturn(new Client(TEST_SLUG, TEST_VERSION, UPDATED, tempDir));
        when(configDb.getTokens(TEST_SLUG)).thenReturn(List.of(new Token(1, TEST_SLUG, VALID_TOKEN)));

        Session session = mockSession(newValidToken);

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(TEST_USERNAME, TEST_PASSWORD, API_BASE_URL)).thenReturn(session);

            final LoginService loginService = new LoginService(configDb);
            String slug = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir, API_BASE_URL);
            assertThat(slug).isEqualTo(TEST_SLUG);

            verify(configDb, Mockito.times(0)).addClient(any());
            verify(configDb, Mockito.times(0)).updateClientStatus(eq(TEST_SLUG), eq(UPDATED));
            verify(configDb, Mockito.times(0)).addToken(any());
            verify(configDb, Mockito.times(1)).updateToken(eq(1), eq(newValidToken));
        }
    }

    private static Session mockSession(String token) {
        return mockSession(token, TEST_VERSION);
    }

    private static Session mockSession(String token, String version) {
        Session session = Mockito.mock(Session.class);
        when(session.getSlug()).thenReturn(TEST_SLUG);
        when(session.getToken()).thenReturn(token);
        when(session.getVersion()).thenReturn(version);
        return session;
    }
}
