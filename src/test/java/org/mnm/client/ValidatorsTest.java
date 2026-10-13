package org.mnm.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.api.ApiConnector;
import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.client.Factories.apiConnector;
import static org.mnm.config.Environment.API_BASE_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ValidatorsTest {

    ApiConnector apiConnector;

    @BeforeEach
    void before() {
        apiConnector = apiConnector(API_BASE_URL);
    }

    @Test
    void shouldAllowMatchingVersion() {
        Client client = new Client("mnm", "1.2.3", Client.Status.UPDATED, null);
        Session session = Mockito.mock(Session.class);
        when(session.getVersion()).thenReturn("1.2.3");
        when(session.select("mnm")).thenReturn(session);

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(eq("token"), any())).thenReturn(session);

            assertThatCode(() -> Validators.checkVersion("token", client))
                .doesNotThrowAnyException();

            sessionMock.verify(() -> Session.login(eq("token"), any()));
        }
    }

    @Test
    void shouldPanicWhenVersionDiffers() {
        Client client = new Client("mnm", "1.2.3", Client.Status.UPDATED, null);
        Session session = Mockito.mock(Session.class);
        when(session.getVersion()).thenReturn("2.0.0");
        when(session.select("mnm")).thenReturn(session);

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(eq("token"), any())).thenReturn(session);

            assertThatThrownBy(() -> Validators.checkVersion("token", client))
                .isInstanceOf(PanicException.class)
                .hasMessage("Version mismatch: run 'repair' to update the client");
        }
    }

    @Test
    void shouldCompareVersionOfClientSlugWhenMultipleAreFound() {
        Client client = new Client("mnm", "1.2.3", Client.Status.UPDATED, null);
        Session session = Mockito.mock(Session.class);
        Session mnmSession = Mockito.mock(Session.class);
        // Default (first) slug has a different version
        when(session.getVersion()).thenReturn("9.9.9");
        when(session.select("mnm")).thenReturn(mnmSession);
        when(mnmSession.getVersion()).thenReturn("1.2.3");

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login(eq("token"), any())).thenReturn(session);

            assertThatCode(() -> Validators.checkVersion("token", client))
                .doesNotThrowAnyException();
        }
    }
}
