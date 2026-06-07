package org.mnm.gui;

import javax.swing.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mnm.config.Client;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(client.status()).thenReturn(Client.Status.UPDATED);

        handler.setClient(client, true);
        handler.setHasToken(false);
        handler.refresh();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(play.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isTrue();
        assertThat(logout.isEnabled()).isFalse();
    }

    @Test
    void shouldRefreshWhenTokenExistsAndClientNotCompletedButNotUpToDate() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.REPAIRING);

        handler.setClient(client, true);
        handler.setHasToken(true);
        handler.refresh();

        assertThat(install.isEnabled()).isTrue();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(play.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isTrue();
    }

    @Test
    void shouldRefreshWhenTokenExistsAndClientNotCompletedAndNotUpToDate() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.REPAIRING);

        handler.setClient(client, true);
        handler.setHasToken(true);
        handler.refresh();

        // This happens only if Repair process is aborted
        assertThat(install.isEnabled()).isTrue();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(play.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isTrue();
    }

    @Test
    void shouldRefreshWhenClientCompletedButNotUpToDate() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.UPDATED);

        handler.setClient(client, false);
        handler.setHasToken(true);
        handler.refresh();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(play.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isTrue();
    }

    @Test
    void shouldRefreshWhenClientCompletedAnUptoDate() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.UPDATED);

        handler.setClient(client, true);
        handler.setHasToken(true);
        handler.refresh();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(play.isEnabled()).isTrue();
        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isTrue();
    }


    @Test
    void installationStartShouldDisableRelevantButtons() {
        handler.installationStart();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isFalse();
        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void repairStartShouldDisableRelevantButtons() {
        handler.repairStart();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isFalse();
        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void loginStartShouldDisableLoginButton() {
        login.setEnabled(true);

        handler.loginStart();

        assertThat(login.isEnabled()).isFalse();
    }

    @Test
    void loginDoneShouldSetTokenAndRefreshButtons() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.UPDATED);

        handler.loginDone(client);

        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isTrue();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(play.isEnabled()).isFalse();
        assertThat(install.isEnabled()).isFalse();
    }

    @Test
    void logoutDoneShouldClearTokenAndRefreshButtons() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.UPDATED);

        handler.loginDone(client);
        handler.logoutDone();

        assertThat(login.isEnabled()).isTrue();
        assertThat(logout.isEnabled()).isFalse();
        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void installationDoneShouldRefreshState() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.UPDATED);

        handler.setClient(client, true);
        handler.setHasToken(true);

        handler.installationStart();
        handler.installationDone(client);

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(play.isEnabled()).isTrue();
        assertThat(logout.isEnabled()).isTrue();
    }

    @Test
    void repairDoneShouldRefreshState() {
        Client client = mock(Client.class);
        when(client.status()).thenReturn(Client.Status.UPDATED);

        handler.setClient(client, true);
        handler.setHasToken(true);

        handler.repairStart();
        handler.repairDone(client);

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(play.isEnabled()).isTrue();
        assertThat(logout.isEnabled()).isTrue();
    }
}
