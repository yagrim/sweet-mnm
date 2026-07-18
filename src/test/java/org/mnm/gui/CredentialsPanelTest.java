package org.mnm.gui;

import javax.swing.JCheckBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.gui.ReflectionTestTools.get;

class CredentialsPanelTest {

    @Test
    void shouldRestoreStoredCredentials() {
        CredentialsPanel credentialsPanel = new CredentialsPanel(new InMemorySettingsStore(Map.of(
            CredentialsPanel.EMAIL_KEY, "user@example.com",
            CredentialsPanel.PASSWORD_KEY, "secret")));

        assertThat(credentialsPanel.getUsername()).isEqualTo("user@example.com");
        assertThat(credentialsPanel.getPassword()).isEqualTo("secret");
    }

    @Test
    void shouldLeaveCredentialsEmptyWhenNotStored() {
        CredentialsPanel credentialsPanel = new CredentialsPanel(new InMemorySettingsStore(Map.of()));

        assertThat(credentialsPanel.getUsername()).isEmpty();
        assertThat(credentialsPanel.getPassword()).isEmpty();
    }

    @Test
    void shouldRestoreAndPersistStoreCredentialsOption() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            CredentialsPanel.STORE_CREDENTIALS_KEY, "true"));
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);

        JCheckBox checkbox = (JCheckBox) get(credentialsPanel, "storeCredentials");
        assertThat(checkbox.isSelected()).isTrue();

        checkbox.doClick();

        assertThat(settings.get(CredentialsPanel.STORE_CREDENTIALS_KEY)).isEqualTo("false");
    }

    @Test
    void shouldStoreNonEmptyCredentialsWhenOptionIsSelected() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);
        ((JCheckBox) get(credentialsPanel, "storeCredentials")).doClick();
        ((JTextField) get(credentialsPanel, "username")).setText("user@example.com");
        ((JPasswordField) get(credentialsPanel, "password")).setText("secret");

        credentialsPanel.storeCredentials();

        assertThat(settings.get(CredentialsPanel.EMAIL_KEY)).isEqualTo("user@example.com");
        assertThat(settings.get(CredentialsPanel.PASSWORD_KEY)).isEqualTo("secret");
    }

    @Test
    void shouldDeleteStoredCredentialsWhenOptionIsNotSelected() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            CredentialsPanel.EMAIL_KEY, "user@example.com",
            CredentialsPanel.PASSWORD_KEY, "secret"));
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);

        credentialsPanel.storeCredentials();

        assertThat(settings.get(CredentialsPanel.EMAIL_KEY)).isNull();
        assertThat(settings.get(CredentialsPanel.PASSWORD_KEY)).isNull();
    }

    @Test
    void shouldNotStoreEmptyUsername() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            CredentialsPanel.EMAIL_KEY, "existing@example.com",
            CredentialsPanel.PASSWORD_KEY, "existing-password"));
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);
        ((JCheckBox) get(credentialsPanel, "storeCredentials")).doClick();
        ((JPasswordField) get(credentialsPanel, "password")).setText("new-password");

        credentialsPanel.storeCredentials();

        assertThat(settings.get(CredentialsPanel.EMAIL_KEY)).isEqualTo("existing@example.com");
        assertThat(settings.get(CredentialsPanel.PASSWORD_KEY)).isEqualTo("new-password");
    }

    @Test
    void shouldNotStoreEmptyPassword() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            CredentialsPanel.EMAIL_KEY, "existing@example.com",
            CredentialsPanel.PASSWORD_KEY, "existing-password"));
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);
        ((JCheckBox) get(credentialsPanel, "storeCredentials")).doClick();
        ((JTextField) get(credentialsPanel, "username")).setText("new@example.com");

        credentialsPanel.storeCredentials();

        assertThat(settings.get(CredentialsPanel.EMAIL_KEY)).isEqualTo("new@example.com");
        assertThat(settings.get(CredentialsPanel.PASSWORD_KEY)).isEqualTo("existing-password");
    }

}
