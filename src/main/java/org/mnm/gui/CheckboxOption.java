package org.mnm.gui;

import javax.swing.JCheckBox;
import java.awt.Dimension;

import org.mnm.config.SettingsStore;

class CheckboxOption extends JCheckBox {

    CheckboxOption(String text, SettingsStore settingsStore, String settingKey, boolean defaultValue) {
        super(text);
        addActionListener(_ -> settingsStore.putBoolean(settingKey, this.isSelected()));

        this.setSelected(settingsStore.getBoolean(settingKey, defaultValue));
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension d = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, d.height);
    }

}
