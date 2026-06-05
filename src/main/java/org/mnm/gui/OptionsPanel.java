package org.mnm.gui;

import javax.swing.*;
import java.awt.*;

import static org.mnm.GeneralOptions.toggleDebug;

class OptionsPanel extends JPanel {

    private final JCheckBox inMemoryHashingOption = new JCheckBox("In-memory hashing");

    OptionsPanel() {
        super();
    }

    JPanel create() {
        final JCheckBox debugOption = new JCheckBox("Enable debug");
        debugOption.setActionCommand("debug");
        debugOption.addActionListener(_ -> toggleDebug(debugOption.isSelected()));

        inMemoryHashingOption.setActionCommand("in-memory-hashing");
        inMemoryHashingOption.setSelected(true);

        this.add(debugOption);
        this.add(Box.createVerticalStrut(8));
        this.add(inMemoryHashingOption);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        return this;
    }

    boolean useInMemoryHashing() {
        return inMemoryHashingOption.isSelected();
    }

}
