package org.mnm.gui;

import javax.swing.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mnm.config.Client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientButtonsHandlerTest {

    private JButton install;
    private JButton repair;
    private JButton play;
    private JButton login;
    private JButton logout;

    private ClientButtonsHandler handler;

    @BeforeEach
    void setUp() {
        install = new JButton();
        repair = new JButton();
        play = new JButton();
        login = new JButton();
        logout = new JButton();

        handler = new ClientButtonsHandler(
            install,
            repair,
            play,
            login,
            logout
        );
    }

    @Test
    void shouldEnableOnlyLoginWhenNoTokenAndNoClient() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.COMPLETED);

        handler.setClient(client);
        handler.setHasToken(false);

        assertFalse(install.isEnabled());
        assertFalse(repair.isEnabled());
        assertFalse(play.isEnabled());

        assertTrue(login.isEnabled());
        assertFalse(logout.isEnabled());
    }

    @Test
    void shouldEnableInstallWhenTokenExistsAndClientNotCompleted() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.INSTALLING);

        handler.setClient(client);
        handler.setHasToken(true);

        assertTrue(install.isEnabled());
        assertFalse(repair.isEnabled());
        assertTrue(play.isEnabled());

        assertFalse(login.isEnabled());
        assertTrue(logout.isEnabled());
    }

    @Test
    void shouldEnableRepairWhenClientCompleted() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.COMPLETED);

        handler.setClient(client);
        handler.setHasToken(true);

        assertFalse(install.isEnabled());
        assertTrue(repair.isEnabled());
        assertTrue(play.isEnabled());

        assertFalse(login.isEnabled());
        assertTrue(logout.isEnabled());
    }

    @Test
    void installationStartShouldDisableRelevantButtons() {
        handler.installationStart();

        assertFalse(install.isEnabled());
        assertFalse(repair.isEnabled());
        assertFalse(logout.isEnabled());
        assertFalse(play.isEnabled());
    }

    @Test
    void repairStartShouldDisableRelevantButtons() {
        handler.repairStart();

        assertFalse(install.isEnabled());
        assertFalse(repair.isEnabled());
        assertFalse(logout.isEnabled());
        assertFalse(play.isEnabled());
    }

    @Test
    void loginStartShouldDisableLoginButton() {
        login.setEnabled(true);

        handler.loginStart();

        assertFalse(login.isEnabled());
    }

    @Test
    void loginDoneShouldSetTokenAndRefreshButtons() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.COMPLETED);

        handler.loginDone(client);

        assertFalse(login.isEnabled());
        assertTrue(logout.isEnabled());
        assertTrue(repair.isEnabled());
        assertTrue(play.isEnabled());
        assertFalse(install.isEnabled());
    }

    @Test
    void logoutDoneShouldClearTokenAndRefreshButtons() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.COMPLETED);

        handler.loginDone(client);
        handler.logoutDone();

        assertTrue(login.isEnabled());
        assertFalse(logout.isEnabled());
        assertFalse(install.isEnabled());
        assertFalse(repair.isEnabled());
        assertFalse(play.isEnabled());
    }

    @Test
    void installationDoneShouldRefreshState() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.COMPLETED);

        handler.setClient(client);
        handler.setHasToken(true);

        handler.installationStart();
        handler.installationDone();

        assertFalse(install.isEnabled());
        assertTrue(repair.isEnabled());
        assertTrue(play.isEnabled());
        assertTrue(logout.isEnabled());
    }

    @Test
    void repairDoneShouldRefreshState() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.COMPLETED);

        handler.setClient(client);
        handler.setHasToken(true);

        handler.repairStart();
        handler.repairDone();

        assertFalse(install.isEnabled());
        assertTrue(repair.isEnabled());
        assertTrue(play.isEnabled());
        assertTrue(logout.isEnabled());
    }
}
