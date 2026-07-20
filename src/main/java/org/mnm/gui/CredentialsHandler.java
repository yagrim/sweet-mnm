package org.mnm.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.config.SettingsStore;

public class CredentialsHandler {

    private static final Logger logger = LoggerFactory.getLogger(CredentialsHandler.class);

    private static final String STORE_CREDENTIALS_KEY = "user.store-credentials";
    private static final String EMAIL_KEY = "user.email";
    private static final String PASSWORD_KEY = "user.password";

    private final SettingsStore settingsStore;

    public CredentialsHandler(SettingsStore settingsStore) {
        this.settingsStore = settingsStore;
    }

    public void clearCredentials() {
        settingsStore.delete(EMAIL_KEY);
        settingsStore.delete(PASSWORD_KEY);
        settingsStore.delete(STORE_CREDENTIALS_KEY);
        logger.debug("Deleted stored credentials from settings");
    }

    public void saveEmail(String username) {
        settingsStore.put(EMAIL_KEY, username);
        logger.debug("Stored username in settings");
    }

    public void savePassword(String password) {
        settingsStore.put(PASSWORD_KEY, password);
        logger.debug("Stored password in settings");
    }

    public void saveStoreCredentials(boolean value) {
        settingsStore.putBoolean(STORE_CREDENTIALS_KEY, value);
    }

    public String getEmail() {
        return settingsStore.get(EMAIL_KEY);
    }

    public String getPassword() {
        return settingsStore.get(PASSWORD_KEY);
    }

    public boolean getStoreCredentials() {
        return settingsStore.getBoolean(STORE_CREDENTIALS_KEY, false);
    }
}
