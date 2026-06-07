package org.mnm.client;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.config.Environment.API_BASE_URL;
import static org.mockito.Mockito.when;

class ValidatorsTest {

    @Test
    void shouldAllowMatchingVersion() {
        Client client = new Client("mnm", "1.2.3", Client.Status.UPDATED, null);
        Session session = Mockito.mock(Session.class);
        when(session.getVersion()).thenReturn("1.2.3");

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login("token", API_BASE_URL)).thenReturn(session);

            assertThatCode(() -> Validators.checkVersion("token", client))
                .doesNotThrowAnyException();

            sessionMock.verify(() -> Session.login("token", API_BASE_URL));
        }
    }

    @Test
    void shouldPanicWhenVersionDiffers() {
        Client client = new Client("mnm", "1.2.3", Client.Status.UPDATED, null);
        Session session = Mockito.mock(Session.class);
        when(session.getVersion()).thenReturn("2.0.0");

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.login("token", API_BASE_URL)).thenReturn(session);

            assertThatThrownBy(() -> Validators.checkVersion("token", client))
                .isInstanceOf(PanicException.class)
                .hasMessage("Version mismatch: run 'repair' to update the client");
        }
    }
}
