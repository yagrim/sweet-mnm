package org.mnm.gui;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.gui.CredentialsHandler.EMAIL_KEY;
import static org.mnm.gui.CredentialsHandler.PASSWORD_KEY;
import static org.mnm.gui.InMemorySettingsStore.STORE_CREDENTIALS_KEY;

class CredentialsHandlerTest {

    @Test
    void defaults() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of());

        var credentialsHandler = new CredentialsHandler(settingsStore);

        assertThat(credentialsHandler.getEmail()).isNull();
        assertThat(credentialsHandler.getPassword()).isNull();
        assertThat(credentialsHandler.isStoreCredentials()).isFalse();
        assertThat(credentialsHandler.isHideCredentials()).isFalse();
    }

    @Nested
    class ClearCredentials {

        @Test
        void deletesAllCredentialKeys() {
            SettingsStore settingsStore = new InMemorySettingsStore(Map.of(
                EMAIL_KEY, "something",
                PASSWORD_KEY, "passw0rd",
                STORE_CREDENTIALS_KEY, "true"
            ));

            var credentialsHandler = new CredentialsHandler(settingsStore);

            assertThat(credentialsHandler.getEmail()).isNotBlank();
            assertThat(credentialsHandler.getPassword()).isNotBlank();
            assertThat(credentialsHandler.isStoreCredentials()).isTrue();

            credentialsHandler.clearCredentials();

            assertThat(credentialsHandler.getEmail()).isNull();
            assertThat(credentialsHandler.getPassword()).isNull();
            assertThat(credentialsHandler.isStoreCredentials()).isFalse();
        }
    }

    @Test
    void storesEmail() {
        SettingsStore settingsStore = new InMemorySettingsStore(new HashMap<>());
        var credentialsHandler = new CredentialsHandler(settingsStore);

        credentialsHandler.saveEmail("user@example.com");

        assertThat(credentialsHandler.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void storesPassword() {
        SettingsStore settingsStore = new InMemorySettingsStore(new HashMap<>());
        var credentialsHandler = new CredentialsHandler(settingsStore);

        credentialsHandler.savePassword("s3cr3t");

        assertThat(credentialsHandler.getPassword()).isEqualTo("s3cr3t");
    }

    @Nested
    class SaveStoreCredentials {

        @Test
        void storesTrue() {
            SettingsStore settingsStore = new InMemorySettingsStore(new HashMap<>());
            var credentialsHandler = new CredentialsHandler(settingsStore);

            credentialsHandler.saveStoreCredentials(true);

            assertThat(credentialsHandler.isStoreCredentials()).isTrue();
        }

        @Test
        void storesFalse() {
            SettingsStore settingsStore = new InMemorySettingsStore(new HashMap<>());
            var credentialsHandler = new CredentialsHandler(settingsStore);

            credentialsHandler.saveStoreCredentials(false);

            assertThat(credentialsHandler.isStoreCredentials()).isFalse();
        }
    }

}
