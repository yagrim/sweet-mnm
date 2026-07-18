package org.mnm.gui;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import org.mnm.config.SettingsStore;

import static org.mnm.tools.StringUtils.isEmpty;

class CredentialsPanel {

    static final String STORE_CREDENTIALS_KEY = "user.store-credentials";
    static final String EMAIL_KEY = "user.email";
    static final String PASSWORD_KEY = "user.password";

    private final SettingsStore settingsStore;

    private final JPanel panel;
    private final JTextField username;
    private final JPasswordField password;
    private final JCheckBox storeCredentials;

    CredentialsPanel(SettingsStore settingsStore) {
        this.settingsStore = settingsStore;

        final JTextField emailField = new JTextField(20);
        final JPasswordField passwordField = new JPasswordField(20);
        final JCheckBox storeCredentialsOption = new JCheckBox("Save login details");

        final JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(new JLabel("Email"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(emailField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Password"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(passwordField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 0;
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(storeCredentialsOption, constraints);

        this.panel = panel;
        this.username = emailField;
        this.password = passwordField;
        this.storeCredentials = storeCredentialsOption;

        storeCredentials.setSelected(settingsStore.getBoolean(STORE_CREDENTIALS_KEY, false));
        storeCredentials.addActionListener(_ ->
            settingsStore.putBoolean(STORE_CREDENTIALS_KEY, storeCredentials.isSelected()));
    }

    public String getUsername() {
        return username.getText().trim();
    }

    public String getPassword() {
        return new String(password.getPassword());
    }

    void storeCredentials() {
        if (!storeCredentials.isSelected()) {
            return;
        }
        if (!isEmpty(getUsername())) {
            settingsStore.put(EMAIL_KEY, getUsername());
        }
        if (!isEmpty(getPassword())) {
            settingsStore.put(PASSWORD_KEY, getPassword());
        }
    }

    public int show(Container parent) {
        return JOptionPane.showConfirmDialog(
            parent,
            panel,
            "Account credentials",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
    }
}
