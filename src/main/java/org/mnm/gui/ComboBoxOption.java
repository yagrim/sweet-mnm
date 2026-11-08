package org.mnm.gui;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.function.Consumer;

class ComboBoxOption<T> extends JPanel {

    private final JLabel label;
    private final JComboBox<T> comboBox = new JComboBox<>();

    ComboBoxOption(String text, List<T> items, T selectedItem, Consumer<T> onChange) {
        super(new FlowLayout(FlowLayout.LEFT, 5, 0));
        this.setAlignmentX(Component.LEFT_ALIGNMENT);

        label = new JLabel(text);
        items.forEach(comboBox::addItem);
        comboBox.setSelectedItem(selectedItem);
        // Add the listener after the initial selection, to not trigger it on init.
        // It triggers only when the value changes.
        comboBox.addItemListener(evt -> {
            if (evt.getStateChange() == ItemEvent.SELECTED) {
                @SuppressWarnings("unchecked")
                T item = (T) evt.getItem();
                onChange.accept(item);
            }
        });

        this.add(label);
        this.add(comboBox);
    }

    @Override
    public boolean isEnabled() {
        return comboBox.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        comboBox.setEnabled(enabled);
    }

    String getLabelText() {
        return label.getText();
    }

    JComboBox<T> getComboBox() {
        return comboBox;
    }

}
