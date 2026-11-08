package org.mnm.gui;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComboBoxOptionTest {

    private static final String LABEL = "Pick a value";
    private static final List<Integer> ITEMS = List.of(1, 2, 3);

    @Test
    void shouldShowLabelAndItems() {
        ComboBoxOption<Integer> option = new ComboBoxOption<>(LABEL, ITEMS, 2, _ -> {});

        assertThat(option.getLabelText()).isEqualTo(LABEL);
        assertThat(((JLabel) option.getComponent(0)).getText()).isEqualTo(LABEL);
        assertThat(option.getComponent(1)).isSameAs(option.getComboBox());
        JComboBox<Integer> comboBox = option.getComboBox();
        assertThat(comboBox.getItemCount()).isEqualTo(3);
        assertThat(comboBox.getSelectedItem()).isEqualTo(2);
    }

    @Test
    void shouldNotCallListenerOnInit() {
        List<Integer> changes = new ArrayList<>();
        new ComboBoxOption<>(LABEL, ITEMS, 3, changes::add);

        assertThat(changes).isEmpty();
    }

    @Test
    void shouldCallListenerOnlyWhenValueChanges() {
        List<Integer> changes = new ArrayList<>();
        ComboBoxOption<Integer> option = new ComboBoxOption<>(LABEL, ITEMS, 1, changes::add);
        JComboBox<Integer> comboBox = option.getComboBox();

        comboBox.setSelectedItem(1);
        comboBox.setSelectedItem(3);
        comboBox.setSelectedItem(3);
        comboBox.setSelectedItem(2);

        assertThat(changes).containsExactly(3, 2);
    }

    @Test
    void shouldEnableAndDisableComboBox() {
        ComboBoxOption<Integer> option = new ComboBoxOption<>(LABEL, ITEMS, 1, _ -> {});

        option.setEnabled(false);
        assertThat(option.isEnabled()).isFalse();
        assertThat(option.getComboBox().isEnabled()).isFalse();

        option.setEnabled(true);
        assertThat(option.isEnabled()).isTrue();
        assertThat(option.getComboBox().isEnabled()).isTrue();
    }

}
