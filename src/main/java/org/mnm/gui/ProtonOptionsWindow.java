package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import org.mnm.config.SettingsStore;

import static org.mnm.config.SettingsStore.PROTON_ENABLE_WAYLAND;
import static org.mnm.config.SettingsStore.PROTON_USE_WINED3D;
import static org.mnm.gui.Style.SCALE;

class ProtonOptionsWindow extends JDialog {

    private final CheckboxOption useWined3D;
    private final CheckboxOption enableWayland;

    ProtonOptionsWindow(JFrame owner, SettingsStore settingsStore) {
        super(owner, "Proton options", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBorder(BorderFactory.createEmptyBorder(SCALE, SCALE, SCALE, SCALE));

        useWined3D = new CheckboxOption("PROTON_USE_WINED3D", settingsStore, PROTON_USE_WINED3D, false);
        enableWayland = new CheckboxOption("PROTON_ENABLE_WAYLAND", settingsStore, PROTON_ENABLE_WAYLAND, false);
        addOption(options, useWined3D);
        addOption(options, enableWayland);
        options.add(Box.createVerticalStrut(SCALE));

        add(options, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(_ -> close());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        footer.add(close);

        add(footer, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private static void addOption(JPanel options, CheckboxOption option) {
        options.add(option);
    }

    public void close() {
        SwingUtilities.invokeLater(this::dispose);
    }

    public boolean isUseWined3dEnabled() {
        return useWined3D.isSelected();
    }

    public boolean isEnableWayland() {
        return enableWayland.isSelected();
    }
}
