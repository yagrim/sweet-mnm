package org.mnm.gui;

import javax.swing.*;
import java.awt.*;

class CredentialsPanel {

    private final JPanel panel;
    private final JTextField username;
    private final JPasswordField password;

    public CredentialsPanel() {
        final JTextField emailField = new JTextField(20);
        final JPasswordField passwordField = new JPasswordField(20);

        final JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Email"));
        panel.add(emailField);
        panel.add(new JLabel("Password"));
        panel.add(passwordField);

        this.panel = panel;
        this.username = emailField;
        this.password = passwordField;
    }

    public String getUsername() {
        return username.getText();
    }

    public String getPassword() {
        return new String(password.getPassword());
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
