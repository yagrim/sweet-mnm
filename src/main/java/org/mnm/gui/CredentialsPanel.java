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

import static org.mnm.tools.StringUtils.isEmpty;

class CredentialsPanel {

    private final CredentialsHandler credentialsHandler;

    private final JPanel panel;
    private final JTextField username;
    private final JPasswordField password;
    private final JCheckBox storeCredentials;

    // TODO Simplify this Grid
    CredentialsPanel(CredentialsHandler credentialsHandler) {
        this.credentialsHandler = credentialsHandler;

        final JTextField emailField = new JTextField(20);
        final JPasswordField passwordField = new JPasswordField(20);
        final JCheckBox storeCredentialsOption = new JCheckBox("Remember login information");
        storeCredentialsOption.setToolTipText("WARNING: Password will be saved locally, use this at your own risk");

        loadSettings(emailField, passwordField, storeCredentialsOption);

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
    }

    private void loadSettings(JTextField emailField, JPasswordField passwordField, JCheckBox storeCredentialsOption) {

        String storedEmail = credentialsHandler.getEmail();
        if (storedEmail != null) {
            emailField.setText(storedEmail);
        }
        String storedPassword = credentialsHandler.getPassword();
        if (storedPassword != null) {
            passwordField.setText(storedPassword);
        }
        storeCredentialsOption.setSelected(credentialsHandler.getStoreCredentials());
    }

    public String getUsername() {
        return username.getText().trim();
    }

    public String getPassword() {
        return new String(password.getPassword());
    }

    void storeCredentials() {
        if (!storeCredentials.isSelected()) {
            credentialsHandler.clearCredentials();
            return;
        }
        boolean credentialsStored = false;
        if (!isEmpty(getUsername())) {
            credentialsHandler.saveEmail(getUsername());
            credentialsStored = true;
        }
        if (!isEmpty(getPassword())) {
            credentialsHandler.savePassword(getPassword());
            credentialsStored = true;
        }
        if (credentialsStored) {
            credentialsHandler.saveStoreCredentials(true);
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
