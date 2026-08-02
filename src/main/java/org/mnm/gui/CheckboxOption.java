package org.mnm.gui;

import javax.swing.JCheckBox;
import java.awt.Dimension;

import org.mnm.config.SettingsStore;

class CheckboxOption extends JCheckBox {

    CheckboxOption(String text, SettingsStore settingsStore, String settingKey, boolean defaultValue) {
        super(text);
        this.setSelected(settingsStore.getBoolean(settingKey, defaultValue));
        this.addActionListener(_ -> settingsStore.putBoolean(settingKey, this.isSelected()));
    }

    // UI workaround
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

}
