package org.mnm.gui;

import javax.swing.*;

import org.mnm.client.RunnerOptions;

import static org.mnm.GeneralOptions.toggleDebug;

// NOTE: so far options can be grouped as repair or run.
class OptionsPanel extends JPanel {

    private final JCheckBox debugOption = new JCheckBox("Enable debug");
    private final JCheckBox inMemoryHashingOption = new JCheckBox("In-memory hashing");
    private final JCheckBox mangoHudOption = new JCheckBox("Enable MangoHud");

    OptionsPanel() {
        super();
    }

    JPanel create() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));

        debugOption.setActionCommand("debug");
        debugOption.addActionListener(_ -> toggleDebug(debugOption.isSelected()));

        inMemoryHashingOption.setActionCommand("in-memory-hashing");
        inMemoryHashingOption.setSelected(true);

        mangoHudOption.setActionCommand("mangohud");

        this.add(debugOption);
        this.add(Box.createVerticalStrut(8));
        this.add(inMemoryHashingOption);
        this.add(Box.createVerticalStrut(8));
        this.add(mangoHudOption);

        return this;
    }

    boolean useInMemoryHashing() {
        return inMemoryHashingOption.isSelected();
    }

    RunnerOptions getRunnerOptions() {
        return new RunnerOptions(null, null, false, mangoHudOption.isSelected());
    }

}
