package org.mnm.gui;

import javax.swing.*;
import java.awt.*;

import static org.mnm.GeneralOptions.toggleDebug;

class OptionsPanel extends JPanel {

    OptionsPanel() {
        super(new FlowLayout(FlowLayout.LEFT, 8, 8));
    }

    JPanel create() {
        final JCheckBox debugOption = new JCheckBox("Enable debug");
        debugOption.setActionCommand("debug");
        debugOption.addActionListener(_ -> toggleDebug(debugOption.isSelected()));


        this.add(debugOption);
        return this;
    }

}
