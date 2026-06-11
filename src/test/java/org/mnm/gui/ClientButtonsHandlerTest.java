package org.mnm.gui;

import javax.swing.*;
import java.lang.reflect.Field;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.mnm.config.Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.ApiServerStubs.TEST_SLUG;
import static org.mnm.config.Client.Status.REPAIRING;
import static org.mnm.config.Client.Status.UPDATED;

class ClientButtonsHandlerTest {

    private JButton install;
    private JButton repair;
    private JButton play;
    private JButton login;
    private JButton logout;

    @Test
    void shouldEnableOnlyLoginWhenNoTokenAndNoClient() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, false, null);
        var handler = initComponents(clientStatus);

        handler.refresh();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(play.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isTrue();
        assertThat(logout.isEnabled()).isFalse();
    }

    @Test
    void shouldRefreshWhenTokenExistsAndClientNotCompletedButNotUpToDate() {
        Client client = new Client(TEST_SLUG, "1.2.3", REPAIRING, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, true, null);
        var handler = initComponents(clientStatus);

        handler.refresh();

        assertThat(install.isEnabled()).isTrue();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(play.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isTrue();
    }

    @Test
    void shouldRefreshWhenTokenExistsAndClientNotCompletedAndNotUpToDate() {
        Client client = new Client(TEST_SLUG, "1.2.3", REPAIRING, Path.of("."));

        ClientStatus clientStatus = new ClientStatus(client, true, false, null);
        var handler = initComponents(clientStatus);

        handler.refresh();

        // This happens only if Install/Repair is aborted without completing
        assertThat(install.isEnabled()).isTrue();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(play.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isTrue();
    }

    @Test
    void shouldRefreshWhenClientCompletedButNotUpToDate() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, false, true, null);
        var handler = initComponents(clientStatus);

        handler.refresh();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(play.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isTrue();
    }

    @Test
    void shouldRefreshWhenClientCompletedAnUptoDate() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, true, null);
        var handler = initComponents(clientStatus);

        handler.refresh();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(play.isEnabled()).isTrue();
        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isTrue();
    }

//    @Test
//    void installationStartShouldDisableRelevantButtons() {
//        handler.installationStart();
//
//        assertThat(install.isEnabled()).isFalse();
//        assertThat(repair.isEnabled()).isFalse();
//        assertThat(logout.isEnabled()).isFalse();
//        assertThat(play.isEnabled()).isFalse();
//    }

    @Test
    void repairStartShouldDisableRelevantButtons() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, true, null);
        var handler = initComponents(clientStatus);

        handler.repairStart();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isFalse();
        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void loginStartShouldDisableLoginButton() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, true, null);
        var handler = initComponents(clientStatus);

        handler.loginStart();

        assertThat(login.isEnabled()).isFalse();
    }

    @Test
    void loginDoneShouldSetTokenAndRefreshButtons() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, true, null);
        var handler = initComponents(clientStatus);

        handler.loginDone(clientStatus);

        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isTrue();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(play.isEnabled()).isFalse();
        assertThat(install.isEnabled()).isFalse();
    }

    @Test
    void logoutDoneShouldClearTokenAndRefreshButtons() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, true, null);
        var handler = initComponents(clientStatus);

        handler.loginDone(clientStatus);
        handler.logoutDone();

        assertThat(login.isEnabled()).isTrue();
        assertThat(logout.isEnabled()).isFalse();
        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(play.isEnabled()).isFalse();
    }

//    @Test
//    void installationDoneShouldRefreshState() {
//        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
//        ClientStatus clientStatus = new ClientStatus(client, true, true, null);
//
//        handler.setClient(client, true);
//        handler.setHasToken(true);
//
//        handler.installationStart();
//
//        handler.installationDone(clientStatus);
//
//        assertThat(install.isEnabled()).isFalse();
//        assertThat(repair.isEnabled()).isTrue();
//        assertThat(play.isEnabled()).isTrue();
//        assertThat(logout.isEnabled()).isTrue();
//    }

    @Test
    void repairDoneShouldRefreshState() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, true, null);
        var handler = initComponents(clientStatus);

        handler.repairStart();
        handler.repairDone(clientStatus);

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(play.isEnabled()).isTrue();
        assertThat(logout.isEnabled()).isTrue();
    }

    private ClientButtonsPanel initComponents(ClientStatus clientStatus) {
        var handler = new ClientButtonsPanel();
        install = getButton(handler, "install");
        repair = getButton(handler, "repair");
        play = getButton(handler, "play");
        login = getButton(handler, "login");
        logout = getButton(handler, "logout");
        return handler;
    }

    private static JButton getButton(ClientButtonsPanel handler, String name) {
        try {
            Field field = handler.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (JButton) field.get(handler);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
