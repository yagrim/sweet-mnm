package org.mnm.gui;

import javax.swing.JCheckBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.gui.CredentialsPanel.EMAIL_KEY;
import static org.mnm.gui.CredentialsPanel.PASSWORD_KEY;
import static org.mnm.gui.CredentialsPanel.STORE_CREDENTIALS_KEY;
import static org.mnm.gui.ReflectionTestTools.get;

class CredentialsPanelTest {

    @Test
    void shouldRestoreStoredCredentials() {
        CredentialsPanel credentialsPanel = new CredentialsPanel(new InMemorySettingsStore(Map.of(
            EMAIL_KEY, "user@example.com",
            PASSWORD_KEY, "secret")));

        assertThat(credentialsPanel.getUsername()).isEqualTo("user@example.com");
        assertThat(credentialsPanel.getPassword()).isEqualTo("secret");
    }

    @Test
    void shouldRestoreStoreCredentialsOption() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            STORE_CREDENTIALS_KEY, "true"));
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);

        JCheckBox checkbox = (JCheckBox) get(credentialsPanel, "storeCredentials");
        assertThat(checkbox.isSelected()).isTrue();

        checkbox.doClick();

        assertThat(checkbox.isSelected()).isFalse();
        assertThat(settings.get(STORE_CREDENTIALS_KEY)).isEqualTo("true");
    }

    @Test
    void shouldSetRestoreStoreFalsAsDefault() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);

        JCheckBox checkbox = (JCheckBox) get(credentialsPanel, "storeCredentials");
        assertThat(checkbox.isSelected()).isFalse();

        checkbox.doClick();

        assertThat(settings.get(STORE_CREDENTIALS_KEY)).isNull();
    }

    @Test
    void shouldNotStoreCredentialsOptionOnCheckboxClick() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            STORE_CREDENTIALS_KEY, "true"));
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);

        JCheckBox checkbox = (JCheckBox) get(credentialsPanel, "storeCredentials");
        assertThat(checkbox.isSelected()).isTrue();

        checkbox.doClick();

        assertThat(checkbox.isSelected()).isFalse();
        assertThat(settings.get(STORE_CREDENTIALS_KEY)).isEqualTo("true");
    }

    @Test
    void shouldLeaveCredentialsEmptyWhenNotStored() {
        CredentialsPanel credentialsPanel = new CredentialsPanel(new InMemorySettingsStore(Map.of()));

        assertThat(credentialsPanel.getUsername()).isEmpty();
        assertThat(credentialsPanel.getPassword()).isEmpty();
    }

    @Test
    void shouldStoreNonEmptyCredentialsWhenOptionIsSelected() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);
        ((JCheckBox) get(credentialsPanel, "storeCredentials")).doClick();
        ((JTextField) get(credentialsPanel, "username")).setText("user@example.com");
        ((JPasswordField) get(credentialsPanel, "password")).setText("secret");

        credentialsPanel.storeCredentials();

        assertThat(settings.get(EMAIL_KEY)).isEqualTo("user@example.com");
        assertThat(settings.get(PASSWORD_KEY)).isEqualTo("secret");
        assertThat(settings.get(STORE_CREDENTIALS_KEY)).isEqualTo("true");
    }

    @Test
    void shouldDeleteStoredCredentialsWhenOptionIsNotSelected() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            EMAIL_KEY, "user@example.com",
            PASSWORD_KEY, "secret"));
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);

        credentialsPanel.storeCredentials();

        assertThat(settings.get(EMAIL_KEY)).isNull();
        assertThat(settings.get(PASSWORD_KEY)).isNull();
        assertThat(settings.get(STORE_CREDENTIALS_KEY)).isNull();
    }

    @Test
    void shouldNotStoreEmptyUsername() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            EMAIL_KEY, "existing@example.com",
            PASSWORD_KEY, "existing-password"));
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);
        ((JCheckBox) get(credentialsPanel, "storeCredentials")).doClick();
        ((JPasswordField) get(credentialsPanel, "password")).setText("new-password");

        credentialsPanel.storeCredentials();

        assertThat(settings.get(EMAIL_KEY)).isEqualTo("existing@example.com");
        assertThat(settings.get(PASSWORD_KEY)).isEqualTo("new-password");
        assertThat(settings.get(STORE_CREDENTIALS_KEY)).isEqualTo("true");
    }

    @Test
    void shouldNotStoreEmptyPassword() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            EMAIL_KEY, "existing@example.com",
            PASSWORD_KEY, "existing-password"));
        CredentialsPanel credentialsPanel = new CredentialsPanel(settings);
        ((JCheckBox) get(credentialsPanel, "storeCredentials")).doClick();
        ((JTextField) get(credentialsPanel, "username")).setText("new@example.com");

        credentialsPanel.storeCredentials();

        assertThat(settings.get(EMAIL_KEY)).isEqualTo("new@example.com");
        assertThat(settings.get(PASSWORD_KEY)).isEqualTo("existing-password");
        assertThat(settings.get(STORE_CREDENTIALS_KEY)).isEqualTo("true");
    }

}
