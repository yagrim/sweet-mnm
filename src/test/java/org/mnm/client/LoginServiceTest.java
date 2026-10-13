package org.mnm.client;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.api.Session;
import org.mnm.api.TokenSupplier;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.TestUtils.expiredToken;
import static org.mnm.TestUtils.validToken;
import static org.mnm.config.Client.Status.NEEDS_UPDATE;
import static org.mnm.config.Client.Status.NOT_INSTALLED;
import static org.mnm.config.Client.Status.UPDATED;
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
        when(session.getSlugs()).thenReturn(List.of(TEST_SLUG));
        when(session.select(TEST_SLUG)).thenReturn(session);

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(eq(TEST_USERNAME), eq(TEST_PASSWORD), any(), any())).thenReturn(session);

            final LoginService loginService = new LoginService(configDb, null);
            List<String> slugs = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir, new TestTokenSupplier());
            assertThat(slugs).containsExactly(TEST_SLUG);

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
            sessionMock.when(() -> Session.login(eq(TEST_USERNAME), eq(TEST_PASSWORD), any(), any())).thenReturn(session);

            final LoginService loginService = new LoginService(configDb, null);
            List<String> slugs = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir, new TestTokenSupplier());
            assertThat(slugs).containsExactly(TEST_SLUG);

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
            sessionMock.when(() -> Session.login(eq(TEST_USERNAME), eq(TEST_PASSWORD), any(), any())).thenReturn(session);

            final LoginService loginService = new LoginService(configDb, null);
            List<String> slugs = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir,  new TestTokenSupplier());
            assertThat(slugs).containsExactly(TEST_SLUG);

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
            sessionMock.when(() -> Session.login(eq(TEST_USERNAME), eq(TEST_PASSWORD), any(), any())).thenReturn(session);

            final LoginService loginService = new LoginService(configDb, null);
            List<String> slugs = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir,  new TestTokenSupplier());
            assertThat(slugs).containsExactly(TEST_SLUG);

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
            sessionMock.when(() -> Session.login(eq(TEST_USERNAME), eq(TEST_PASSWORD), any(), any())).thenReturn(session);

            final LoginService loginService = new LoginService(configDb, null);
            List<String> slugs = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir,  new TestTokenSupplier());
            assertThat(slugs).containsExactly(TEST_SLUG);

            verify(configDb, Mockito.times(0)).addClient(any());
            verify(configDb, Mockito.times(0)).updateClientStatus(eq(TEST_SLUG), eq(UPDATED));
            verify(configDb, Mockito.times(0)).addToken(any());
            verify(configDb, Mockito.times(1)).updateToken(eq(1), eq(newValidToken));
        }
    }

    @Test
    void shouldLoginAndSaveClientAndTokenForEachSlug(@TempDir Path tempDir) {
        final String otherSlug = "other-slug";
        final String otherVersion = "publish-0.0.1.0-other";

        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        when(configDb.getClient(TEST_SLUG)).thenReturn(null);
        when(configDb.getClient(otherSlug)).thenReturn(null);

        Session session = Mockito.mock(Session.class);
        Session testSlugSession = mockSlugSession(TEST_SLUG, TEST_VERSION);
        Session otherSlugSession = mockSlugSession(otherSlug, otherVersion);
        when(session.getSlugs()).thenReturn(List.of(TEST_SLUG, otherSlug));
        when(session.select(TEST_SLUG)).thenReturn(testSlugSession);
        when(session.select(otherSlug)).thenReturn(otherSlugSession);

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(eq(TEST_USERNAME), eq(TEST_PASSWORD), any(), any())).thenReturn(session);

            final LoginService loginService = new LoginService(configDb, null);
            List<String> slugs = loginService.login(TEST_USERNAME, TEST_PASSWORD, tempDir, new TestTokenSupplier());
            assertThat(slugs).containsExactly(TEST_SLUG, otherSlug);

            verify(configDb, Mockito.times(1))
                .addClient(eq(new Client(TEST_SLUG, TEST_VERSION, NOT_INSTALLED, tempDir)));
            verify(configDb, Mockito.times(1))
                .addClient(eq(new Client(otherSlug, otherVersion, NOT_INSTALLED, tempDir)));
            verify(configDb, Mockito.times(1))
                .addToken(eq(new Token(TEST_SLUG, VALID_TOKEN)));
            verify(configDb, Mockito.times(1))
                .addToken(eq(new Token(otherSlug, VALID_TOKEN)));
        }
    }

    @Test
    void shouldMarkOnlyOutdatedClientsWhenLoginWithMultipleSlugs(@TempDir Path tempDir) {
        final String otherSlug = "other-slug";
        final String otherVersion = "publish-0.0.1.0-other";

        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        when(configDb.getClient(TEST_SLUG)).thenReturn(new Client(TEST_SLUG, TEST_VERSION, UPDATED, tempDir));
        when(configDb.getClient(otherSlug)).thenReturn(new Client(otherSlug, "old-version", UPDATED, tempDir));
        when(configDb.getTokens(TEST_SLUG)).thenReturn(List.of(new Token(1, TEST_SLUG, VALID_TOKEN)));
        when(configDb.getTokens(otherSlug)).thenReturn(List.of(new Token(2, otherSlug, VALID_TOKEN)));

        Session session = Mockito.mock(Session.class);
        Session testSlugSession = mockSlugSession(TEST_SLUG, TEST_VERSION);
        Session otherSlugSession = mockSlugSession(otherSlug, otherVersion);
        when(session.getSlugs()).thenReturn(List.of(TEST_SLUG, otherSlug));
        when(session.select(TEST_SLUG)).thenReturn(testSlugSession);
        when(session.select(otherSlug)).thenReturn(otherSlugSession);

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(eq(TEST_USERNAME), eq(TEST_PASSWORD), any(), any())).thenReturn(session);

            new LoginService(configDb, null).login(TEST_USERNAME, TEST_PASSWORD, tempDir, new TestTokenSupplier());

            verify(configDb, Mockito.times(0)).updateClient(eq(TEST_SLUG), any(), any(), any());
            verify(configDb, Mockito.times(1)).updateClient(eq(otherSlug), eq(otherVersion), eq(NEEDS_UPDATE), eq(tempDir));
        }
    }

    private static Session mockSlugSession(String slug, String version) {
        Session session = Mockito.mock(Session.class);
        when(session.getSlug()).thenReturn(slug);
        when(session.getVersion()).thenReturn(version);
        when(session.getToken()).thenReturn(VALID_TOKEN);
        return session;
    }

    private static Session mockSession(String token) {
        return mockSession(token, TEST_VERSION);
    }

    private static Session mockSession(String token, String version) {
        Session session = Mockito.mock(Session.class);
        when(session.getSlug()).thenReturn(TEST_SLUG);
        when(session.getToken()).thenReturn(token);
        when(session.getVersion()).thenReturn(version);
        when(session.getSlugs()).thenReturn(List.of(TEST_SLUG));
        when(session.select(TEST_SLUG)).thenReturn(session);
        return session;
    }

    class TestTokenSupplier implements TokenSupplier {

        @Override
        public String getToken(String method, List<String> methods, String challengeToken) {
            return "";
        }
    }
}
