package org.mnm.gui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import org.mnm.config.SettingsStore;

class TextOption extends JPanel {

    private final JTextField textField;

    TextOption(String label, SettingsStore settingsStore, String settingKey) {
        this(label, settingsStore, settingKey, null);
    }

    TextOption(String label, SettingsStore settingsStore, String settingKey, String defaultValue) {
        super(new GridBagLayout());
        // Commenting this centers the mangoHud option
        this.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 5);

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        this.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        textField = textField(settingsStore, settingKey, defaultValue);

        this.add(textField, gbc);
    }

    private JTextField textField(SettingsStore settingsStore, String settingKey, String defaultValue) {
        final String initialValue = settingsStore.get(settingKey, defaultValue);
        final JTextField textField = new JTextField(initialValue);
        textField.getDocument()
            .addDocumentListener(new SaveListener(settingsStore, settingKey));
        return textField;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public boolean isEnabled() {
        return textField.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        textField.setEnabled(enabled);
    }

    public String getText() {
        return textField.getText();
    }

    private class SaveListener implements DocumentListener {

        private final Timer saveTimer;

        private SaveListener(SettingsStore settingsStore, String settingKey) {
            // Wait 2 seconds after the last edit before saving
            saveTimer = new Timer(2000, _ -> {
                settingsStore.put(settingKey, textField.getText());
            });
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            restartTimer();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            restartTimer();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            restartTimer();
        }

        private void restartTimer() {
            saveTimer.restart();
        }
    }
}
