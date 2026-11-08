package org.mnm.gui;

import javax.swing.JCheckBox;
import java.awt.Dimension;
import java.util.function.Consumer;

import org.mnm.config.SettingsStore;

class CheckboxOption extends JCheckBox {

    CheckboxOption(String text, SettingsStore settingsStore, String settingKey, boolean defaultValue) {
        this(text, settingsStore.getBoolean(settingKey, defaultValue), selected -> settingsStore.putBoolean(settingKey, selected));
    }

    CheckboxOption(String text, boolean selected, Consumer<Boolean> onChange) {
        super(text);
        this.setSelected(selected);
        this.addActionListener(_ -> onChange.accept(this.isSelected()));
    }

    // UI workaround
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

}
