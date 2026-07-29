package org.mnm.gui;

import javax.swing.JButton;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.mnm.config.Client;
import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.ApiServerStubs.TEST_SLUG;
import static org.mnm.config.Client.Status.INSTALLING;
import static org.mnm.config.Client.Status.NEEDS_UPDATE;
import static org.mnm.config.Client.Status.REPAIRING;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.ReflectionTestTools.getButton;

class ClientButtonsHandlerTest {

    private JButton install;
    private JButton repair;
    private JButton login;
    private JButton logout;

    @Test
    void shouldEnableOnlyLoginWhenThereIsNoClient() {
        ClientStatus clientStatus = new ClientStatus(null, false, null);
        var handler = initComponents();

        handler.refresh(clientStatus);

        installEnabled();
    }

    @Test
    void shouldEnableOnlyLoginWhenTokenIsNotValid() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, false, null);
        var handler = initComponents();

        handler.refresh(clientStatus);

        installEnabled();
    }

    @Test
    void shouldRefreshWhenTokenExistsAndClientInstallDidNotCompleted() {
        shouldRefreshWhenTokenExistsAndClientRepairDidNotCompleted(INSTALLING);
    }

    @Test
    void shouldRefreshWhenTokenExistsAndClientRepairDidNotCompleted() {
        shouldRefreshWhenTokenExistsAndClientRepairDidNotCompleted(REPAIRING);
    }

    private void shouldRefreshWhenTokenExistsAndClientRepairDidNotCompleted(Client.Status foundStatus) {
        Client client = new Client(TEST_SLUG, "1.2.3", foundStatus, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, null);
        var handler = initComponents();

        handler.refresh(clientStatus);

        assertThat(install.isEnabled()).isTrue();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isFalse();
        assertLoginButton("Login", "Obtain a new token");
        assertThat(logout.isEnabled()).isTrue();
    }

    @Test
    void shouldRefreshWhenClientCompletedAndIsUpToDate() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, null);
        var handler = initComponents();

        handler.refresh(clientStatus);

        refreshEnabled();
    }

    @Test
    void shouldRefreshWhenClientCompletedAndNeedsUpdate() {
        Client client = new Client(TEST_SLUG, "1.2.3", NEEDS_UPDATE, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, null);
        var handler = initComponents();

        handler.refresh(clientStatus);

        refreshEnabled();
    }

    @Test
    void shouldShowRefreshWhenTokenIsValidAndCredentialsAreStored() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, null);
        var handler = initComponents(true);

        handler.refresh(clientStatus);

        assertLoginButton("Refresh", "Regenerate token with stored credentials");
    }

    @Test
    void repairStartShouldDisableButtons() {
        var handler = initComponents();

        handler.repairStart();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isFalse();
        assertThat(logout.isEnabled()).isFalse();
    }

    @Test
    void loginStartShouldDisableLoginButton() {
        var handler = initComponents();

        handler.loginStart();

        assertThat(login.isEnabled()).isFalse();
    }

    @Test
    void loginDoneShouldSetTokenAndRefreshButtonsWhenUpToDate() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, null);
        var handler = initComponents();

        handler.loginDone(clientStatus);

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(login.isEnabled()).isFalse();
        assertLoginButton("Login", "Obtain a new token");
        assertThat(logout.isEnabled()).isTrue();
    }

    @Test
    void loginDoneShouldSetTokenAndRefreshButtonsWhenNotUpToDate() {
        Client client = new Client(TEST_SLUG, "1.2.3", NEEDS_UPDATE, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, null);
        var handler = initComponents();

        handler.loginDone(clientStatus);

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(login.isEnabled()).isFalse();
        assertLoginButton("Login", "Obtain a new token");
        assertThat(logout.isEnabled()).isTrue();
    }

    @Test
    void logoutDoneShouldClearTokenAndRefreshButtons() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, null);
        var handler = initComponents();

        handler.loginDone(clientStatus);
        handler.logoutDone();

        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isTrue();
        assertLoginButton("Login", "Obtain a new token");
        assertThat(logout.isEnabled()).isFalse();
    }

    @Test
    void repairDoneShouldRefreshState() {
        Client client = new Client(TEST_SLUG, "1.2.3", UPDATED, Path.of("."));
        ClientStatus clientStatus = new ClientStatus(client, true, null);
        var handler = initComponents();

        handler.repairStart();
        handler.repairDone(clientStatus);

        refreshEnabled();
    }

    private ClientButtonsPanel initComponents() {
        return initComponents(false);
    }

    private ClientButtonsPanel initComponents(boolean storeCredentials) {
        var handler = new ClientButtonsPanel(null, null, null, null, null, new CredentialsHandler(new SettingsStore() {
            @Override
            public String get(String key) {
                return storeCredentials && key.equals("user.store-credentials") ? "true" : null;
            }

            @Override
            public void put(String key, String value) {
            }

            @Override
            public void delete(String key) {
            }
        }));
        install = getButton(handler, "install");
        repair = getButton(handler, "repair");
        login = getButton(handler, "login");
        logout = getButton(handler, "logout");
        return handler;
    }

    private void installEnabled() {
        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isFalse();
        assertThat(login.isEnabled()).isTrue();
        assertLoginButton("Login", "Obtain a new token");
        assertThat(logout.isEnabled()).isFalse();
    }

    private void refreshEnabled() {
        assertThat(install.isEnabled()).isFalse();
        assertThat(repair.isEnabled()).isTrue();
        assertThat(login.isEnabled()).isFalse();
        assertLoginButton("Login", "Obtain a new token");
        assertThat(logout.isEnabled()).isTrue();
    }

    private void assertLoginButton(String text, String toolTipText) {
        assertThat(login.getText()).isEqualTo(text);
        assertThat(login.getToolTipText()).isEqualTo(toolTipText);
    }
}
